package com.example.expensetracker.core.network.ai.dto;

import com.google.gson.annotations.SerializedName;

import java.util.Collections;
import java.util.List;

public class GeminiRequest {
    @SerializedName("system_instruction")
    public final Content systemInstruction;

    @SerializedName("contents")
    public final List<Content> contents;

    public GeminiRequest(String systemText, String userText) {
        this.systemInstruction = new Content(null, Collections.singletonList(new Part(systemText)));
        this.contents = Collections.singletonList(new Content("user", Collections.singletonList(new Part(userText))));
    }

    public static class Content {
        @SerializedName("role")
        public final String role;

        @SerializedName("parts")
        public final List<Part> parts;

        public Content(String role, List<Part> parts) {
            this.role = role;
            this.parts = parts;
        }
    }

    public static class Part {
        @SerializedName("text")
        public final String text;

        public Part(String text) {
            this.text = text;
        }
    }
}
