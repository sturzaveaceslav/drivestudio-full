package md.drivestudio.drivestudio.controller;

import jakarta.servlet.http.HttpSession;
import md.drivestudio.drivestudio.dto.UserStatsDTO;
import md.drivestudio.drivestudio.entity.UploadedFile;
import md.drivestudio.drivestudio.model.User;
import md.drivestudio.drivestudio.repository.UploadedFileRepository;
import md.drivestudio.drivestudio.repository.UserRepository;
import md.drivestudio.drivestudio.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@CrossOrigin
public class FileUploadController {

    @Value("${upload.directory}")
    private String uploadDirectory;

    private final UploadedFileRepository fileRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    public FileUploadController(UploadedFileRepository fileRepository, UserService userService, UserRepository userRepository) {
        this.fileRepository = fileRepository;
        this.userService = userService;
        this.userRepository = userRepository;
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadFiles(@RequestParam("files") MultipartFile[] files,
                                              @RequestParam(value = "customName", required = false) String customName,
                                              HttpSession session,
                                              @RequestParam(value = "targetUserId", required = false) Long targetUserId) {
        User user = (User) session.getAttribute("user");

        long totalSize = Arrays.stream(files).filter(f -> !f.isEmpty()).mapToLong(MultipartFile::getSize).sum();

        if (user == null && totalSize > 500 * 1024 * 1024) {
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body("⚠️ Anonimii pot încărca max. 500MB.");
        }

        if (user != null && user.getRole().name().equals("USER")) {
            long current = fileRepository.findByUser(user).stream().mapToLong(UploadedFile::getSize).sum();
            if (current + totalSize > user.getMaxUploadSize()) {
                return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body("⚠️ Ai depășit limita. Șterge fișiere.");
            }
        }

        String galleryId = UUID.randomUUID().toString().substring(0, 8);

        try {
            Path uploadPath = Paths.get(uploadDirectory);
            if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);

            for (MultipartFile file : files) {
                if (file.isEmpty()) continue;

                String uniqueId = UUID.randomUUID().toString().substring(0, 6);
                String originalName = file.getOriginalFilename();

                // ✅ Extragem extensia
                String extension = (originalName != null && originalName.contains("."))
                        ? originalName.substring(originalName.lastIndexOf("."))
                        : "";

                // ✅ Adăugăm extensia dacă lipsește
                String baseName = customName != null && !customName.isBlank() ? customName : originalName;
                String finalName = uniqueId + "_" + baseName;
                if (!finalName.endsWith(extension)) {
                    finalName += extension;
                }

                Path path = uploadPath.resolve(finalName);
                Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

                String fileType = file.getContentType();
                if (fileType == null || fileType.isBlank()) {
                    fileType = Files.probeContentType(path);
                }
                if (fileType == null || fileType.isBlank()) {
                    fileType = "application/octet-stream";
                }

                UploadedFile uf = new UploadedFile();
                uf.setUniqueId(uniqueId);
                uf.setFilename(finalName);
                uf.setPath(path.toString());
                uf.setFileType(fileType);
                uf.setSize(file.getSize());
                uf.setUploadDate(new Date());
                uf.setGalleryId(galleryId);
                uf.setFolderName(customName);
                if (user != null) uf.setUser(user);

                fileRepository.save(uf);
            }

            return ResponseEntity.ok(galleryId);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Eroare: " + e.getMessage());
        }
    }

    @GetMapping("/s/{id}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String id) {
        Optional<UploadedFile> fileOpt = fileRepository.findByUniqueId(id);
        if (fileOpt.isEmpty()) return ResponseEntity.notFound().build();

        UploadedFile file = fileOpt.get();
        try {
            Path path = Paths.get(file.getPath());
            Resource res = new UrlResource(path.toUri());
            if (!res.exists()) return ResponseEntity.notFound().build();

            String mime = Files.probeContentType(path);
            MediaType mediaType = (mime != null) ? MediaType.parseMediaType(mime) : MediaType.APPLICATION_OCTET_STREAM;

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            mediaType.equals(MediaType.APPLICATION_OCTET_STREAM)
                                    ? "attachment; filename=\"" + file.getFilename() + "\""
                                    : "inline; filename=\"" + file.getFilename() + "\"")
                    .body(res);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/api/user/stats")
    public ResponseEntity<?> getUserStats(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Neautorizat");
        }

        List<UploadedFile> files = fileRepository.findByUser(user);
        if (files.isEmpty()) {
            return ResponseEntity.ok(new UserStatsDTO(0, 0, "—", "—"));
        }

        files.sort(Comparator.comparing(UploadedFile::getUploadDate).reversed());
        UploadedFile last = files.get(0);
        return ResponseEntity.ok(new UserStatsDTO(
                files.size(),
                files.stream().mapToLong(UploadedFile::getSize).sum(),
                last.getFilename(),
                last.getUploadDate().toString()
        ));
    }


    @GetMapping("/gallery/{galleryId}")
    public ResponseEntity<List<UploadedFile>> getFilesByGallery(@PathVariable String galleryId) {
        List<UploadedFile> files = fileRepository.findByGalleryId(galleryId);
        return files.isEmpty() ? ResponseEntity.notFound().build() : ResponseEntity.ok(files);
    }

    @GetMapping("/gallery/{galleryId}/download-zip")
    public ResponseEntity<Resource> downloadGalleryZip(@PathVariable String galleryId) {
        List<UploadedFile> files = fileRepository.findByGalleryId(galleryId);
        if (files.isEmpty()) return ResponseEntity.notFound().build();

        try {
            Path zipPath = Files.createTempFile("galerie_" + galleryId + "_", ".zip");

            try (OutputStream os = Files.newOutputStream(zipPath);
                 ZipOutputStream zos = new ZipOutputStream(os)) {
                for (UploadedFile file : files) {
                    Path filePath = Paths.get(file.getPath());
                    if (Files.exists(filePath)) {
                        zos.putNextEntry(new ZipEntry(file.getFilename()));
                        Files.copy(filePath, zos);
                        zos.closeEntry();
                    }
                }
            }

            Resource resource = new UrlResource(zipPath.toUri());
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"galerie_" + galleryId + ".zip\"")
                    .body(resource);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
