package com.team5.catdogeats.storage.domain.repository;

import com.team5.catdogeats.storage.domain.Images;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImagesRepository extends JpaRepository<Images, String> {
    // 기본 CRUD 메서드들이 자동으로 제공됩니다
}
