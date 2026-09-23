package com.example.app.book;

import com.example.app.web.ApiException;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Normalise les couvertures envoyées : le fichier est décodé (le type déclaré par le navigateur n'est pas pris en
 * compte), réduit à {@value #MAX_SIZE} px au plus, puis réencodé en JPEG. Le réencodage supprime les métadonnées (EXIF,
 * position GPS) et tout contenu caché dans le fichier d'origine.
 */
@Component
class CoverImages {

    static final int MAX_SIZE = 800;
    private static final long MAX_SOURCE_PIXELS = 50_000_000; // protège contre les « bombes de décompression »
    private static final Set<String> ACCEPTED_FORMATS = Set.of("jpeg", "png");
    private static final float JPEG_QUALITY = 0.85f;

    byte[] toJpeg(byte[] upload) {
        try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(upload))) {
            Iterator<ImageReader> readers = in == null ? null : ImageIO.getImageReaders(in);
            if (readers == null || !readers.hasNext()) {
                throw unsupported();
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(in, true, true);
                if (!ACCEPTED_FORMATS.contains(reader.getFormatName().toLowerCase())) {
                    throw unsupported();
                }
                // Dimensions lues dans l'en-tête, avant de décoder les pixels
                if ((long) reader.getWidth(0) * reader.getHeight(0) > MAX_SOURCE_PIXELS) {
                    throw new ApiException(HttpStatus.UNPROCESSABLE_CONTENT, "L'image est trop grande.");
                }
                return encode(scale(reader.read(0)));
            } finally {
                reader.dispose();
            }
        } catch (IOException | RuntimeException e) {
            if (e instanceof ApiException api) {
                throw api;
            }
            throw unsupported(); // fichier corrompu ou variante non gérée (JPEG CMYK…)
        }
    }

    private static BufferedImage scale(BufferedImage source) {
        double ratio = Math.min(1.0, (double) MAX_SIZE / Math.max(source.getWidth(), source.getHeight()));
        int width = Math.max(1, (int) Math.round(source.getWidth() * ratio));
        int height = Math.max(1, (int) Math.round(source.getHeight() * ratio));
        BufferedImage target = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = target.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setColor(Color.WHITE); // fond des PNG transparents
            g.fillRect(0, 0, width, height);
            g.drawImage(source, 0, 0, width, height, null);
        } finally {
            g.dispose();
        }
        return target;
    }

    private static byte[] encode(BufferedImage image) throws IOException {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ImageOutputStream out = ImageIO.createImageOutputStream(bytes)) {
            writer.setOutput(out);
            ImageWriteParam params = writer.getDefaultWriteParam();
            params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            params.setCompressionQuality(JPEG_QUALITY);
            writer.write(null, new IIOImage(image, null, null), params);
        } finally {
            writer.dispose();
        }
        return bytes.toByteArray();
    }

    private static ApiException unsupported() {
        return new ApiException(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Format d'image non pris en charge. Utilisez une image JPEG ou PNG.");
    }
}
