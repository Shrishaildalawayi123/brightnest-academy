package com.shrishailacademy.repository;

import com.shrishailacademy.model.SiteVisit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Site Visit Repository - Data access for visitor analytics.
 */
@Repository
public interface SiteVisitRepository extends JpaRepository<SiteVisit, Long> {

    long countByVisitedAtAfter(LocalDateTime after);

    @Query("SELECT COUNT(DISTINCT sv.sessionId) FROM SiteVisit sv WHERE sv.visitedAt > :after")
    long countUniqueSessionsAfter(@Param("after") LocalDateTime after);

    @Query("SELECT sv.pageUrl, COUNT(sv) as cnt FROM SiteVisit sv " +
            "WHERE sv.visitedAt > :after GROUP BY sv.pageUrl ORDER BY cnt DESC")
    List<Object[]> findTopPagesSince(@Param("after") LocalDateTime after);

    @Query("SELECT COUNT(DISTINCT sv.sessionId) FROM SiteVisit sv " +
            "WHERE sv.visitedAt BETWEEN :start AND :end")
    long countUniqueSessionsBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
