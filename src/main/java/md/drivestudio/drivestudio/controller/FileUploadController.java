package md.drivestudio.drivestudio.controller;

import md.drivestudio.drivestudio.entity.UploadedFile;
import md.drivestudio.drivestudio.model.User;
import md.drivestudio.drivestudio.repository.UploadedFileRepository;
import md.drivestudio.drivestudio.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@CrossOrigin
public class FileUploadController {

    @Value("${upload.directory}")
    private String uploadDirectory;

    private final UploadedFileRepository fileRepository;
    private final UserService userService;

    public FileUploadController(UploadedFileRepository fileRepository, UserService userService) {
        this.fileRepository = fileRepository;
        this.userService = userService;
    }

    // 🔼 Upload multiplu
    @PostMapping("/upload")
    public ResponseEntity<String> uploadFiles(@RequestParam("file") MultipartFile[] files,
                                              @RequestParam(value = "customName", required = false) String customName,
                                              @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String galleryId = UUID.randomUUID().toString().substring(0, 8);
        User user = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            user = userService.getUserFromToken(token);
        }

        try {
            Path uploadPath = Paths.get(uploadDirectory);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            for (MultipartFile file : files) {
                if (file.isEmpty()) continue;

                String uniqueId = UUID.randomUUID().toString().substring(0, 6);
                String originalFilename = file.getOriginalFilename();
                String extension = "";

                if (originalFilename != null && originalFilename.contains(".")) {
                    extension = originalFilename.substring(originalFilename.lastIndexOf("."));
                }

                String storedFilename = (customName != null && !customName.isBlank())
                        ? uniqueId + "_" + customName + extension
                        : uniqueId + "_" + originalFilename;

                Path filePath = uploadPath.resolve(storedFilename);
                Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

                UploadedFile uploadedFile = new UploadedFile();
                uploadedFile.setUniqueId(uniqueId);
                uploadedFile.setFilename(storedFilename);
                uploadedFile.setPath(filePath.toString());
                uploadedFile.setFileType(Files.probeContentType(filePath));
                uploadedFile.setSize(file.getSize());
                uploadedFile.setUploadDate(new Date());
                uploadedFile.setUser(user);
                uploadedFile.setGalleryId(galleryId);

                fileRepository.save(uploadedFile);
            }

            return ResponseEntity.ok(galleryId);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Eroare la încărcare: " + e.getMessage());
        }
    }

    // 🔽 Descărcare fișier individual
    @GetMapping("/s/{id}")
    public ResponseEntity<Resource> downloadFile(@PathVariable("id") String id) {
        try {
            Optional<UploadedFile> optionalFile = fileRepository.findByUniqueId(id);
            if (optionalFile.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }

            UploadedFile uploadedFile = optionalFile.get();
            Path filePath = Paths.get(uploadedFile.getPath());
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }

            String mimeType = Files.probeContentType(filePath);
            MediaType mediaType = (mimeType != null) ? MediaType.parseMediaType(mimeType) : MediaType.APPLICATION_OCTET_STREAM;

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            mediaType.equals(MediaType.APPLICATION_OCTET_STREAM)
                                    ? "attachment; filename=\"" + uploadedFile.getFilename() + "\""
                                    : "inline; filename=\"" + uploadedFile.getFilename() + "\"")
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // 🔍 HEAD pentru MIME type (preview)
    @RequestMapping(value = "/s/{id}", method = RequestMethod.HEAD)
    public ResponseEntity<Void> checkFileType(@PathVariable("id") String id) {
        try {
            Optional<UploadedFile> optionalFile = fileRepository.findByUniqueId(id);
            if (optionalFile.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Path filePath = Paths.get(optionalFile.get().getPath());
            String mimeType = Files.probeContentType(filePath);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(mimeType != null ? mimeType : "application/octet-stream"))
                    .build();

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // 🔁 Returnează toate fișierele din galerie
    @GetMapping("/gallery/{galleryId}")
    public ResponseEntity<List<UploadedFile>> getFilesByGallery(@PathVariable String galleryId) {
        List<UploadedFile> files = fileRepository.findByGalleryId(galleryId);
        if (files.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Collections.emptyList());
        }
        return ResponseEntity.ok(files);
    }

    // ✅ DESCĂRCARE TOATE FIȘIERELE CA ZIP (corect)
    @GetMapping("/gallery/{galleryId}/download-zip")
    public ResponseEntity<Resource> downloadGalleryAsZip(@PathVariable String galleryId) {
        try {
            List<UploadedFile> files = fileRepository.findByGalleryId(galleryId);
            if (files.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Path zipPath = Files.createTempFile("galerie_" + galleryId + "_", ".zip");

            try (OutputStream fos = Files.newOutputStream(zipPath);
                 ZipOutputStream zos = new ZipOutputStream(fos)) {

                for (UploadedFile file : files) {
                    Path filePath = Paths.get(file.getPath());
                    if (Files.exists(filePath)) {
                        ZipEntry entry = new ZipEntry(file.getFilename());
                        zos.putNextEntry(entry);
                        Files.copy(filePath, zos);
                        zos.closeEntry();
                    }
                }
            }

            Resource resource = new UrlResource(zipPath.toUri());

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"galerie_" + galleryId + ".zip\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}
