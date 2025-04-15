package md.drivestudio.drivestudio.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import md.drivestudio.drivestudio.entity.UploadedFile;
import md.drivestudio.drivestudio.repository.UploadedFileRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class ScheduledCleanupService {

    private final UploadedFileRepository fileRepository;

    @Value("${upload.directory}")
    private String uploadDirectory;

    @Scheduled(cron = "0 0 3 * * *") // zilnic la ora 03:00
    @Transactional
    public void cleanupOldAnonymousFiles() {
        Date threshold = new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(3));

        List<UploadedFile> oldFiles = fileRepository.findAll().stream()
                .filter(f -> f.getUser() == null && f.getUploadDate().before(threshold))
                .toList();

        oldFiles.forEach(f -> {
            try {
                Files.deleteIfExists(Paths.get(f.getPath()));
            } catch (Exception ignored) {}
        });

        fileRepository.deleteAll(oldFiles);
        System.out.println("✅ Curățat fișiere anonime mai vechi de 3 zile: " + oldFiles.size());
    }
}
