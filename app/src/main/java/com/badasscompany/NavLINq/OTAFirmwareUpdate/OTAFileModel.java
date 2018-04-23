package com.badasscompany.NavLINq.OTAFirmwareUpdate;

/**
 * Data Model class for OTA File
 */
public class OTAFileModel {
    /**
     *File name
     */
    private String mFileName = null;
    /**
     *File path
     */
    private String mFilePath = null;
    /**
     * File parent
     */
    private String mFileParent = null;
    /**
     * Remote file
     */
    private String mFileRemote = null;
    /**
     * File Description
     */
    private String mFileDescription = null;
    /**
     *Selection Flag
     *
     */
    private boolean mSelected = false;


    // Constructor
    public OTAFileModel(String fileName, String filePath, boolean selected, String fileParent, String mFileRemote, String mFileDescription) {
        super();
        this.mFileName = fileName;
        this.mFilePath = filePath;
        this.mSelected = selected;
        this.mFileParent = fileParent;
        this.mFileRemote = mFileRemote;
        this.mFileDescription = mFileDescription;
    }

    public OTAFileModel() {
        super();
    }

    public String getFileName() {
        return mFileName;
    }

    public String getmFileParent() {
        return mFileParent;
    }

    public String getFileRemote() {
        return mFileRemote;
    }

    public String getFileDescription() {
        return mFileDescription;
    }

    public void setmFileParent(String mFileParent) {
        this.mFileParent = mFileParent;
    }

    public void setFileName(String mFileName) {
        this.mFileName = mFileName;
    }

    public String getFilePath() {
        return mFilePath;
    }

    public void setName(String mFilePath) {
        this.mFilePath = mFilePath;
    }

    public void setRemote(String mFileRemote) {
        this.mFileRemote = mFileRemote;
    }

    public void setDescription(String mFileDescription) {
        this.mFileDescription = mFileDescription;
    }

    public boolean isSelected() {
        return mSelected;
    }

    public void setSelected(boolean selected) {
        this.mSelected = selected;
    }
}
