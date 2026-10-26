package seoultech.se.tetris.ui.end;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;

import javax.swing.JPanel;

import seoultech.se.tetris.ui.menu.MenuItem;
import seoultech.se.tetris.ui.scoreboard.ScoreboardTablePainter;

/** 최종 점수, 이름 입력, 스코어보드와 종료 후 선택지를 그리는 게임 종료 화면. */
public class EndScreenPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final String TITLE = "게임 종료";
    private static final String EMPTY_MESSAGE = "등록된 기록이 없습니다";
    private static final String STORAGE_ERROR = "기록을 불러오거나 저장하지 못했습니다";
    private static final String NAME_PROMPT = "순위에 올랐습니다! 이름을 입력하세요";
    private static final String NAME_HINT = "Enter 등록   Backspace 지우기";
    private static final String KEY_HINT = "사용 가능한 키:  ↑ ↓ 이동   Enter 선택";

    private final transient EndScreenModel model;

    public EndScreenPanel(EndScreenModel model) {
        this.model = model;
        setBackground(Color.BLACK);
        setPreferredSize(new Dimension(480, 560));
        setFocusable(true);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent event) {
                handleKey(event.getKeyCode());
            }

            @Override
            public void keyTyped(KeyEvent event) {
                model.typeCharacter(event.getKeyChar());
                repaint();
            }
        });
    }

    private void handleKey(int keyCode) {
        if (model.getPhase() == EndScreenModel.Phase.NAME_ENTRY) {
            switch (keyCode) {
                case KeyEvent.VK_ENTER -> model.confirmName();
                case KeyEvent.VK_BACK_SPACE -> model.deleteCharacter();
                default -> { }
            }
        } else {
            switch (keyCode) {
                case KeyEvent.VK_UP -> model.moveUp();
                case KeyEvent.VK_DOWN -> model.moveDown();
                case KeyEvent.VK_ENTER -> model.select();
                default -> model.rejectInput();
            }
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g2.setFont(new Font(Font.DIALOG, Font.BOLD, 36));
        g2.setColor(Color.WHITE);
        drawCentered(g2, TITLE, 60);

        g2.setFont(new Font(Font.DIALOG, Font.BOLD, 22));
        g2.setColor(Color.CYAN);
        drawCentered(g2, "점수  " + model.getFinalScore(), 100);

        if (model.getPhase() == EndScreenModel.Phase.NAME_ENTRY) {
            paintNameEntry(g2);
        } else {
            paintScoreboard(g2);
            paintMenu(g2);
        }
        g2.dispose();
    }

    private void paintNameEntry(Graphics2D g2) {
        g2.setFont(new Font(Font.DIALOG, Font.BOLD, 20));
        g2.setColor(Color.WHITE);
        drawCentered(g2, NAME_PROMPT, 240);

        g2.setFont(new Font(Font.DIALOG, Font.BOLD, 28));
        g2.setColor(Color.YELLOW);
        drawCentered(g2, model.getName() + "_", 300);

        g2.setFont(new Font(Font.DIALOG, Font.PLAIN, 14));
        g2.setColor(Color.WHITE);
        drawCentered(g2, NAME_HINT, getHeight() - 30);
    }

    private void paintScoreboard(Graphics2D g2) {
        boolean failed = model.isStorageFailed();
        ScoreboardTablePainter.paint(g2, getWidth(), 150, model.getRecords(), model.getHighlightedRank(),
                failed ? STORAGE_ERROR : EMPTY_MESSAGE, failed ? Color.RED : Color.GRAY);
    }

    private void paintMenu(Graphics2D g2) {
        int y = 440;
        g2.setFont(new Font(Font.DIALOG, Font.BOLD, 22));
        List<MenuItem> items = model.getItems();
        for (int i = 0; i < items.size(); i++) {
            boolean selected = i == model.getSelectedIndex();
            g2.setColor(selected ? Color.YELLOW : Color.GRAY);
            drawCentered(g2, (selected ? "▶ " : "   ") + items.get(i).label(), y);
            y += 40;
        }

        if (model.isKeyHintVisible()) {
            g2.setFont(new Font(Font.DIALOG, Font.PLAIN, 14));
            g2.setColor(Color.WHITE);
            drawCentered(g2, KEY_HINT, getHeight() - 30);
        }
    }

    private void drawCentered(Graphics2D g2, String text, int y) {
        FontMetrics metrics = g2.getFontMetrics();
        g2.drawString(text, (getWidth() - metrics.stringWidth(text)) / 2, y);
    }
}
