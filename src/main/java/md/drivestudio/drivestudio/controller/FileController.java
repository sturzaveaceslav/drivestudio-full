package md.drivestudio.drivestudio.controller;

import md.drivestudio.drivestudio.model.FileInfo;
import md.drivestudio.drivestudio.service.FileService;
import md.drivestudio.drivestudio.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/files")
public class FileController {

    @Autowired
    private FileService fileService;

    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping
    public ResponseEntity<List<FileInfo>> listUserFiles(@AuthenticationPrincipal UserDetails userDetails) {
        String username = userDetails.getUsername();
        List<FileInfo> files = fileService.listFiles(username);
        return ResponseEntity.ok(files);
    }

    @GetMapping("/download/{username}/{fileName}")
    public ResponseEntity<byte[]> downloadFile(@PathVariable String username,
                                               @PathVariable String fileName) {
        return fileService.downloadFile(username, fileName);
    }
}
