package com.smartstorage.app.db;

/**
 * Created by kasun on 8/15/17.
 */

public class FileDetails {
    private int id;
    private String file_name;
    private String drive_link;
    private String deleted;

    public FileDetails(String file_name,String drive_link){
        this.file_name=file_name;
        this.drive_link=drive_link;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFile_name() {
        return file_name;
    }

    public void setFile_name(String file_name) {
        this.file_name = file_name;
    }

    public String getDrive_link() {
        return drive_link;
    }

    public void setDrive_link(String drive_link) {
        this.drive_link = drive_link;
    }

    public String getDeleted() {
        return deleted;
    }

    public void setDeleted(String deleted) {
        this.deleted = deleted;
    }
}
