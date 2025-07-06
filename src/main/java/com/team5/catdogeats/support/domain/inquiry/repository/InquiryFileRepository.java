package com.team5.catdogeats.storage.repository;

import com.team5.catdogeats.storage.domain.mapping.InquiryFiles;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InquiryFileRepository extends JpaRepository<InquiryFiles, String> {


    // 특정 문의의 모든 첨부 파일 조회
    @Query("SELECT if FROM InquiryFiles if " +
            "LEFT JOIN FETCH if.images " +
            "LEFT JOIN FETCH if.files " +
            "WHERE if.inquires.id = :inquiryId " +
            "ORDER BY if.createdAt ASC")
    List<InquiryFiles> findByInquiryIdOrderByCreatedAt(@Param("inquiryId") String inquiryId);


    // 특정 문의와 연결된 특정 파일 조회
    @Query("SELECT if FROM InquiryFiles if " +
            "LEFT JOIN FETCH if.images " +
            "LEFT JOIN FETCH if.files " +
            "LEFT JOIN FETCH if.inquires " +
            "WHERE if.inquires.id = :inquiryId AND " +
            "(if.images.id = :fileId OR if.files.id = :fileId)")
    Optional<InquiryFiles> findByInquiryIdAndFileId(@Param("inquiryId") String inquiryId,
                                                    @Param("fileId") String fileId);


    // 특정 문의 스레드의 모든 첨부 파일 조회
    @Query("SELECT if FROM InquiryFiles if " +
            "LEFT JOIN FETCH if.images " +
            "LEFT JOIN FETCH if.files " +
            "LEFT JOIN FETCH if.inquires i " +
            "WHERE (i.id = :rootInquiryId OR i.parent.id = :rootInquiryId) " +
            "ORDER BY i.createdAt ASC, if.createdAt ASC")
    List<InquiryFiles> findByInquiryThreadOrderByCreatedAt(@Param("rootInquiryId") String rootInquiryId);


    // 특정 파일이 어떤 문의에 속하는지 조회
    @Query("SELECT if FROM InquiryFiles if " +
            "LEFT JOIN FETCH if.inquires " +
            "LEFT JOIN FETCH if.images " +
            "LEFT JOIN FETCH if.files " +
            "WHERE if.images.id = :fileId OR if.files.id = :fileId")
    Optional<InquiryFiles> findByFileId(@Param("fileId") String fileId);
}