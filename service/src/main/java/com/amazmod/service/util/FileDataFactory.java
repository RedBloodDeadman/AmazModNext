package com.amazmod.service.util;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import com.google.gson.Gson;

import java.io.File;
import java.util.ArrayList;

import amazmod.com.transport.Transport;
import amazmod.com.transport.data.DirectoryData;
import amazmod.com.transport.data.FileData;

public class FileDataFactory {

    public static FileData fromFile(File file) {
        FileData fileData = new FileData();

        fileData.setPath(file.getAbsolutePath());
        fileData.setName(file.getName());
        fileData.setLastEditDate(file.lastModified());
        fileData.setSize(file.length());
        fileData.setDirectory(file.isDirectory());

        return fileData;
    }

    public static DirectoryData directoryFromFile(File file) {
        return directoryFromFile(file, new ArrayList<FileData>());
    }

    public static DirectoryData directoryFromFile(File file, ArrayList<FileData> filesData) {
        DirectoryData directoryData = new DirectoryData();

        directoryData.setPath(file.getAbsolutePath());
        directoryData.setName(file.getName());
        directoryData.setLastEditDate(file.lastModified());

        Gson gson = new Gson();
        directoryData.setFiles(gson.toJson(filesData));

        directoryData.setResult(Transport.RESULT_OK);

        return directoryData;
    }

    public static DirectoryData notFound() {
        DirectoryData directoryData = new DirectoryData();
        directoryData.setResult(Transport.RESULT_NOT_FOUND);
        return directoryData;
    }
}
