package com.uit.petrescueapi.application.port.out;

import java.util.List;
import java.util.Map;

public interface PushNotificationPort {

    void sendPushToTokens(List<String> expoPushTokens, String title, String body, Map<String, Object> data);

}
