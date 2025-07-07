package com.team5.catdogeats.reviews.service.impl;

import com.esotericsoftware.minlog.Log;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.team5.catdogeats.global.annotation.JpaTransactional;
import com.team5.catdogeats.products.domain.Products;
import com.team5.catdogeats.products.repository.ProductRepository;
import com.team5.catdogeats.reviews.domain.Reviews;
import com.team5.catdogeats.reviews.domain.dto.ReviewClassificationResultDto;
import com.team5.catdogeats.reviews.domain.dto.ReviewSummaryItemDto;
import com.team5.catdogeats.reviews.domain.dto.ReviewSummaryResponseDto;
import com.team5.catdogeats.reviews.domain.mapping.ReviewClassificationLLM;
import com.team5.catdogeats.reviews.domain.mapping.ReviewsSummaryLLM;
import com.team5.catdogeats.reviews.repository.ReviewClassificationLLMRepository;
import com.team5.catdogeats.reviews.repository.ReviewRepository;
import com.team5.catdogeats.reviews.repository.ReviewSummaryLLMRepository;
import com.team5.catdogeats.reviews.service.GeminiAIService;
import com.team5.catdogeats.reviews.service.ReviewSummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@JpaTransactional
public class ReviewSummaryServiceImpl implements ReviewSummaryService {
    private final ProductRepository productRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewSummaryLLMRepository reviewSummaryLLMRepository;
    private final GeminiAIService geminiAIService;
    private final ReviewClassificationLLMRepository reviewClassificationLLMRepository;

