package com.shrishailacademy.service;

import com.shrishailacademy.model.SiteVisit;
import com.shrishailacademy.repository.SiteVisitRepository;
import com.shrishailacademy.util.InputSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Visitor Analytics Service - Tracks and reports site traffic.
 */
@Service
public class VisitorAnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(VisitorAnalyticsService.class);

    private final SiteVisitRepository siteVisitRepository;

    public VisitorAnalyticsService(SiteVisitRepository siteVisitRepository) {
        this.siteVisitRepository = siteVisitRepository;
    }

    /**
     * Record a page visit asynchronously.
     */
    @Async
    @Transactional
    public void recordVisit(String sessionId, String pageUrl, String referrer,
            String ipAddress, String userAgent) {
        try {
            SiteVisit visit = new SiteVisit();
            visit.setSessionId(InputSanitizer.sanitizeAndTruncate(sessionId, 100));
            visit.setPageUrl(InputSanitizer.sanitizeAndTruncate(pageUrl, 500));
            visit.setReferrer(InputSanitizer.sanitizeAndTruncateNullable(referrer, 500));
            visit.setIpAddress(InputSanitizer.sanitizeAndTruncateNullable(ipAddress, 45));
            visit.setUserAgent(InputSanitizer.sanitizeAndTruncateNullable(userAgent, 500));
            siteVisitRepository.save(visit);
        } catch (Exception e) {
            log.error("Failed to record site visit: {}", e.getMessage());
        }
    }

    /**
     * Get analytics summary for the admin dashboard.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getAnalyticsSummary() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime last24h = now.minusHours(24);
        LocalDateTime last7d = now.minusDays(7);
        LocalDateTime last30d = now.minusDays(30);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalPageViews24h", siteVisitRepository.countByVisitedAtAfter(last24h));
        summary.put("uniqueVisitors24h", siteVisitRepository.countUniqueSessionsAfter(last24h));
        summary.put("totalPageViews7d", siteVisitRepository.countByVisitedAtAfter(last7d));
        summary.put("uniqueVisitors7d", siteVisitRepository.countUniqueSessionsAfter(last7d));
        summary.put("totalPageViews30d", siteVisitRepository.countByVisitedAtAfter(last30d));
        summary.put("uniqueVisitors30d", siteVisitRepository.countUniqueSessionsAfter(last30d));

        // Top 10 pages in last 7 days
        List<Object[]> topPages = siteVisitRepository.findTopPagesSince(last7d);
        Map<String, Long> topPagesMap = new LinkedHashMap<>();
        for (Object[] row : topPages) {
            if (topPagesMap.size() >= 10)
                break;
            topPagesMap.put((String) row[0], (Long) row[1]);
        }
        summary.put("topPages7d", topPagesMap);

        return summary;
    }
}
