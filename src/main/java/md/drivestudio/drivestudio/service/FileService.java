package md.drivestudio.drivestudio.service;

import md.drivestudio.drivestudio.model.FileInfo;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
public class FileService {

    private final String uploadBaseDir = "F:/DriveStudioNou/uploads";

    public List<FileInfo> listFiles(String username) {
        List<FileInfo> fileInfos = new ArrayList<>();
        Path userDir = Paths.get(uploadBaseDir, username);

        File folder = userDir.toFile();
        if (folder.exists() && folder.isDirectory()) {
            for (File file : folder.listFiles()) {
                if (file.isFile()) {
                    String url = "http://localhost:8080/api/files/download/" + username + "/" + file.getName();
                    fileInfos.add(new FileInfo(file.getName(), url));
                }
            }
        }

        return fileInfos;
    }

    public ResponseEntity<byte[]> downloadFile(String username, String fileName) {
        try {
            Path filePath = Paths.get(uploadBaseDir, username, fileName);
            byte[] fileBytes = Files.readAllBytes(filePath);

            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                    .body(fileBytes);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
