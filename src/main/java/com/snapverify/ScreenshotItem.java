package com.snapverify;

import java.io.File;

public class ScreenshotItem {

    private final File file;
    private String ocrText = "";

    public ScreenshotItem(File file) {
        this.file = file;
    }

    public File file() {
        return file;
    }

    public String ocrText() {
        return ocrText;
    }

    public void setOcrText(String ocrText) {
        this.ocrText = ocrText;
    }

    public String displayName() {
        return file.getName();
    }

    @Override
    public String toString() {
        return displayName();
    }
}