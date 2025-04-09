package md.drivestudio.drivestudio.controller;

import jakarta.servlet.http.HttpServletRequest;
import md.drivestudio.drivestudio.security.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;

@RestController
@RequestMapping("/api/files")
public class FileDownloadController {

    @Value("${upload.directory}")
    private String uploadBaseDir;

    private final JwtUtil jwtUtil;

    public FileDownloadController(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @GetMapping("/download/{fileName}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName,
                                                 HttpServletRequest request) {
        try {
            // 1. Verifică JWT
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(401).build();
            }

            String token = authHeader.substring(7);
            String username = jwtUtil.extractUsername(token);

            // 2. Construiește calea către fișierul userului
            Path filePath = Paths.get(uploadBaseDir, username, fileName);

            if (!Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }

            // 3. Deschide fișierul ca resursă
            InputStream inputStream = Files.newInputStream(filePath);
            Resource resource = new InputStreamResource(inputStream);

            // 4. Setează headers pentru download
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}
