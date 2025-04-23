package md.drivestudio.drivestudio.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileDTO {
    private String filename;   // ex: poza.jpg
    private String fileType;   // ex: image/jpeg, video/mp4, application/pdf
    private String uniqueId;   // ex: a1b2c3 (pentru /s/{id})
}
