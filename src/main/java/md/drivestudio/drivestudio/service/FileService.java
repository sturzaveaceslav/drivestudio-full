package md.drivestudio.drivestudio.service;

import lombok.RequiredArgsConstructor;
import md.drivestudio.drivestudio.entity.UploadedFile;
import md.drivestudio.drivestudio.entity.User;
import md.drivestudio.drivestudio.repository.UploadedFileRepository;
import md.drivestudio.drivestudio.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileService {

    private final UploadedFileRepository fileRepository;
    private final UserRepository userRepository;

    private final String uploadDirectory = System.getProperty("user.dir") + "/uploads";

    public void storeFile(MultipartFile file) throws IOException {
        File dir = new File(uploadDirectory);
        if (!dir.exists()) {
            dir.mkdirs(); // creează folderul dacă nu există
        }

        if (file.isEmpty()) {
            throw new RuntimeException("Fișierul este gol.");
        }

        String originalFilename = file.getOriginalFilename();
        String uniqueFileName = UUID.randomUUID() + "_" + originalFilename;
        String filePath = uploadDirectory + "/" + uniqueFileName;

        File dest = new File(filePath);
        file.transferTo(dest);

        // Obține utilizatorul logat
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Creează obiectul UploadedFile
        UploadedFile uploadedFile = new UploadedFile();
        uploadedFile.setFilename(originalFilename);
        uploadedFile.setFileType(file.getContentType());
        uploadedFile.setSize(file.getSize());
        uploadedFile.setPath(filePath);
        uploadedFile.setUploadDate(new Date());
        uploadedFile.setUser(user); // Legătură cu userul

        fileRepository.save(uploadedFile);
    }

    public List<UploadedFile> getAllFiles() {
        return fileRepository.findAll();
    }
}
