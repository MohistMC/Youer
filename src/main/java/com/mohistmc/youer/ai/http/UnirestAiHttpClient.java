package com.mohistmc.youer.ai.http;

import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import kong.unirest.core.Header;
import kong.unirest.core.HttpResponse;
import kong.unirest.core.Unirest;
import kong.unirest.core.UnirestException;

public final class UnirestAiHttpClient implements AiHttpClient {

    @Override
    public AiHttpResponse execute(AiHttpRequest request, Duration timeout) {
        try {
            HttpResponse<String> response = Unirest.post(request.uri().toString())
                    .headers(request.headers())
                    .requestTimeout(Math.toIntExact(timeout.toMillis()))
                    .body(request.body())
                    .asString();
            Map<String, String> headers = new LinkedHashMap<>();
            for (Header header : response.getHeaders().all()) {
                headers.merge(header.getName(), header.getValue(), (first, second) -> first + ", " + second);
            }
            return new AiHttpResponse(response.getStatus(), headers, response.getBody());
        } catch (UnirestException | ArithmeticException exception) {
            throw new AiHttpException(isTimeout(exception), exception);
        }
    }

    private static boolean isTimeout(Throwable failure) {
        Throwable current = failure;
        while (current != null && current.getCause() != current) {
            if (current instanceof HttpTimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
