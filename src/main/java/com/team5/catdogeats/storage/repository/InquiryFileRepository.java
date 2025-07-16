package com.team5.catdogeats.storage.repository;

import com.team5.catdogeats.storage.domain.mapping.InquiryFiles;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InquiryFileRepository extends JpaRepository<InquiryFiles, String> {

    /**
     * 특정 문의 스레드의 모든 첨부 파일 조회 (부모 문의 포함)
     * N+1 문제 방지를 위해 모든 연관 관계를 fetch
     */
    @Query("SELECT if FROM InquiryFiles if " +
            "LEFT JOIN FETCH if.images " +
            "LEFT JOIN FETCH if.files " +
            "LEFT JOIN FETCH if.inquires i " +
            "LEFT JOIN FETCH i.parent " +
            "LEFT JOIN FETCH i.users " +
            "WHERE (i.id = :rootInquiryId OR i.parent.id = :rootInquiryId) " +
            "ORDER BY i.createdAt ASC, if.createdAt ASC")
    List<InquiryFiles> findByInquiryThreadOrderByCreatedAt(@Param("rootInquiryId") String rootInquiryId);

    /**
     * 🆕 추가: 특정 메시지의 첨부파일만 조회
     */
    @Query("SELECT if FROM InquiryFiles if " +
            "LEFT JOIN FETCH if.images " +
            "LEFT JOIN FETCH if.files " +
            "LEFT JOIN FETCH if.inquires i " +
            "WHERE i.id = :messageId " +
            "ORDER BY if.createdAt ASC")
    List<InquiryFiles> findByInquiresIdOrderByCreatedAt(@Param("messageId") String messageId);

    /**
     * 파일 ID로 문의 첨부 파일 조회 (기본)
     */
    @Query("SELECT if FROM InquiryFiles if " +
            "LEFT JOIN FETCH if.inquires " +
            "LEFT JOIN FETCH if.images " +
            "LEFT JOIN FETCH if.files " +
            "WHERE (if.images IS NOT NULL AND if.images.id = :fileId) OR " +
            "(if.files IS NOT NULL AND if.files.id = :fileId)")
    Optional<InquiryFiles> findByFileId(@Param("fileId") String fileId);

    /**
     * 파일 ID로 문의 첨부 파일 조회 (사용자 정보 포함 - 권한 검증용)
     */
    @Query("SELECT if FROM InquiryFiles if " +
            "LEFT JOIN FETCH if.images " +
            "LEFT JOIN FETCH if.files " +
            "LEFT JOIN FETCH if.inquires i " +
            "LEFT JOIN FETCH i.parent " +
            "LEFT JOIN FETCH i.users u " +
            "WHERE (if.images IS NOT NULL AND if.images.id = :fileId) OR " +
            "(if.files IS NOT NULL AND if.files.id = :fileId)")
    Optional<InquiryFiles> findByFileIdWithUserAndInquiry(@Param("fileId") String fileId);
}