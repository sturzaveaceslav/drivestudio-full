package md.drivestudio.drivestudio.controller;

import lombok.RequiredArgsConstructor;
import md.drivestudio.drivestudio.dto.FileDTO;
import md.drivestudio.drivestudio.entity.UploadedFile;
import md.drivestudio.drivestudio.repository.UploadedFileRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/gallery")
public class GalleryController {

    private final UploadedFileRepository fileRepository;

    @GetMapping("/{galleryId}")
    public ResponseEntity<List<FileDTO>> getGalleryFiles(@PathVariable String galleryId) {
        List<UploadedFile> files = fileRepository.findByGalleryId(galleryId);

        List<FileDTO> dtos = files.stream()
                .map(file -> new FileDTO(
                        file.getFilename(),
                        file.getUniqueId(),
                        file.getFileType(),
                        file.getSize(),
                        file.getGalleryId()
                ))
                .toList();


        return ResponseEntity.ok(dtos);
    }
    @GetMapping("/file/{id}")
    public ResponseEntity<FileDTO> getFileByUniqueId(@PathVariable String id) {
        return fileRepository.findByUniqueId(id)
                .map(file -> ResponseEntity.ok(new FileDTO(
                        file.getFilename(),
                        file.getUniqueId(),
                        file.getFileType(),
                        file.getSize(),
                        file.getGalleryId()
                )))
                .orElse(ResponseEntity.notFound().build());
    }


}
