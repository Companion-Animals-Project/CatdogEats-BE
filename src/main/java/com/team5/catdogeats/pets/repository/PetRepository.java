package com.team5.catdogeats.pets.repository;

import com.team5.catdogeats.pets.domain.Pets;
import com.team5.catdogeats.users.domain.mapping.Buyers;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

public interface PetRepository extends JpaRepository<Pets, String> {
    Page<Pets> findByBuyer(Buyers buyer, Pageable pageable);

    Optional<Pets> findById(String id);
    @Query("""
        SELECT p
        FROM Pets p
        JOIN p.buyer b
        JOIN b.user u
        WHERE u.provider =:provider AND u.providerId =:providerId AND p.id =:id
    """)
    Optional<Pets> findByProviderAndProviderId(@Param("provider") String provider, @Param("providerId") String providerId, @Param("id") String id);

    void deleteById(String id);

    // 커서가 없는 경우 (최초 요청)
    List<Pets> findByBuyerUserIdOrderByUpdatedAtDesc(String buyerId, Pageable pageable);

    // 커서가 있는 경우
    List<Pets> findByBuyerUserIdAndUpdatedAtLessThanOrderByUpdatedAtDesc(
            String buyerId, ZonedDateTime cursorUpdatedAt, Pageable pageable
    );
}
