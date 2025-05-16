package md.drivestudio.drivestudio.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import md.drivestudio.drivestudio.entity.UploadedFile;
import md.drivestudio.drivestudio.entity.UserFolder;
import md.drivestudio.drivestudio.model.User;
import md.drivestudio.drivestudio.repository.UploadedFileRepository;
import md.drivestudio.drivestudio.repository.UserFolderRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("/api/user/folders")
@RequiredArgsConstructor
public class FolderController {
    private final UploadedFileRepository fileRepository;

    private final UserFolderRepository folderRepository;


    // ✅ 1. Listare mape (filtrare după parentId)
    @GetMapping
    public ResponseEntity<?> getFolders(@RequestParam(required = false) Long parentId, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) return ResponseEntity.status(401).body("Neautentificat");

        List<UserFolder> folders;
        if (parentId == null) {
            folders = folderRepository.findByUserAndParentIsNull(user);
        } else {
            Optional<UserFolder> parentOpt = folderRepository.findById(parentId);
            if (parentOpt.isEmpty() || !parentOpt.get().getUser().getId().equals(user.getId()))
                return ResponseEntity.status(403).body("❌ Acces interzis la mapa părinte");

            folders = folderRepository.findByUserAndParent(user, parentOpt.get());
        }

        List<Map<String, Object>> result = folders.stream().map(folder -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", folder.getId());
            map.put("name", folder.getName());
            map.put("parentId", folder.getParent() != null ? folder.getParent().getId() : null);
            return map;
        }).toList();

        return ResponseEntity.ok(result);
    }

    // ✅ 2. Creare mapă (poate fi și submapă)
    @PostMapping
    public ResponseEntity<?> createFolder(@RequestBody Map<String, Object> payload, HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) return ResponseEntity.status(401).body("Neautentificat");

        String name = (String) payload.get("name");
        Long parentId = payload.get("parentId") != null ? Long.valueOf(payload.get("parentId").toString()) : null;

        if (name == null || name.isBlank()) {
            return ResponseEntity.badRequest().body("❌ Numele mapei este necesar");
        }

        UserFolder folder = new UserFolder();
        folder.setName(name);
        folder.setUser(user);

        if (parentId != null) {
            Optional<UserFolder> parentOpt = folderRepository.findById(parentId);
            if (parentOpt.isEmpty() || !parentOpt.get().getUser().getId().equals(user.getId()))
                return ResponseEntity.status(403).body("❌ Nu poți crea submapă în mapă străină");
            folder.setParent(parentOpt.get());
        }

        folderRepository.save(folder);
        return ResponseEntity.ok("✅ Mapă creată");
    }

    // ✅ 3. Redenumire mapă
    @PutMapping("/{id}")
    public ResponseEntity<?> renameFolder(@PathVariable Long id, @RequestBody String newName, HttpSession session) {
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

    // ✅ 4. Ștergere mapă
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
    @GetMapping("/{folderId}/files")
    public ResponseEntity<List<UploadedFile>> getFilesInFolderRecursive(@PathVariable("folderId") Long folderId) {
        Optional<UserFolder> rootFolderOpt = folderRepository.findById(folderId);
        if (rootFolderOpt.isEmpty()) {
            return ResponseEntity.status(404).build();
        }

        Set<Long> allFolderIds = new HashSet<>();
        collectFolderIdsRecursively(rootFolderOpt.get(), allFolderIds);

        List<UploadedFile> files = fileRepository.findByFolderIdIn(allFolderIds);
        return ResponseEntity.ok(files);
    }

    private void collectFolderIdsRecursively(UserFolder folder, Set<Long> ids) {
        ids.add(folder.getId());
        List<UserFolder> children = folderRepository.findByParent(folder);
        for (UserFolder child : children) {
            collectFolderIdsRecursively(child, ids);
        }
    }



    @GetMapping("/{folderId}/download-zip")
    public void downloadFolderZip(@PathVariable Long folderId, HttpServletResponse response, HttpSession session) throws IOException {
        User user = (User) session.getAttribute("user");
        if (user == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        Optional<UserFolder> rootFolderOpt = folderRepository.findById(folderId);
        if (rootFolderOpt.isEmpty() || !rootFolderOpt.get().getUser().getId().equals(user.getId())) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        // 1. Adună toate folderId-urile recursive
        Set<Long> allFolderIds = new HashSet<>();
        collectFolderIdsRecursively(rootFolderOpt.get(), allFolderIds);

        // 2. Găsește toate fișierele din aceste mape
        List<UploadedFile> files = fileRepository.findByFolderIdIn(allFolderIds);
        if (files.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // 3. Construiește arhiva ZIP
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=folder_" + folderId + ".zip");

        try (ZipOutputStream zipOut = new ZipOutputStream(response.getOutputStream())) {
            for (UploadedFile file : files) {
                Path path = Paths.get(file.getPath());
                if (Files.exists(path)) {
                    zipOut.putNextEntry(new ZipEntry(file.getFilename()));
                    Files.copy(path, zipOut);
                    zipOut.closeEntry();
                }
            }
            zipOut.finish();
        }
    }




}