    @Override
    public ReviewSummaryResponseDto summarizeReviewsByProductNumber(Long productNumber, boolean forceRefresh) throws JsonProcessingException {
        Products product = productRepository.findByProductNumber(productNumber)
                .orElseThrow(() -> new NoSuchElementException("해당 상품이 존재하지 않습니다."));

        // 이미 요약 이력이 있고, 강제 갱신 요청이 아니면 기존의 요약 사용
        ReviewsSummaryLLM latestSummary = reviewSummaryLLMRepository.findTopByProductOrderByCreatedAtDesc(product);
        if (latestSummary != null && !forceRefresh) {
            // 저장된 요약 JSON을 파싱해서 DTO로 변환
            ObjectMapper mapper = new ObjectMapper();
            List<String> positiveSummary = new ArrayList<>();
            List<String> negativeSummary = new ArrayList<>();
            try {
                if (latestSummary.getPositiveReview() != null && !latestSummary.getPositiveReview().isBlank())
                    positiveSummary = mapper.readValue(latestSummary.getPositiveReview(), List.class);
                if (latestSummary.getNegativeReview() != null && !latestSummary.getNegativeReview().isBlank())
                    negativeSummary = mapper.readValue(latestSummary.getNegativeReview(), List.class);
            } catch (JsonProcessingException e) {
                Log.error("기존 요약 JSON 파싱 실패", e);
            }
            // 리뷰 요약이 모두 비어있다면
            if ((positiveSummary == null || positiveSummary.isEmpty()) &&
                    (negativeSummary == null || negativeSummary.isEmpty())) {
                throw new NoSuchElementException("해당 상품에 대한 검증된 리뷰가 존재하지 않아 리뷰 요약이 존재하지 않습니다.");
            }
            return new ReviewSummaryResponseDto(
                    product.getTitle(),
                    positiveSummary,
                    negativeSummary
            );
        }

        // 최신 리뷰 최대 30개 조회 (최신순)
        List<Reviews> latestReviews = reviewRepository
                .findTop30ByProductOrderByCreatedAtDesc(product);

        if (latestReviews.isEmpty()) {
            throw new NoSuchElementException("해당 상품에 대한 리뷰가 존재하지 않습니다.");
        }

        // --- 1. LLM 분류 프롬프트 (prompt1) 생성 ---
        String prompt1 = buildPrompt1(product.getTitle(), latestReviews);
        Log.info(prompt1);

        // LLM 분류 실행 (JSON 결과 파싱)
        String llmResult = geminiAIService.chatWithGemini(prompt1);
        List<ReviewClassificationResultDto> classificationResults = parseClassificationResults(llmResult);
        Log.info("classificationResults: %s".formatted(classificationResults));

        // --- 2. 긍/부정/검증 통과 리뷰 분리 ---
        List<String> positiveReviews = new ArrayList<>();
        List<String> negativeReviews = new ArrayList<>();
        for (int i = 0; i < classificationResults.size(); i++) {
            ReviewClassificationResultDto result = classificationResults.get(i);
            // 검증 및 분류된 리뷰 DB에 저장
            Reviews reviewEntity = (i < latestReviews.size()) ? latestReviews.get(i) : null;

            ReviewClassificationLLM entity = ReviewClassificationLLM.builder()
                    .review(reviewEntity)
                    .product(product)
                    .result(result.result())
                    .sentiment(result.sentiment())
                    .reason(result.reason())
                    .reviewContent(result.review())
                    .build();

            reviewClassificationLLMRepository.save(entity);

            // 1. "result"가 "true"일 때만 통과
            if (!"true".equalsIgnoreCase(result.result())) {
                continue; // "false"이면 무시
            }
            // 2. "sentiment"가 정확히 "positive" 또는 "negative"일 때만 분리
            String sentiment = result.sentiment();
            String content = latestReviews.get(i).getContents();
            if ("positive".equalsIgnoreCase(sentiment)) {
                positiveReviews.add(content);
            } else if ("negative".equalsIgnoreCase(sentiment)) {
                negativeReviews.add(content);
            }
        }
        Log.debug("positiveReviews: %s".formatted(positiveReviews));
        Log.debug("negativeReviews: %s".formatted(negativeReviews));

        // --- 3. 각 리뷰 집합별 요약 프롬프트 (prompt2) 생성 및 Gemini 호출 ---
        List<String> positiveSummary = parseSummaries(summarizeByLLM(positiveReviews));
        List<String> negativeSummary = parseSummaries(summarizeByLLM(negativeReviews));

        Log.debug("positiveSummary: %s".formatted(positiveSummary));
        Log.debug("negativeSummary: %s".formatted(negativeSummary));

        ObjectMapper mapper = new ObjectMapper();
        String positiveReviewJson = mapper.writeValueAsString(positiveSummary);
        String negativeReviewJson = mapper.writeValueAsString(negativeSummary);

        // --- 4. 요약 저장 ---
        reviewSummaryLLMRepository.save(
                ReviewsSummaryLLM.builder()
                        .product(product)
                        .positiveReview(positiveReviewJson)
                        .negativeReview(negativeReviewJson)
                        .reviewCount(latestReviews.size())
                        .build()
        );

        return new ReviewSummaryResponseDto(
                product.getTitle(),
                positiveSummary,
                negativeSummary
        );
    }

    private String buildPrompt1(String productName, List<Reviews> reviews) {
        StringBuilder sb = new StringBuilder();
        sb.append("product_name: ").append(productName).append("\n");
        sb.append("reviews:\n");
        for (Reviews review : reviews) {
            sb.append("- ").append(review.getContents()).append("\n");
        }
        return
            """
            너는 텍스트 리뷰를 읽고 작성자의 의도를 파악해내는 리뷰 분석가야.
            아래의 입력 포맷으로 여러 개의 리뷰와 상품명을 전달받으면, 각 리뷰를 개별적으로 분석하고
            모든 검증 단계를 통과한 리뷰만 분류하여, 긍정 리뷰는 positiveReview, 부정 리뷰는 negativeReview 배열에 구조적으로 누적 저장해.
            
            [분석 및 검증 단계]
            각 리뷰에 대해 아래 순서로 검증해:
            1. 리뷰가 product_name(아래 입력에서 지정) 외의 다른 제품에 대한 내용이 포함되어 있지 않은가?
            2. 단순한 긍/부정(예: 맛있다, 좋다 등) 표현 외에, 상품의 속성(특징, 장점, 단점, 사용 경험 등)에 대한 충분한 구체적 설명이 있는가?
    
            입력 포맷은 아래와 같아.
            product_name: {product_name}
            reviews:
            - {review}
            - {review}
            ...
    
            결과 포맷은 아래와 같아.
            [
              {"review": {review}, "result": "true", sentiment: "positive", "reason": "" // 종합적으로 result에 대한 사유를 설명},
              {"review": {review}, "result": "false", sentiment: "", "reason": "" // 종합적으로 result에 대한 사유를 설명},
              ...
            ]
            답변의 시작과 끝에 아무런 텍스트, 설명, 인삿말, 여는말, 닫는말을 넣지 말고,
            반드시 아래 형식만 순수하게 출력해라.
            결과는 반드시 JSON 배열만, 추가 설명 없이 출력해라.
            각 리뷰별로 result와 sentiment만 결과로 반환해.
            
            """ + "\n" + sb.toString();
    }

