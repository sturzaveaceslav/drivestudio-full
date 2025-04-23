package md.drivestudio.drivestudio.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import md.drivestudio.drivestudio.dto.AdminGalleryWithFolderDTO;
import md.drivestudio.drivestudio.entity.UploadedFile;
import md.drivestudio.drivestudio.model.User;
import md.drivestudio.drivestudio.repository.UploadedFileRepository;
import md.drivestudio.drivestudio.repository.UserRepository;
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
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UploadedFileRepository fileRepository;
    private final UserRepository userRepository;

    @Value("${upload.directory}")
    private String uploadDirectory;

    @GetMapping("/galleries")
    public ResponseEntity<?> getAllGalleries(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null || !user.getRole().name().equals("ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Nu ai acces ADMIN.");
        }

        List<AdminGalleryWithFolderDTO> result = fileRepository.findAll().stream()
                .collect(Collectors.groupingBy(UploadedFile::getGalleryId))
                .entrySet().stream()
                .map(entry -> {
                    List<UploadedFile> files = entry.getValue();
                    UploadedFile first = files.get(0);
                    String uploader = (first.getUser() != null)
                            ? first.getUser().getUsername()
                            : "anonim";
                    String folderName = first.getFolderName() != null ? first.getFolderName() : "(fără nume)";
                    return new AdminGalleryWithFolderDTO(
                            entry.getKey(),
                            first.getUploadDate(),
                            uploader,
                            files.size(),
                            folderName
                    );
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/galleries/{galleryId}")
    public ResponseEntity<String> deleteGallery(@PathVariable String galleryId, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null || !user.getRole().name().equals("ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Nu ai acces ADMIN.");
        }

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
    public ResponseEntity<Resource> downloadGalleryZip(@PathVariable String galleryId, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null || !user.getRole().name().equals("ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<UploadedFile> files = fileRepository.findByGalleryId(galleryId);
        if (files.isEmpty()) return ResponseEntity.notFound().build();

        try {
            Path zipPath = Files.createTempFile("gallery_" + galleryId + "_", ".zip");

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

    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null || !user.getRole().name().equals("ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Nu ai acces ADMIN.");
        }

        List<Map<String, Object>> users = new ArrayList<>();
        userRepository.findAll().forEach(u -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", u.getId());
            map.put("username", u.getUsername());
            map.put("role", u.getRole().name());
            map.put("maxUploadSize", u.getMaxUploadSize());
            users.add(map);
        });
        return ResponseEntity.ok(users);
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<String> updateLimit(@PathVariable Long id, @RequestBody Map<String, Object> body, HttpSession session) {
        User admin = (User) session.getAttribute("user");
        if (admin == null || !admin.getRole().name().equals("ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Nu ai acces ADMIN.");
        }

        Optional<User> optional = userRepository.findById(id);
        if (optional.isEmpty()) return ResponseEntity.notFound().build();

        User user = optional.get();
        long newLimit = Long.parseLong(body.get("maxUploadSize").toString());
        user.setMaxUploadSize(newLimit);
        userRepository.save(user);

        return ResponseEntity.ok("✅ Limită actualizată");
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<String> deleteUser(@PathVariable Long id, HttpSession session) {
        User admin = (User) session.getAttribute("user");
        if (admin == null || !admin.getRole().name().equals("ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Nu ai acces ADMIN.");
        }

        Optional<User> optional = userRepository.findById(id);
        if (optional.isEmpty()) return ResponseEntity.notFound().build();

        User user = optional.get();
        List<UploadedFile> files = fileRepository.findByUser(user);

        files.forEach(file -> {
            try {
                Files.deleteIfExists(Paths.get(file.getPath()));
            } catch (IOException ignored) {}
        });

        fileRepository.deleteAll(files);
        userRepository.delete(user);

        return ResponseEntity.ok("✅ Utilizator șters");
    }

    @GetMapping("/check-session")
    public ResponseEntity<String> checkSession(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null || !user.getRole().name().equals("ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("❌ Nu ești logat sau nu ai drepturi admin");
        }
        return ResponseEntity.ok("✅ Ești logat ca admin");
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFiles(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam("customName") String customName,
            @RequestParam(value = "targetUserId", required = false) Long targetUserId,
            HttpSession session
    ) {
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null || !currentUser.getRole().name().equals("ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Nu ai acces ADMIN.");
        }

        User uploader = currentUser;
        if (targetUserId != null) {
            Optional<User> optional = userRepository.findById(targetUserId);
            if (optional.isPresent()) {
                uploader = optional.get();
            }
        }

        String galleryId = UUID.randomUUID().toString().substring(0, 8);
        for (MultipartFile file : files) {
            try {
                String filename = file.getOriginalFilename();
                Path path = Paths.get(uploadDirectory, galleryId + "_" + filename);
                Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

                UploadedFile uploaded = new UploadedFile();
                uploaded.setUser(uploader);
                uploaded.setGalleryId(galleryId);
                uploaded.setFolderName(customName);
                uploaded.setFilename(filename);
                uploaded.setFileType(file.getContentType());
                uploaded.setPath(path.toString());
                uploaded.setSize(file.getSize());
                uploaded.setUploadDate(new Date());
                uploaded.setUniqueId(UUID.randomUUID().toString().substring(0, 8)); // 🔥 FIX crucial aici

                fileRepository.save(uploaded);

            } catch (IOException e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Eroare la salvare: " + e.getMessage());
            }
        }

        return ResponseEntity.ok("Galerie încărcată cu succes");
    }
}
