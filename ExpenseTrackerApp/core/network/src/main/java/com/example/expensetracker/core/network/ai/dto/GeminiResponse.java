package com.example.expensetracker.core.network.ai.dto;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class GeminiResponse {
    @SerializedName("candidates")
    public List<Candidate> candidates;

    public static class Candidate {
        @SerializedName("content")
        public Content content;
    }

    public static class Content {
        @SerializedName("parts")
        public List<Part> parts;
    }

    public static class Part {
        @SerializedName("text")
        public String text;
    }

    public String firstText() {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        Candidate c = candidates.get(0);
        if (c == null || c.content == null || c.content.parts == null || c.content.parts.isEmpty()) {
            return null;
        }
        Part p = c.content.parts.get(0);
        return p != null ? p.text : null;
    }
}
