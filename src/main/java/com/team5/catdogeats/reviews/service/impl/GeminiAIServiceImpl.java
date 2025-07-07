package com.team5.catdogeats.reviews.service.impl;

import com.team5.catdogeats.reviews.service.GeminiAIService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GeminiAIServiceImpl implements GeminiAIService {
    private final ChatLanguageModel chatLanguageModel;

    /**
     * 프롬프트를 Gemini로 전달하고 응답을 반환
     */
    public String chatWithGemini(String prompt) {
        // 단순 String → Gemini API에 직접 전달
        return chatLanguageModel.generate(prompt);
    }
}