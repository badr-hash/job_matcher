package com.JobMatcher.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/assistant")
@CrossOrigin(origins = "*")
public class AssistantController {

    private static final URI OLLAMA_GENERATE_URI = URI.create("http://localhost:11434/api/generate");
    private static final Duration TIMEOUT = Duration.ofSeconds(60);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    @PostMapping(value = "/chat", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> chat(@RequestBody Map<String, Object> body) {
        String message = body.get("message") == null ? "" : String.valueOf(body.get("message")).trim();
        if (message.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "ok", false,
                    "error", "Message vide."
            ));
        }

        String model = body.get("model") == null ? "llama3" : String.valueOf(body.get("model")).trim();
        if (model.isEmpty()) model = "llama3";

        String requestJson = """
                {
                  "model": "%s",
                  "prompt": %s,
                  "stream": false
                }
                """.formatted(escapeJsonString(model), toJsonString(message));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(OLLAMA_GENERATE_URI)
                .timeout(TIMEOUT)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                .build();

        try {
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of(
                        "ok", false,
                        "error", "Ollama a renvoyé une erreur.",
                        "status", resp.statusCode(),
                        "details", truncate(resp.body(), 1200)
                ));
            }

            // Ollama returns JSON like: { "response": "...", "done": true, ... }
            // We keep parsing simple without adding new deps: return raw body + convenience field.
            String raw = resp.body();
            String extracted = extractJsonField(raw, "response");

            return ResponseEntity.ok(Map.of(
                    "ok", true,
                    "reply", extracted == null ? "" : extracted,
                    "raw", raw
            ));
        } catch (java.net.ConnectException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                    "ok", false,
                    "error", "Ollama n'est pas démarré (port 11434). Lance Ollama puis réessaie."
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "ok", false,
                    "error", "Erreur serveur lors de l'appel à Ollama.",
                    "details", truncate(String.valueOf(e.getMessage()), 1200)
            ));
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        if (s.length() <= max) return s;
        return s.substring(0, max) + "…";
    }

    // Minimal JSON helpers (avoid extra dependencies)
    private static String toJsonString(String s) {
        return "\"" + escapeJsonString(s) + "\"";
    }

    private static String escapeJsonString(String s) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\' -> out.append("\\\\");
                case '"' -> out.append("\\\"");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) out.append(String.format("\\u%04x", (int) c));
                    else out.append(c);
                }
            }
        }
        return out.toString();
    }

    /**
     * Best-effort extraction of a top-level string field from JSON.
     * This is intentionally simple to avoid extra dependencies.
     */
    private static String extractJsonField(String json, String fieldName) {
        if (json == null || fieldName == null) return null;
        String needle = "\"" + fieldName + "\":";
        int idx = json.indexOf(needle);
        if (idx < 0) return null;
        int start = idx + needle.length();
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) start++;
        if (start >= json.length() || json.charAt(start) != '"') return null;
        start++; // after opening quote
        StringBuilder out = new StringBuilder();
        boolean escaping = false;
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escaping) {
                // handle basic escapes
                switch (c) {
                    case '"', '\\', '/' -> out.append(c);
                    case 'n' -> out.append('\n');
                    case 'r' -> out.append('\r');
                    case 't' -> out.append('\t');
                    case 'u' -> {
                        if (i + 4 < json.length()) {
                            String hex = json.substring(i + 1, i + 5);
                            try {
                                out.append((char) Integer.parseInt(hex, 16));
                                i += 4;
                            } catch (NumberFormatException ignored) {
                                return out.toString();
                            }
                        }
                    }
                    default -> out.append(c);
                }
                escaping = false;
                continue;
            }
            if (c == '\\') {
                escaping = true;
                continue;
            }
            if (c == '"') {
                return out.toString();
            }
            out.append(c);
        }
        return out.toString();
    }
}

