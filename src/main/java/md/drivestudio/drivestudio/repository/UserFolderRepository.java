package md.drivestudio.drivestudio.repository;

import md.drivestudio.drivestudio.entity.UserFolder;
import md.drivestudio.drivestudio.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserFolderRepository extends JpaRepository<UserFolder, Long> {
    List<UserFolder> findByUser(User user);
}
