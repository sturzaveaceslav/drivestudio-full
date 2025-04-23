package md.drivestudio.drivestudio.repository;

import md.drivestudio.drivestudio.entity.UploadedFile;
import md.drivestudio.drivestudio.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UploadedFileRepository extends JpaRepository<UploadedFile, Long> {

    // ✅ Găsește toate fișierele încărcate de un utilizator
    List<UploadedFile> findByUser(User user);

    // ✅ Găsește fișierele dintr-o galerie
    List<UploadedFile> findByGalleryId(String galleryId);




    // ✅ Găsește un fișier după ID unic (pentru descărcare)
    Optional<UploadedFile> findByUniqueId(String uniqueId);

    // ✅ Șterge toate fișierele dintr-o galerie
    void deleteByGalleryId(String galleryId);

    // ✅ Șterge toate fișierele unui utilizator (opțional)
    void deleteByUser(User user);
}