    // 리뷰들 검증 및 분류한 응답 결과 파싱하기
    private List<ReviewClassificationResultDto> parseClassificationResults(String json) {
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
            // 이제 순수 JSON 배열 형태가 됨
            Log.info(json);

            ReviewClassificationResultDto[] arr = mapper.readValue(json, ReviewClassificationResultDto[].class);
            if (arr == null) return new ArrayList<>(); // null 방어
            return Arrays.asList(arr);
        } catch (Exception e) {
            Log.error("LLM 분류 결과 파싱 실패, 원본 JSON=" + json, e);
            throw new RuntimeException("LLM 분류 결과 파싱 실패", e);
        }
    }

    private String summarizeByLLM(List<String> reviews) {
        if (reviews == null || reviews.isEmpty()) return "[]"; // 빈 JSON 배열로 반환
        StringBuilder sb = new StringBuilder();
        sb.append("reviews:\n");

        for (String review : reviews) {
            sb.append("- ").append(review).append("\n");
        }
        String prompt2 = """
                아래의 리뷰들을 읽고 상품의 핵심만 각각 한 문장으로 간결하게 요약해줘.
                
                입력 포맷은 아래와 같아.
                reviews:
                - {review},
                - {review},
                ...
                
                결과 포맷은 아래와 같아.
                [
                  {"summary": "강아지가 정말 잘 먹어요."},
                  {"summary": "간식 크기가 적당하고 향도 좋아요."},
                  ...
                ]
                답변의 시작과 끝에 아무런 텍스트, 설명, 인삿말, 여는말, 닫는말을 넣지 말고,
                반드시 아래 형식만 순수하게 출력해라.
                결과는 반드시 JSON 배열만, 추가 설명 없이 출력해라.
                각 리뷰별로 summary 결과 반환해
                
                """ + "\n" + sb.toString();
        return geminiAIService.chatWithGemini(prompt2);
    }

    // 리뷰 요약 프롬프트 응답 파싱하기
    private List<String> parseSummaries(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        json = json.trim();
        if (json.equals("[]")) return new ArrayList<>();
        // 코드블럭, 개행 등 처리(필요시)
        if (json.startsWith("```json")) json = json.substring(7).trim();
        if (json.startsWith("```")) json = json.substring(3).trim();
        if (json.endsWith("```")) json = json.substring(0, json.length() - 3).trim();

        ObjectMapper mapper = new ObjectMapper();
        try {
            ReviewSummaryItemDto[] arr = mapper.readValue(json, ReviewSummaryItemDto[].class);
            List<String> result = new ArrayList<>();
            if (arr != null) {
                for (ReviewSummaryItemDto dto : arr) {
                    if (dto.summary() != null && !dto.summary().isBlank()) {
                        result.add(dto.summary());
                    }
                }
            }
            return result;
        } catch (Exception e) {
            Log.error("LLM 요약 파싱 실패, 원본 JSON=" + json, e);
            throw new RuntimeException("LLM 요약 파싱 실패", e);
        }
    }

}
