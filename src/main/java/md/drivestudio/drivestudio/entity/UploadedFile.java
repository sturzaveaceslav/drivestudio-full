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

    @JsonProperty("uniqueId") // 🔥 Asigurăm includerea în JSON
    private String uniqueId;  // 🔑 ID scurt, folosit pentru URL (/s/{uniqueId})

    private String filename;
    private String fileType;
    private long size;
    private String path;
    private Date uploadDate;
    private String galleryId;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    // 👇 Forțăm getter explicit (dacă cumva Lombok nu include în JSON)
    public String getUniqueId() {
        return uniqueId;
    }
}
