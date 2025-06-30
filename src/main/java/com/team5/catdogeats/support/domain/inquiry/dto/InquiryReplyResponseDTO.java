package com.team5.catdogeats.support.domain.inquiry.dto;

import com.team5.catdogeats.support.domain.Inquires;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

//문의 답변 응답 DTO
public record InquiryReplyResponseDTO(
        String replyId,
        String content,
        String createdAt) // Todo: 현재 프론트에서는 답변 날짜가 표기가 없는데, 레퍼런스들과 UX 관점에서 볼 때, 필요한 부분인 것 같습니다

{
    // Inquires 엔티티에서 답변 DTO 생성
    public static InquiryReplyResponseDTO from(Inquires reply) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        ZoneId koreaZone = ZoneId.of("Asia/Seoul");

        return new InquiryReplyResponseDTO(
                reply.getId(),
                reply.getContent(),
                reply.getCreatedAt().withZoneSameInstant(koreaZone).format(formatter)
        );
    }
}
