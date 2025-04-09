package md.drivestudio.drivestudio.controller;

import jakarta.servlet.http.HttpServletRequest;
import md.drivestudio.drivestudio.security.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.*;

@RestController
@RequestMapping("/api/files")
public class FileDeleteController {

    @Value("${upload.directory}")
    private String uploadBaseDir;

    private final JwtUtil jwtUtil;

    public FileDeleteController(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @DeleteMapping("/delete/{fileName}")
    public ResponseEntity<String> deleteFile(@PathVariable String fileName, HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        String token = authHeader.substring(7);
        String username = jwtUtil.extractUsername(token);

        Path filePath = Paths.get(uploadBaseDir, username, fileName);
        try {
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                return ResponseEntity.ok("Fișierul a fost șters cu succes.");
            } else {
                return ResponseEntity.status(404).body("Fișierul nu există.");
            }
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Eroare la ștergere: " + e.getMessage());
        }
    }
}
