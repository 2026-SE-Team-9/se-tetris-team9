package seoultech.se.tetris.ui.theme;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;

/**
 * 모든 화면이 함께 쓰는 레트로 아케이드 디자인의 색·글꼴·그리기 방법.
 *
 * <p>짙은 남보라 배경에 옅은 격자를 깔고, 하늘색·분홍·노랑 강조색으로 틀과 제목, 선택 항목을 그린다.
 * 빛 번짐 효과는 쓰지 않는다. 제목은 분홍 글자 뒤에 하늘색 글자를 비껴 겹쳐 입체감을 낸다.
 *
 * <p>블럭 색은 여기서 정하지 않는다. 블럭과 아이템, 색맹 모드의 색은 {@code BlockPalette}를 따른다.
 */
public final class RetroTheme {

    /** 배경 그라데이션의 위쪽 색 */
    public static final Color BACKGROUND_TOP = new Color(0x07, 0x06, 0x12);
    /** 배경 그라데이션의 아래쪽 색 */
    public static final Color BACKGROUND_BOTTOM = new Color(0x0E, 0x08, 0x20);
    /** 배경 격자 선 */
    public static final Color GRID = new Color(0x16, 0x12, 0x2E);
    /** 틀 안쪽을 채우는 색 */
    public static final Color SURFACE = new Color(0x0C, 0x0A, 0x1C);

    /** 틀과 테두리에 쓰는 하늘색 */
    public static final Color CYAN = new Color(0x3E, 0xF0, 0xFF);
    /** 제목과 보조 강조에 쓰는 분홍 */
    public static final Color PINK = new Color(0xFF, 0x3E, 0xA5);
    /** 선택한 항목과 중요한 값에 쓰는 노랑 */
    public static final Color YELLOW = new Color(0xFF, 0xE5, 0x3E);

    /** 기본 글자색 */
    public static final Color TEXT = new Color(0xF2, 0xF2, 0xFF);
    /** 선택하지 않은 항목과 보조 문구의 글자색 */
    public static final Color MUTED = new Color(0x8A, 0x7F, 0xC0);

    /** 배경 격자 한 칸의 크기(px) */
    public static final int GRID_SIZE = 24;
    /** 제목 그림자를 비껴 놓는 거리(px) */
    public static final int TITLE_SHADOW_OFFSET = 4;
    /** 틀 바깥선의 굵기(px) */
    public static final float FRAME_STROKE = 2f;
    /** 틀 안쪽 보조선까지의 간격(px) */
    public static final int FRAME_INSET = 4;

    private RetroTheme() {
    }

    /** 고정폭 글꼴. 레트로 느낌을 위해 메뉴와 라벨에 쓴다. 한글은 시스템의 대체 글꼴로 그린다. */
    public static Font mono(int style, int size) {
        return new Font(Font.MONOSPACED, style, size);
    }

    /** 글자와 도형의 가장자리를 매끄럽게 그린다. */
    public static void applyRenderingHints(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    /** 위에서 아래로 짙어지는 남보라 배경과 옅은 격자를 그린다. */
    public static void paintBackground(Graphics2D g2, int width, int height) {
        g2.setPaint(new GradientPaint(0, 0, BACKGROUND_TOP, 0, height, BACKGROUND_BOTTOM));
        g2.fillRect(0, 0, width, height);
        g2.setColor(GRID);
        for (int x = 0; x < width; x += GRID_SIZE) {
            g2.drawLine(x, 0, x, height);
        }
        for (int y = 0; y < height; y += GRID_SIZE) {
            g2.drawLine(0, y, width, y);
        }
    }

    /**
     * 각진 이중선 틀을 그린다. 안쪽을 {@link #SURFACE}로 채우고, 바깥선은 {@code line} 색으로,
     * 안쪽 보조선은 같은 색을 옅게 그린다.
     */
    public static void drawFrame(Graphics2D g2, int x, int y, int width, int height, Color line) {
        Stroke previous = g2.getStroke();
        g2.setColor(SURFACE);
        g2.fillRect(x, y, width, height);
        g2.setStroke(new BasicStroke(FRAME_STROKE));
        g2.setColor(line);
        g2.drawRect(x, y, width, height);
        g2.setStroke(new BasicStroke(1f));
        g2.setColor(withAlpha(line, 70));
        g2.drawRect(x + FRAME_INSET, y + FRAME_INSET, width - 2 * FRAME_INSET, height - 2 * FRAME_INSET);
        g2.setStroke(previous);
    }

    /**
     * 제목을 가운데에 그린다. 분홍 글자 뒤에 하늘색 글자를 오른쪽 아래로 비껴 겹친다. 빛 번짐은 쓰지 않는다.
     * 글꼴은 미리 정해 둔다.
     *
     * @param width 가운데를 맞출 영역의 너비. 영역은 x = 0에서 시작한다
     */
    public static void drawShadowTitle(Graphics2D g2, String text, int width, int baseline) {
        int x = centeredX(g2, text, 0, width);
        g2.setColor(CYAN);
        g2.drawString(text, x + TITLE_SHADOW_OFFSET, baseline + TITLE_SHADOW_OFFSET);
        g2.setColor(PINK);
        g2.drawString(text, x, baseline);
    }

    /**
     * 선택한 항목의 강조 상자를 그린다. 옅은 노랑으로 채우고 노란 테두리를 두르며, 왼쪽에 작은 픽셀 화살표를 둔다.
     * 항목 글자는 그 위에 {@link #YELLOW}로 따로 그린다.
     */
    public static void drawSelection(Graphics2D g2, int x, int y, int width, int height) {
        Stroke previous = g2.getStroke();
        g2.setColor(withAlpha(YELLOW, 28));
        g2.fillRect(x, y, width, height);
        g2.setStroke(new BasicStroke(1.5f));
        g2.setColor(YELLOW);
        g2.drawRect(x, y, width, height);
        g2.setStroke(previous);
        drawPixelArrow(g2, x + 12, y + (height - 14) / 2);
    }

    /** 오른쪽을 가리키는 14px 높이의 계단 모양 화살표. 왼쪽 위 모서리가 (x, y)다. */
    static void drawPixelArrow(Graphics2D g2, int x, int y) {
        g2.setColor(YELLOW);
        g2.fillRect(x, y, 4, 14);
        g2.fillRect(x + 4, y + 3, 4, 8);
        g2.fillRect(x + 8, y + 5, 4, 4);
    }

    /** 0부터 {@code width}까지의 영역 가운데에 글자를 그린다. 색과 글꼴은 미리 정해 둔다. */
    public static void drawCentered(Graphics2D g2, String text, int width, int baseline) {
        g2.drawString(text, centeredX(g2, text, 0, width), baseline);
    }

    /** {@code left}부터 {@code width}만큼의 영역에서 글자를 가운데에 놓을 x 좌표 */
    public static int centeredX(Graphics2D g2, String text, int left, int width) {
        FontMetrics metrics = g2.getFontMetrics();
        return left + (width - metrics.stringWidth(text)) / 2;
    }

    /** 같은 색의 투명도만 바꾼다. */
    public static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }
}
