package com.semweb.semanticwebhw2.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class VectorStoreService {

    @Autowired
    private RdfService rdfService;

    private List<Map<String, String>> bookChunks = new ArrayList<>();

    @PostConstruct
    public void buildIndex() {
        bookChunks = rdfService.getAllBooks();
    }

    public List<Map<String, String>> search(String query, String currentBookId) {
        String lowerQuery = query.toLowerCase();

        List<Map.Entry<Map<String, String>, Integer>> scored = new ArrayList<>();

        for (Map<String, String> book : bookChunks) {
            int score = 0;

            String title = book.getOrDefault("title", "").toLowerCase();
            String author = book.getOrDefault("author", "").toLowerCase();
            String genres = book.getOrDefault("genres", "").toLowerCase();
            String level = book.getOrDefault("readingLevel", "").toLowerCase();
            String id = book.getOrDefault("id", "").toLowerCase();

            if (lowerQuery.contains(title) || title.contains(lowerQuery)) score += 10;

            for (String part : author.split("\\s+")) {
                if (part.length() > 2 && lowerQuery.contains(part)) score += 5;
            }

            for (String genre : genres.split(",\\s*")) {
                String g = genre.trim();
                if (lowerQuery.contains(g)) score += 4;
                if (lowerQuery.contains("science fiction") && g.equals("sciencefiction")) score += 4;
                if (lowerQuery.contains("sci-fi") && g.equals("sciencefiction")) score += 4;
                if (lowerQuery.contains("scifi") && g.equals("sciencefiction")) score += 4;
            }

            if (lowerQuery.contains(level)) score += 3;

            if (currentBookId != null && !currentBookId.isEmpty()
                    && id.equalsIgnoreCase(currentBookId)) score += 8;

            if (lowerQuery.contains("recommend") || lowerQuery.contains("enjoy")
                    || lowerQuery.contains("suggest") || lowerQuery.contains("like")) score += 2;

            scored.add(new AbstractMap.SimpleEntry<>(book, score));
        }

        scored.sort((a, b) -> b.getValue() - a.getValue());

        List<Map<String, String>> result = scored.stream()
                .filter(e -> e.getValue() > 0)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        if (result.isEmpty()) {
            return bookChunks.subList(0, Math.min(3, bookChunks.size()));
        }

        return result.subList(0, Math.min(5, result.size()));
    }

    public List<Map<String, String>> getAllBooks() {
        return bookChunks;
    }
}