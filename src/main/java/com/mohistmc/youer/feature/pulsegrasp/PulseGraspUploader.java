package com.mohistmc.youer.feature.pulsegrasp;

import com.mohistmc.youer.YouerConfig;
import com.mohistmc.youer.util.I18n;
import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.net.ssl.SSLException;

/**
 * Uploads a PulseGrasp report to the report service and returns a shareable link.
 * Address comes from {@code pulsegrasp.upload.url}; empty means uploading is off.
 * Protocol: POST {base}/api/tools/pulse-grasp, expect 201 and a 10-char base62 id.
 */
public final class PulseGraspUploader {

    private static final String UPLOAD_PATH = "/api/tools/pulse-grasp";
    private static final String VIEW_PATH = "/tools/pulse-grasp/";
    private static final Pattern ID_PATTERN = Pattern.compile("\"id\"\\s*:\\s*\"([A-Za-z0-9]{10})\"");
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofMinutes(5);
    /** Server bodies can be whole HTML pages, so they are trimmed before going into any message. */
    private static final int MAX_DETAIL_LENGTH = 300;
    /** A player-facing reason has to fit on one chat line. */
    private static final int MAX_REASON_LENGTH = 80;

    // HTTP/1.1 pinned: the JDK default sends an h2c upgrade on plain http://, and some
    // proxies answer it by dropping the connection without any response.
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(CONNECT_TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private PulseGraspUploader() {
    }

    /** Configured base URL without trailing slashes, or "" when unset. */
    public static String baseUrl() {
        String raw = YouerConfig.pulsegrasp_uploadBaseUrl;
        if (raw == null) {
            return "";
        }
        raw = raw.trim();
        while (raw.endsWith("/")) {
            raw = raw.substring(0, raw.length() - 1);
        }
        return raw;
    }

    public static boolean isEnabled() {
        return !baseUrl().isEmpty();
    }

    /** Full endpoint, used in failure logs. */
    public static String uploadUrl() {
        String base = baseUrl();
        return base.isEmpty() ? "" : base + UPLOAD_PATH;
    }

    /**
     * Upload the report and return its share link. Any failure is thrown; the caller decides how
     * to keep the report, which is why the payload is taken as text rather than a file.
     */
    public static String upload(String json) throws Exception {
        String base = baseUrl();
        if (base.isEmpty()) {
            throw new IllegalStateException("pulsegrasp.upload.url is not configured");
        }
        HttpRequest request = HttpRequest.newBuilder(URI.create(base + UPLOAD_PATH))
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("User-Agent", "Youer-PulseGrasp")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 201) {
            throw new ResponseException(response.statusCode(), response.body());
        }
        return base + VIEW_PATH + extractId(response.body());
    }

    /**
     * Maps a failure to a short reason for the player. Anything the service sent back is reduced to
     * a single, length-capped line so a stray HTML error page can never end up in chat.
     */
    public static String describeFailure(Throwable t) {
        if (t instanceof ResponseException e) {
            return switch (e.status()) {
                case 0 -> I18n.as("pulsegrasp.upload.reason.malformed");
                case 413 -> I18n.as("pulsegrasp.upload.reason.toolarge");
                case 429 -> I18n.as("pulsegrasp.upload.reason.ratelimit");
                default -> I18n.as("pulsegrasp.upload.reason.http", String.valueOf(e.status()));
            };
        }
        if (t instanceof UnknownHostException) {
            return I18n.as("pulsegrasp.upload.reason.dns");
        }
        if (t instanceof HttpConnectTimeoutException || t instanceof HttpTimeoutException) {
            return I18n.as("pulsegrasp.upload.reason.timeout");
        }
        if (t instanceof ConnectException) {
            return I18n.as("pulsegrasp.upload.reason.connect");
        }
        if (t instanceof SSLException) {
            return I18n.as("pulsegrasp.upload.reason.tls");
        }
        String message = t.getMessage();
        if (message != null && message.contains("header parser received no bytes")) {
            return I18n.as("pulsegrasp.upload.reason.noresponse");
        }
        if (message != null && !message.isBlank()) {
            return flatten(message);
        }
        if (t instanceof IOException) {
            return I18n.as("pulsegrasp.upload.reason.noresponse");
        }
        return t.getClass().getSimpleName();
    }

    /** Collapse to one line, drop any markup from the first tag on, and cap the length. */
    private static String flatten(String message) {
        String flat = message.replaceAll("\\s+", " ").trim();
        int tag = flat.indexOf('<');
        if (tag >= 0) {
            flat = flat.substring(0, tag).trim();
        }
        if (flat.isEmpty()) {
            return I18n.as("pulsegrasp.upload.reason.noresponse");
        }
        return flat.length() > MAX_REASON_LENGTH ? flat.substring(0, MAX_REASON_LENGTH) + "…" : flat;
    }

    /**
     * A non-201 answer, or a 201 whose body carried no usable id ({@code status} 0). The service's
     * own text is flattened into the message so it reaches the console log, never the player.
     */
    public static final class ResponseException extends IOException {

        private final int status;

        ResponseException(int status, String body) {
            super(detail(status, body));
            this.status = status;
        }

        public int status() {
            return status;
        }

        private static String detail(int status, String body) {
            String head = status > 0 ? "HTTP " + status : "unexpected response";
            String flat = body == null ? "" : body.replaceAll("\\s+", " ").trim();
            if (flat.isEmpty()) {
                return head;
            }
            return head + " - " + (flat.length() > MAX_DETAIL_LENGTH ? flat.substring(0, MAX_DETAIL_LENGTH) + "…" : flat);
        }
    }

    private static String extractId(String json) throws ResponseException {
        // an empty body takes the same "unrecognised response" path as a body without an id, so a
        // 201 that carried nothing is reported like any other unusable answer
        if (json == null || json.isEmpty()) {
            throw new ResponseException(0, json);
        }
        Matcher m = ID_PATTERN.matcher(json);
        if (!m.find()) {
            throw new ResponseException(0, json);
        }
        return m.group(1);
    }
}
