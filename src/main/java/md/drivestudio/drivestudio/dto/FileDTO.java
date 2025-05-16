package md.drivestudio.drivestudio.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileDTO {
    private String filename;
    private String uniqueId;
    private String fileType;
    private long size;
    private String galleryId; // ← adaugă acest câmp

    // Getteri și setteri
}

