package md.drivestudio.drivestudio.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import md.drivestudio.drivestudio.entity.UploadedFile;
import md.drivestudio.drivestudio.repository.UploadedFileRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

@Service
@RequiredArgsConstructor
public class FileCleanupService {

    private final UploadedFileRepository fileRepository;

    @Value("${upload.directory}")
    private String uploadDirectory;

    // ✅ Rulează o dată pe zi la ora 02:00 dimineața
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void deleteExpiredAnonymousFiles() {
        Date cutoff = new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000); // 24h în milisecunde

        List<UploadedFile> expired = fileRepository.findAll().stream()
                .filter(f -> f.getUser() == null && f.getUploadDate().before(cutoff))
                .toList();

        int count = 0;

        for (UploadedFile file : expired) {
            try {
                Files.deleteIfExists(Paths.get(file.getPath()));
                fileRepository.delete(file);
                count++;
            } catch (IOException e) {
                System.err.println("❌ Eroare la ștergerea fișierului: " + file.getFilename());
            }
        }

        if (count > 0) {
            System.out.println("🧹 Fișiere anonime șterse automat: " + count);
        }
    }
}
