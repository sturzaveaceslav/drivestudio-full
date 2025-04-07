package md.drivestudio.drivestudio.repository;

import md.drivestudio.drivestudio.entity.UploadedFile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UploadedFileRepository extends JpaRepository<UploadedFile, Long> {
}
