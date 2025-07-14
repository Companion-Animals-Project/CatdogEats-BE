package com.team5.catdogeats.batch.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team5.catdogeats.batch.dto.ProductReviewBatchDto;
import com.team5.catdogeats.batch.dto.ReviewSummaryResult;
import com.team5.catdogeats.products.domain.Products;
import com.team5.catdogeats.reviews.domain.Reviews;
import com.team5.catdogeats.reviews.domain.dto.ReviewClassificationResultDto;
import com.team5.catdogeats.reviews.domain.dto.ReviewSummaryItemDto;
import com.team5.catdogeats.reviews.domain.mapping.ReviewsSummaryLLM;
import com.team5.catdogeats.reviews.service.GeminiAIService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@StepScope
public class ReviewSummaryItemProcessor implements ItemProcessor<ProductReviewBatchDto, ReviewSummaryResult> {
    private final GeminiAIService geminiAIService;

    public ReviewSummaryItemProcessor(GeminiAIService geminiAIService) {
        this.geminiAIService = geminiAIService;
    }

    @Override
    public ReviewSummaryResult process(ProductReviewBatchDto dto) {
        try {
            Products product = dto.product();
            List<Reviews> reviews = dto.latestReviews();
            ReviewsSummaryLLM summary = dto.latestSummary();

            // 요약이 아예 없는 경우 → 무조건 요약!
            if (summary == null) {
                log.info("summary is null → summarize!");
                return doSummarize(product, reviews);
            }

            // 리뷰 갯수 변화 체크 (리뷰 추가/삭제)
            if (summary.getReviewCount() != reviews.size()) {
                log.info("reviewCount changed (summary: {}, actual: {}) → summarize!", summary.getReviewCount(), reviews.size());
                return doSummarize(product, reviews);
            }

            // 리뷰 중 updatedAt > summary.createdAt 이 있는지 체크 (리뷰 수정)
            boolean hasUpdated = reviews.stream()
                    .anyMatch(r -> r.getUpdatedAt().isAfter(summary.getCreatedAt()));
            if (hasUpdated) {
                log.info("some review updated after summary createdAt → summarize!");
                return doSummarize(product, reviews);
            }

            // 변화 없음 → 기존 요약 재사용(Writer에서 insert하지 않게 null 리턴)
            log.info("no review change, skipping summarization!");
            return null;
        } catch (Exception e) {
            log.error("[배치프로세서] ReviewSummaryResult process 중 에러: {}", e.getMessage(), e);
            return null;
        }
    }

    private ReviewSummaryResult doSummarize(Products product, List<Reviews> reviews) throws Exception {
        // 1. LLM 분류 프롬프트 생성
        String prompt1 = buildPrompt1(product, reviews);
        String llmResult = geminiAIService.chatWithGemini(prompt1);
        log.info(prompt1);
        List<ReviewClassificationResultDto> classificationResults = parseClassificationResults(llmResult, reviews);
        log.info("classificationResults: %s".formatted(classificationResults));

        // 2. 긍정/부정 리뷰 분리
        List<String> positiveReviews = new ArrayList<>();
        List<String> negativeReviews = new ArrayList<>();
        for (int i = 0; i < classificationResults.size(); i++) {
            var result = classificationResults.get(i);
            if (!"true".equalsIgnoreCase(result.result())) continue;
            String sentiment = result.sentiment();
            String content = reviews.get(i).getContents();
            if ("positive".equalsIgnoreCase(sentiment)) positiveReviews.add(content);
            else if ("negative".equalsIgnoreCase(sentiment)) negativeReviews.add(content);
        }

        // 3. 각 집합별 요약 및 파싱
        ReviewSummaryItemDto positiveSummary = null;
        ReviewSummaryItemDto negativeSummary = null;
        if (!positiveReviews.isEmpty()) {
            String posResultJson = summarizeByLLM(positiveReviews);
            positiveSummary = parseSummaryItem(posResultJson);
        }
        if (!negativeReviews.isEmpty()) {
            String negResultJson = summarizeByLLM(negativeReviews);
            negativeSummary = parseSummaryItem(negResultJson);
        }
        // null-safe 처리
        ObjectMapper mapper = new ObjectMapper();
        String positiveSummaryStr = positiveSummary != null ? positiveSummary.summary() : "";
        String negativeSummaryStr = negativeSummary != null ? negativeSummary.summary() : "";

        String positiveMainPointsStr = positiveSummary != null
                ? mapper.writeValueAsString(positiveSummary.mainPoints())
                : "[]";
        String negativeMainPointsStr = negativeSummary != null
                ? mapper.writeValueAsString(negativeSummary.mainPoints())
                : "[]";
        String positiveKeywordsStr = positiveSummary != null
                ? mapper.writeValueAsString(positiveSummary.keywords())
                : "[]";
        String negativeKeywordsStr = negativeSummary != null
                ? mapper.writeValueAsString(negativeSummary.keywords())
                : "[]";

        return new ReviewSummaryResult(
                product,
                positiveSummaryStr,
                negativeSummaryStr,
                reviews.size(),
                classificationResults,
                positiveMainPointsStr,
                negativeMainPointsStr,
                positiveKeywordsStr,
                negativeKeywordsStr
        );
    }

