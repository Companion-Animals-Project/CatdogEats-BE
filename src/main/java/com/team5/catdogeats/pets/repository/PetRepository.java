package com.team5.catdogeats.pets.repository;

import com.team5.catdogeats.pets.domain.Pets;
import com.team5.catdogeats.users.domain.mapping.Buyers;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

public interface PetRepository extends JpaRepository<Pets, String> {
    Page<Pets> findByBuyer(Buyers buyer, Pageable pageable);

    Optional<Pets> findById(String id);

    void deleteById(String id);

    // 커서가 없는 경우 (최초 요청)
    List<Pets> findByBuyerUserIdOrderByUpdatedAtDesc(String buyerId, Pageable pageable);

    // 커서가 있는 경우
    List<Pets> findByBuyerUserIdAndUpdatedAtLessThanOrderByUpdatedAtDesc(
            String buyerId, ZonedDateTime cursorUpdatedAt, Pageable pageable
    );
}
