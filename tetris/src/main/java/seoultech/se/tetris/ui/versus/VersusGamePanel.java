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
 * 대전 화면에서는 각 보드의 점수 아래에 공격 대기열을 함께 표시한다.
 * 시간제한 모드에서는 위쪽 줄 오른쪽에 남은 시간을 표시한다.
 */
public class VersusGamePanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final int HEADER_HEIGHT = 76;
    private static final int LABEL_HEIGHT = 30;
    private static final int GAP = 12;
    /** 남은 시간을 그리는 오른쪽 여백(px) */
    private static final int TIMER_MARGIN = 24;
    static final Color TIMER_COLOR = Color.WHITE;
    static final Color TIMER_URGENT_COLOR = new Color(255, 110, 110);

    private final transient VersusScreenModel model;
    private final Map<Side, GamePanel> boards = new EnumMap<>(Side.class);
    private final Header header = new Header();

    public VersusGamePanel(VersusScreenModel model, GamePanel p1Board, GamePanel p2Board) {
        super(new BorderLayout());
        this.model = Objects.requireNonNull(model, "model");
        boards.put(Side.P1, Objects.requireNonNull(p1Board, "p1Board"));
        boards.put(Side.P2, Objects.requireNonNull(p2Board, "p2Board"));
        // 대전 화면에서는 두 보드 모두 점수 아래에 공격 대기열을 표시한다
        boards.values().forEach(board -> board.setAttackQueueVisible(true));
        setBackground(Color.BLACK);
        setFocusable(true);

        add(header, BorderLayout.NORTH);
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

    /** 남은 시간 표시만 다시 그린다. 시간제한 모드에서 주기적으로 호출한다. */
    public void repaintTimer() {
        header.repaint();
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
            if (model.hasTimer()) {
                drawTimer(g2);
            }
            g2.dispose();
        }

        /** 오른쪽에 "남은 시간" 이름과 분:초를 오른쪽 정렬로 그린다. 남은 시간이 얼마 없으면 붉게 그린다. */
        private void drawTimer(Graphics2D g2) {
            int right = getWidth() - TIMER_MARGIN;
            g2.setFont(new Font(Font.DIALOG, Font.BOLD, 13));
            g2.setColor(Color.GRAY);
            String label = "남은 시간";
            g2.drawString(label, right - g2.getFontMetrics().stringWidth(label), 22);

            g2.setFont(new Font(Font.MONOSPACED, Font.BOLD, 30));
            g2.setColor(model.isTimerUrgent() ? TIMER_URGENT_COLOR : TIMER_COLOR);
            String time = model.timerText();
            g2.drawString(time, right - g2.getFontMetrics().stringWidth(time), 54);
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
