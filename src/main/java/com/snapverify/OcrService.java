package com.snapverify;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

public class OcrService {

    private final Tesseract tesseract;

    public OcrService() {
        tesseract = new Tesseract();

        tesseract.setDatapath("C:\\Program Files\\Tesseract-OCR\\tessdata");

        // Language
        tesseract.setLanguage("eng");

        // Better for screenshots
        tesseract.setPageSegMode(6);
    }

    public String extractText(File file) throws Exception {
        System.out.println("Running OCR on: " + file.getAbsolutePath());

        try {
            // Step 1: Read image
            BufferedImage original = ImageIO.read(file);

            if (original == null) {
                throw new Exception("Unsupported or unreadable image: " + file.getName());
            }

            // Step 2: Convert to grayscale (improves OCR a LOT)
            BufferedImage gray = new BufferedImage(
                    original.getWidth(),
                    original.getHeight(),
                    BufferedImage.TYPE_BYTE_GRAY
            );

            Graphics g = gray.getGraphics();
            g.drawImage(original, 0, 0, null);
            g.dispose();

            // Step 3: Run OCR
            String text = tesseract.doOCR(gray);

            // Step 4: Debug output
            if (text == null || text.trim().isEmpty()) {
                System.out.println("OCR returned EMPTY text");
            } else {
                System.out.println("OCR SUCCESS:\n" + text);
            }

            return text;

        } catch (TesseractException e) {
            System.out.println("OCR ERROR: " + e.getMessage());
            throw new Exception("OCR failed: " + e.getMessage());
        }
    }
}