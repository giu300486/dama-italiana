package com.damaitaliana.client.buildtools;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;

/**
 * One-shot build tool that generates {@code assets/icons/app-icon.ico} for the Windows installer
 * (Task 3.5.12). Run with:
 *
 * <pre>
 *   mvn -pl client exec:java
 *       -Dexec.classpathScope=test
 *       -Dexec.mainClass="com.damaitaliana.client.buildtools.AppIconGenerator"
 *       -Dexec.args="src/main/resources/assets/icons/app-icon.ico"
 * </pre>
 *
 * <p>The icon is a wood-themed disc (walnut radial gradient + gold rim) with a stylised crown glyph
 * — same visual vocabulary as the in-app king markers. Written as a multi-resolution ICO with PNG
 * payloads at 16/32/48/64/128/256 px. The PNG-in-ICO format is supported on Windows Vista+ which
 * comfortably covers our SPEC §1 target of Windows 10/11.
 *
 * <p>This is build-time tooling, not runtime code: it lives under {@code test/} so it stays out of
 * the production jar (same pattern as {@code OggToWavConverter}).
 */
public final class AppIconGenerator {

  private static final int[] SIZES = {16, 32, 48, 64, 128, 256};

  // Wood theme palette (mirrors theme-light.css tokens).
  private static final Color WALNUT = new Color(0x6B, 0x42, 0x26);
  private static final Color DEEP_WALNUT = new Color(0x3E, 0x26, 0x14);
  private static final Color GOLD = new Color(0xC8, 0xA0, 0x4A);
  private static final Color GOLD_LIGHT = new Color(0xE8, 0xC9, 0x7B);
  private static final Color CREAM = new Color(0xF5, 0xEC, 0xD7);

  private AppIconGenerator() {}

  public static void main(String[] args) throws IOException {
    Path output =
        Path.of(args.length >= 1 ? args[0] : "src/main/resources/assets/icons/app-icon.ico");
    Files.createDirectories(output.getParent());

    byte[][] pngs = new byte[SIZES.length][];
    for (int i = 0; i < SIZES.length; i++) {
      BufferedImage img = renderIcon(SIZES[i]);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ImageIO.write(img, "PNG", baos);
      pngs[i] = baos.toByteArray();
    }

    try (OutputStream out = Files.newOutputStream(output)) {
      writeIco(out, SIZES, pngs);
    }
    System.out.println("Wrote " + output.toAbsolutePath() + " (" + Files.size(output) + " bytes)");
  }

  private static BufferedImage renderIcon(int size) {
    BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = img.createGraphics();
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

    double s = size;
    double pad = s * 0.04;
    double diameter = s - 2 * pad;
    double cx = s / 2.0;
    double cy = s / 2.0;
    double radius = diameter / 2.0;

    // Walnut disc with radial gradient (bright top-left → deep-walnut bottom-right).
    g.setPaint(
        new RadialGradientPaint(
            (float) (cx - radius * 0.35),
            (float) (cy - radius * 0.35),
            (float) radius,
            new float[] {0f, 1f},
            new Color[] {WALNUT, DEEP_WALNUT}));
    g.fill(new Ellipse2D.Double(pad, pad, diameter, diameter));

    // Gold rim.
    float rimWidth = (float) Math.max(1.0, s * 0.025);
    g.setPaint(GOLD);
    g.setStroke(new java.awt.BasicStroke(rimWidth));
    g.draw(new Ellipse2D.Double(pad, pad, diameter, diameter));

    // Crown glyph centred on the disc. Smaller sizes get a simplified version because fine
    // points alias into mush below ~24 px.
    if (size >= 24) {
      drawCrown(g, cx, cy, radius * 0.62);
    } else {
      drawCrownSimple(g, cx, cy, radius * 0.55);
    }

    g.dispose();
    return img;
  }

