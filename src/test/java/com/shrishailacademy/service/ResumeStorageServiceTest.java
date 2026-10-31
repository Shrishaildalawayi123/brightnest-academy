package com.shrishailacademy.service;

import com.shrishailacademy.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ResumeStorageServiceTest {
    @TempDir Path temp;

    private MockMultipartFile pdf() {
        return new MockMultipartFile("resume", "resume.pdf", "application/pdf", "%PDF-test".getBytes());
    }

    @Test
    void s3ModeWorksWhenLocalPathCannotBeADirectory() throws Exception {
        Path file = Files.writeString(temp.resolve("not-a-directory"), "existing");
        S3Client client = mock(S3Client.class);
        var service = new ResumeStorageService(file.resolve("resumes").toString(), 1024, "", "",
                true, "test-bucket", "resumes", () -> client);
        String[] result = service.storeResume(pdf());
        assertTrue(result[1].startsWith("s3://test-bucket/resumes/"));
        verify(client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        assertEquals("existing", Files.readString(file));
    }

    @Test
    void failedS3UploadNeverFallsBackToLocalDisk() {
        S3Client client = mock(S3Client.class);
        when(client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(new IllegalStateException("storage unavailable"));
        Path local = temp.resolve("ephemeral");
        var service = new ResumeStorageService(local.toString(), 1024, "", "",
                true, "test-bucket", "resumes", () -> client);
        assertThrows(BusinessException.class, () -> service.storeResume(pdf()));
        assertFalse(Files.exists(local));
    }

    @Test
    void missingBucketFailsBeforeCreatingFilesOrClient() {
        assertThrows(IllegalStateException.class, () -> new ResumeStorageService(temp.resolve("unused").toString(),
                1024, "", "", true, "", "resumes", () -> { fail("Client must not be created"); return null; }));
        assertFalse(Files.exists(temp.resolve("unused")));
    }

    @Test
    void explicitLocalModeStillStoresFilesForDevelopment() throws Exception {
        Path local = temp.resolve("resumes");
        var service = new ResumeStorageService(local.toString(), 1024, "", "",
                false, "", "resumes", () -> { fail("S3 must not be created"); return null; });
        String[] result = service.storeResume(pdf());
        assertArrayEquals(pdf().getBytes(), Files.readAllBytes(Path.of(result[1])));
    }
}
