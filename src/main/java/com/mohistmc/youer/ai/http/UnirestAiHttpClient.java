package com.mohistmc.youer.ai.http;

import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import kong.unirest.core.Header;
import kong.unirest.core.HttpResponse;
import kong.unirest.core.Unirest;
import kong.unirest.core.UnirestException;

public final class UnirestAiHttpClient implements AiHttpClient {

    @Override
    public CompletionStage<AiHttpResponse> execute(AiHttpRequest request, Duration timeout) {
        try {
            return Unirest.post(request.uri().toString())
                    .headers(request.headers())
                    .requestTimeout(Math.toIntExact(timeout.toMillis()))
                    .body(request.body())
                    .asStringAsync()
                    .handle((response, failure) -> {
                        if (failure != null) {
                            throw new AiHttpException(isTimeout(failure), unwrap(failure));
                        }
                        return response(response);
                    });
        } catch (UnirestException | ArithmeticException exception) {
            return CompletableFuture.failedFuture(new AiHttpException(isTimeout(exception), exception));
        }
    }

    private static AiHttpResponse response(HttpResponse<String> response) {
        Map<String, String> headers = new LinkedHashMap<>();
        for (Header header : response.getHeaders().all()) {
            headers.merge(header.getName(), header.getValue(), (first, second) -> first + ", " + second);
        }
        return new AiHttpResponse(response.getStatus(), headers, response.getBody());
    }

    private static Throwable unwrap(Throwable failure) {
        Throwable current = failure;
        while (current instanceof CompletionException && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
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
