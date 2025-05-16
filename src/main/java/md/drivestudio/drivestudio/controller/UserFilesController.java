package md.drivestudio.drivestudio.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import md.drivestudio.drivestudio.entity.UploadedFile;
import md.drivestudio.drivestudio.model.User;
import md.drivestudio.drivestudio.repository.UploadedFileRepository;
import md.drivestudio.drivestudio.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserFilesController {

    private final UploadedFileRepository fileRepository;
    private final UserService userService;

    // ✅ Returnează fișierele utilizatorului logat
    @GetMapping("/files")
    public ResponseEntity<List<UploadedFile>> getOwnFiles(
            @RequestParam(value = "folderId", required = false) Long folderId,
            HttpSession session) {

        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<UploadedFile> files = (folderId == null)
                ? fileRepository.findByUserAndFolderIdIsNull(user)
                : fileRepository.findByUserAndFolderId(user, folderId);

        files.sort(Comparator.comparing(UploadedFile::getUploadDate).reversed());
        return ResponseEntity.ok(files);
    }




    // ✅ Obține spațiul folosit și limita maximă
    @GetMapping("/space")
    public ResponseEntity<Map<String, Long>> getUsedStorage(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        long totalUsed = fileRepository.findByUser(user)
                .stream()
                .mapToLong(UploadedFile::getSize)
                .sum();

        Map<String, Long> response = new HashMap<>();
        response.put("used", totalUsed);
        response.put("max", user.getMaxUploadSize());

        return ResponseEntity.ok(response);
    }






    // ✅ Link public pentru previzualizare
    @GetMapping("/files/{id}/link")
    public ResponseEntity<String> getPreviewLink(@PathVariable Long id,
                                                 @RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token lipsă.");
        }

        User user = userService.getUserFromToken(authHeader.substring(7));
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Utilizator invalid.");
        }

        return fileRepository.findById(id)
                .map(file -> {
                    if (!file.getUser().getId().equals(user.getId())) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Nu ai acces la acest fișier.");
                    }
                    return ResponseEntity.ok("http://localhost:8082/s/" + file.getUniqueId());
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body("Fișier inexistent."));
    }
    @DeleteMapping("/gallery/{galleryId}")
    public ResponseEntity<String> deleteGallery(@PathVariable String galleryId, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Neautentificat.");
        }

        List<UploadedFile> files = fileRepository.findByGalleryId(galleryId);
        boolean isOwner = files.stream().allMatch(f -> f.getUser() != null && f.getUser().getId().equals(user.getId()));
        boolean isAdmin = user.getRole().name().equals("ADMIN");

        if (!isOwner && !isAdmin) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Nu ai voie să ștergi această galerie.");
        }

        try {
            for (UploadedFile file : files) {
                Files.deleteIfExists(Path.of(file.getPath()));
                fileRepository.delete(file);
            }
            return ResponseEntity.ok("Galerie ștearsă cu succes.");
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Eroare la ștergere: " + e.getMessage());
        }
    }

}
