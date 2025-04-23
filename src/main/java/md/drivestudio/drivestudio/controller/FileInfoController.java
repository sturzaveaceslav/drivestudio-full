package md.drivestudio.drivestudio.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

@RestController
@RequestMapping("/api/files")
public class FileInfoController {

    @Value("${upload.directory}")
    private String uploadDirectory;

    @GetMapping("/info/{id}")
    public ResponseEntity<Map<String, Object>> getFileInfo(@PathVariable String id) {
        try {
            Path folder = Paths.get(uploadDirectory);
            Optional<Path> file = Files.list(folder)
                    .filter(f -> f.getFileName().toString().startsWith(id + "_"))
                    .findFirst();

            if (file.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Path filePath = file.get();
            String storedFilename = filePath.getFileName().toString();
            String originalFilename = storedFilename.substring(id.length() + 1);
            long fileSizeBytes = Files.size(filePath);

            Map<String, Object> response = new HashMap<>();
            response.put("name", originalFilename);
            response.put("size", humanReadableByteCountSI(fileSizeBytes));
            response.put("bytes", fileSizeBytes);

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private String humanReadableByteCountSI(long bytes) {
        if (bytes < 1000) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1000));
        String pre = "kMGTPE".charAt(exp - 1) + "";
        return String.format(Locale.US, "%.1f %sB", bytes / Math.pow(1000, exp), pre);
    }
}
