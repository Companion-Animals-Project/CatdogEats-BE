package com.team5.catdogeats.users.repository;

import com.team5.catdogeats.admins.domain.dto.dashboard.DailyUserStatsDTO;
import com.team5.catdogeats.users.domain.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<Users, String> {
    Optional<Users> findByProviderAndProviderId(String provider, String providerId);

    Optional<Users> findById(String id);
    boolean existsById(String id);


    Users getReferenceById(String id);

    @Query("""
            SELECT u.name FROM Users u WHERE u.id = :id
        """)
    Optional<String> findNameById(@Param("id") String id);


    // 대시보드용 메서드 추가
    long countByCreatedAtAfter(ZonedDateTime date);

    long countByCreatedAtBetween(ZonedDateTime startDate, ZonedDateTime endDate);


    @Query("""
    SELECT new com.team5.catdogeats.admins.domain.dto.dashboard.DailyUserStatsDTO(
        CAST(u.createdAt AS date),
        COUNT(u.id)
    )
    FROM Users u
    WHERE u.createdAt >= :startDate
    GROUP BY CAST(u.createdAt AS date)
    ORDER BY CAST(u.createdAt AS date)
""")
    List<DailyUserStatsDTO> getDailyNewUsers(@Param("startDate") ZonedDateTime startDate);
}
