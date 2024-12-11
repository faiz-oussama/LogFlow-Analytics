package com.example;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class WebScrapingService {
    private static final String API_BASE_URL = "https://api.stackexchange.com/2.1";
    private static final String SITE = "stackoverflow";
    private static final int PAGE_SIZE = 3;
    private static final String API_KEY = "rl_GYbCkhtQ62PF1nNRXjxYPE41A";
    private final OkHttpClient client;
    private final Gson gson;

    public WebScrapingService() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        this.gson = new Gson();
    }

    private URL createQuery(String method, Map<String, String> params) throws MalformedURLException {
        StringBuilder baseUrl = new StringBuilder(API_BASE_URL + "/" + method);
        
        // Add all parameters including site
        params.put("site", SITE);
        
        boolean first = true;
        for (Map.Entry<String, String> param : params.entrySet()) {
            try {
                String encodedValue = URLEncoder.encode(param.getValue(), StandardCharsets.UTF_8.toString());
                baseUrl.append(first ? "?" : "&")
                      .append(param.getKey())
                      .append("=")
                      .append(encodedValue);
                first = false;
            } catch (IOException e) {
                System.err.println("Error encoding parameter: " + e.getMessage());
            }
        }
        return new URL(baseUrl.toString());
    }

    private List<String> getAnswersForQuestion(String questionId) {
        List<String> answers = new ArrayList<>();
        try {
            Map<String, String> params = new HashMap<>();
            params.put("pagesize", String.valueOf(PAGE_SIZE));
            params.put("order", "desc");
            params.put("sort", "votes");
            params.put("filter", "!9Z(-wwYGT"); // Custom filter to include answers
            params.put("key", API_KEY);

            URL url = createQuery("questions/" + questionId + "/answers", params);
            System.out.println("Fetching answers for question " + questionId + " with URL: " + url);

            Request request = new Request.Builder()
                    .url(url)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    System.err.println("Error: " + response.code());
                    return answers;
                }

                String responseBody = response.body().string();
                System.out.println("Response Body: " + responseBody);
                JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
                JsonArray items = jsonResponse.getAsJsonArray("items");

                if (items != null) {
                    for (JsonElement item : items) {
                        JsonObject answer = item.getAsJsonObject();
                        String body = answer.has("body") ? 
                            answer.get("body").getAsString().substring(0, Math.min(200, answer.get("body").getAsString().length())) + "..."
                            : "";

                        answers.add(body);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error fetching answers for question " + questionId + ": " + e.getMessage());
            e.printStackTrace();
        }
        return answers;
    }

    public List<String> searchSolutions(String query) {
        List<String> solutions = new ArrayList<>();
        try {
            Map<String, String> params = new HashMap<>();
            params.put("pagesize", String.valueOf(PAGE_SIZE));
            params.put("order", "desc");
            params.put("sort", "relevance");
            params.put("q", query);
            params.put("filter", "!9Z(-wwYGT"); // Custom filter to include question body and answers
            params.put("tagged", "java");
            params.put("answers", "1");
            params.put("key", API_KEY);

            URL url = createQuery("search/advanced", params);
            System.out.println("Searching Stack Overflow with URL: " + url);

            Request request = new Request.Builder()
                    .url(url)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    System.err.println("Error: " + response.code());
                    return solutions;
                }

                String responseBody = response.body().string();
                System.out.println("Response Body: " + responseBody);
                JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
                JsonArray items = jsonResponse.getAsJsonArray("items");

                if (items != null) {
                    for (JsonElement item : items) {
                        JsonObject question = item.getAsJsonObject();
                        String title = question.get("title").getAsString();
                        String link = question.get("link").getAsString();
                        boolean isAnswered = question.get("is_answered").getAsBoolean();
                        int score = question.get("score").getAsInt();
                        String body = question.has("body") ? 
                            question.get("body").getAsString()
                            : "";

                        // Fetch answers for this question
                        String questionId = question.get("question_id").getAsString();
                        List<String> answers = getAnswersForQuestion(questionId);

                        StringBuilder solution = new StringBuilder();
                        solution.append("Problem Summary:\n")
                               .append(title)
                               .append("\n\nSolutions Found:\n")
                               .append("Question Details:\n")
                               .append(body)
                               .append("\n");

                        // Add answers in separate blocks
                        if (!answers.isEmpty()) {
                            solution.append("\nAnswers:\n");
                            for (int i = 0; i < answers.size(); i++) {
                                solution.append("\nAnswer ").append(i + 1).append(":\n")
                                       .append(answers.get(i))
                                       .append("\n");
                            }
                        }

                        // Tools section
                        solution.append("\nTools or Resources Needed:\n")
                               .append("- Java Development Environment\n")
                               .append("- Required dependencies mentioned in the solution\n");

                        // Additional Notes section
                        solution.append("\nAdditional Notes:\n")
                               .append("Score: ").append(score);
                        
                        if (isAnswered) {
                            solution.append(" (Answered)");
                        }
                        
                        // References section
                        solution.append("\n\nReferences:\n")
                               .append("- Stack Overflow Answer: ").append(link);

                        solutions.add(solution.toString());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error searching for solutions: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("Found " + solutions.size() + " solutions");
        return solutions;
    }
}