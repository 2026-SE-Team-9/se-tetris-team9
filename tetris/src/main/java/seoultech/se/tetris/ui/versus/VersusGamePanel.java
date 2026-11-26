package seoultech.se.tetris.ui.versus;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

import javax.swing.JComponent;
import javax.swing.JPanel;

import seoultech.se.tetris.ui.game.GamePanel;
import seoultech.se.tetris.ui.theme.RetroTheme;
import seoultech.se.tetris.versus.Side;

/**
 * 로컬 대전 화면. 위쪽에 제목·결과·키 안내를, 아래에 P1(왼쪽)·P2(오른쪽) 보드를 나란히 표시한다.
 *
 * <p>각 보드는 1인 모드와 같은 {@link GamePanel}로 그리므로 블럭·고스트·아이템·줄 삭제 애니메이션이 1인 모드와 같다.
 * 대전 화면에서는 각 보드의 점수 아래에 공격 대기열을 함께 표시한다.
 * 제목·플레이어 이름·보드 순서는 {@link VersusPresentation}을 따르므로 P2P 대전 화면에도 그대로 쓴다.
 * 시간제한 모드에서는 위쪽 줄 오른쪽에 남은 시간을 표시한다.
 *
 * <p>레트로 디자인({@link RetroTheme})으로 그린다. 배경 격자를 화면 전체에 한 번에 그리고, 머리줄·이름표·보드는
 * 배경을 따로 칠하지 않아 두 보드 사이까지 같은 배경이 이어진다.
 */
public class VersusGamePanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final int HEADER_HEIGHT = 76;
    private static final int LABEL_HEIGHT = 30;
    private static final int GAP = 12;
    /** 플레이어 이름의 색. 대전 화면과 종료 화면이 같은 색을 쓴다 */
    static final Color P1_COLOR = RetroTheme.PINK;
    static final Color P2_COLOR = RetroTheme.CYAN;
    /** 남은 시간 틀의 오른쪽 여백과 크기(px) */
    private static final int TIMER_MARGIN = 24;
    private static final int TIMER_WIDTH = 120;
    private static final int TIMER_HEIGHT = 56;
    static final Color TIMER_COLOR = RetroTheme.TEXT;
    static final Color TIMER_URGENT_COLOR = RetroTheme.PINK;

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
        // 배경은 이 화면이 한 번에 그리므로 보드는 자기 배경을 그리지 않는다
        boards.values().forEach(board -> board.setOpaque(false));
        setBackground(RetroTheme.BACKGROUND_TOP);
        setFocusable(true);

        add(header, BorderLayout.NORTH);
        JPanel center = new JPanel(new FlowLayout(FlowLayout.CENTER, GAP, 0));
        center.setOpaque(false);
        // 로컬 대전은 P1이, P2P 대전은 내 보드가 왼쪽이다
        center.add(column(model.presentation().leftSide()));
        center.add(column(model.presentation().rightSide()));
        add(center, BorderLayout.CENTER);
    }

    private JPanel column(Side side) {
        JPanel column = new JPanel(new BorderLayout());
        column.setOpaque(false);
        column.add(new PlayerLabel(side, model.labelOf(side)), BorderLayout.NORTH);
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

    /**
     * 화면 전체에 레트로 배경(그라데이션과 격자)을 그린다. 머리줄·이름표·보드는 그 위에 그린다.
     * P2P 대전 화면처럼 바깥 화면이 배경을 이어서 그리는 경우에는 불투명하지 않게 두어 배경을 그리지 않는다.
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (!isOpaque()) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        RetroTheme.paintBackground(g2, getWidth(), getHeight());
        g2.dispose();
    }

    /** 남은 시간 표시만 다시 그린다. 시간제한 모드에서 주기적으로 호출한다. */
    public void repaintTimer() {
        header.repaint();
    }

    /** 플레이어 이름의 색 */
    static Color playerColor(Side side) {
        return Objects.requireNonNull(side, "side") == Side.P1 ? P1_COLOR : P2_COLOR;
    }

    /** 제목, 결과 요약, 키 안내를 그리는 위쪽 줄 */
    private final class Header extends JComponent {

        private static final long serialVersionUID = 1L;

        Header() {
            setOpaque(false);
            setPreferredSize(new Dimension(0, HEADER_HEIGHT));
        }

        /** 제목은 분홍 글자와 하늘색 그림자, 결과는 노랑, 안내는 보조 글자색으로 가운데에 그린다. */
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            RetroTheme.applyRenderingHints(g2);

            g2.setFont(RetroTheme.mono(Font.BOLD, 24));
            RetroTheme.drawShadowText(g2, model.title(), 0, getWidth(), 32, 2);

            if (model.isEnded()) {
                g2.setFont(RetroTheme.mono(Font.BOLD, 15));
                g2.setColor(RetroTheme.YELLOW);
                RetroTheme.drawCentered(g2, model.resultText(), getWidth(), 54);
            }
            g2.setFont(RetroTheme.mono(Font.PLAIN, 12));
            g2.setColor(RetroTheme.MUTED);
            RetroTheme.drawCentered(g2, model.hintText(), getWidth(), 71);
            if (model.hasTimer()) {
                drawTimer(g2);
            }
            g2.dispose();
        }

        /** 남은 시간 틀의 위치. 머리줄 오른쪽에 둔다 */
        Rectangle timerFrame() {
            return new Rectangle(getWidth() - TIMER_MARGIN - TIMER_WIDTH, (HEADER_HEIGHT - TIMER_HEIGHT) / 2,
                    TIMER_WIDTH, TIMER_HEIGHT);
        }

        /** 오른쪽 하늘색 틀 안에 "남은 시간"과 분:초를 그린다. 남은 시간이 얼마 없으면 분홍으로 그린다. */
        private void drawTimer(Graphics2D g2) {
            Rectangle frame = timerFrame();
            RetroTheme.drawFrame(g2, frame.x, frame.y, frame.width, frame.height, RetroTheme.CYAN);
            g2.setFont(RetroTheme.mono(Font.BOLD, 11));
            g2.setColor(RetroTheme.MUTED);
            g2.drawString("남은 시간", frame.x + 10, frame.y + 18);

            g2.setFont(RetroTheme.mono(Font.BOLD, 24));
            g2.setColor(model.isTimerUrgent() ? TIMER_URGENT_COLOR : TIMER_COLOR);
            String time = model.timerText();
            g2.drawString(time, RetroTheme.centeredX(g2, time, frame.x, frame.width), frame.y + 46);
        }
    }

    /** 보드 위에 플레이어 이름을 그리는 줄 */
    private static final class PlayerLabel extends JComponent {

        private static final long serialVersionUID = 1L;

        private final Side side;
        private final String label;

        PlayerLabel(Side side, String label) {
            this.side = side;
            this.label = label;
            setOpaque(false);
            setPreferredSize(new Dimension(0, LABEL_HEIGHT));
        }

        /** 플레이어 색(P1 분홍, P2 하늘색)의 고정폭 글자로 이름을 가운데에 그린다. */
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            RetroTheme.applyRenderingHints(g2);
            g2.setFont(RetroTheme.mono(Font.BOLD, 17));
            g2.setColor(playerColor(side));
            RetroTheme.drawCentered(g2, label, getWidth(), 21);
            g2.dispose();
        }
    }
}
