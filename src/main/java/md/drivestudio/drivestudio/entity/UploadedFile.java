package md.drivestudio.drivestudio.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Entity
@Getter
@Setter
public class UploadedFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String filename;
    private String fileType;
    private long size;
    private String path;
    private Date uploadDate;

    @ManyToOne
    @JoinColumn(name = "user_id") // creează coloana "user_id" în tabel
    private User user;
}
