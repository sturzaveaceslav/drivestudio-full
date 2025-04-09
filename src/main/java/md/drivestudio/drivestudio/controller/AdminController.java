package md.drivestudio.drivestudio.controller;

import md.drivestudio.drivestudio.entity.User;
import md.drivestudio.drivestudio.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Value("${upload.directory}")
    private String uploadBaseDir;

    private final UserRepository userRepository;

    public AdminController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/users")
    public ResponseEntity<List<String>> getAllUsernames() {
        List<String> usernames = userRepository.findAll()
                .stream()
                .map(User::getUsername)
                .collect(Collectors.toList());
        return ResponseEntity.ok(usernames);
    }

    @GetMapping("/files/{username}")
    public ResponseEntity<List<String>> getUserFiles(@PathVariable String username) {
        Path userDir = Paths.get(uploadBaseDir, username);
        List<String> files = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(userDir)) {
            for (Path path : stream) {
                if (Files.isRegularFile(path)) {
                    files.add(path.getFileName().toString());
                }
            }
        } catch (Exception e) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        return ResponseEntity.ok(files);
    }

    @DeleteMapping("/files/{username}/{fileName}")
    public ResponseEntity<String> deleteUserFile(@PathVariable String username,
                                                 @PathVariable String fileName) {
        Path filePath = Paths.get(uploadBaseDir, username, fileName);
        try {
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                return ResponseEntity.ok("Fișierul a fost șters.");
            } else {
                return ResponseEntity.status(404).body("Fișierul nu există.");
            }
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Eroare la ștergere: " + e.getMessage());
        }
    }
}
