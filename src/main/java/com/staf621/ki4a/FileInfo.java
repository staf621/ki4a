package com.staf621.ki4a;

public class FileInfo implements Comparable {
    private String fileName;
    private String fileType;
    private String filePath;
    private boolean isDirectory;
    private boolean isParent;

    public FileInfo(String str, String str2, String str3, boolean dir, boolean parent) {
        this.fileName = str;
        this.fileType = str2;
        this.filePath = str3;
        this.isDirectory = dir;
        this.isParent = parent;
    }

    public int compare(FileInfo file) {
        if (this.fileName != null) {
            return this.fileName.toLowerCase().compareTo(file.getFileName().toLowerCase());
        }
        throw new IllegalArgumentException();
    }

    public String getFileName() {
        return this.fileName;
    }

    public String getFileType() {
        return this.fileType;
    }

    public String getFilePath() {
        return this.filePath;
    }

    public int compareTo(Object obj) {
        return compare((FileInfo) obj);
    }

    public boolean isDirectory() {
        return this.isDirectory;
    }

    public boolean isParent() {
        return this.isParent;
    }
}
