package com.buguagaoshu.community.captcha;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;

/**
 * 内置图形验证码生成器。
 *
 * <p>原项目依赖 libs 下的 EasyCaptcha（system scope + javax.servlet），
 * 无法在 Spring Boot 4 / Jakarta EE 11 下使用。这里用 JDK 自带的 Java2D
 * 自行生成 PNG 验证码，随机数使用 SecureRandom，避免旧版 kaptcha /
 * EasyCaptcha 的 Random 安全问题（CVE-2018-18531）。
 */
public final class ImageCaptcha {

    /** 与旧版 EasyCaptcha 默认尺寸保持一致，避免前端页面布局变化 */
    public static final int WIDTH = 130;
    public static final int HEIGHT = 48;
    public static final int LENGTH = 5;

    /** 去掉 0/O/1/l/I 等易混淆字符 */
    private static final char[] CHARS =
            "23456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz".toCharArray();

    private static final SecureRandom RANDOM = new SecureRandom();

    private final String text;
    private final byte[] png;

    private ImageCaptcha(String text, byte[] png) {
        this.text = text;
        this.png = png;
    }

    public String text() {
        return text;
    }

    public byte[] png() {
        return png;
    }

    public static ImageCaptcha generate() throws IOException {
        char[] chars = new char[LENGTH];
        for (int i = 0; i < LENGTH; i++) {
            chars[i] = CHARS[RANDOM.nextInt(CHARS.length)];
        }

        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // 背景
            g.setColor(new Color(245, 247, 250));
            g.fillRect(0, 0, WIDTH, HEIGHT);

            // 背景色块干扰
            for (int i = 0; i < 12; i++) {
                g.setColor(new Color(210 + RANDOM.nextInt(40), 210 + RANDOM.nextInt(40), 210 + RANDOM.nextInt(40)));
                int w = 8 + RANDOM.nextInt(24);
                int h = 6 + RANDOM.nextInt(18);
                g.fillOval(RANDOM.nextInt(WIDTH), RANDOM.nextInt(HEIGHT), w, h);
            }

            // 干扰线
            g.setStroke(new BasicStroke(1.2f));
            for (int i = 0; i < 8; i++) {
                g.setColor(new Color(170 + RANDOM.nextInt(60), 170 + RANDOM.nextInt(60), 170 + RANDOM.nextInt(60)));
                g.drawLine(RANDOM.nextInt(WIDTH), RANDOM.nextInt(HEIGHT),
                        RANDOM.nextInt(WIDTH), RANDOM.nextInt(HEIGHT));
            }

            // 逐字符绘制：随机字号、颜色、旋转角度
            int charWidth = (WIDTH - 12) / LENGTH;
            for (int i = 0; i < LENGTH; i++) {
                int fontSize = 26 + RANDOM.nextInt(6);
                Font font = new Font("SansSerif", Font.BOLD, fontSize);
                g.setFont(font);
                g.setColor(new Color(30 + RANDOM.nextInt(80), 30 + RANDOM.nextInt(80), 30 + RANDOM.nextInt(80)));

                AffineTransform original = g.getTransform();
                AffineTransform transform = new AffineTransform(original);
                double x = 8 + i * charWidth + RANDOM.nextInt(4);
                double y = HEIGHT / 2.0 + 9 - RANDOM.nextInt(5);
                transform.rotate((RANDOM.nextDouble() - 0.5) * 0.5, x, y);
                g.setTransform(transform);
                g.drawString(String.valueOf(chars[i]), (float) x, (float) y);
                g.setTransform(original);
            }

            // 细边框
            g.setColor(new Color(210, 216, 224));
            g.drawRect(0, 0, WIDTH - 1, HEIGHT - 1);
        } finally {
            g.dispose();
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return new ImageCaptcha(new String(chars), out.toByteArray());
    }
}
