package com.team5.catdogeats.carts.service;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.carts.dto.request.ProductComparisonRequest;
import com.team5.catdogeats.carts.dto.response.ProductComparisonResponse;
import com.team5.catdogeats.carts.repository.CartItemRepository;
import com.team5.catdogeats.carts.domain.mapping.CartItems;
import com.team5.catdogeats.products.domain.Products;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AIComparisonService {

    private final CartItemRepository cartItemRepository;

    @Value("${langchain4j.google-ai-gemini.api-key}")
    private String geminiApiKey;

    // AI를 활용한 제품 비교 분석
    public ProductComparisonResponse compareProducts(ProductComparisonRequest request, UserPrincipal userPrincipal) {
        try {
            log.info("AI 제품 비교 시작 - 사용자: {}, 제품1: {}, 제품2: {}",
                    userPrincipal.providerId(), request.getProduct1Id(), request.getProduct2Id());

            // 1. 제품 정보 조회
            CartItems cartItem1 = findCartItemById(request.getProduct1Id());
            CartItems cartItem2 = findCartItemById(request.getProduct2Id());

            Products product1 = cartItem1.getProduct();
            Products product2 = cartItem2.getProduct();

            // 2. 제품이 같은 카테고리인지 확인 (선택사항)
            validateProductsComparable(product1, product2, request.getPetCategory());

            // 3. AI 모델 초기화
            ChatLanguageModel chatModel = createGeminiModel();

            // 4. 프롬프트 생성 및 AI 분석 요청
            String prompt = buildComparisonPrompt(product1, product2, request);
            String aiResponse = chatModel.generate(prompt);

            // 5. AI 응답 파싱 및 응답 구성
            ProductComparisonResponse response = parseAiResponse(aiResponse, product1, product2, request);

            log.info("AI 제품 비교 완료 - 사용자: {}", userPrincipal.providerId());
            return response;

        } catch (Exception e) {
            log.error("AI 제품 비교 중 오류 발생 - 사용자: {}, 오류: {}",
                    userPrincipal.providerId(), e.getMessage(), e);
            throw new RuntimeException("AI 제품 비교 분석 중 오류가 발생했습니다.", e);
        }
    }

    // Gemini AI 모델 생성
    private ChatLanguageModel createGeminiModel() {
        return GoogleAiGeminiChatModel.builder()
                .apiKey(geminiApiKey)
                .modelName("gemini-1.5-flash")
                .temperature(0.7)
                .maxOutputTokens(1000)
                .build();
    }

    // 장바구니 아이템 조회
    private CartItems findCartItemById(String cartItemId) {
        return cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new IllegalArgumentException("해당 장바구니 상품을 찾을 수 없습니다: " + cartItemId));
    }

    // 제품 비교 가능성 검증
    private void validateProductsComparable(Products product1, Products product2, String petCategory) {
        // 기본적인 검증 로직
        if (product1.getId().equals(product2.getId())) {
            throw new IllegalArgumentException("같은 제품은 비교할 수 없습니다.");
        }

        // 검증 로직 추가 예정
    }

    // AI 비교 분석용 프롬프트 생성
    private String buildComparisonPrompt(Products product1, Products product2, ProductComparisonRequest request) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("반려동물 용품 전문가로서 다음 두 제품을 비교 분석해주세요.\n\n");

        // 반려동물 정보
        prompt.append("## 반려동물 정보\n");
        prompt.append("- 종류: ").append(request.getPetCategory()).append("\n");
        if (request.getBreed() != null) {
            prompt.append("- 품종: ").append(request.getBreed()).append("\n");
        }
        if (request.getAge() != null) {
            prompt.append("- 나이: ").append(request.getAge()).append("세\n");
        }
        if (request.getGender() != null) {
            prompt.append("- 성별: ").append(request.getGender()).append("\n");
        }
        if (request.getHasAllergies() != null && request.getHasAllergies()) {
            prompt.append("- 알레르기 유무: 있음\n");
        }
        if (request.getHealthCondition() != null) {
            prompt.append("- 건강상태: ").append(request.getHealthCondition()).append("\n");
        }
        if (request.getSpecialRequests() != null) {
            prompt.append("- 특별요청: ").append(request.getSpecialRequests()).append("\n");
        }

        // 제품 정보 (Products 엔티티의 실제 필드 사용)
        prompt.append("\n## 비교 제품\n");
        prompt.append("### 제품 1: ").append(product1.getTitle()).append("\n");
        prompt.append("- 부제목: ").append(product1.getSubTitle()).append("\n");
        prompt.append("- 가격: ").append(product1.getPrice().toString()).append("원\n");
        prompt.append("- 제품 정보: ").append(product1.getProductInfo()).append("\n");
        prompt.append("- 카테고리: ").append(product1.getProductCategory()).append("\n");
        if (product1.isDiscounted()) {
            prompt.append("- 할인율: ").append(product1.getDiscountRate()).append("%\n");
        }

        prompt.append("\n### 제품 2: ").append(product2.getTitle()).append("\n");
        prompt.append("- 부제목: ").append(product2.getSubTitle()).append("\n");
        prompt.append("- 가격: ").append(product2.getPrice().toString()).append("원\n");
        prompt.append("- 제품 정보: ").append(product2.getProductInfo()).append("\n");
        prompt.append("- 카테고리: ").append(product2.getProductCategory()).append("\n");
        if (product2.isDiscounted()) {
            prompt.append("- 할인율: ").append(product2.getDiscountRate()).append("%\n");
        }

        // 분석 요청사항
        prompt.append("\n## 분석 요청\n");
        prompt.append("다음 형식으로 구체적이고 실용적인 분석을 해주세요:\n\n");
        prompt.append("### 종합분석\n");
        prompt.append("[두 제품의 전반적인 비교와 특징]\n\n");
        prompt.append("### 영양분석\n");
        prompt.append("[영양성분, 건강 효과, 안전성 비교]\n\n");
        prompt.append("### 가격분석\n");
        prompt.append("[가격 대비 효과와 경제성 분석]\n\n");
        prompt.append("### 적합성분석\n");
        prompt.append("[등록된 반려동물 정보에 따른 적합성]\n\n");
        prompt.append("### 최종추천\n");
        prompt.append("[명확한 추천 결과와 구체적인 이유]\n\n");
        prompt.append("각 섹션은 2-3문장으로 간결하게 작성해주세요.");

        return prompt.toString();
    }

    // AI 응답 파싱 및 응답 객체 생성
    private ProductComparisonResponse parseAiResponse(String aiResponse, Products product1, Products product2, ProductComparisonRequest request) {

        // AI 응답을 섹션별로 파싱 (간단한 구현)
        String analysis = extractSection(aiResponse, "종합분석");
        String nutritionAnalysis = extractSection(aiResponse, "영양분석");
        String priceAnalysis = extractSection(aiResponse, "가격분석");
        String petSuitability = extractSection(aiResponse, "적합성분석");
        String recommendation = extractSection(aiResponse, "최종추천");

        // 파싱 실패 시 전체 응답 사용
        if (analysis.isEmpty() && nutritionAnalysis.isEmpty() && priceAnalysis.isEmpty()) {
            analysis = aiResponse;
        }

        return ProductComparisonResponse.builder()
                .product1(ProductComparisonResponse.ProductInfo.builder()
                        .id(product1.getId())
                        .name(product1.getTitle())
                        .option(product1.getSubTitle())
                        .price(product1.getPrice())
                        .image(null) // 추후 이미지 처리 추가 가능
                        .category(product1.getPetCategory().toString())
                        .build())
                .product2(ProductComparisonResponse.ProductInfo.builder()
                        .id(product2.getId())
                        .name(product2.getTitle())
                        .option(product2.getSubTitle())
                        .price(product2.getPrice())
                        .image(null) // 추후 이미지 처리 추가 가능
                        .category(product2.getPetCategory().toString())
                        .build())
                .analysis(analysis)
                .nutritionAnalysis(nutritionAnalysis)
                .priceAnalysis(priceAnalysis)
                .petSuitability(petSuitability)
                .recommendation(recommendation)
                .petInfo(ProductComparisonResponse.PetInfo.builder()
                        .petCategory(request.getPetCategory())
                        .breed(request.getBreed())
                        .age(request.getAge())
                        .gender(request.getGender())
                        .hasAllergies(request.getHasAllergies())
                        .healthCondition(request.getHealthCondition())
                        .specialRequests(request.getSpecialRequests())
                        .build())
                .build();
    }

    // AI 응답에서 특정 섹션 추출
    private String extractSection(String aiResponse, String sectionName) {
        try {
            String marker = "### " + sectionName;
            int startIndex = aiResponse.indexOf(marker);
            if (startIndex == -1) {
                return "";
            }

            startIndex = startIndex + marker.length();
            int endIndex = aiResponse.indexOf("### ", startIndex);
            if (endIndex == -1) {
                endIndex = aiResponse.length();
            }

            return aiResponse.substring(startIndex, endIndex).trim();
        } catch (Exception e) {
            log.warn("AI 응답 파싱 중 오류: {}", e.getMessage());
            return "";
        }
    }
}