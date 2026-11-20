package seoultech.se.tetris.ui.versus;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

import javax.swing.JComponent;
import javax.swing.JPanel;

import seoultech.se.tetris.ui.game.GamePanel;
import seoultech.se.tetris.versus.Side;

/**
 * 로컬 대전 화면. 위쪽에 제목·결과·키 안내를, 아래에 P1(왼쪽)·P2(오른쪽) 보드를 나란히 표시한다.
 *
 * <p>각 보드는 1인 모드와 같은 {@link GamePanel}로 그리므로 블럭·고스트·아이템·줄 삭제 애니메이션이 1인 모드와 같다.
 */
public class VersusGamePanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final int HEADER_HEIGHT = 76;
    private static final int LABEL_HEIGHT = 30;
    private static final int GAP = 12;

    private final transient VersusScreenModel model;
    private final Map<Side, GamePanel> boards = new EnumMap<>(Side.class);

    public VersusGamePanel(VersusScreenModel model, GamePanel p1Board, GamePanel p2Board) {
        super(new BorderLayout());
        this.model = Objects.requireNonNull(model, "model");
        boards.put(Side.P1, Objects.requireNonNull(p1Board, "p1Board"));
        boards.put(Side.P2, Objects.requireNonNull(p2Board, "p2Board"));
        setBackground(Color.BLACK);
        setFocusable(true);

        add(new Header(), BorderLayout.NORTH);
        JPanel center = new JPanel(new FlowLayout(FlowLayout.CENTER, GAP, 0));
        center.setBackground(Color.BLACK);
        for (Side side : Side.values()) {
            center.add(column(side));
        }
        add(center, BorderLayout.CENTER);
    }

    private JPanel column(Side side) {
        JPanel column = new JPanel(new BorderLayout());
        column.setBackground(Color.BLACK);
        column.add(new PlayerLabel(side), BorderLayout.NORTH);
        column.add(boards.get(side), BorderLayout.CENTER);
        return column;
    }

    public GamePanel board(Side side) {
        return boards.get(Objects.requireNonNull(side, "side"));
    }

    /** 대전 결과에 맞춰 각 보드의 승패 표시와 위쪽 문구를 다시 그린다. */
    public void refresh() {
        for (Side side : Side.values()) {
            boards.get(side).setOverlayText(model.overlayOf(side));
        }
        repaint();
    }

    private static void drawCentered(Graphics2D g2, String text, int width, int y) {
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(text, (width - fm.stringWidth(text)) / 2, y);
    }

    /** 제목, 결과 요약, 키 안내를 그리는 위쪽 줄 */
    private final class Header extends JComponent {

        private static final long serialVersionUID = 1L;

        Header() {
            setPreferredSize(new Dimension(0, HEADER_HEIGHT));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(Color.BLACK);
            g2.fillRect(0, 0, getWidth(), getHeight());

            g2.setFont(new Font(Font.DIALOG, Font.BOLD, 22));
            g2.setColor(Color.CYAN);
            drawCentered(g2, model.title(), getWidth(), 30);

            g2.setFont(new Font(Font.DIALOG, Font.BOLD, 16));
            if (model.isEnded()) {
                g2.setColor(Color.YELLOW);
                drawCentered(g2, model.resultText(), getWidth(), 54);
            }
            g2.setFont(new Font(Font.DIALOG, Font.PLAIN, 13));
            g2.setColor(Color.GRAY);
            drawCentered(g2, model.hintText(), getWidth(), 72);
            g2.dispose();
        }
    }

    /** 보드 위에 플레이어 이름을 그리는 줄 */
    private static final class PlayerLabel extends JComponent {

        private static final long serialVersionUID = 1L;

        private final Side side;

        PlayerLabel(Side side) {
            this.side = side;
            setPreferredSize(new Dimension(0, LABEL_HEIGHT));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(Color.BLACK);
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setFont(new Font(Font.DIALOG, Font.BOLD, 18));
            g2.setColor(side == Side.P1 ? new Color(255, 120, 120) : new Color(120, 170, 255));
            drawCentered(g2, VersusScreenModel.playerLabel(side), getWidth(), 22);
            g2.dispose();
        }
    }
}
