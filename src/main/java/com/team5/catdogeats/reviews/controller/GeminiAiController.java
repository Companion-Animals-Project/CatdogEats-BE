package com.team5.catdogeats.reviews.controller;

import com.team5.catdogeats.reviews.domain.dto.GeminiPromptRequestDto;
import com.team5.catdogeats.reviews.service.GeminiAIService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/api/gemini")
@RequiredArgsConstructor
public class GeminiAiController {
    private final GeminiAIService geminiAiService;

    @PostMapping("/chat")
    public String chat(@RequestBody GeminiPromptRequestDto request) {
        System.out.println("===[Prompt value]===" + request.prompt());

        return geminiAiService.chatWithGemini(request.prompt());
    }
}
