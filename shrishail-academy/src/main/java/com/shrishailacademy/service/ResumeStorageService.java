package com.shrishailacademy.service;

import com.shrishailacademy.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;

/**
 * Service to handle resume file uploads for teacher applications.
 * Stores files on disk under a configurable directory.
 */
@Service
public class ResumeStorageService {

    private static final Logger log = LoggerFactory.getLogger(ResumeStorageService.class);

    private static final long DEFAULT_MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB
    private static final Set<String> DEFAULT_ALLOWED_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
    private static final Set<String> DEFAULT_ALLOWED_EXTENSIONS = Set.of("pdf", "doc", "docx");

    private final Path uploadDir;
    private final long maxFileSize;
    private final Set<String> allowedTypes;
    private final Set<String> allowedExtensions;

    public ResumeStorageService(
            @Value("${resume.upload.dir:uploads/resumes}") String uploadPath,
            @Value("${resume.upload.max-bytes:" + DEFAULT_MAX_FILE_SIZE + "}") long maxFileSize,
            @Value("${resume.upload.allowed-types:}") String allowedTypes,
            @Value("${resume.upload.allowed-extensions:}") String allowedExtensions) {
        this.uploadDir = Paths.get(uploadPath).toAbsolutePath().normalize();
        this.maxFileSize = maxFileSize > 0 ? maxFileSize : DEFAULT_MAX_FILE_SIZE;
        this.allowedTypes = parseCsvOrDefault(allowedTypes, DEFAULT_ALLOWED_TYPES);
        this.allowedExtensions = parseCsvOrDefault(allowedExtensions, DEFAULT_ALLOWED_EXTENSIONS);
        try {
            Files.createDirectories(this.uploadDir);
            log.info("Resume upload directory: {}", this.uploadDir);
        } catch (IOException e) {
            throw new RuntimeException("Could not create resume upload directory: " + this.uploadDir, e);
        }
    }

    /**
     * Store the uploaded resume file and return the stored file path.
     */
    public String[] storeResume(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        // Validate file size
        if (file.getSize() > maxFileSize) {
            throw new BusinessException("Resume file must be less than 5 MB", "FILE_TOO_LARGE");
        }

        // Validate content type
        String contentType = file.getContentType();
        if (contentType == null || !allowedTypes.contains(contentType)) {
            throw new BusinessException("Only PDF and Word documents are allowed", "INVALID_FILE_TYPE");
        }

        // Validate extension
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new BusinessException("File name is missing", "INVALID_FILE_NAME");
        }
        String extension = getExtension(originalFilename);
        if (!allowedExtensions.contains(extension.toLowerCase())) {
            throw new BusinessException("Only .pdf, .doc, .docx files are allowed", "INVALID_EXTENSION");
        }

        // Generate unique filename
        String storedName = UUID.randomUUID() + "." + extension;
        try {
            Path target = uploadDir.resolve(storedName).normalize();
            // Security: ensure target is within upload dir
            if (!target.startsWith(uploadDir)) {
                throw new BusinessException("Invalid file path", "INVALID_PATH");
            }
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            log.info("RESUME_UPLOADED: original='{}' stored='{}'", originalFilename, storedName);
            return new String[] { originalFilename, target.toString() };
        } catch (IOException e) {
            log.error("Failed to store resume: {}", e.getMessage());
            throw new BusinessException("Failed to upload resume. Please try again.", "UPLOAD_FAILED");
        }
    }

    private String getExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex >= 0 ? filename.substring(dotIndex + 1) : "";
    }

    private Set<String> parseCsvOrDefault(String csv, Set<String> defaultSet) {
        if (csv == null || csv.isBlank()) {
            return defaultSet;
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }
}
