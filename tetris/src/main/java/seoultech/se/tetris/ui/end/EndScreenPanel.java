package seoultech.se.tetris.ui.end;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;

import javax.swing.JPanel;

import seoultech.se.tetris.ui.menu.MenuItem;
import seoultech.se.tetris.ui.scoreboard.ScoreboardTablePainter;
import seoultech.se.tetris.ui.theme.RetroTheme;

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
        setBackground(RetroTheme.BACKGROUND_TOP);
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

    /** 키 하나를 처리한다. */
    void handleKey(int keyCode) {
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

    /** 이름 입력 단계에서 입력칸을 감싸는 틀 */
    static final Rectangle NAME_FRAME = new Rectangle(60, 186, 360, 160);
    /** 이름 입력칸 */
    static final Rectangle NAME_FIELD = new Rectangle(90, 254, 300, 46);
    /** 스코어보드 단계에서 표를 감싸는 틀 */
    static final Rectangle TABLE_FRAME = new Rectangle(20, 124, 440, 284);
    static final int MENU_FIRST_BASELINE = 450;
    static final int MENU_SPACING = 42;

    /** {@code index}번째 메뉴 항목을 선택했을 때 그리는 강조 상자의 위치 */
    static Rectangle selectionBounds(int index) {
        int baseline = MENU_FIRST_BASELINE + index * MENU_SPACING;
        return new Rectangle(110, baseline - 24, 260, 34);
    }

    /**
     * 레트로 디자인({@link RetroTheme})으로 그린다. 위에서부터 분홍 제목과 하늘색 그림자, 노란 점수를 그리고,
     * 이름 입력 단계에서는 틀 안의 입력칸을, 스코어보드 단계에서는 틀 안의 표와 메뉴를 그린다.
     */
    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g2 = (Graphics2D) graphics.create();
        RetroTheme.applyRenderingHints(g2);
        RetroTheme.paintBackground(g2, getWidth(), getHeight());

        g2.setFont(RetroTheme.mono(Font.BOLD, 34));
        RetroTheme.drawShadowTitle(g2, TITLE, getWidth(), 60);

        paintScore(g2);
        if (model.getPhase() == EndScreenModel.Phase.NAME_ENTRY) {
            paintNameEntry(g2);
        } else {
            paintScoreboard(g2);
            paintMenu(g2);
        }
        g2.dispose();
    }

    /** "점수" 이름은 보조 글자색, 값은 노랑으로 한 줄 가운데에 그린다. */
    private void paintScore(Graphics2D g2) {
        String label = "점수  ";
        String value = String.valueOf(model.getFinalScore());
        g2.setFont(RetroTheme.mono(Font.BOLD, 22));
        int labelWidth = g2.getFontMetrics().stringWidth(label);
        int x = RetroTheme.centeredX(g2, label + value, 0, getWidth());
        g2.setColor(RetroTheme.MUTED);
        g2.drawString(label, x, 100);
        g2.setColor(RetroTheme.YELLOW);
        g2.drawString(value, x + labelWidth, 100);
    }

    private void paintNameEntry(Graphics2D g2) {
        RetroTheme.drawFrame(g2, NAME_FRAME.x, NAME_FRAME.y, NAME_FRAME.width, NAME_FRAME.height, RetroTheme.PINK);
        g2.setFont(RetroTheme.mono(Font.BOLD, 16));
        g2.setColor(RetroTheme.TEXT);
        RetroTheme.drawCentered(g2, NAME_PROMPT, getWidth(), NAME_FRAME.y + 40);

        // 입력칸: 옅은 노랑으로 채우고 노란 테두리, 이름 뒤에 커서(_)
        g2.setColor(RetroTheme.withAlpha(RetroTheme.YELLOW, 28));
        g2.fillRect(NAME_FIELD.x, NAME_FIELD.y, NAME_FIELD.width, NAME_FIELD.height);
        g2.setColor(RetroTheme.YELLOW);
        g2.drawRect(NAME_FIELD.x, NAME_FIELD.y, NAME_FIELD.width, NAME_FIELD.height);
        g2.setFont(RetroTheme.mono(Font.BOLD, 24));
        RetroTheme.drawCentered(g2, model.getName() + "_", getWidth(), NAME_FIELD.y + 32);

        g2.setFont(RetroTheme.mono(Font.PLAIN, 13));
        g2.setColor(RetroTheme.MUTED);
        RetroTheme.drawCentered(g2, NAME_HINT, getWidth(), getHeight() - 30);
    }

    private void paintScoreboard(Graphics2D g2) {
        RetroTheme.drawFrame(g2, TABLE_FRAME.x, TABLE_FRAME.y, TABLE_FRAME.width, TABLE_FRAME.height,
                RetroTheme.CYAN);
        boolean failed = model.isStorageFailed();
        ScoreboardTablePainter.paint(g2, getWidth(), 150, model.getRecords(), model.getHighlightedRank(),
                failed ? STORAGE_ERROR : EMPTY_MESSAGE, failed ? RetroTheme.PINK : RetroTheme.MUTED);
    }

    /** 메뉴는 시작 메뉴와 같이 선택 항목을 노란 글자·노란 테두리 상자·픽셀 화살표로 표시한다. */
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
            RetroTheme.drawCentered(g2, items.get(i).label(), getWidth(), MENU_FIRST_BASELINE + i * MENU_SPACING);
        }

        if (model.isKeyHintVisible()) {
            g2.setFont(RetroTheme.mono(Font.PLAIN, 13));
            g2.setColor(RetroTheme.TEXT);
            RetroTheme.drawCentered(g2, KEY_HINT, getWidth(), getHeight() - 22);
        }
    }
}
