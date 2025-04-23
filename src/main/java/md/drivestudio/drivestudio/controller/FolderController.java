package md.drivestudio.drivestudio.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import md.drivestudio.drivestudio.entity.UserFolder;
import md.drivestudio.drivestudio.model.User;
import md.drivestudio.drivestudio.repository.UserFolderRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user/folders")
@RequiredArgsConstructor
public class FolderController {

    private final UserFolderRepository folderRepository;

    // ✅ 1. Listare toate mapele userului
    @GetMapping
    public ResponseEntity<List<String>> getUserFolders(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) return ResponseEntity.status(401).build();

        List<String> folderNames = folderRepository.findByUser(user)
                .stream()
                .map(UserFolder::getName)
                .toList();

        return ResponseEntity.ok(folderNames);
    }


    // ✅ 2. Creare mapă nouă
    @PostMapping
    public ResponseEntity<?> createFolder(@RequestBody UserFolder folder, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) return ResponseEntity.status(401).body("Neautentificat");

        folder.setUser(user);
        folderRepository.save(folder);
        return ResponseEntity.ok("✅ Mapă creată cu succes");
    }

    // ✅ 3. Redenumire mapă
    @PutMapping("/{id}")
    public ResponseEntity<?> renameFolder(@PathVariable Long id,
                                          @RequestBody String newName,
                                          HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) return ResponseEntity.status(401).body("Neautentificat");

        return folderRepository.findById(id)
                .filter(folder -> folder.getUser().getId().equals(user.getId()))
                .map(folder -> {
                    folder.setName(newName);
                    folderRepository.save(folder);
                    return ResponseEntity.ok("✅ Nume mapă actualizat");
                })
                .orElse(ResponseEntity.status(404).body("❌ Mapă inexistentă sau acces interzis"));
    }

    // ✅ 4. Ștergere mapă (nu șterge fișierele asociate)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteFolder(@PathVariable Long id, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) return ResponseEntity.status(401).body("Neautentificat");

        return folderRepository.findById(id)
                .filter(folder -> folder.getUser().getId().equals(user.getId()))
                .map(folder -> {
                    folderRepository.delete(folder);
                    return ResponseEntity.ok("✅ Mapă ștearsă");
                })
                .orElse(ResponseEntity.status(404).body("❌ Mapă inexistentă sau acces interzis"));
    }
}
