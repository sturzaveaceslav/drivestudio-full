package md.drivestudio.drivestudio.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import md.drivestudio.drivestudio.model.User;

import java.util.Date;

@Entity
@Getter
@Setter
public class UploadedFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonProperty("uniqueId") // ✅ folosit pentru /s/{uniqueId}
    private String uniqueId;

    private String filename;

    @JsonProperty("fileType") // ✅ inclus în JSON pentru preview video/img/pdf
    private String fileType;

    private long size;
    private String path;
    private Date uploadDate;
    private String galleryId;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "folder_name")
    @JsonProperty("folderName") // ✅ trimis la frontend
    private String folderName;

    // 👇 Gettere explicite pentru a te asigura că apar în JSON
    public String getUniqueId() {
        return uniqueId;
    }

    public String getFileType() {
        return fileType;
    }

    public String getFolderName() {
        return folderName;
    }

    public String getFilename() {
        return filename;
    }

    public long getSize() {
        return size;
    }

    public String getPath() {
        return path;
    }

    public Date getUploadDate() {
        return uploadDate;
    }
}
