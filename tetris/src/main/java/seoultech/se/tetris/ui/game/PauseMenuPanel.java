package seoultech.se.tetris.ui.game;

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
import seoultech.se.tetris.ui.menu.StartMenuModel;
import seoultech.se.tetris.ui.theme.RetroTheme;

/** 게임 화면 위에서 재개, 설정, 타이틀 이동을 선택하는 일시정지 메뉴. */
public final class PauseMenuPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    static final String TITLE = "일시정지";
    /** 게임 화면이 살짝 비쳐 보이도록 덮는 투명도 */
    static final int DIM_ALPHA = 215;
    static final int FRAME_WIDTH = 320;
    static final int FRAME_HEIGHT = 250;
    private static final int ITEM_FIRST_BASELINE = 112;
    private static final int ITEM_SPACING = 44;
    private static final int SELECTION_INSET = 22;

    private final transient StartMenuModel model;
    private final transient Runnable resume;

    public PauseMenuPanel(Runnable resume, Runnable showSettings, Runnable returnToTitle) {
        this.resume = Objects.requireNonNull(resume, "resume");
        model = new StartMenuModel(List.of(
                new MenuItem("재개", resume),
                new MenuItem("설정", Objects.requireNonNull(showSettings, "showSettings")),
                new MenuItem("타이틀로 돌아가기", Objects.requireNonNull(returnToTitle, "returnToTitle"))));
        setOpaque(false);
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
            case KeyEvent.VK_UP -> model.moveUp();
            case KeyEvent.VK_DOWN -> model.moveDown();
            case KeyEvent.VK_ENTER -> model.select();
            case KeyEvent.VK_ESCAPE -> resume.run();
            default -> model.rejectInput();
        }
        repaint();
    }

    /** 가운데 메뉴 틀의 위치. 창 크기에 따라 가운데에 놓는다 */
    Rectangle menuFrame() {
        return new Rectangle((getWidth() - FRAME_WIDTH) / 2, (getHeight() - FRAME_HEIGHT) / 2,
                FRAME_WIDTH, FRAME_HEIGHT);
    }

    /** {@code index}번째 항목을 선택했을 때의 강조 상자 위치 */
    Rectangle selectionBounds(int index) {
        Rectangle frame = menuFrame();
        int baseline = frame.y + ITEM_FIRST_BASELINE + index * ITEM_SPACING;
        return new Rectangle(frame.x + SELECTION_INSET, baseline - 24, frame.width - 2 * SELECTION_INSET, 34);
    }

    /**
     * 게임 화면을 어둡게 덮고, 가운데 하늘색 틀 안에 제목과 항목을 그린다. 선택한 항목은 시작 메뉴와 같이
     * 노란 글자·노란 테두리 상자·픽셀 화살표로 표시한다. (레트로 디자인)
     */
    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g2 = (Graphics2D) graphics.create();
        RetroTheme.applyRenderingHints(g2);
        g2.setColor(RetroTheme.withAlpha(RetroTheme.BACKGROUND_TOP, DIM_ALPHA));
        g2.fillRect(0, 0, getWidth(), getHeight());

        Rectangle frame = menuFrame();
        RetroTheme.drawFrame(g2, frame.x, frame.y, frame.width, frame.height, RetroTheme.CYAN);
        g2.setFont(RetroTheme.mono(Font.BOLD, 30));
        RetroTheme.drawShadowText(g2, TITLE, frame.x, frame.width, frame.y + 52, 3);

        g2.setFont(RetroTheme.mono(Font.BOLD, 20));
        List<MenuItem> items = model.getItems();
        for (int i = 0; i < items.size(); i++) {
            boolean selected = i == model.getSelectedIndex();
            if (selected) {
                Rectangle box = selectionBounds(i);
                RetroTheme.drawSelection(g2, box.x, box.y, box.width, box.height);
            }
            g2.setColor(selected ? RetroTheme.YELLOW : RetroTheme.MUTED);
            int x = RetroTheme.centeredX(g2, items.get(i).label(), frame.x, frame.width);
            g2.drawString(items.get(i).label(), x, frame.y + ITEM_FIRST_BASELINE + i * ITEM_SPACING);
        }

        g2.setFont(RetroTheme.mono(Font.PLAIN, 13));
        g2.setColor(model.isKeyHintVisible() ? RetroTheme.TEXT : RetroTheme.MUTED);
        RetroTheme.drawCentered(g2, model.isKeyHintVisible()
                ? "사용 가능한 키: ↑ ↓ 이동 · Enter 선택 · Esc 재개"
                : "↑ ↓ 이동 · Enter 선택 · Esc 재개", getWidth(), getHeight() - 28);
        g2.dispose();
    }
}
