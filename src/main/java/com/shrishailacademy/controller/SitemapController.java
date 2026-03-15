package com.shrishailacademy.controller;

import com.shrishailacademy.model.BlogPost;
import com.shrishailacademy.repository.BlogPostRepository;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@RestController
public class SitemapController {

    private static final String BASE_URL = "https://brightnest-academy.com";
    private static final Set<String> EXCLUDED_STATIC_PAGES = Set.of(
            "404.html",
            "admin-dashboard.html",
            "blog-post.html",
            "login.html",
            "manage-assignments.html",
            "manage-schedules.html",
            "manage-sessions.html",
            "qrcode.html",
            "register.html",
            "student-dashboard.html");

    private final BlogPostRepository blogPostRepository;
    private final PathMatchingResourcePatternResolver resourcePatternResolver =
            new PathMatchingResourcePatternResolver();

    public SitemapController(BlogPostRepository blogPostRepository) {
        this.blogPostRepository = blogPostRepository;
    }

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> sitemap() throws IOException {
        List<SitemapUrl> urls = new ArrayList<>();
        urls.addAll(getStaticPageUrls());
        urls.addAll(getBlogUrls());
        urls.sort(Comparator.comparing(SitemapUrl::loc));

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");
        for (SitemapUrl url : urls) {
            xml.append("  <url>\n");
            xml.append("    <loc>").append(escapeXml(url.loc())).append("</loc>\n");
            xml.append("    <lastmod>").append(url.lastModified()).append("</lastmod>\n");
            xml.append("    <changefreq>").append(url.changeFrequency()).append("</changefreq>\n");
            xml.append("    <priority>").append(url.priority()).append("</priority>\n");
            xml.append("  </url>\n");
        }
        xml.append("</urlset>\n");

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .body(xml.toString());
    }

    private List<SitemapUrl> getStaticPageUrls() throws IOException {
        List<SitemapUrl> urls = new ArrayList<>();
        Resource[] resources = resourcePatternResolver.getResources("classpath:/static/*.html");
        for (Resource resource : resources) {
            String filename = resource.getFilename();
            if (filename == null || EXCLUDED_STATIC_PAGES.contains(filename)) {
                continue;
            }

            String loc = switch (filename) {
                case "index.html" -> BASE_URL + "/";
                case "tuition-classes-in-bangalore.html" -> BASE_URL + "/tuition-classes-in-bangalore";
                default -> BASE_URL + "/" + filename;
            };

            LocalDate lastModified = resolveLastModified(resource);
            String priority = "index.html".equals(filename) ? "1.0"
                    : "tuition-classes-in-bangalore.html".equals(filename) ? "0.95" : "0.8";
            String changeFrequency = "blog.html".equals(filename) ? "weekly" : "monthly";
            urls.add(new SitemapUrl(loc, lastModified, changeFrequency, priority));
        }
        return urls;
    }

    private List<SitemapUrl> getBlogUrls() {
        List<SitemapUrl> urls = new ArrayList<>();
        for (BlogPost post : blogPostRepository.findByPublishedTrueOrderByPublishedAtDesc()) {
            LocalDate lastModified = post.getPublishedAt() != null
                    ? post.getPublishedAt().toLocalDate()
                    : LocalDate.now(ZoneOffset.UTC);
            String encodedSlug = URLEncoder.encode(post.getSlug(), StandardCharsets.UTF_8);
            urls.add(new SitemapUrl(
                    BASE_URL + "/blog-post.html?slug=" + encodedSlug,
                    lastModified,
                    "monthly",
                    "0.7"));
        }
        return urls;
    }

    private LocalDate resolveLastModified(Resource resource) {
        try {
            return Instant.ofEpochMilli(resource.lastModified()).atZone(ZoneOffset.UTC).toLocalDate();
        } catch (IOException ex) {
            return LocalDate.now(ZoneOffset.UTC);
        }
    }

    private String escapeXml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private record SitemapUrl(String loc, LocalDate lastModified, String changeFrequency, String priority) {
    }
}
