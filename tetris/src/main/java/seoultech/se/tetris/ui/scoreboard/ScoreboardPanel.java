package seoultech.se.tetris.ui.scoreboard;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.JPanel;

import seoultech.se.tetris.engine.GameMode;

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
        setBackground(Color.BLACK);
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

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g2.setFont(new Font(Font.DIALOG, Font.BOLD, 32));
        g2.setColor(Color.WHITE);
        drawCentered(g2, TITLE, 60);

        g2.setFont(new Font(Font.DIALOG, Font.BOLD, 20));
        g2.setColor(Color.CYAN);
        drawCentered(g2, "◀  " + modeLabel(model.getMode()) + "  ▶", 100);

        boolean failed = model.isStorageFailed();
        ScoreboardTablePainter.paint(g2, getWidth(), 150, model.getRecords(), 0,
                failed ? STORAGE_ERROR : EMPTY_MESSAGE, failed ? Color.RED : Color.GRAY);

        g2.setFont(new Font(Font.DIALOG, Font.PLAIN, 14));
        g2.setColor(Color.WHITE);
        drawCentered(g2, KEY_HINT, getHeight() - 30);
        g2.dispose();
    }

    private void drawCentered(Graphics2D g2, String text, int y) {
        FontMetrics metrics = g2.getFontMetrics();
        g2.drawString(text, (getWidth() - metrics.stringWidth(text)) / 2, y);
    }
}
