package md.drivestudio.drivestudio.controller;

import lombok.RequiredArgsConstructor;
import md.drivestudio.drivestudio.dto.AdminGalleryDTO;
import md.drivestudio.drivestudio.entity.UploadedFile;
import md.drivestudio.drivestudio.repository.UploadedFileRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UploadedFileRepository fileRepository;

    @Value("${upload.directory}")
    private String uploadDirectory;

    @GetMapping("/galleries")
    public List<AdminGalleryDTO> getAllGalleries() {
        return fileRepository.findAll().stream()
                .collect(Collectors.groupingBy(UploadedFile::getGalleryId))
                .entrySet().stream()
                .map(entry -> {
                    List<UploadedFile> files = entry.getValue();
                    UploadedFile first = files.get(0);
                    String uploader = (first.getUser() != null)
                            ? first.getUser().getUsername()
                            : "anonim";
                    return new AdminGalleryDTO(
                            entry.getKey(),
                            first.getUploadDate(),
                            uploader,
                            files.size()
                    );
                })
                .collect(Collectors.toList());
    }

    @DeleteMapping("/galleries/{galleryId}")
    public ResponseEntity<String> deleteGallery(@PathVariable String galleryId) {
        List<UploadedFile> files = fileRepository.findByGalleryId(galleryId);
        if (files.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        files.forEach(file -> {
            try {
                Files.deleteIfExists(Paths.get(file.getPath()));
            } catch (IOException ignored) {}
        });

        fileRepository.deleteAll(files);
        return ResponseEntity.ok("Galerie ștearsă: " + galleryId);
    }

    @GetMapping("/galleries/{galleryId}/download-zip")
    public ResponseEntity<Resource> downloadZip(@PathVariable String galleryId) {
        try {
            List<UploadedFile> files = fileRepository.findByGalleryId(galleryId);
            if (files.isEmpty()) return ResponseEntity.notFound().build();

            Path zipPath = Files.createTempFile("gallery_" + galleryId + "_", ".zip");

            try (FileSystem zipFs = FileSystems.newFileSystem(zipPath, (ClassLoader) null)) {
                for (UploadedFile file : files) {
                    Path filePath = Paths.get(file.getPath());
                    if (Files.exists(filePath)) {
                        Path pathInZip = zipFs.getPath("/" + file.getFilename());
                        Files.copy(filePath, pathInZip, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }

            Resource resource = new UrlResource(zipPath.toUri());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + galleryId + ".zip\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}
