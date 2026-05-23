package com.uit.petrescueapi.infrastructure.notifications;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uit.petrescueapi.application.port.out.PushNotificationPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExpoPushNotificationAdapter implements PushNotificationPort {

    private final ObjectMapper objectMapper;

    private static final String EXPO_URL = "https://exp.host/--/api/v2/push/send";

    @Override
    public void sendPushToTokens(List<String> expoPushTokens, String title, String body, Map<String, Object> data) {
        if (expoPushTokens == null || expoPushTokens.isEmpty()) return;

        List<Map<String, Object>> messages = new ArrayList<>();
        for (String token : expoPushTokens) {
            if (token == null || token.isBlank()) continue;
            Map<String, Object> msg = new HashMap<>();
            msg.put("to", token);
            msg.put("title", title);
            msg.put("body", body);
            if (data != null && !data.isEmpty()) msg.put("data", data);
            messages.add(msg);
        }
        if (messages.isEmpty()) return;

        try {
            String payload = objectMapper.writeValueAsString(messages);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(EXPO_URL))
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpClient client = HttpClient.newHttpClient();
            int maxAttempts = 3;
            long backoff = 500L;
            for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                try {
                    HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString());
                    int status = resp.statusCode();
                    String bodyResp = resp.body();
                    if (status >= 200 && status < 300) {
                        log.debug("Expo push sent (attempt {}): {}", attempt, bodyResp);
                        break;
                    } else {
                        log.warn("Expo push non-2xx response (attempt {}): {} - {}", attempt, status, bodyResp);
                    }
                } catch (Exception e) {
                    log.warn("Expo push attempt {} failed", attempt, e);
                }

                if (attempt < maxAttempts) {
                    try { Thread.sleep(backoff); } catch (InterruptedException ignored) {}
                    backoff *= 2;
                }
            }
        } catch (Exception ex) {
            log.warn("Failed to build expo push payload", ex);
        }
    }

}
