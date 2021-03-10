package net.sjw.blog.utils;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

public class QrCodeUtils {

    public static int QRCODE_SIZE= 300;
    public static String format= "png";
    public static String RESPONSE_CONTENT_TYPE= "image/png";

    public static byte[] encodeQrCode(String text) {
        try{
            Map<EncodeHintType, Object> hints = new HashMap<>();

            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);


            BitMatrix bitMatrix = new MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, QRCODE_SIZE, QRCODE_SIZE);
            bitMatrix = deleteWhite(bitMatrix);
            BufferedImage bufferedImage = toBufferedImage(bitMatrix);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, format, out);
            return out.toByteArray();
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public static final int BORDER_WIDTH = 4;

    public static BitMatrix deleteWhite(BitMatrix matrix) {
        int[] rec = matrix.getEnclosingRectangle();

        int resWidth = rec[2] + BORDER_WIDTH;
        int resHeight = rec[3] + BORDER_WIDTH;
        BitMatrix resMatrix = new BitMatrix(resWidth, resHeight);
        for (int i = BORDER_WIDTH; i < resWidth; i++) {
            for (int j = BORDER_WIDTH; j < resHeight; j++) {
                if (matrix.get(i+rec[0],j+rec[1])) resMatrix.set(i, j);
            }
        }
        return resMatrix;
    }

    public static BufferedImage toBufferedImage(BitMatrix matrix) {
        int width = matrix.getWidth();
        int height = matrix.getHeight();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        int onColor = 0xFF000000;
        int offColor = 0xFFFFFFFF;
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                image.setRGB(i, j, matrix.get(i, j) ? onColor : offColor);
            }
        }
        return image;
    }
}
