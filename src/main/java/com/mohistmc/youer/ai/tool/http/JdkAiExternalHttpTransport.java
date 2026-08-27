package com.mohistmc.youer.ai.tool.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CompletionException;

public final class JdkAiExternalHttpTransport implements AiExternalHttpTransport {
    private final HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();

    @Override public CompletionStage<AiExternalHttpResponse> execute(AiExternalHttpRequest request) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(request.uri()).timeout(request.timeout());
        request.headers().forEach(builder::header);
        HttpRequest.BodyPublisher body = request.body() == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(request.body(), StandardCharsets.UTF_8);
        builder.method(request.method(), body);
        return client.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofInputStream())
                .thenApply(response -> {
                    String value = readBounded(response.body(), request.maxResponseLength());
                    LinkedHashMap<String, String> headers = new LinkedHashMap<>();
                    response.headers().map().forEach((key, values) -> headers.put(key, String.join(",", values)));
                    return new AiExternalHttpResponse(response.statusCode(), headers, value);
                });
    }

    private static String readBounded(InputStream stream, int maxChars) {
        if (stream == null) return "";
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            char[] buffer = new char[Math.min(2048, maxChars)];
            StringBuilder result = new StringBuilder(Math.min(maxChars, 4096));
            while (result.length() < maxChars) {
                int read = reader.read(buffer, 0, Math.min(buffer.length, maxChars - result.length()));
                if (read < 0) break;
                result.append(buffer, 0, read);
            }
            return result.toString();
        } catch (IOException exception) {
            throw new CompletionException(exception);
        }
    }
}
