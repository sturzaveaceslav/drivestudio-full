package md.drivestudio.drivestudio.model;

public class FileInfo {
    private String fileName;
    private String downloadUrl;

    public FileInfo(String fileName, String downloadUrl) {
        this.fileName = fileName;
        this.downloadUrl = downloadUrl;
    }

    public String getFileName() {
        return fileName;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }
}
