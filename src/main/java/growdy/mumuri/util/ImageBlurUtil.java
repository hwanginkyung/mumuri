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

    /**
     * downScaleFactor가 클수록 더 강한 블러
     * 예) 12(약함) -> 18(중간) -> 24(강함)
     */
    public static byte[] blurToJpeg(InputStream input, int downScaleFactor) throws IOException {
        BufferedImage src = ImageIO.read(input);
        if (src == null) throw new IOException("Unsupported image format");

        int factor = Math.max(2, downScaleFactor);
        int w2 = Math.max(1, src.getWidth() / factor);
        int h2 = Math.max(1, src.getHeight() / factor);

        BufferedImage small = new BufferedImage(w2, h2, BufferedImage.TYPE_INT_RGB);
        Graphics2D g1 = small.createGraphics();
        g1.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g1.drawImage(src, 0, 0, w2, h2, null);
        g1.dispose();

        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = out.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(small, 0, 0, src.getWidth(), src.getHeight(), null);
        g2.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(out, "jpg", baos);
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

