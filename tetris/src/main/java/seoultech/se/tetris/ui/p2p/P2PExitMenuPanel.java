package seoultech.se.tetris.ui.p2p;

import java.awt.BasicStroke;
import java.awt.Color;
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
import seoultech.se.tetris.ui.menu.StartMenuModel;

/**
 * P2P 대전 중의 게임 나가기 메뉴. 창의 덮개(glass pane)로 대전 화면 위에 띄운다.
 *
 * <p>P2P 대전에는 일시정지가 없으므로 메뉴를 여는 동안에도 게임은 계속 진행된다. (3차 팀 결정)
 * 그래서 보드가 비쳐 보이도록 옅게 덮고, 그 사실을 메뉴에 안내한다.
 * 항목은 "계속하기"와 "게임 나가기 (기권패)"이며, 고른 뒤의 동작은 대전 흐름을 맡는 쪽이 정한다.
 */
public class P2PExitMenuPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    /** 보드가 비쳐 보이도록 일시정지 메뉴보다 옅게 덮는다 */
    static final Color DIM = new Color(0, 0, 0, 150);
    private static final Color BOX = new Color(20, 20, 20, 235);
    private static final int BOX_WIDTH = 360;
    private static final int BOX_HEIGHT = 230;

    private final transient StartMenuModel model;
    private final transient Runnable onContinue;

    /**
     * @param onContinue 계속하기(또는 Esc)를 골랐을 때. 보통 메뉴를 닫는다
     * @param onLeave    게임 나가기를 골랐을 때. 기권패로 처리하고 첫 화면으로 간다
     */
    public P2PExitMenuPanel(Runnable onContinue, Runnable onLeave) {
        this.onContinue = Objects.requireNonNull(onContinue, "onContinue");
        model = new StartMenuModel(List.of(
                new MenuItem(P2PScreenTexts.EXIT_ITEMS.get(0), onContinue),
                new MenuItem(P2PScreenTexts.EXIT_ITEMS.get(1), Objects.requireNonNull(onLeave, "onLeave"))));
        setOpaque(false);
        setFocusable(true);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent event) {
                handleKey(event.getKeyCode());
            }
        });
    }

    public List<MenuItem> getItems() {
        return model.getItems();
    }

    public int getSelectedIndex() {
        return model.getSelectedIndex();
    }

    /** 키 하나를 처리한다. */
    void handleKey(int keyCode) {
        switch (keyCode) {
            case KeyEvent.VK_UP -> model.moveUp();
            case KeyEvent.VK_DOWN -> model.moveDown();
            case KeyEvent.VK_ENTER -> model.select();
            case KeyEvent.VK_ESCAPE -> onContinue.run();
            default -> model.rejectInput();
        }
        repaint();
    }

    /** 다시 열 때 첫 항목(계속하기)이 선택되도록 한다. 실수로 나가는 일을 막는다. */
    public void reset() {
        while (model.getSelectedIndex() != 0) {
            model.moveUp();
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(DIM);
        g2.fillRect(0, 0, getWidth(), getHeight());

        int left = (getWidth() - BOX_WIDTH) / 2;
        int top = (getHeight() - BOX_HEIGHT) / 2;
        g2.setColor(BOX);
        g2.fillRoundRect(left, top, BOX_WIDTH, BOX_HEIGHT, 16, 16);
        g2.setStroke(new BasicStroke(1.5f));
        g2.setColor(new Color(90, 90, 90));
        g2.drawRoundRect(left, top, BOX_WIDTH, BOX_HEIGHT, 16, 16);

        g2.setFont(new Font(Font.DIALOG, Font.BOLD, 26));
        g2.setColor(Color.CYAN);
        drawCentered(g2, P2PScreenTexts.EXIT_TITLE, top + 46);

        g2.setFont(new Font(Font.DIALOG, Font.PLAIN, 13));
        g2.setColor(P2PPanel.SELECTED);
        drawCentered(g2, P2PScreenTexts.EXIT_NOTE, top + 74);

        g2.setFont(new Font(Font.DIALOG, Font.BOLD, 20));
        List<MenuItem> items = model.getItems();
        for (int i = 0; i < items.size(); i++) {
            boolean selected = i == model.getSelectedIndex();
            g2.setColor(selected ? P2PPanel.SELECTED : Color.LIGHT_GRAY);
            drawCentered(g2, (selected ? "▶ " : "   ") + items.get(i).label(), top + 122 + i * 40);
        }

        g2.setFont(new Font(Font.DIALOG, Font.PLAIN, 12));
        g2.setColor(model.isKeyHintVisible() ? Color.WHITE : new Color(170, 170, 170));
        drawCentered(g2, P2PScreenTexts.EXIT_HINT, top + BOX_HEIGHT - 18);
        g2.dispose();
    }

    private void drawCentered(Graphics2D g2, String text, int y) {
        FontMetrics metrics = g2.getFontMetrics();
        g2.drawString(text, (getWidth() - metrics.stringWidth(text)) / 2, y);
    }
}
