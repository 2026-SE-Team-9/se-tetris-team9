package seoultech.se.tetris.ui.versus;

import java.awt.BasicStroke;
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
import java.util.Objects;

import javax.swing.JPanel;

import seoultech.se.tetris.ui.menu.MenuItem;
import seoultech.se.tetris.versus.Side;

/**
 * 로컬 대전 종료 화면. 승패, 종료 사유, 두 플레이어의 최종 점수와 메뉴를 그린다.
 *
 * <p>기존 게임 종료 화면과 같은 크기·제목·메뉴 형태를 따르고, 스코어보드 자리에 두 플레이어의 점수 카드를 둔다.
 */
public class VersusEndPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    static final String KEY_HINT = "사용 가능한 키:  ↑ ↓ 이동   Enter 선택";

    private static final int CARD_TOP = 220;
    private static final int CARD_W = 180;
    private static final int CARD_H = 140;
    private static final int CARD_GAP = 24;
    private static final int MENU_TOP = 440;

    /** 승리한 플레이어의 점수 카드 테두리 색 */
    static final Color WINNER_BORDER = Color.YELLOW;
    private static final Color CARD_BORDER = new Color(70, 70, 70);
    private static final Color DRAW_COLOR = Color.YELLOW;

    private final transient VersusEndModel model;

    public VersusEndPanel(VersusEndModel model) {
        this.model = Objects.requireNonNull(model, "model");
        setBackground(Color.BLACK);
        setPreferredSize(new Dimension(480, 560));
        setFocusable(true);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent event) {
                handleKey(event.getKeyCode());
            }
        });
    }

    /** 키 하나를 처리한다. */
    void handleKey(int keyCode) {
        switch (keyCode) {
            case KeyEvent.VK_UP -> model.moveUp();
            case KeyEvent.VK_DOWN -> model.moveDown();
            case KeyEvent.VK_ENTER -> model.select();
            default -> model.rejectInput();
        }
        repaint();
    }

    /** 플레이어 점수 카드의 왼쪽 x 좌표 */
    int cardLeft(Side side) {
        int left = (getWidth() - CARD_W * 2 - CARD_GAP) / 2;
        return side == Side.P1 ? left : left + CARD_W + CARD_GAP;
    }

    int cardTop() {
        return CARD_TOP;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setFont(new Font(Font.DIALOG, Font.BOLD, 36));
        g2.setColor(Color.WHITE);
        drawCentered(g2, model.title(), 60);

        g2.setFont(new Font(Font.DIALOG, Font.PLAIN, 15));
        g2.setColor(Color.GRAY);
        drawCentered(g2, model.modeText(), 90);

        Side winner = model.winner();
        g2.setFont(new Font(Font.DIALOG, Font.BOLD, 34));
        g2.setColor(winner == null ? DRAW_COLOR : VersusGamePanel.playerColor(winner));
        drawCentered(g2, model.outcomeText(), 150);

        g2.setFont(new Font(Font.DIALOG, Font.PLAIN, 16));
        g2.setColor(Color.LIGHT_GRAY);
        drawCentered(g2, model.reasonText(), 182);

        for (Side side : Side.values()) {
            paintCard(g2, side, side == winner);
        }
        paintMenu(g2);
        g2.dispose();
    }

    /** 플레이어 이름, 승패, 최종 점수를 담은 카드. 승리한 플레이어의 카드는 테두리를 강조한다. */
    private void paintCard(Graphics2D g2, Side side, boolean winner) {
        int x = cardLeft(side);
        int y = CARD_TOP;
        g2.setColor(new Color(20, 20, 20));
        g2.fillRoundRect(x, y, CARD_W, CARD_H, 16, 16);
        g2.setStroke(new BasicStroke(winner ? 3f : 1.5f));
        g2.setColor(winner ? WINNER_BORDER : CARD_BORDER);
        g2.drawRoundRect(x, y, CARD_W, CARD_H, 16, 16);

        g2.setFont(new Font(Font.DIALOG, Font.BOLD, 22));
        g2.setColor(VersusGamePanel.playerColor(side));
        drawCentered(g2, VersusScreenModel.playerLabel(side), x, CARD_W, y + 34);

        g2.setFont(new Font(Font.DIALOG, Font.BOLD, 16));
        g2.setColor(winner ? WINNER_BORDER : Color.GRAY);
        drawCentered(g2, model.outcomeOf(side), x, CARD_W, y + 60);

        g2.setFont(new Font(Font.DIALOG, Font.PLAIN, 13));
        g2.setColor(Color.GRAY);
        drawCentered(g2, "최종 점수", x, CARD_W, y + 92);
        g2.setFont(new Font(Font.MONOSPACED, Font.BOLD, 26));
        g2.setColor(Color.WHITE);
        drawCentered(g2, String.valueOf(model.scoreOf(side)), x, CARD_W, y + 124);
    }

    private void paintMenu(Graphics2D g2) {
        int y = MENU_TOP;
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
        drawCentered(g2, text, 0, getWidth(), y);
    }

    private static void drawCentered(Graphics2D g2, String text, int left, int width, int y) {
        FontMetrics metrics = g2.getFontMetrics();
        g2.drawString(text, left + (width - metrics.stringWidth(text)) / 2, y);
    }
}
