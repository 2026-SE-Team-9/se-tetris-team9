package seoultech.se.tetris.ui.p2p;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.Objects;

import javax.swing.JComponent;
import javax.swing.JPanel;

import seoultech.se.tetris.ui.game.GamePanel;
import seoultech.se.tetris.ui.versus.VersusGamePanel;
import seoultech.se.tetris.ui.versus.VersusPresentation;
import seoultech.se.tetris.ui.versus.VersusScreenModel;
import seoultech.se.tetris.versus.p2p.ConnectionState;

/**
 * P2P 대전 화면. 로컬 대전 화면({@link VersusGamePanel})을 그대로 쓰고, 그 위에 연결 상태와 왕복 시간을 한 줄로 표시한다.
 *
 * <p>제목·이름·보드 순서는 {@link VersusPresentation#p2p}를 쓴 {@link VersusScreenModel}을 따른다.
 * 내 보드가 왼쪽에 놓이고, 결과는 내 관점의 승리 / 패배로 표시된다. 카운트다운도 로컬 대전과 같이 보드 위에 표시된다.
 *
 * <p>값을 바꾸는 것은 대전 흐름을 맡는 쪽이 한다. 연결 상태가 바뀌면 {@link #setConnection}을 호출한다.
 */
public class P2PMatchPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    static final int STATUS_HEIGHT = 24;
    private static final int STATUS_MARGIN = 24;

    private final VersusGamePanel game;
    private final StatusLine statusLine = new StatusLine();
    private ConnectionState connectionState = ConnectionState.NORMAL;
    private int roundTripMillis = -1;

    public P2PMatchPanel(VersusGamePanel game) {
        super(new BorderLayout());
        this.game = Objects.requireNonNull(game, "game");
        setBackground(Color.BLACK);
        add(statusLine, BorderLayout.NORTH);
        add(game, BorderLayout.CENTER);
    }

    /**
     * P2P 대전 화면을 만든다.
     *
     * @param model   {@link VersusPresentation#p2p}로 만든 화면 모델
     * @param p1Board 방장(P1) 보드
     * @param p2Board 참여자(P2) 보드
     */
    public static P2PMatchPanel create(VersusScreenModel model, GamePanel p1Board, GamePanel p2Board) {
        return new P2PMatchPanel(new VersusGamePanel(model, p1Board, p2Board));
    }

    /** 안쪽의 대전 화면. 보드 다시 그리기, 결과 표시, 키 입력 연결에 쓴다. */
    public VersusGamePanel game() {
        return game;
    }

    /** 연결 상태와 왕복 시간을 바꾸고 다시 그린다. 왕복 시간을 아직 잰 적이 없으면 -1 */
    public void setConnection(ConnectionState state, int roundTripMillis) {
        this.connectionState = Objects.requireNonNull(state, "state");
        this.roundTripMillis = roundTripMillis;
        statusLine.repaint();
    }

    /** 위쪽 줄에 표시하는 연결 상태 문구. 예: "연결 상태: 정상 · 12ms" */
    String connectionText() {
        return P2PScreenTexts.connection(connectionState, roundTripMillis);
    }

    Color connectionColor() {
        return P2PPanel.connectionColor(connectionState);
    }

    /** 오른쪽 위에 연결 상태를 그리는 줄 */
    private final class StatusLine extends JComponent {

        private static final long serialVersionUID = 1L;

        StatusLine() {
            setPreferredSize(new Dimension(0, STATUS_HEIGHT));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(Color.BLACK);
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setFont(new Font(Font.DIALOG, Font.BOLD, 13));
            g2.setColor(connectionColor());
            String text = connectionText();
            FontMetrics metrics = g2.getFontMetrics();
            g2.drawString(text, getWidth() - STATUS_MARGIN - metrics.stringWidth(text), 18);
            g2.dispose();
        }
    }
}
