package seoultech.se.tetris.ui.menu;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;

import javax.swing.JPanel;

import seoultech.se.tetris.blocks.BlockType;
import seoultech.se.tetris.ui.game.BlockPalette;
import seoultech.se.tetris.ui.theme.RetroTheme;

/**
 * 게임 이름과 메뉴 리스트를 그리는 시작 메뉴 화면. 레트로 디자인({@link RetroTheme})으로 그린다.
 *
 * <p>위에서부터 분홍 제목과 하늘색 그림자, 부제, 블럭 장식, 이중선 틀 안의 메뉴 항목을 그린다.
 * 선택한 항목은 노란 글자와 노란 테두리 상자, 픽셀 화살표로 표시한다.
 */
public class StartMenuPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    static final String TITLE = "TETRIS";
    static final String SUBTITLE = "SEOULTECH SE · TEAM 9";
    private static final String KEY_HINT = "사용 가능한 키:  ↑ ↓ 이동   Enter 선택";

    private static final int WIDTH = 480;
    private static final int HEIGHT = 540;

    static final int TITLE_BASELINE = 105;
    private static final int TITLE_SIZE = 56;
    private static final int SUBTITLE_BASELINE = 134;
    private static final int DECORATION_TOP = 148;
    private static final int DECORATION_CELL = 14;
    private static final int DECORATION_GAP = 22;
    /** 제목 아래에 늘어놓는 블럭 장식. 색은 블럭 표준색을 그대로 쓴다 */
    private static final BlockType[] DECORATION = {BlockType.I, BlockType.T, BlockType.Z, BlockType.L};

    /** 메뉴 항목을 감싸는 틀 */
    static final Rectangle MENU_FRAME = new Rectangle(90, 176, 300, 306);
    static final int ITEM_FIRST_BASELINE = 212;
    static final int ITEM_SPACING = 41;
    private static final int ITEM_SIZE = 19;
    private static final int SELECTION_INSET = 20;
    private static final int SELECTION_HEIGHT = 34;

    private final transient StartMenuModel model;

    public StartMenuPanel(StartMenuModel model) {
        this.model = model;
        setBackground(RetroTheme.BACKGROUND_TOP);
        // 메뉴 7개 항목과 하단 키 안내가 겹치지 않는 높이
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setFocusable(true);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                handleKey(e.getKeyCode());
            }
        });
    }

    void handleKey(int keyCode) {
        switch (keyCode) {
            case KeyEvent.VK_UP -> model.moveUp();
            case KeyEvent.VK_DOWN -> model.moveDown();
            case KeyEvent.VK_ENTER -> model.select();
            default -> model.rejectInput();
        }
        repaint();
    }

    /** {@code index}번째 항목을 선택했을 때 그리는 강조 상자의 위치 */
    static Rectangle selectionBounds(int index) {
        int baseline = ITEM_FIRST_BASELINE + index * ITEM_SPACING;
        return new Rectangle(MENU_FRAME.x + SELECTION_INSET, baseline - 24,
                MENU_FRAME.width - 2 * SELECTION_INSET, SELECTION_HEIGHT);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        RetroTheme.applyRenderingHints(g2);
        RetroTheme.paintBackground(g2, getWidth(), getHeight());

        g2.setFont(RetroTheme.mono(Font.BOLD, TITLE_SIZE));
        RetroTheme.drawShadowTitle(g2, TITLE, getWidth(), TITLE_BASELINE);

        g2.setFont(RetroTheme.mono(Font.BOLD, 13));
        g2.setColor(RetroTheme.MUTED);
        RetroTheme.drawCentered(g2, SUBTITLE, getWidth(), SUBTITLE_BASELINE);
        paintDecoration(g2);

        RetroTheme.drawFrame(g2, MENU_FRAME.x, MENU_FRAME.y, MENU_FRAME.width, MENU_FRAME.height,
                RetroTheme.CYAN);
        paintItems(g2);

        if (model.isKeyHintVisible()) {
            g2.setFont(RetroTheme.mono(Font.PLAIN, 13));
            g2.setColor(RetroTheme.TEXT);
            RetroTheme.drawCentered(g2, KEY_HINT, getWidth(), getHeight() - 26);
        }
        g2.dispose();
    }

    /** 제목 아래에 블럭 네 개를 작게 늘어놓는다. */
    private void paintDecoration(Graphics2D g2) {
        int total = DECORATION.length * DECORATION_CELL + (DECORATION.length - 1) * DECORATION_GAP;
        int x = (getWidth() - total) / 2;
        for (BlockType type : DECORATION) {
            g2.setColor(BlockPalette.colorOf(type));
            g2.fillRect(x, DECORATION_TOP, DECORATION_CELL, DECORATION_CELL);
            x += DECORATION_CELL + DECORATION_GAP;
        }
    }

    private void paintItems(Graphics2D g2) {
        g2.setFont(RetroTheme.mono(Font.BOLD, ITEM_SIZE));
        List<MenuItem> items = model.getItems();
        for (int i = 0; i < items.size(); i++) {
            int baseline = ITEM_FIRST_BASELINE + i * ITEM_SPACING;
            boolean selected = i == model.getSelectedIndex();
            if (selected) {
                Rectangle box = selectionBounds(i);
                RetroTheme.drawSelection(g2, box.x, box.y, box.width, box.height);
            }
            g2.setColor(selected ? RetroTheme.YELLOW : RetroTheme.MUTED);
            RetroTheme.drawCentered(g2, items.get(i).label(), getWidth(), baseline);
        }
    }
}
