package md.drivestudio.drivestudio.controller;

import jakarta.servlet.http.HttpServletRequest;
import md.drivestudio.drivestudio.security.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.nio.file.*;

@RestController
@RequestMapping("/api/files")
public class FileUploadController {

    @Value("${upload.directory}")
    private String uploadBaseDir;

    private final JwtUtil jwtUtil;

    public FileUploadController(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file,
                                             HttpServletRequest request) {
        try {
            // 1. Extrage tokenul JWT din header
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(401).body("Missing or invalid Authorization header");
            }

            String token = authHeader.substring(7);
            String username = jwtUtil.extractUsername(token);

            // 2. Creează directorul pentru utilizator
            Path userUploadDir = Paths.get(uploadBaseDir, username);
            if (!Files.exists(userUploadDir)) {
                Files.createDirectories(userUploadDir);
            }

            // 3. Salvează fișierul în directorul utilizatorului
            String fileName = StringUtils.cleanPath(file.getOriginalFilename());
            Path filePath = userUploadDir.resolve(fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // 4. Construiește linkul de descărcare
            String fileUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/uploads/")
                    .path(username + "/")
                    .path(fileName)
                    .toUriString();

            return ResponseEntity.ok("File uploaded successfully: " + fileUrl);

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Upload failed: " + e.getMessage());
        }
    }
}