  private static void drawCrown(Graphics2D g, double cx, double cy, double r) {
    // 5-point crown silhouette: base trapezoid + 5 spikes with ball tips.
    Path2D.Double crown = new Path2D.Double();
    double baseY = cy + r * 0.55;
    double topY = cy - r * 0.55;
    double midY = cy - r * 0.05;
    double leftX = cx - r;
    double rightX = cx + r;

    crown.moveTo(leftX, baseY);
    crown.lineTo(leftX, midY);
    crown.lineTo(cx - r * 0.6, topY + r * 0.25); // dip 1
    crown.lineTo(cx - r * 0.32, midY - r * 0.05); // valley
    crown.lineTo(cx, topY); // centre spike
    crown.lineTo(cx + r * 0.32, midY - r * 0.05);
    crown.lineTo(cx + r * 0.6, topY + r * 0.25);
    crown.lineTo(rightX, midY);
    crown.lineTo(rightX, baseY);
    crown.closePath();

    g.setPaint(
        new RadialGradientPaint(
            (float) cx,
            (float) (cy - r * 0.2),
            (float) r,
            new float[] {0f, 1f},
            new Color[] {GOLD_LIGHT, GOLD}));
    g.fill(crown);
    g.setPaint(DEEP_WALNUT);
    g.setStroke(new java.awt.BasicStroke((float) Math.max(0.8, r * 0.04)));
    g.draw(crown);

    // Ball tips on each spike.
    double ballR = r * 0.13;
    drawBall(g, cx - r * 0.95, midY, ballR);
    drawBall(g, cx - r * 0.6, topY + r * 0.18, ballR);
    drawBall(g, cx, topY - ballR * 0.4, ballR);
    drawBall(g, cx + r * 0.6, topY + r * 0.18, ballR);
    drawBall(g, cx + r * 0.95, midY, ballR);

    // Cream gem in the centre band.
    g.setPaint(CREAM);
    double gemR = r * 0.16;
    g.fill(new Ellipse2D.Double(cx - gemR, cy + r * 0.15 - gemR, gemR * 2, gemR * 2));
  }

  private static void drawCrownSimple(Graphics2D g, double cx, double cy, double r) {
    // For tiny icons (16 px): a chunky crown-shape rectangle with 3 points.
    Path2D.Double crown = new Path2D.Double();
    double baseY = cy + r * 0.5;
    double topY = cy - r * 0.5;
    double leftX = cx - r;
    double rightX = cx + r;

    crown.moveTo(leftX, baseY);
    crown.lineTo(leftX, cy - r * 0.05);
    crown.lineTo(cx - r * 0.5, topY + r * 0.4);
    crown.lineTo(cx, topY);
    crown.lineTo(cx + r * 0.5, topY + r * 0.4);
    crown.lineTo(rightX, cy - r * 0.05);
    crown.lineTo(rightX, baseY);
    crown.closePath();

    g.setPaint(GOLD);
    g.fill(crown);
  }

  private static void drawBall(Graphics2D g, double x, double y, double r) {
    g.setPaint(GOLD_LIGHT);
    g.fill(new Ellipse2D.Double(x - r, y - r, r * 2, r * 2));
    g.setPaint(DEEP_WALNUT);
    g.setStroke(new java.awt.BasicStroke((float) Math.max(0.6, r * 0.2)));
    g.draw(new Ellipse2D.Double(x - r, y - r, r * 2, r * 2));
  }

  /**
   * Writes a Windows ICO container with PNG payloads. Format reference: Microsoft ICONDIR /
   * ICONDIRENTRY (PNG-in-ICO supported on Vista+).
   */
  private static void writeIco(OutputStream out, int[] sizes, byte[][] pngs) throws IOException {
    int n = sizes.length;
    int headerSize = 6 + 16 * n;
    int offset = headerSize;

    ByteBuffer header = ByteBuffer.allocate(headerSize).order(ByteOrder.LITTLE_ENDIAN);
    header.putShort((short) 0); // reserved
    header.putShort((short) 1); // type = icon
    header.putShort((short) n); // image count
    for (int i = 0; i < n; i++) {
      int size = sizes[i];
      // Width/Height: 0 means 256.
      header.put((byte) (size >= 256 ? 0 : size));
      header.put((byte) (size >= 256 ? 0 : size));
      header.put((byte) 0); // colorCount (0 for ≥256 colors)
      header.put((byte) 0); // reserved
      header.putShort((short) 1); // planes
      header.putShort((short) 32); // bit count
      header.putInt(pngs[i].length); // size in bytes
      header.putInt(offset); // offset to image data
      offset += pngs[i].length;
    }
    out.write(header.array());
    for (byte[] png : pngs) {
      out.write(png);
    }
  }
}
