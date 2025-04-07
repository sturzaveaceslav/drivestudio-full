package md.drivestudio.drivestudio.service;

import lombok.RequiredArgsConstructor;
import md.drivestudio.drivestudio.entity.UploadedFile;
import md.drivestudio.drivestudio.repository.UploadedFileRepository;
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
    private final String uploadDirectory = "uploads";

    public void storeFile(MultipartFile file) throws IOException {
        File dir = new File(uploadDirectory);
        if (!dir.exists()) {
            dir.mkdirs(); // creează folderul dacă nu există
        }

        String originalFilename = file.getOriginalFilename();
        String uniqueFileName = UUID.randomUUID() + "_" + originalFilename;
        String filePath = uploadDirectory + "/" + uniqueFileName;

        File dest = new File(filePath);
        file.transferTo(dest);

        UploadedFile uploadedFile = new UploadedFile();
        uploadedFile.setFilename(originalFilename);
        uploadedFile.setFileType(file.getContentType());
        uploadedFile.setSize(file.getSize());
        uploadedFile.setPath(filePath);
        uploadedFile.setUploadDate(new Date());

        fileRepository.save(uploadedFile);
    }

    public List<UploadedFile> getAllFiles() {
        return fileRepository.findAll();
    }
}
