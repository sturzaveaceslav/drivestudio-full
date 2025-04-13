package md.drivestudio.drivestudio.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

@RestController
public class FileUploadController {

    @Value("${upload.directory}")
    private String uploadDirectory;

    // 🔼 Upload fișier — returnează doar ID-ul unic (ex: "a1b2c3")
    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            Path uploadPath = Paths.get(uploadDirectory);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String uniqueId = UUID.randomUUID().toString().substring(0, 6);
            String originalFilename = file.getOriginalFilename();
            String storedFilename = uniqueId + "_" + originalFilename;

            Path filePath = uploadPath.resolve(storedFilename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // 🔁 Returnăm doar ID-ul pentru a genera frontend link-ul: /download.html?id=ID
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(uniqueId);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Eroare la încărcare: " + e.getMessage());
        }
    }

    // 🔽 Descărcare publică pe bază de ID — /s/{id}
    @GetMapping("/s/{id}")
    public ResponseEntity<Resource> downloadFile(@PathVariable("id") String id) {
        try {
            Path folder = Paths.get(uploadDirectory);
            Optional<Path> file = Files.list(folder)
                    .filter(f -> f.getFileName().toString().startsWith(id + "_"))
                    .findFirst();

            if (file.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }

            Path filePath = file.get();
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }

            String storedFilename = filePath.getFileName().toString();
            String originalFilename = storedFilename.substring(id.length() + 1);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + originalFilename + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // 🔍 HEAD request pentru preview (verificăm tipul fișierului)
    @RequestMapping(value = "/s/{id}", method = RequestMethod.HEAD)
    public ResponseEntity<Void> checkFileType(@PathVariable("id") String id) {
        try {
            Path folder = Paths.get(uploadDirectory);
            Optional<Path> file = Files.list(folder)
                    .filter(f -> f.getFileName().toString().startsWith(id + "_"))
                    .findFirst();

            if (file.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Path filePath = file.get();
            String mimeType = Files.probeContentType(filePath);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(mimeType != null ? mimeType : "application/octet-stream"))
                    .build();

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
