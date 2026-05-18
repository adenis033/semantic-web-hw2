package com.semweb.semanticwebhw2.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;

@Service
public class ChatService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Autowired
    private RdfService rdfService;

    private final WebClient webClient = WebClient.builder()
            .baseUrl("https://generativelanguage.googleapis.com")
            .build();

    public String chat(String userMessage, List<Map<String, String>> relevantBooks) {
        String context = buildContext(relevantBooks);
        String userContext = buildUserContext();

        String prompt = "You are a helpful book recommendation assistant. " +
                "Answer the user's question based ONLY on the following book data from our database. " +
                "Do not use any outside knowledge about books.\n\n" +
                context + "\n" + userContext + "\n" +
                "User question: " + userMessage;

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", prompt)
                        ))
                )
        );

        try {
            Map response = webClient.post()
                    .uri("/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            List candidates = (List) response.get("candidates");
            Map candidate = (Map) candidates.get(0);
            Map content = (Map) candidate.get("content");
            List parts = (List) content.get("parts");
            Map part = (Map) parts.get(0);
            return (String) part.get("text");

        } catch (Exception e) {
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }

    private String buildContext(List<Map<String, String>> books) {
        StringBuilder sb = new StringBuilder();
        sb.append("Books in our database:\n");
        for (Map<String, String> book : books) {
            sb.append("- Title: ").append(book.getOrDefault("title", "Unknown")).append("\n");
            sb.append("  Author: ").append(book.getOrDefault("author", "Unknown")).append("\n");
            sb.append("  Reading Level: ").append(book.getOrDefault("readingLevel", "Unknown")).append("\n");
            sb.append("  Genres: ").append(book.getOrDefault("genres", "Unknown")).append("\n");
        }
        return sb.toString();
    }

    private String buildUserContext() {
        return "User preferences:\n" +
                "- Alice: Reading Level = Intermediate, Preferred Genre = Science Fiction, Recommended: The Silent Patient\n" +
                "- Bob: Reading Level = Beginner, Preferred Genre = Mystery, Recommended: Hunger Games\n";
    }

    public List<String> getConversationStarters(String currentBookId, String page) {
        if (currentBookId != null && !currentBookId.isEmpty()) {
            Map<String, String> book = rdfService.getBookById(currentBookId);
            if (book != null) {
                String title = book.getOrDefault("title", "this book");
                return Arrays.asList(
                        "Who is the author of " + title + "?",
                        "What genre is " + title + "?",
                        "What reading level is " + title + " suitable for?"
                );
            }
        }

        if ("books".equals(page)) {
            return Arrays.asList(
                    "What book am I most likely to enjoy from this list?",
                    "Which book is suitable for a beginner reader?",
                    "Find me a Science Fiction book"
            );
        }

        return Arrays.asList(
                "What books do you have in the database?",
                "Recommend a book for a beginner reader",
                "Find a book by Frank Herbert in the Science Fiction genre"
        );
    }
}