package com.blackboxembedded.WunderLINQ.OTAFirmwareUpdate;

import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Class created to read the .cycad files.The read file is stored temporarily
 */
class CustomFileReader {
    private String mSiliconID;
    private final String mHeader;
    private final File mFile;
    private int mReadingLine = 0;

    //File read status updater
    private FileReadStatusUpdater mFileReadStatusUpdaterUpdater;

    //Constructor
    public CustomFileReader(String filepath) {
        mFile = new File(filepath);
        mHeader = getTheHeaderString(mFile);
        Log.d("CustomFileReader","PATH>>>"+filepath);
    }

    public void setFileReadStatusUpdater(FileReadStatusUpdater fileReadStatusUpdater) {
        this.mFileReadStatusUpdaterUpdater = fileReadStatusUpdater;
    }

    /**
     * Analysing the header file and extracting the silicon ID,Check Sum Type and Silicon rev
     */
    public String[] analyseFileHeader() {
        String[] headerData = new String[3];
        String MSBString = Utils.getMSB(mHeader);
        mSiliconID = getSiliconID(MSBString);
        String mSiliconRev = getSiliconRev(MSBString);
        String mCheckSumType = getCheckSumType(MSBString);
        headerData[0] = mSiliconID;
        headerData[1] = mSiliconRev;
        headerData[2] = mCheckSumType;
        return headerData;
    }


    /**
     * Method to parse the file a read each line and put the line to a data model
     *
     * @return
     */
    public ArrayList<OTAFlashRowModel> readDataLines() {
        ArrayList<OTAFlashRowModel> flashDataLines = new ArrayList<OTAFlashRowModel>();
        String dataLine = null;
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(mFile));
            while ((dataLine = bufferedReader.readLine()) != null) {
                mReadingLine++;
                mFileReadStatusUpdaterUpdater.onFileReadProgressUpdate(mReadingLine);
                byte[] data;

                OTAFlashRowModel model = new OTAFlashRowModel();
                if (mReadingLine != 1) {
                    StringBuilder dataBuilder = new StringBuilder(dataLine);
                    dataBuilder.deleteCharAt(0);
                    model.mArrayId = Integer.parseInt(dataBuilder.substring(0, 2), 16);
                    model.mRowNo = Utils.getMSB(dataBuilder.substring(2, 6));
                    model.mDataLength = Integer.parseInt(dataBuilder.
                            substring(6, 10), 16);
                    model.mRowCheckSum = Integer.parseInt(dataBuilder.
                            substring(dataLine.length() - 3, dataLine.length() - 1), 16);
                    String datacharacters = dataBuilder.
                            substring(10, dataLine.length() - 2);
                    data = new byte[model.mDataLength];
                    for (int i = 0, j = 0; i < model.mDataLength; i++, j += 2) {
                        data[i] = (byte) Integer.parseInt(datacharacters.substring(j, j + 2), 16);
                    }
                    model.mData = data;
                    flashDataLines.add(model);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return flashDataLines;
    }

    /**
     * Method to count the total lines in the selected file
     *
     * @return totalLines
     */
    public int getTotalLines() {
        int totalLines = 0;
        String dataLine = "";
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(mFile));
            while ((dataLine = bufferedReader.readLine()) != null) {
                totalLines++;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return totalLines;
    }

    /**
     * Reading the first line from the file
     *
     * @param file
     * @return
     */
    private String getTheHeaderString(File file) {
        String header = "";
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
            header = bufferedReader.readLine();
            bufferedReader.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return header;
    }

    private String getSiliconID(String header) {
        String siliconID = header.substring(4, 12);
        return siliconID;
    }

    private String getSiliconRev(String header) {
        String siliconRev = header.substring(2, 4);
        return siliconRev;
    }

    private String getCheckSumType(String header) {
        String checkSumType = header.substring(0, 2);
        return checkSumType;
    }

}