    private String buildPrompt1(Products product, List<Reviews> reviews) {
        StringBuilder sb = new StringBuilder();
        sb.append("product_name: ").append(product.getTitle()).append("\n");
        sb.append("product_description: ").append(product.getSubTitle()).append("\n");
        sb.append("product_info: ").append(product.getProductInfo()).append("\n");
        sb.append("product_content: ").append(product.getContents()).append("\n");
        sb.append("reviews:\n");
        for (Reviews review : reviews) {
            sb.append("- ").append(review.getContents()).append("\n");
        }
        return
                """
                너는 텍스트 리뷰를 읽고 작성자의 의도를 파악해내는 리뷰 분석가야.
                아래의 입력 포맷으로 product_name, product_description, product_info, product_content와 상품에 대한 리뷰들을 받아, 각 리뷰를 개별적으로 분석하고
                모든 검증 단계를 통과한 리뷰만 분류하여 검증 결과의 이유를 reason에 저장하고,
                긍정 리뷰는 positiveReview, 부정 리뷰는 negativeReview 배열에 구조적으로 누적 저장해.
               
                [분석 및 검증 단계]
                각 리뷰에 대해 아래 순서로 검증해:
                1. 리뷰가 product_name(아래 입력에서 지정) 외의 다른 제품에 대한 내용이 포함되어 있지 않은가?
                2. 단순한 긍/부정(예: 맛있다, 좋다 등) 표현 외에, 상품의 속성(특징, 장점, 단점, 사용 경험 등)에 대한 충분한 구체적 설명이 있는가?
        
                입력 포맷은 아래와 같아.
                product_name: {product_name}
                product_description: {product_description}
                product_info: {product_info}
                product_content: {product_content}
                reviews:
                - {review}
                - {review}
                ...
        
                결과 포맷은 아래와 같아.
                [
                  {"review": {review}, "result": "true", sentiment: "positive", "reason": "검증 결과에 대한 이유"},
                  {"review": {review}, "result": "true", sentiment: "negative", "reason": "검증 결과에 대한 이유"},
                  {"review": {review}, "result": "false", sentiment: "", "reason": "검증 결과에 대한 이유"},
                  ...
                ]
                
                각 리뷰별로 result와 sentiment, reason만 결과로 반환해.
                결과는 반드시 JSON 배열만, 추가 설명 없이 출력해라.
                결과 JSON 배열에서 마지막 항목 뒤에 ,(쉼표) 절대 붙이지 말 것!
                
                """ + "\n" + sb;
    }

