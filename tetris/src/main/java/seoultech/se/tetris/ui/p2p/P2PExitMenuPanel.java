package seoultech.se.tetris.ui.p2p;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.Objects;

import javax.swing.JPanel;

import seoultech.se.tetris.ui.menu.MenuItem;
import seoultech.se.tetris.ui.menu.StartMenuModel;
import seoultech.se.tetris.ui.theme.RetroTheme;

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
    static final Color DIM = RetroTheme.withAlpha(RetroTheme.BACKGROUND_TOP, 150);
    private static final int BOX_WIDTH = 360;
    private static final int BOX_HEIGHT = 230;
    private static final int ITEM_FIRST_BASELINE = 124;
    private static final int ITEM_SPACING = 44;

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

    /** 가운데 메뉴 틀의 위치. 창 크기에 따라 가운데에 놓는다 */
    Rectangle menuFrame() {
        return new Rectangle((getWidth() - BOX_WIDTH) / 2, (getHeight() - BOX_HEIGHT) / 2, BOX_WIDTH, BOX_HEIGHT);
    }

    /** {@code index}번째 항목을 선택했을 때의 강조 상자 위치 */
    Rectangle selectionBounds(int index) {
        Rectangle frame = menuFrame();
        int baseline = frame.y + ITEM_FIRST_BASELINE + index * ITEM_SPACING;
        return new Rectangle(frame.x + 24, baseline - 24, frame.width - 48, 34);
    }

    /**
     * 보드가 비쳐 보이도록 옅게 덮고, 가운데 분홍 틀 안에 제목·안내·항목을 그린다. 선택한 항목은 시작 메뉴와 같은
     * 노란 상자와 픽셀 화살표로 표시한다. (레트로 디자인) 게임 나가기는 기권패라 일시정지 메뉴(하늘색)와 틀 색을 달리한다.
     */
    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g2 = (Graphics2D) graphics.create();
        RetroTheme.applyRenderingHints(g2);
        g2.setColor(DIM);
        g2.fillRect(0, 0, getWidth(), getHeight());

        Rectangle frame = menuFrame();
        RetroTheme.drawFrame(g2, frame.x, frame.y, frame.width, frame.height, RetroTheme.PINK);

        g2.setFont(RetroTheme.mono(Font.BOLD, 26));
        RetroTheme.drawShadowText(g2, P2PScreenTexts.EXIT_TITLE, frame.x, frame.width, frame.y + 46, 3);

        g2.setFont(RetroTheme.mono(Font.PLAIN, 12));
        g2.setColor(P2PPanel.SELECTED);
        drawCentered(g2, P2PScreenTexts.EXIT_NOTE, frame.y + 74);

        g2.setFont(RetroTheme.mono(Font.BOLD, 19));
        List<MenuItem> items = model.getItems();
        for (int i = 0; i < items.size(); i++) {
            boolean selected = i == model.getSelectedIndex();
            if (selected) {
                Rectangle box = selectionBounds(i);
                RetroTheme.drawSelection(g2, box.x, box.y, box.width, box.height);
            }
            g2.setColor(selected ? P2PPanel.SELECTED : RetroTheme.MUTED);
            drawCentered(g2, items.get(i).label(), frame.y + ITEM_FIRST_BASELINE + i * ITEM_SPACING);
        }

        g2.setFont(RetroTheme.mono(Font.PLAIN, 11));
        g2.setColor(model.isKeyHintVisible() ? RetroTheme.TEXT : RetroTheme.MUTED);
        drawCentered(g2, P2PScreenTexts.EXIT_HINT, frame.y + BOX_HEIGHT - 16);
        g2.dispose();
    }

    private void drawCentered(Graphics2D g2, String text, int y) {
        FontMetrics metrics = g2.getFontMetrics();
        g2.drawString(text, (getWidth() - metrics.stringWidth(text)) / 2, y);
    }
}
