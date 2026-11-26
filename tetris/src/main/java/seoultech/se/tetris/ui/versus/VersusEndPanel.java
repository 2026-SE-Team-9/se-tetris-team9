package seoultech.se.tetris.ui.versus;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.Objects;

import javax.swing.JPanel;

import seoultech.se.tetris.ui.menu.MenuItem;
import seoultech.se.tetris.ui.theme.RetroTheme;
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
    private static final int MENU_TOP = 450;
    private static final int MENU_SPACING = 42;

    /** 승리한 플레이어의 점수 카드 틀 색 */
    static final Color WINNER_BORDER = RetroTheme.YELLOW;
    private static final Color CARD_BORDER = RetroTheme.CYAN;
    private static final Color DRAW_COLOR = RetroTheme.YELLOW;

    private final transient VersusEndModel model;

    public VersusEndPanel(VersusEndModel model) {
        this.model = Objects.requireNonNull(model, "model");
        setBackground(RetroTheme.BACKGROUND_TOP);
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

    /** {@code index}번째 메뉴 항목을 선택했을 때 그리는 강조 상자의 위치 */
    static Rectangle selectionBounds(int index) {
        int baseline = MENU_TOP + index * MENU_SPACING;
        return new Rectangle(110, baseline - 24, 260, 34);
    }

    /**
     * 레트로 디자인({@link RetroTheme})으로 그린다. 분홍 제목과 하늘색 그림자, 플레이어 색의 승패, 두 플레이어의
     * 점수 카드(승자는 노란 틀), 시작 메뉴와 같은 선택 표시의 메뉴를 그린다.
     */
    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g2 = (Graphics2D) graphics.create();
        RetroTheme.applyRenderingHints(g2);
        RetroTheme.paintBackground(g2, getWidth(), getHeight());

        g2.setFont(RetroTheme.mono(Font.BOLD, 34));
        RetroTheme.drawShadowTitle(g2, model.title(), getWidth(), 60);

        g2.setFont(RetroTheme.mono(Font.PLAIN, 14));
        g2.setColor(RetroTheme.MUTED);
        RetroTheme.drawCentered(g2, model.modeText(), getWidth(), 90);

        Side winner = model.winner();
        g2.setFont(RetroTheme.mono(Font.BOLD, 32));
        g2.setColor(winner == null ? DRAW_COLOR : VersusGamePanel.playerColor(winner));
        RetroTheme.drawCentered(g2, model.outcomeText(), getWidth(), 150);

        g2.setFont(RetroTheme.mono(Font.PLAIN, 14));
        g2.setColor(RetroTheme.TEXT);
        RetroTheme.drawCentered(g2, model.reasonText(), getWidth(), 182);

        for (Side side : Side.values()) {
            paintCard(g2, side, side == winner);
        }
        paintMenu(g2);
        g2.dispose();
    }

    /** 플레이어 이름, 승패, 최종 점수를 담은 카드. 승리한 플레이어의 카드는 노란 틀, 나머지는 하늘색 틀이다. */
    private void paintCard(Graphics2D g2, Side side, boolean winner) {
        int x = cardLeft(side);
        int y = CARD_TOP;
        RetroTheme.drawFrame(g2, x, y, CARD_W, CARD_H, winner ? WINNER_BORDER : CARD_BORDER);

        g2.setFont(RetroTheme.mono(Font.BOLD, 22));
        g2.setColor(VersusGamePanel.playerColor(side));
        g2.drawString(VersusScreenModel.playerLabel(side),
                RetroTheme.centeredX(g2, VersusScreenModel.playerLabel(side), x, CARD_W), y + 36);

        g2.setFont(RetroTheme.mono(Font.BOLD, 15));
        g2.setColor(winner ? WINNER_BORDER : RetroTheme.MUTED);
        String outcome = model.outcomeOf(side);
        g2.drawString(outcome, RetroTheme.centeredX(g2, outcome, x, CARD_W), y + 62);

        g2.setFont(RetroTheme.mono(Font.PLAIN, 12));
        g2.setColor(RetroTheme.MUTED);
        g2.drawString("최종 점수", RetroTheme.centeredX(g2, "최종 점수", x, CARD_W), y + 94);
        g2.setFont(RetroTheme.mono(Font.BOLD, 26));
        g2.setColor(winner ? RetroTheme.YELLOW : RetroTheme.TEXT);
        String score = String.valueOf(model.scoreOf(side));
        g2.drawString(score, RetroTheme.centeredX(g2, score, x, CARD_W), y + 124);
    }

    /** 시작 메뉴와 같이 선택 항목을 노란 글자·노란 테두리 상자·픽셀 화살표로 표시한다. */
    private void paintMenu(Graphics2D g2) {
        g2.setFont(RetroTheme.mono(Font.BOLD, 20));
        List<MenuItem> items = model.getItems();
        for (int i = 0; i < items.size(); i++) {
            boolean selected = i == model.getSelectedIndex();
            if (selected) {
                Rectangle box = selectionBounds(i);
                RetroTheme.drawSelection(g2, box.x, box.y, box.width, box.height);
            }
            g2.setColor(selected ? RetroTheme.YELLOW : RetroTheme.MUTED);
            RetroTheme.drawCentered(g2, items.get(i).label(), getWidth(), MENU_TOP + i * MENU_SPACING);
        }

        if (model.isKeyHintVisible()) {
            g2.setFont(RetroTheme.mono(Font.PLAIN, 13));
            g2.setColor(RetroTheme.TEXT);
            RetroTheme.drawCentered(g2, KEY_HINT, getWidth(), getHeight() - 26);
        }
    }
}
