package growdy.mumuri.util;


import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public final class ImageBlurUtil {

    private ImageBlurUtil() {}

    public static byte[] blurToJpeg(InputStream input, int radius) throws IOException {
        BufferedImage src = ImageIO.read(input);
        if (src == null) throw new IOException("Unsupported image format");

        BufferedImage rgb = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgb.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, 0, 0, null);
        g.dispose();

        BufferedImage blurred = boxBlur(rgb, radius);
        blurred = boxBlur(blurred, Math.max(1, radius / 2));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(blurred, "jpg", baos);
        return baos.toByteArray();
    }

    private static BufferedImage boxBlur(BufferedImage src, int radius) {
        int r = Math.max(1, radius);
        int size = r * 2 + 1;
        float weight = 1.0f / (size * size);
        float[] data = new float[size * size];
        for (int i = 0; i < data.length; i++) data[i] = weight;

        Kernel kernel = new Kernel(size, size, data);
        ConvolveOp op = new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null);

        BufferedImage dst = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
        op.filter(src, dst);
        return dst;
    }
}

