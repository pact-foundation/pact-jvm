package com.dius.pact.consumer;

import java.util.Map;

public class ConsumerInteractionJavaDsl {
    static ConsumerInteractionJavaDsl given(String state) {
        return new ConsumerInteractionJavaDsl();
    }

    public ConsumerInteractionJavaDsl uponReceiving(
        String description,
        String path,
        String method,
        Map<String, String> headers,
        String body) {
        return this;
    }

    public ConsumerInteractionJavaDsl willRespondWith(
        int status,
        Map<String, String> headers,
        String body) {
        return this;
    }
}
