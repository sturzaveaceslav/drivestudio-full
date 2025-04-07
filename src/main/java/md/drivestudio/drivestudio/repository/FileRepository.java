package md.drivestudio.drivestudio.repository;

import md.drivestudio.drivestudio.entity.FileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FileRepository extends JpaRepository<FileEntity, Long> {
    List<FileEntity> findByUsername(String username);
}
