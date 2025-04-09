package md.drivestudio.drivestudio.controller;

import jakarta.servlet.http.HttpServletRequest;
import md.drivestudio.drivestudio.security.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/files")
public class FileListController {

    @Value("${upload.directory}")
    private String uploadBaseDir;

    private final JwtUtil jwtUtil;

    public FileListController(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @GetMapping("/list")
    public ResponseEntity<List<String>> listUserFiles(HttpServletRequest request) {
        try {
            // 1. Extrage utilizatorul din JWT
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(401).build();
            }

            String token = authHeader.substring(7);
            String username = jwtUtil.extractUsername(token);

            // 2. Creează calea către directorul utilizatorului
            Path userDir = Paths.get(uploadBaseDir, username);

            if (!Files.exists(userDir)) {
                return ResponseEntity.ok(new ArrayList<>()); // folderul nu există încă
            }

            // 3. Parcurge fișierele și construiește lista de linkuri
            List<String> fileLinks = new ArrayList<>();
            DirectoryStream<Path> directoryStream = Files.newDirectoryStream(userDir);
            for (Path path : directoryStream) {
                if (Files.isRegularFile(path)) {
                    String fileName = path.getFileName().toString();
                    String fileUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                            .path("/uploads/")
                            .path(username + "/")
                            .path(fileName)
                            .toUriString();
                    fileLinks.add(fileUrl);
                }
            }

            return ResponseEntity.ok(fileLinks);

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}
