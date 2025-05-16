package md.drivestudio.drivestudio.repository;

import md.drivestudio.drivestudio.entity.UserFolder;
import md.drivestudio.drivestudio.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserFolderRepository extends JpaRepository<UserFolder, Long> {

    // Toate mapele (nu se schimbă)
    List<UserFolder> findByUser(User user);

    // 🔥 Mapele fără părinte (rădăcină)
    List<UserFolder> findByUserAndParentIsNull(User user);

    // 🔥 Mapele cu părinte specific
    List<UserFolder> findByUserAndParent(User user, UserFolder parent);

    List<UserFolder> findByParent(UserFolder parent);

}
