package com.aicourse.ai;

public interface AiTextClient {

    String getResponse(String prompt);

    Iterable<String> getResponseStream(String prompt);
}

