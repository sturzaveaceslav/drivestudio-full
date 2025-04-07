package md.drivestudio.drivestudio.controller;

import lombok.RequiredArgsConstructor;
import md.drivestudio.drivestudio.entity.UploadedFile;
import md.drivestudio.drivestudio.service.FileService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            fileService.storeFile(file);
            return ResponseEntity.ok("Fișierul a fost încărcat cu succes.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Eroare la încărcarea fișierului.");
        }
    }

    @GetMapping("/all")
    public ResponseEntity<List<UploadedFile>> listAllFiles() {
        return ResponseEntity.ok(fileService.getAllFiles());
    }
}
