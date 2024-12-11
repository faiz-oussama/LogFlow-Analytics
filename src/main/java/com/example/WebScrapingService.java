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
import java.util.stream.Collectors;

public class WebScrapingService {
    private static final String API_BASE_URL = "https://api.stackexchange.com/2.3";
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

    private String cleanHtmlContent(String html) {
        if (html == null || html.isEmpty()) {
            return "";
        }
        // Remove HTML tags
        String noHtml = html.replaceAll("<[^>]+>", "");
        // Replace HTML entities
        noHtml = noHtml.replaceAll("&quot;", "\"")
                      .replaceAll("&amp;", "&")
                      .replaceAll("&lt;", "<")
                      .replaceAll("&gt;", ">")
                      .replaceAll("&nbsp;", " ")
                      .replaceAll("&#39;", "'");
        // Remove extra whitespace
        noHtml = noHtml.replaceAll("\\s+", " ").trim();
        return noHtml;
    }

    private String getAnswersForQuestion(String questionId) throws IOException {
        String answersUrl = API_BASE_URL + "/questions/" + questionId + "/answers";
        Map<String, String> params = new HashMap<>();
        params.put("order", "desc");
        params.put("sort", "votes");
        params.put("site", "stackoverflow");
        params.put("filter", "withbody"); // Include the answer body
        params.put("key", API_KEY);

        Request request = new Request.Builder()
                .url(createQuery("questions/" + questionId + "/answers", params))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                System.err.println("Error: " + response.code());
                return "";
            }

            return response.body().string();
        }
    }

    public List<String> searchSolutions(String query) {
        List<String> solutions = new ArrayList<>();
        try {
            Map<String, String> params = new HashMap<>();
            params.put("order", "desc");
            params.put("sort", "relevance");
            params.put("q", query);
            params.put("filter", "!9Z(-wwYGT");
            params.put("answers", "1");
            params.put("key", API_KEY);
            params.put("site", "stackoverflow");

            URL url = createQuery("search/advanced", params);
            Request request = new Request.Builder()
                    .url(url)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    System.err.println("Error: " + response.code());
                    return solutions;
                }

                String responseBody = response.body().string();
                JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
                JsonArray items = jsonResponse.getAsJsonArray("items");

                if (items != null) {
                    for (JsonElement item : items) {
                        JsonObject question = item.getAsJsonObject();
                        String title = cleanHtmlContent(question.get("title").getAsString());
                        String link = question.get("link").getAsString();
                        String body = question.has("body") ? 
                            cleanHtmlContent(question.get("body").getAsString())
                            : "";

                        String questionId = question.get("question_id").getAsString();

                        // Get answers for this question
                        String answersResponse = getAnswersForQuestion(questionId);
                        JsonObject answersJson = gson.fromJson(answersResponse, JsonObject.class);
                        JsonArray answers = answersJson.getAsJsonArray("items");

                        StringBuilder solution = new StringBuilder();
                        solution.append("<div class='solution-container'>");
                        solution.append("<h1 class='solution-title'>Problem Summary</h1>");
                        solution.append("<div class='solution-content'>").append(title).append("</div>");
                        
                        if (answers != null) {
                            solution.append("<h2 class='solution-section'>Solutions Found</h2>");
                            for (JsonElement answer : answers) {
                                JsonObject answerJson = answer.getAsJsonObject();
                                String answerBody = answerJson.has("body") ? 
                                    cleanHtmlContent(answerJson.get("body").getAsString())
                                    : "";
                                
                                solution.append("<div class='answer-container'>");
                                solution.append("<h4 class='answer-title'>Answer</h4>");
                                solution.append("<div class='solution-content'>").append(answerBody).append("</div>");
                                solution.append("</div>");
                            }
                        }

                        // Tools section
                        solution.append("<h3 class='solution-subsection'>Tools or Resources Needed</h3>")
                               .append("<ul class='resource-list'>")
                               .append("<li>Java Development Environment</li>")
                               .append("<li>Required dependencies mentioned in the solution</li>")
                               .append("</ul>");
                        
                        // References section
                        solution.append("<h3 class='solution-subsection'>References</h3>")
                               .append("<ul class='resource-list'>")
                               .append("<li><a href='").append(link).append("'>Stack Overflow Answer</a></li>")
                               .append("</ul>")
                               .append("</div>");

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