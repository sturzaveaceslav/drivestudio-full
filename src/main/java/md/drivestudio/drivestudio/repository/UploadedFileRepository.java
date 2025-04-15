package md.drivestudio.drivestudio.repository;

import md.drivestudio.drivestudio.entity.UploadedFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UploadedFileRepository extends JpaRepository<UploadedFile, Long> {

    // 🔍 Caută toate fișierele care au același galleryId
    List<UploadedFile> findByGalleryId(String galleryId);

    // 🔍 Caută un fișier după uniqueId (folosit pentru descărcare și preview)
    Optional<UploadedFile> findByUniqueId(String uniqueId);
}
