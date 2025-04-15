package md.drivestudio.drivestudio.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Date;

@Data
@AllArgsConstructor
public class AdminGalleryDTO {
    private String galleryId;
    private Date uploadDate;
    private String uploader; // "anonim" dacă user == null
    private int fileCount;
}
