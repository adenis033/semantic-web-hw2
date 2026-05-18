package com.semweb.semanticwebhw2.controller;

import com.semweb.semanticwebhw2.service.ChatService;
import com.semweb.semanticwebhw2.service.VectorStoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private ChatService chatService;

    @Autowired
    private VectorStoreService vectorStoreService;

    @PostMapping("/message")
    public Map<String, String> chat(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        String currentBookId = request.getOrDefault("currentBookId", "");
        String currentPage = request.getOrDefault("currentPage", "home");

        List<Map<String, String>> relevantBooks = vectorStoreService.search(message, currentBookId);
        String response = chatService.chat(message, relevantBooks);

        return Map.of("response", response);
    }

    @GetMapping("/starters")
    public Map<String, List<String>> getStarters(
            @RequestParam(defaultValue = "") String bookId,
            @RequestParam(defaultValue = "home") String page) {

        List<String> starters = chatService.getConversationStarters(bookId, page);
        return Map.of("starters", starters);
    }
}