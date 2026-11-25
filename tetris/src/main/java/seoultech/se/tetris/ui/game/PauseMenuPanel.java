package seoultech.se.tetris.ui.game;

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

/** 게임 화면 위에서 재개, 설정, 타이틀 이동을 선택하는 일시정지 메뉴. */
public final class PauseMenuPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private final transient StartMenuModel model;
    private final transient Runnable resume;

    /** 1인 모드의 일시정지 메뉴. 마지막 항목은 "타이틀로 돌아가기"다. */
    public PauseMenuPanel(Runnable resume, Runnable showSettings, Runnable returnToTitle) {
        this(resume, showSettings, "타이틀로 돌아가기", returnToTitle);
    }

    /**
     * @param leaveLabel 마지막 항목(게임을 그만두고 나가기)의 이름. 예: 로컬 대전은 "시작 메뉴로 돌아가기"
     * @param leave      마지막 항목을 골랐을 때의 동작
     */
    public PauseMenuPanel(Runnable resume, Runnable showSettings, String leaveLabel, Runnable leave) {
        this.resume = Objects.requireNonNull(resume, "resume");
        model = new StartMenuModel(List.of(
                new MenuItem("재개", resume),
                new MenuItem("설정", Objects.requireNonNull(showSettings, "showSettings")),
                new MenuItem(Objects.requireNonNull(leaveLabel, "leaveLabel"), Objects.requireNonNull(leave, "leave"))));
        setOpaque(false);
        setFocusable(true);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent event) {
                handleKey(event.getKeyCode());
            }
        });
    }

    /** 메뉴 항목. 위에서부터 재개, 설정, 나가기 순서다. */
    public List<MenuItem> getItems() {
        return model.getItems();
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

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setColor(new Color(0, 0, 0, 215));
        g2.fillRect(0, 0, getWidth(), getHeight());

        g2.setFont(new Font(Font.DIALOG, Font.BOLD, 30));
        g2.setColor(Color.CYAN);
        drawCentered(g2, "일시정지", getHeight() / 2 - 110);

        g2.setFont(new Font(Font.DIALOG, Font.BOLD, 22));
        List<MenuItem> items = model.getItems();
        for (int i = 0; i < items.size(); i++) {
            g2.setColor(i == model.getSelectedIndex() ? Color.YELLOW : Color.LIGHT_GRAY);
            drawCentered(g2, (i == model.getSelectedIndex() ? "▶ " : "   ") + items.get(i).label(),
                    getHeight() / 2 - 35 + i * 44);
        }

        g2.setFont(new Font(Font.DIALOG, Font.PLAIN, 13));
        g2.setColor(Color.WHITE);
        drawCentered(g2, model.isKeyHintVisible()
                ? "사용 가능한 키: ↑ ↓ 이동 · Enter 선택 · Esc 재개"
                : "↑ ↓ 이동 · Enter 선택 · Esc 재개", getHeight() - 28);
        g2.dispose();
    }

    private void drawCentered(Graphics2D graphics, String text, int y) {
        FontMetrics metrics = graphics.getFontMetrics();
        graphics.drawString(text, (getWidth() - metrics.stringWidth(text)) / 2, y);
    }
}
