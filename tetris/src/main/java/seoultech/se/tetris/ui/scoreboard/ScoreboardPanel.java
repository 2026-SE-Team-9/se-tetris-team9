package seoultech.se.tetris.ui.scoreboard;

import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.JPanel;

import seoultech.se.tetris.engine.GameMode;
import seoultech.se.tetris.ui.theme.RetroTheme;

/** 시작 메뉴에서 여는, 모드별 상위 기록을 보여주는 스코어보드 화면. */
public class ScoreboardPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final String TITLE = "스코어보드";
    private static final String EMPTY_MESSAGE = "등록된 기록이 없습니다";
    private static final String STORAGE_ERROR = "기록을 불러오지 못했습니다";
    private static final String KEY_HINT = "← → 모드 전환   Enter / Esc 메뉴로";

    private final transient ScoreboardModel model;

    public ScoreboardPanel(ScoreboardModel model) {
        this.model = model;
        setBackground(RetroTheme.BACKGROUND_TOP);
        setPreferredSize(new Dimension(480, 480));
        setFocusable(true);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent event) {
                handleKey(event.getKeyCode());
            }
        });
    }

    private void handleKey(int keyCode) {
        switch (keyCode) {
            case KeyEvent.VK_LEFT -> model.previousMode();
            case KeyEvent.VK_RIGHT -> model.nextMode();
            case KeyEvent.VK_ENTER, KeyEvent.VK_ESCAPE -> model.back();
            default -> { }
        }
        repaint();
    }

    static String modeLabel(GameMode mode) {
        return switch (mode) {
            case NORMAL -> "일반 모드";
            case ITEM -> "아이템 모드";
        };
    }

    /** 모드 전환 표시를 감싸는 상자 */
    static final Rectangle MODE_BOX = new Rectangle(120, 76, 240, 34);
    /** 표를 감싸는 틀. 종료 화면의 표 틀과 같은 위치다 */
    static final Rectangle TABLE_FRAME = new Rectangle(20, 124, 440, 284);

    /**
     * 레트로 디자인({@link RetroTheme})으로 그린다. 분홍 제목과 하늘색 그림자, 노란 상자 안의 모드 전환 표시,
     * 하늘색 틀 안의 표, 아래 키 안내를 그린다.
     */
    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g2 = (Graphics2D) graphics.create();
        RetroTheme.applyRenderingHints(g2);
        RetroTheme.paintBackground(g2, getWidth(), getHeight());

        g2.setFont(RetroTheme.mono(Font.BOLD, 32));
        RetroTheme.drawShadowTitle(g2, TITLE, getWidth(), 56);

        // 좌우 키로 바꾸는 항목이므로 노란 상자 안에 ◀ ▶와 함께 그린다
        g2.setColor(RetroTheme.withAlpha(RetroTheme.YELLOW, 28));
        g2.fillRect(MODE_BOX.x, MODE_BOX.y, MODE_BOX.width, MODE_BOX.height);
        g2.setStroke(new BasicStroke(1.5f));
        g2.setColor(RetroTheme.YELLOW);
        g2.drawRect(MODE_BOX.x, MODE_BOX.y, MODE_BOX.width, MODE_BOX.height);
        g2.setFont(RetroTheme.mono(Font.BOLD, 18));
        RetroTheme.drawCentered(g2, "◀  " + modeLabel(model.getMode()) + "  ▶", getWidth(), MODE_BOX.y + 24);

        RetroTheme.drawFrame(g2, TABLE_FRAME.x, TABLE_FRAME.y, TABLE_FRAME.width, TABLE_FRAME.height,
                RetroTheme.CYAN);
        boolean failed = model.isStorageFailed();
        ScoreboardTablePainter.paint(g2, getWidth(), 150, model.getRecords(), 0,
                failed ? STORAGE_ERROR : EMPTY_MESSAGE, failed ? RetroTheme.PINK : RetroTheme.MUTED);

        g2.setFont(RetroTheme.mono(Font.PLAIN, 12));
        g2.setColor(RetroTheme.MUTED);
        RetroTheme.drawCentered(g2, KEY_HINT, getWidth(), getHeight() - 30);
        g2.dispose();
    }
}
