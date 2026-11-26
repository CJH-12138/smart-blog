package com.blog.service.storage;

import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;

public class ImageStorageProcessor {

    private static final long JPEG_HIGH_QUALITY_THRESHOLD = 2L * 1024 * 1024;
    private static final int JPEG_MAX_DIMENSION = 2560;
    private static final float JPEG_QUALITY_HIGH = 0.95f;
    private static final float JPEG_QUALITY_LARGE = 0.92f;
    private static final float PNG_COMPRESSION = 0.9f;

    public StoredFile toStoredFile(MultipartFile file) throws IOException {
        String extension = normalizeImageExtension(file.getOriginalFilename(), file.getContentType());
        if (extension == null) {
            return originalFile(file);
        }

        BufferedImage sourceImage;
        try (var inputStream = file.getInputStream()) {
            sourceImage = ImageIO.read(inputStream);
        }
        if (sourceImage == null) {
            return originalFile(file);
        }

        BufferedImage outputImage = resizeIfNeeded(sourceImage, extension);
        byte[] content = writeImage(outputImage, extension, file.getSize());
        String contentType = ".png".equals(extension) ? "image/png" : "image/jpeg";
        return new StoredFile(content, extension, contentType);
    }

    public StoredFile originalFile(MultipartFile file) throws IOException {
        return new StoredFile(file.getBytes(), getFileExtension(file.getOriginalFilename()), file.getContentType());
    }

    private String getFileExtension(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            return "";
        }
        return originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase(Locale.ROOT);
    }

    private String normalizeImageExtension(String originalFilename, String contentType) {
        String extension = getFileExtension(originalFilename);
        if (".jpg".equals(extension) || ".jpeg".equals(extension) || "image/jpeg".equalsIgnoreCase(contentType)) {
            return ".jpg";
        }
        if (".png".equals(extension) || "image/png".equalsIgnoreCase(contentType)) {
            return ".png";
        }
        return null;
    }

    private BufferedImage resizeIfNeeded(BufferedImage sourceImage, String extension) {
        if (!".jpg".equals(extension)) {
            return sourceImage;
        }
        int width = sourceImage.getWidth();
        int height = sourceImage.getHeight();
        int maxDimension = Math.max(width, height);
        if (maxDimension <= JPEG_MAX_DIMENSION) {
            return sourceImage;
        }
        double scale = (double) JPEG_MAX_DIMENSION / maxDimension;
        int targetWidth = Math.max(1, (int) Math.round(width * scale));
        int targetHeight = Math.max(1, (int) Math.round(height * scale));
        BufferedImage resized = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = resized.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.drawImage(sourceImage, 0, 0, targetWidth, targetHeight, null);
        graphics.dispose();
        return resized;
    }

    private byte[] writeImage(BufferedImage image, String extension, long originalSize) throws IOException {
        String formatName = ".png".equals(extension) ? "png" : "jpg";
        BufferedImage outputImage = ".jpg".equals(extension) ? toJpegImage(image) : image;
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(formatName);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        if (!writers.hasNext()) {
            ImageIO.write(outputImage, formatName, bytes);
            return bytes.toByteArray();
        }

        ImageWriter writer = writers.next();
        try (ImageOutputStream outputStream = ImageIO.createImageOutputStream(bytes)) {
            writer.setOutput(outputStream);
            ImageWriteParam writeParam = writer.getDefaultWriteParam();
            if (writeParam.canWriteCompressed()) {
                writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                if (".jpg".equals(extension)) {
                    writeParam.setCompressionQuality(originalSize > JPEG_HIGH_QUALITY_THRESHOLD ? JPEG_QUALITY_LARGE : JPEG_QUALITY_HIGH);
                } else {
                    writeParam.setCompressionQuality(PNG_COMPRESSION);
                }
            }
            writer.write(null, new IIOImage(outputImage, null, null), writeParam);
        } finally {
            writer.dispose();
        }
        return bytes.toByteArray();
    }

    private BufferedImage toJpegImage(BufferedImage sourceImage) {
        if (sourceImage.getType() == BufferedImage.TYPE_INT_RGB) {
            return sourceImage;
        }
        BufferedImage rgbImage = new BufferedImage(sourceImage.getWidth(), sourceImage.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = rgbImage.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.drawImage(sourceImage, 0, 0, null);
        graphics.dispose();
        return rgbImage;
    }
}
