package seoultech.se.tetris.ui.versus;

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

import javax.swing.JPanel;

/** 대전 모드에서 일반 / 아이템 / 시간제한 모드 중 하나를 고르는 화면. 시작 메뉴와 같은 방식으로 조작한다. */
public class VersusModeSelectPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final String TITLE = "대전 모드";
    private static final String SUBTITLE = "모드를 선택하세요";
    private static final String KEY_HINT = "사용 가능한 키:  ↑ ↓ 이동   Enter 선택   Esc 뒤로";

    private final transient VersusModeSelectModel model;

    public VersusModeSelectPanel(VersusModeSelectModel model) {
        this.model = model;
        setBackground(Color.BLACK);
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

    private void handleKey(int keyCode) {
        switch (keyCode) {
            case KeyEvent.VK_UP -> model.moveUp();
            case KeyEvent.VK_DOWN -> model.moveDown();
            case KeyEvent.VK_ENTER -> model.select();
            case KeyEvent.VK_ESCAPE -> model.back();
            default -> model.rejectInput();
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g2.setFont(new Font(Font.DIALOG, Font.BOLD, 32));
        g2.setColor(Color.CYAN);
        drawCentered(g2, TITLE, 90);

        g2.setFont(new Font(Font.DIALOG, Font.PLAIN, 16));
        g2.setColor(Color.WHITE);
        drawCentered(g2, SUBTITLE, 125);

        Font labelFont = new Font(Font.DIALOG, Font.BOLD, 22);
        Font descriptionFont = new Font(Font.DIALOG, Font.PLAIN, 13);
        List<VersusModeSelectModel.Option> options = model.getOptions();
        int y = 200;
        for (int i = 0; i < options.size(); i++) {
            VersusModeSelectModel.Option option = options.get(i);
            boolean selected = i == model.getSelectedIndex();

            g2.setFont(labelFont);
            g2.setColor(selected ? Color.YELLOW : Color.GRAY);
            drawCentered(g2, (selected ? "▶ " : "   ") + option.label(), y);

            if (!option.description().isEmpty()) {
                g2.setFont(descriptionFont);
                g2.setColor(selected ? Color.WHITE : Color.DARK_GRAY);
                drawCentered(g2, option.description(), y + 22);
            }
            y += 68;
        }

        if (model.isKeyHintVisible()) {
            g2.setFont(new Font(Font.DIALOG, Font.PLAIN, 14));
            g2.setColor(Color.WHITE);
            drawCentered(g2, KEY_HINT, getHeight() - 30);
        }
        g2.dispose();
    }

    private void drawCentered(Graphics2D g2, String text, int y) {
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(text, (getWidth() - fm.stringWidth(text)) / 2, y);
    }
}
