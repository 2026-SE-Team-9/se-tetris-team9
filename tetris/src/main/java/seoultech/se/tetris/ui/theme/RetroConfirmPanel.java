package seoultech.se.tetris.ui.theme;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import javax.swing.JPanel;

/**
 * 레트로 디자인의 확인 창 내용. 제목, 문구, 예·아니오 버튼을 그리고 키 입력으로 하나를 고른다.
 *
 * <p>방향키와 Tab으로 버튼을 바꾸고 Enter로 고른다. Esc는 아니오다. 처음에는 기존 자바 확인 창과 같이 예가 선택되어 있다.
 * 창으로 띄우는 것은 {@link RetroConfirmDialog}가 한다.
 */
public class RetroConfirmPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    static final int WIDTH = 420;
    static final int HEIGHT = 210;
    static final String YES = "예";
    static final String NO = "아니오";
    static final int YES_INDEX = 0;
    static final int NO_INDEX = 1;

    private static final int BUTTON_WIDTH = 140;
    private static final int BUTTON_HEIGHT = 36;
    private static final int BUTTON_GAP = 20;
    private static final int BUTTON_TOP = 140;

    private final String title;
    private final List<String> messageLines;
    private final transient Consumer<Boolean> onChoice;
    private int selected = YES_INDEX;

    /**
     * @param message  문구. 문장마다 한 줄로 나눠 그린다
     * @param onChoice 고르면 예이면 {@code true}, 아니오이면 {@code false}를 받는다
     */
    public RetroConfirmPanel(String title, String message, Consumer<Boolean> onChoice) {
        this.title = Objects.requireNonNull(title, "title");
        this.messageLines = splitSentences(Objects.requireNonNull(message, "message"));
        this.onChoice = Objects.requireNonNull(onChoice, "onChoice");
        setBackground(RetroTheme.SURFACE);
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setFocusable(true);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent event) {
                handleKey(event.getKeyCode());
            }
        });
    }

    /** 문구를 문장 단위로 나눈다. "…다. …까?"는 두 줄이 된다. */
    static List<String> splitSentences(String message) {
        return List.of(message.strip().split("(?<=[.?!])\\s+"));
    }

    List<String> messageLines() {
        return messageLines;
    }

    /** 선택된 버튼. {@link #YES_INDEX} 또는 {@link #NO_INDEX} */
    int selectedIndex() {
        return selected;
    }

    /** 키 하나를 처리한다. */
    void handleKey(int keyCode) {
        switch (keyCode) {
            case KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT, KeyEvent.VK_UP, KeyEvent.VK_DOWN, KeyEvent.VK_TAB ->
                    selected = 1 - selected;
            case KeyEvent.VK_ENTER -> onChoice.accept(selected == YES_INDEX);
            case KeyEvent.VK_ESCAPE -> onChoice.accept(false);
            default -> { }
        }
        repaint();
    }

    /** {@code index}번째 버튼의 위치. 예가 왼쪽, 아니오가 오른쪽이다 */
    static Rectangle buttonBounds(int index) {
        int left = (WIDTH - 2 * BUTTON_WIDTH - BUTTON_GAP) / 2;
        return new Rectangle(left + index * (BUTTON_WIDTH + BUTTON_GAP), BUTTON_TOP, BUTTON_WIDTH, BUTTON_HEIGHT);
    }

    /**
     * 분홍 틀 안에 분홍 제목과 하늘색 그림자, 문구, 두 버튼을 그린다. 선택한 버튼은 시작 메뉴와 같은 노란 상자와
     * 픽셀 화살표로 표시한다. 게임을 그만두는 확인이므로 게임 나가기 메뉴와 같은 분홍 틀을 쓴다.
     */
    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g2 = (Graphics2D) graphics.create();
        RetroTheme.applyRenderingHints(g2);
        RetroTheme.drawFrame(g2, 1, 1, getWidth() - 3, getHeight() - 3, RetroTheme.PINK);

        g2.setFont(RetroTheme.mono(Font.BOLD, 22));
        RetroTheme.drawShadowText(g2, title, 0, getWidth(), 44, 2);

        g2.setFont(RetroTheme.mono(Font.PLAIN, 13));
        g2.setColor(RetroTheme.TEXT);
        int y = 82;
        for (String line : messageLines) {
            RetroTheme.drawCentered(g2, line, getWidth(), y);
            y += 22;
        }

        paintButton(g2, YES, YES_INDEX);
        paintButton(g2, NO, NO_INDEX);
        g2.dispose();
    }

    private void paintButton(Graphics2D g2, String label, int index) {
        Rectangle box = buttonBounds(index);
        boolean isSelected = index == selected;
        if (isSelected) {
            RetroTheme.drawSelection(g2, box.x, box.y, box.width, box.height);
        } else {
            g2.setColor(RetroTheme.MUTED);
            g2.drawRect(box.x, box.y, box.width, box.height);
        }
        g2.setFont(RetroTheme.mono(Font.BOLD, 17));
        g2.setColor(isSelected ? RetroTheme.YELLOW : RetroTheme.MUTED);
        g2.drawString(label, RetroTheme.centeredX(g2, label, box.x, box.width), box.y + 24);
    }
}
