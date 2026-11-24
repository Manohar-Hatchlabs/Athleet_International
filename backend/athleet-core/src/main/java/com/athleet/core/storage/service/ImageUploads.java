package com.athleet.core.storage.service;

import com.athleet.core.common.error.BusinessRuleException;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

/**
 * Validates an uploaded picture and re-encodes it as a fresh JPEG. Re-encoding drops EXIF data (including GPS
 * coordinates, which are personal data) and neutralises files that are valid in two formats at once.
 */
final class ImageUploads {

    static final long MAX_BYTES = 5L * 1024 * 1024;
    static final int MAX_DIMENSION = 4096;
    static final String OUTPUT_TYPE = "image/jpeg";

    private static final Set<String> EXTENSIONS = Set.of("jpg", "jpeg", "png");
    private static final Set<String> CONTENT_TYPES = Set.of("image/jpeg", "image/png");

    private ImageUploads() {
    }

    /** @return the JPEG bytes to store */
    static byte[] normalise(String originalName, String declaredType, byte[] data) {
        if (data == null || data.length == 0 || data.length > MAX_BYTES) {
            throw invalid("The picture must be between 1 byte and 5 MB.");
        }
        String ext = extension(originalName);
        String type = declaredType == null ? "" : declaredType.toLowerCase(Locale.ROOT);
        if (!EXTENSIONS.contains(ext) || !CONTENT_TYPES.contains(type)) {
            throw invalid("Only JPEG and PNG pictures are accepted.");
        }
        String detected = detect(data);
        boolean jpegExt = ext.equals("jpg") || ext.equals("jpeg");
        if (detected == null || !detected.equals(type) || jpegExt != detected.equals("image/jpeg")) {
            throw invalid("The file content does not match its type.");
        }
        BufferedImage image = decode(data, detected);
        return encodeJpeg(image);
    }

    private static BufferedImage decode(byte[] data, String detectedType) {
        try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(data))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
            if (!readers.hasNext()) {
                throw invalid("The picture could not be read.");
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(in, true, true);
                int w = reader.getWidth(0);
                int h = reader.getHeight(0);
                if (w < 1 || h < 1 || w > MAX_DIMENSION || h > MAX_DIMENSION) {
                    throw invalid("The picture is too large. Maximum size is 4096 x 4096 pixels.");
                }
                return reader.read(0);
            } finally {
                reader.dispose();
            }
        } catch (IOException | RuntimeException e) {
            if (e instanceof BusinessRuleException b) {
                throw b;
            }
            throw invalid("The picture could not be read.");
        }
    }

    private static byte[] encodeJpeg(BufferedImage source) {
        // Flatten any transparency onto white and drop every metadata block by writing a brand new image.
        BufferedImage rgb = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgb.createGraphics();
        try {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, rgb.getWidth(), rgb.getHeight());
            g.drawImage(source, 0, 0, null);
        } finally {
            g.dispose();
        }
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             MemoryCacheImageOutputStream ios = new MemoryCacheImageOutputStream(out)) {
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(0.85f);
            writer.setOutput(ios);
            writer.write(null, new IIOImage(rgb, null, null), param);
            ios.flush();
            return out.toByteArray();
        } catch (IOException e) {
            throw invalid("The picture could not be processed.");
        } finally {
            writer.dispose();
        }
    }

    private static String detect(byte[] d) {
        if (d.length >= 3 && (d[0] & 0xFF) == 0xFF && (d[1] & 0xFF) == 0xD8 && (d[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        byte[] png = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        if (d.length >= png.length) {
            for (int i = 0; i < png.length; i++) {
                if (d[i] != png[i]) {
                    return null;
                }
            }
            return "image/png";
        }
        return null;
    }

    private static String extension(String name) {
        if (name == null) {
            return "";
        }
        int i = name.lastIndexOf('.');
        return i < 0 ? "" : name.substring(i + 1).toLowerCase(Locale.ROOT);
    }

    private static BusinessRuleException invalid(String message) {
        return new BusinessRuleException("INVALID_FILE", message);
    }
}
