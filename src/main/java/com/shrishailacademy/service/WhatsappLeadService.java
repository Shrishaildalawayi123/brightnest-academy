package com.shrishailacademy.service;

import com.shrishailacademy.dto.WhatsappLeadRequest;
import com.shrishailacademy.model.WhatsappLead;
import com.shrishailacademy.repository.WhatsappLeadRepository;
import com.shrishailacademy.util.InputSanitizer;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.util.Locale;

/**
 * Stores WhatsApp click-throughs for marketing attribution.
 */
@Service
@Transactional(readOnly = true)
public class WhatsappLeadService {

    private final WhatsappLeadRepository whatsappLeadRepository;

    public WhatsappLeadService(WhatsappLeadRepository whatsappLeadRepository) {
        this.whatsappLeadRepository = whatsappLeadRepository;
    }

    @Transactional
    public WhatsappLead captureLead(WhatsappLeadRequest request, HttpServletRequest httpServletRequest) {
        WhatsappLead lead = new WhatsappLead();
        lead.setSourcePage(resolveSourcePage(request, httpServletRequest));
        lead.setDeviceType(detectDeviceType(httpServletRequest.getHeader("User-Agent")));
        return whatsappLeadRepository.save(lead);
    }

    private String resolveSourcePage(WhatsappLeadRequest request, HttpServletRequest httpServletRequest) {
        String sourcePage = request == null ? null : request.getSourcePage();
        String sanitized = InputSanitizer.sanitizeAndTruncateNullable(sourcePage, 200);
        if (sanitized != null) {
            return normalizePath(sanitized);
        }

        String referer = httpServletRequest.getHeader("Referer");
        if (StringUtils.hasText(referer)) {
            try {
                URI refererUri = URI.create(referer);
                if (StringUtils.hasText(refererUri.getPath())) {
                    return normalizePath(refererUri.getPath());
                }
            } catch (IllegalArgumentException ignored) {
                // Fall through to unknown when referer is malformed.
            }
        }

        return "/unknown";
    }

    private String normalizePath(String rawPath) {
        String path = rawPath.trim();
        if (path.startsWith("http://") || path.startsWith("https://")) {
            try {
                path = URI.create(path).getPath();
            } catch (IllegalArgumentException ignored) {
                path = "/unknown";
            }
        }

        if (!path.startsWith("/")) {
            path = "/" + path;
        }

        return path.length() > 200 ? path.substring(0, 200) : path;
    }

    private WhatsappLead.DeviceType detectDeviceType(String userAgent) {
        String normalized = userAgent == null ? "" : userAgent.toLowerCase(Locale.ROOT);
        if (normalized.contains("ipad")
                || normalized.contains("tablet")
                || normalized.contains("kindle")
                || normalized.contains("sm-t")) {
            return WhatsappLead.DeviceType.TABLET;
        }
        if (normalized.contains("mobi")
                || normalized.contains("android")
                || normalized.contains("iphone")) {
            return WhatsappLead.DeviceType.MOBILE;
        }
        return WhatsappLead.DeviceType.DESKTOP;
    }
}
