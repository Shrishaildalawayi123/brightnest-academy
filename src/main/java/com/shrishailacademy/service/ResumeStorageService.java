package com.shrishailacademy.service;

import com.shrishailacademy.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

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
    private final boolean s3Enabled;
    private final String s3Bucket;
    private final String s3Prefix;
    private final S3Client s3Client;

    public ResumeStorageService(
            @Value("${resume.upload.dir:uploads/resumes}") String uploadPath,
            @Value("${resume.upload.max-bytes:" + DEFAULT_MAX_FILE_SIZE + "}") long maxFileSize,
            @Value("${resume.upload.allowed-types:}") String allowedTypes,
            @Value("${resume.upload.allowed-extensions:}") String allowedExtensions,
            @Value("${resume.storage.s3.enabled:false}") boolean s3Enabled,
            @Value("${resume.storage.s3.bucket:}") String s3Bucket,
            @Value("${resume.storage.s3.prefix:resumes}") String s3Prefix,
            @Value("${resume.storage.s3.region:ap-south-1}") String s3Region) {
        this.uploadDir = Paths.get(uploadPath).toAbsolutePath().normalize();
        this.maxFileSize = maxFileSize > 0 ? maxFileSize : DEFAULT_MAX_FILE_SIZE;
        this.allowedTypes = parseCsvOrDefault(allowedTypes, DEFAULT_ALLOWED_TYPES);
        this.allowedExtensions = parseCsvOrDefault(allowedExtensions, DEFAULT_ALLOWED_EXTENSIONS);
        this.s3Enabled = s3Enabled;
        this.s3Bucket = s3Bucket == null ? "" : s3Bucket.trim();
        this.s3Prefix = (s3Prefix == null || s3Prefix.isBlank()) ? "resumes" : s3Prefix.trim();
        this.s3Client = initializeS3Client(s3Region);
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

        if (s3Enabled && !s3Bucket.isBlank() && s3Client != null) {
            String key = s3Prefix + "/" + storedName;
            try {
                PutObjectRequest request = PutObjectRequest.builder()
                        .bucket(s3Bucket)
                        .key(key)
                        .contentType(contentType)
                        .build();
                s3Client.putObject(request, RequestBody.fromBytes(file.getBytes()));
                log.info("RESUME_UPLOADED_S3: original='{}' key='s3://{}/{}'", originalFilename, s3Bucket, key);
                return new String[] { originalFilename, "s3://" + s3Bucket + "/" + key };
            } catch (Exception ex) {
                log.warn("S3 resume upload failed; falling back to local storage. reason={}", ex.getMessage());
            }
        }

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

    private S3Client initializeS3Client(String s3Region) {
        if (!s3Enabled) {
            return null;
        }
        try {
            Region region = Region.of((s3Region == null || s3Region.isBlank()) ? "ap-south-1" : s3Region.trim());
            S3Client client = S3Client.builder()
                    .region(region)
                    .credentialsProvider(DefaultCredentialsProvider.create())
                    .build();
            log.info("Resume storage S3 mode enabled for region={} bucket={}", region.id(), s3Bucket);
            return client;
        } catch (Exception ex) {
            log.warn("Unable to initialize S3 client; local fallback remains active. reason={}", ex.getMessage());
            return null;
        }
    }
}
