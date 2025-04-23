package md.drivestudio.drivestudio.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserStatsDTO {
    private int totalFiles;
    private long totalSize;
    private String lastFileName;
    private String lastUploadDate;
}
