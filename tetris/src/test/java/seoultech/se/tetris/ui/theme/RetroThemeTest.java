package seoultech.se.tetris.ui.theme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;

class RetroThemeTest {

    private static BufferedImage image(int width, int height) {
        return new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    }

    private static boolean contains(BufferedImage image, Color color, int x0, int y0, int x1, int y1) {
        for (int x = x0; x < x1; x++) {
            for (int y = y0; y < y1; y++) {
                if (image.getRGB(x, y) == color.getRGB()) {
                    return true;
                }
            }
        }
        return false;
    }

    @Test
    void 배경은_위가_밝고_아래가_짙은_남보라_그라데이션에_격자를_그린다() {
        BufferedImage image = image(100, 100);
        Graphics2D g2 = image.createGraphics();

        RetroTheme.paintBackground(g2, 100, 100);

        // 격자 선 위
        assertEquals(RetroTheme.GRID.getRGB(), image.getRGB(RetroTheme.GRID_SIZE, 10));
        // 격자 사이의 위쪽은 위쪽 색에 가깝다
        Color top = new Color(image.getRGB(5, 1));
        assertTrue(Math.abs(top.getBlue() - RetroTheme.BACKGROUND_TOP.getBlue()) <= 2);
        Color bottom = new Color(image.getRGB(5, 99));
        assertTrue(bottom.getBlue() > top.getBlue());
    }

    @Test
    void 틀은_안쪽을_채우고_바깥선과_옅은_안쪽선을_그린다() {
        BufferedImage image = image(100, 100);
        Graphics2D g2 = image.createGraphics();

        RetroTheme.drawFrame(g2, 10, 10, 80, 80, RetroTheme.CYAN);

        assertEquals(RetroTheme.CYAN.getRGB(), image.getRGB(50, 10));
        assertEquals(RetroTheme.SURFACE.getRGB(), image.getRGB(50, 50));
        // 안쪽선은 같은 색을 옅게 겹쳐 바탕과 하늘색 사이의 색이 된다
        Color inner = new Color(image.getRGB(50, 10 + RetroTheme.FRAME_INSET));
        assertTrue(inner.getBlue() > RetroTheme.SURFACE.getBlue() && inner.getBlue() < RetroTheme.CYAN.getBlue());
    }

    @Test
    void 제목은_분홍_글자와_비껴_겹친_하늘색_그림자로_그린다() {
        BufferedImage image = image(300, 100);
        Graphics2D g2 = image.createGraphics();
        g2.setFont(RetroTheme.mono(Font.BOLD, 48));

        RetroTheme.drawShadowTitle(g2, "TETRIS", 300, 70);

        assertTrue(contains(image, RetroTheme.PINK, 0, 0, 300, 100));
        assertTrue(contains(image, RetroTheme.CYAN, 0, 0, 300, 100));
        assertEquals(4, RetroTheme.TITLE_SHADOW_OFFSET);
    }

    @Test
    void 선택_상자는_노란_테두리와_화살표를_그리고_빛_번짐이_없다() {
        BufferedImage image = image(200, 60);
        Graphics2D g2 = image.createGraphics();

        RetroTheme.drawSelection(g2, 20, 10, 160, 34);

        assertEquals(RetroTheme.YELLOW.getRGB(), image.getRGB(100, 10));
        // 화살표의 첫 기둥
        assertEquals(RetroTheme.YELLOW.getRGB(), image.getRGB(33, 27));
        // 상자 바깥은 그리지 않는다
        assertFalse(contains(image, RetroTheme.YELLOW, 0, 0, 200, 8));
        assertFalse(contains(image, RetroTheme.YELLOW, 0, 47, 200, 60));
    }

    @Test
    void 가운데_정렬과_투명도_변경을_돕는다() {
        BufferedImage image = image(200, 40);
        Graphics2D g2 = image.createGraphics();
        RetroTheme.applyRenderingHints(g2);
        g2.setFont(RetroTheme.mono(Font.PLAIN, 12));
        int width = g2.getFontMetrics().stringWidth("AB");

        assertEquals(10 + (100 - width) / 2, RetroTheme.centeredX(g2, "AB", 10, 100));
        g2.setColor(RetroTheme.TEXT);
        RetroTheme.drawCentered(g2, "AB", 200, 30);
        assertTrue(contains(image, RetroTheme.TEXT, 80, 10, 120, 32));

        Color faded = RetroTheme.withAlpha(RetroTheme.PINK, 40);
        assertEquals(40, faded.getAlpha());
        assertEquals(RetroTheme.PINK.getRed(), faded.getRed());
        Font mono = RetroTheme.mono(Font.BOLD, 10);
        assertEquals(Font.MONOSPACED, mono.getName());
        assertEquals(Font.BOLD, mono.getStyle());
        assertEquals(10, mono.getSize());
    }
}
