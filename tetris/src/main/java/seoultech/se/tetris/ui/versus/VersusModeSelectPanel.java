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

import javax.swing.JPanel;

import seoultech.se.tetris.ui.theme.RetroTheme;

/** 대전 모드에서 일반 / 아이템 / 시간제한 모드 중 하나를 고르는 화면. 시작 메뉴와 같은 방식으로 조작한다. */
public class VersusModeSelectPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final String TITLE = "대전 모드";
    private static final String SUBTITLE = "모드를 선택하세요";
    private static final String KEY_HINT = "사용 가능한 키:  ↑ ↓ 이동   Enter 선택   Esc 뒤로";

    /** 선택하지 않은 항목의 설명 색. 배경 위에서 옅게 보이는 남보라 */
    private static final Color DIM_DESCRIPTION = new Color(0x3A, 0x34, 0x6E);

    private final transient VersusModeSelectModel model;

    public VersusModeSelectPanel(VersusModeSelectModel model) {
        this.model = model;
        setBackground(RetroTheme.BACKGROUND_TOP);
        // 시작 메뉴와 같은 크기로 두어 화면 전환 시 창 크기가 바뀌지 않게 한다
        setPreferredSize(new Dimension(480, 540));
        setFocusable(true);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                handleKey(e.getKeyCode());
            }
        });
    }

    /** 키 하나를 처리한다. */
    void handleKey(int keyCode) {
        switch (keyCode) {
            case KeyEvent.VK_UP -> model.moveUp();
            case KeyEvent.VK_DOWN -> model.moveDown();
            case KeyEvent.VK_ENTER -> model.select();
            case KeyEvent.VK_ESCAPE -> model.back();
            default -> model.rejectInput();
        }
        repaint();
    }

    /** 항목을 감싸는 틀과 항목 위치 */
    static final Rectangle MENU_FRAME = new Rectangle(70, 158, 340, 254);
    static final int ITEM_FIRST_BASELINE = 196;
    static final int ITEM_SPACING = 62;

    /** {@code index}번째 항목을 선택했을 때 그리는 강조 상자의 위치 */
    static Rectangle selectionBounds(int index) {
        int baseline = ITEM_FIRST_BASELINE + index * ITEM_SPACING;
        return new Rectangle(MENU_FRAME.x + 20, baseline - 24, MENU_FRAME.width - 40, 34);
    }

    /**
     * 레트로 디자인({@link RetroTheme})으로 그린다. 분홍 제목과 하늘색 그림자 아래, 하늘색 틀 안에 모드 세 가지와
     * 뒤로 가기를 그린다. 선택 항목은 시작 메뉴와 같은 노란 상자와 픽셀 화살표로 표시하고, 설명은 선택한 항목만 밝게 보인다.
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        RetroTheme.applyRenderingHints(g2);
        RetroTheme.paintBackground(g2, getWidth(), getHeight());

        g2.setFont(RetroTheme.mono(Font.BOLD, 32));
        RetroTheme.drawShadowTitle(g2, TITLE, getWidth(), 84);
        g2.setFont(RetroTheme.mono(Font.PLAIN, 14));
        g2.setColor(RetroTheme.TEXT);
        RetroTheme.drawCentered(g2, SUBTITLE, getWidth(), 118);

        RetroTheme.drawFrame(g2, MENU_FRAME.x, MENU_FRAME.y, MENU_FRAME.width, MENU_FRAME.height, RetroTheme.CYAN);
        List<VersusModeSelectModel.Option> options = model.getOptions();
        for (int i = 0; i < options.size(); i++) {
            VersusModeSelectModel.Option option = options.get(i);
            int y = ITEM_FIRST_BASELINE + i * ITEM_SPACING;
            boolean selected = i == model.getSelectedIndex();
            if (selected) {
                Rectangle box = selectionBounds(i);
                RetroTheme.drawSelection(g2, box.x, box.y, box.width, box.height);
            }
            g2.setFont(RetroTheme.mono(Font.BOLD, 20));
            g2.setColor(selected ? RetroTheme.YELLOW : RetroTheme.MUTED);
            RetroTheme.drawCentered(g2, option.label(), getWidth(), y);

            if (!option.description().isEmpty()) {
                g2.setFont(RetroTheme.mono(Font.PLAIN, 12));
                g2.setColor(selected ? RetroTheme.TEXT : DIM_DESCRIPTION);
                RetroTheme.drawCentered(g2, option.description(), getWidth(), y + 26);
            }
        }

        if (model.isKeyHintVisible()) {
            g2.setFont(RetroTheme.mono(Font.PLAIN, 13));
            g2.setColor(RetroTheme.TEXT);
            RetroTheme.drawCentered(g2, KEY_HINT, getWidth(), getHeight() - 30);
        }
        g2.dispose();
    }
}