    // 리뷰들 검증 및 분류한 응답 결과 파싱하기
    private List<ReviewClassificationResultDto> parseClassificationResults(String json, List<Reviews> reviews) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            // 앞뒤에 코드블럭 ```json, ``` 등 있으면 제거
            json = json.trim();
            if (json.startsWith("```json")) {
                json = json.substring(7).trim(); // "```json" 길이 7
            }
            if (json.startsWith("```")) {
                json = json.substring(3).trim();
            }
            if (json.endsWith("```")) {
                json = json.substring(0, json.length() - 3).trim();
            }
            // 배열 내 마지막 쉼표(,) 제거
            json = json.replaceAll(",\\s*]", "]");

            ReviewClassificationResultDto[] arr = mapper.readValue(json, ReviewClassificationResultDto[].class);
            List<ReviewClassificationResultDto> resultList = new ArrayList<>();
            for (int i = 0; i < arr.length; i++) {
                ReviewClassificationResultDto dto = arr[i];
                Reviews reviewObj = (i < reviews.size()) ? reviews.get(i) : null;
                // 새로 복사 생성 (record라면 with-builder, class면 setReviewObj 등 사용)
                resultList.add(ReviewClassificationResultDto.builder()
                        .result(dto.result())
                        .sentiment(dto.sentiment())
                        .reason(dto.reason())
                        .review(dto.review())
                        .reviewObj(reviewObj)
                        .build()
                );
            }
            return resultList;
        } catch (Exception e) {
            log.error("LLM 분류 결과 파싱 실패, 원본 JSON=" + json, e);
            throw new RuntimeException("LLM 분류 결과 파싱 실패", e);
        }
    }

    private String summarizeByLLM(List<String> reviews) {
        if (reviews == null || reviews.isEmpty()) return null; // 빈 JSON 배열로 반환
        StringBuilder sb = new StringBuilder();
        sb.append("reviews:\n");

        for (String review : reviews) {
            sb.append("- ").append(review).append("\n");
        }
        String prompt2 = """
                아래의 리뷰들을 읽고 상품의 핵심을 한 문장으로 요약하여 summary에 저장해.
                아래의 리뷰들을 읽고 상품에 대한 주요 사항들 중 중요도가 높은 5개 선별하여 문장으로 mainPoints에 저장하고,
                주요 키워드들은 4개 선별하여 keywords에 저장해.
                
                입력 포맷은 아래와 같아.
                reviews:
                - {review},
                - {review},
                ...
                
                결과 포맷은 아래와 같아.
                {
                   "summary": "강아지가 정말 잘 먹어요.",
                   "mainPoints": ["100% 국내산 닭가슴살로 만든 안전한 재료", "강아지들의 뛰어난 기호성과 맛에대한 만족", ...],
                   "keywords": ["맛있어해요", "건강한재료", "국내산", "안심", ...]
                }
                
                모든 리뷰들을 읽고 반드시 결과는 결과 포맷에 맞게 summary, mainPoints, keywords로 구성된 하나의 JSON 객체 반환해.
                
                """ + "\n" + sb;
        return geminiAIService.chatWithGemini(prompt2);
    }

    // 리뷰 요약 프롬프트 응답 파싱하기
    private ReviewSummaryItemDto parseSummaryItem(String json) {
        if (json == null || json.isBlank()) return null;
        json = json.trim();

        // 코드블럭, 개행 등 처리(필요시)
        if (json.startsWith("```json")) json = json.substring(7).trim();
        if (json.startsWith("```")) json = json.substring(3).trim();
        if (json.endsWith("```")) json = json.substring(0, json.length() - 3).trim();

        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(json, ReviewSummaryItemDto.class);
        } catch (Exception e) {
            log.error("LLM 요약 파싱 실패, 원본 JSON=" + json, e);
            throw new RuntimeException("LLM 요약 파싱 실패", e);
        }
    }
}
