package ru.georgdeveloper.assistantcore.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class DiagnosticsFileStorageService {

    private static final Logger log = LoggerFactory.getLogger(DiagnosticsFileStorageService.class);
    private static final String DIAGNOSTICS_SUBDIR = "diagnostics";

    @Value("${files.upload-dir:./uploads}")
    private String uploadDirProperty;

    private Path uploadDir;

    @PostConstruct
    void init() throws IOException {
        uploadDir = resolveUploadDir();
        Files.createDirectories(uploadDir);
        log.info("Diagnostics upload directory: {}", uploadDir);
    }

    public Path getUploadDir() {
        return uploadDir;
    }

    public String saveFile(MultipartFile file) throws IOException {
        String safeName = sanitizeFileName(file.getOriginalFilename());
        String storedName = UUID.randomUUID() + "_" + safeName;
        Path target = uploadDir.resolve(storedName);
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
        }
        log.info("Diagnostics file saved: {}", target);
        return "/uploads/diagnostics/" + storedName;
    }

    public Path resolveStoredFile(String storedPath) {
        if (storedPath == null || storedPath.isBlank()) {
            return null;
        }

        String relativeName = storedPath;
        if (relativeName.startsWith("/uploads/diagnostics/")) {
            relativeName = relativeName.substring("/uploads/diagnostics/".length());
        } else if (relativeName.startsWith("uploads/diagnostics/")) {
            relativeName = relativeName.substring("uploads/diagnostics/".length());
        }

        for (Path root : getSearchRoots()) {
            Path candidate = root.resolve(relativeName).normalize();
            if (candidate.startsWith(root) && Files.isRegularFile(candidate)) {
                return candidate;
            }
        }

        return null;
    }

    private Path[] getSearchRoots() {
        Path currentDir = Paths.get(System.getProperty("user.dir")).normalize();
        return new Path[] {
                uploadDir,
                Paths.get(uploadDirProperty, DIAGNOSTICS_SUBDIR).normalize().toAbsolutePath(),
                currentDir.resolve("assistant-core").resolve("uploads").resolve(DIAGNOSTICS_SUBDIR).normalize(),
                Paths.get("assistant-core", "uploads", DIAGNOSTICS_SUBDIR).normalize().toAbsolutePath()
        };
    }

    public void deleteStoredFile(String storedPath) throws IOException {
        Path file = resolveStoredFile(storedPath);
        if (file != null && Files.exists(file)) {
            Files.delete(file);
        }
    }

    private Path resolveUploadDir() {
        Path configured = Paths.get(uploadDirProperty, DIAGNOSTICS_SUBDIR);
        if (!configured.isAbsolute()) {
            configured = configured.toAbsolutePath().normalize();
        }
        return configured;
    }

    private String sanitizeFileName(String originalFileName) {
        if (originalFileName == null || originalFileName.isBlank()) {
            return "file";
        }
        String name = Paths.get(originalFileName).getFileName().toString().trim();
        return name.isEmpty() ? "file" : name;
    }
}
