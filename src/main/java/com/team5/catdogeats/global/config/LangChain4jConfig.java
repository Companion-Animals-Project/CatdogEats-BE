package com.team5.catdogeats.global.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LangChain4jConfig {

    @Value("${spring.langchain4j.google-ai-gemini.api-key}")
    private String geminiKey;

    @Value("${spring.langchain4j.google-ai-gemini.model-name}")
    private String geminiModel;

    @Bean
    public ChatLanguageModel chatLanguageModel() {
        return GoogleAiGeminiChatModel.builder()
                .apiKey(geminiKey)
                .modelName(geminiModel)
                .build();
    }

}