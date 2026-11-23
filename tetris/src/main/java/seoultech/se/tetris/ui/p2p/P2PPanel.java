package seoultech.se.tetris.ui.p2p;

import java.awt.BasicStroke;
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
import java.util.Objects;

import javax.swing.JPanel;

import seoultech.se.tetris.config.P2PSettingsService.RecentPeer;
import seoultech.se.tetris.versus.p2p.CloseReason;
import seoultech.se.tetris.versus.p2p.ConnectionState;

/**
 * P2P 대전 모드의 연결 화면. 첫 화면, 방 생성, 참여, 대기실을 그린다.
 *
 * <p>그릴 값은 {@link P2PScreenState}에서 읽고, 눌린 키는 {@link P2PScreenActions}에 알린다.
 * 연결과 화면 흐름은 두 인터페이스를 구현하는 쪽이 맡으며, 상태가 바뀌면 그쪽이 {@link #repaint()}를 호출한다.
 *
 * <p>시작 메뉴와 같은 크기와 색을 쓴다. 입력칸도 직접 그려 모든 조작을 키보드로 한다.
 */
public class P2PPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    static final Color SELECTED = Color.YELLOW;
    static final Color ERROR = new Color(255, 110, 110);
    static final Color SUCCESS = new Color(120, 230, 140);
    private static final Color SURFACE = new Color(25, 25, 25);
    private static final Color OUTLINE = new Color(75, 75, 75);
    private static final Color MUTED = new Color(170, 170, 170);

    private static final int FIELD_LEFT = 60;
    private static final int FIELD_WIDTH = 360;
    private static final int FIELD_HEIGHT = 36;
    private static final int RECENT_COLUMNS = 2;
    private static final int RECENT_ROWS = 5;

    private final transient P2PScreenState state;
    private final transient P2PScreenActions actions;

    public P2PPanel(P2PScreenState state, P2PScreenActions actions) {
        this.state = Objects.requireNonNull(state, "state");
        this.actions = Objects.requireNonNull(actions, "actions");
        setBackground(Color.BLACK);
        // 시작 메뉴와 같은 크기로 두어 화면 전환 시 창 크기가 바뀌지 않게 한다
        setPreferredSize(new Dimension(480, 540));
        setFocusable(true);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent event) {
                handleKey(event.getKeyCode());
            }

            @Override
            public void keyTyped(KeyEvent event) {
                handleTyped(event.getKeyChar());
            }
        });
    }

    /** 키 하나를 동작으로 알린다. 화면 이동에 쓰지 않는 키는 무시한다. */
    void handleKey(int keyCode) {
        switch (keyCode) {
            case KeyEvent.VK_UP -> actions.up();
            case KeyEvent.VK_DOWN -> actions.down();
            case KeyEvent.VK_ENTER -> actions.select();
            case KeyEvent.VK_ESCAPE -> actions.back();
            case KeyEvent.VK_BACK_SPACE -> actions.erase();
            default -> { }
        }
    }

    /** 입력한 글자를 알린다. Enter·Backspace 같은 제어 문자는 {@link #handleKey}가 처리하므로 넘기지 않는다. */
    void handleTyped(char c) {
        if (c != KeyEvent.CHAR_UNDEFINED && !Character.isISOControl(c)) {
            actions.type(c);
        }
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g2 = (Graphics2D) graphics.create();
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        switch (state.screen()) {
            case MENU -> paintMenu(g2);
            case HOST -> paintHost(g2);
            case JOIN -> paintJoin(g2);
            case WAITING_ROOM -> paintWaitingRoom(g2);
        }
        g2.dispose();
    }

    // ------------------------------------------------------------------
    // 첫 화면
    // ------------------------------------------------------------------

    private void paintMenu(Graphics2D g2) {
        title(g2, "P2P 대전", "방을 만들거나 상대의 방에 참여하세요");

        List<String> items = P2PScreenTexts.MENU_ITEMS;
        int y = 210;
        for (int i = 0; i < items.size(); i++) {
            boolean selected = i == state.menuIndex();
            g2.setFont(new Font(Font.DIALOG, Font.BOLD, 22));
            g2.setColor(selected ? SELECTED : Color.GRAY);
            drawCentered(g2, (selected ? "▶ " : "   ") + items.get(i), y);
            String description = P2PScreenTexts.MENU_DESCRIPTIONS.get(i);
            if (!description.isEmpty()) {
                g2.setFont(new Font(Font.DIALOG, Font.PLAIN, 13));
                g2.setColor(selected ? Color.WHITE : Color.DARK_GRAY);
                drawCentered(g2, description, y + 22);
            }
            y += 68;
        }

        CloseReason reason = state.closeReason();
        String notice = reason == null ? null : P2PScreenTexts.close(reason);
        if (notice != null) {
            g2.setFont(new Font(Font.DIALOG, Font.BOLD, 15));
            g2.setColor(P2PScreenTexts.isCloseError(reason) ? ERROR : MUTED);
            drawCentered(g2, notice, 450);
        }
        hint(g2, "↑ ↓ 이동   Enter 선택   Esc 뒤로");
    }

    // ------------------------------------------------------------------
    // 방 생성
    // ------------------------------------------------------------------

    private void paintHost(Graphics2D g2) {
        title(g2, "방 생성", "상대에게 아래 주소와 포트를 알려주세요");

        List<String> addresses = P2PScreenTexts.hostAddressLines(state.hostAddresses(), state.hostPort());
        int boxHeight = 30 + addresses.size() * 34;
        box(g2, FIELD_LEFT, 170, FIELD_WIDTH, boxHeight, OUTLINE);
        boolean noAddress = state.hostAddresses().isEmpty();
        int y = 205;
        for (String line : addresses) {
            g2.setFont(noAddress ? new Font(Font.DIALOG, Font.PLAIN, 15) : new Font(Font.MONOSPACED, Font.BOLD, 24));
            g2.setColor(noAddress ? MUTED : Color.WHITE);
            drawCentered(g2, line, y);
            y += 34;
        }

        paintProgress(g2, 170 + boxHeight + 50);
        hint(g2, state.failure() != null ? "Enter 다시 시도   Esc 취소" : "Esc 취소");
    }

    /** 실패 안내(붉은색)가 있으면 그것을, 없으면 진행 단계(하늘색)를 가운데에 그린다. */
    private void paintProgress(Graphics2D g2, int y) {
        g2.setFont(new Font(Font.DIALOG, Font.BOLD, 15));
        String error = errorText();
        if (error != null) {
            g2.setColor(ERROR);
            drawCentered(g2, error, y);
        } else if (state.step() != null) {
            g2.setColor(Color.CYAN);
            drawCentered(g2, P2PScreenTexts.step(state.step()), y);
        }
    }

    /** 연결 실패와 잘못된 입력 중 표시할 안내. 없으면 {@code null} */
    private String errorText() {
        if (state.failure() != null) {
            return P2PScreenTexts.failure(state.failure(), state.hostPort());
        }
        if (state.screen() == P2PScreenState.Screen.JOIN && state.inputError() != null) {
            return P2PScreenTexts.inputError(state.inputError());
        }
        return null;
    }

    // ------------------------------------------------------------------
    // 참여
    // ------------------------------------------------------------------

    private void paintJoin(Graphics2D g2) {
        title(g2, "참여", "상대의 IP 주소와 포트를 입력하세요");

        field(g2, "IP 주소", state.ipText(), 160, P2PScreenState.JOIN_ROW_IP);
        field(g2, "포트", state.portText(), 228, P2PScreenState.JOIN_ROW_PORT);

        boolean connectSelected = state.joinRow() == P2PScreenState.JOIN_ROW_CONNECT && !state.isConnecting();
        int buttonTop = 282;
        box(g2, FIELD_LEFT, buttonTop, FIELD_WIDTH, 34, connectSelected ? SELECTED : OUTLINE);
        g2.setFont(new Font(Font.DIALOG, Font.BOLD, 17));
        g2.setColor(state.isConnecting() ? MUTED : connectSelected ? SELECTED : Color.WHITE);
        drawCentered(g2, state.isConnecting() ? "접속 중..." : "접속", buttonTop + 23);

        paintProgress(g2, 342);
        paintRecent(g2, 372);
        hint(g2, state.isConnecting() ? "Esc 접속 취소" : "↑ ↓ 이동   Enter 접속·선택   Esc 뒤로");
    }

    /** 이름표와 입력칸. 선택된 칸은 노란 테두리와 커서(_)로 표시한다. */
    private void field(Graphics2D g2, String label, String value, int top, int row) {
        boolean selected = state.joinRow() == row && !state.isConnecting();
        g2.setFont(new Font(Font.DIALOG, Font.BOLD, 14));
        g2.setColor(MUTED);
        g2.drawString(label, FIELD_LEFT, top - 8);
        box(g2, FIELD_LEFT, top, FIELD_WIDTH, FIELD_HEIGHT, selected ? SELECTED : OUTLINE);
        g2.setFont(new Font(Font.MONOSPACED, Font.BOLD, 20));
        g2.setColor(Color.WHITE);
        g2.drawString(value + (selected ? "_" : ""), FIELD_LEFT + 12, top + 25);
    }

    /** 최근 접속 기록을 2열 5행으로, 위에서 아래로 최신순으로 그린다. */
    private void paintRecent(Graphics2D g2, int top) {
        g2.setFont(new Font(Font.DIALOG, Font.BOLD, 14));
        g2.setColor(MUTED);
        g2.drawString("최근 접속", FIELD_LEFT, top);

        List<RecentPeer> peers = state.recentPeers();
        if (peers.isEmpty()) {
            g2.setFont(new Font(Font.DIALOG, Font.PLAIN, 13));
            g2.setColor(Color.DARK_GRAY);
            g2.drawString(P2PScreenTexts.NO_RECENT, FIELD_LEFT, top + 24);
            return;
        }
        int columnWidth = FIELD_WIDTH / RECENT_COLUMNS;
        g2.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        for (int i = 0; i < Math.min(peers.size(), RECENT_COLUMNS * RECENT_ROWS); i++) {
            int x = FIELD_LEFT + (i / RECENT_ROWS) * columnWidth;
            int y = top + 22 + (i % RECENT_ROWS) * 20;
            boolean selected = state.joinRow() == P2PScreenState.JOIN_ROW_CONNECT + 1 + i && !state.isConnecting();
            g2.setColor(selected ? SELECTED : Color.WHITE);
            g2.drawString((selected ? "▶" : " ") + peers.get(i).text(), x, y);
        }
    }

    // ------------------------------------------------------------------
    // 대기실
    // ------------------------------------------------------------------

    private void paintWaitingRoom(Graphics2D g2) {
        title(g2, "대기실", "");
        g2.setFont(new Font(Font.DIALOG, Font.BOLD, 22));
        g2.setColor(SUCCESS);
        drawCentered(g2, "상대와 연결되었습니다", 140);

        box(g2, FIELD_LEFT, 180, FIELD_WIDTH, 150, OUTLINE);
        row(g2, "내 역할", P2PScreenTexts.role(state.role()), 220);
        row(g2, "대전 설정", P2PScreenTexts.settings(state.mode(), state.difficulty()), 260);
        ConnectionState connection = state.connectionState();
        g2.setFont(new Font(Font.DIALOG, Font.BOLD, 15));
        g2.setColor(connectionColor(connection));
        drawCentered(g2, P2PScreenTexts.connection(connection, state.roundTripMillis()), 304);

        hint(g2, "Esc 나가기 (연결을 끊고 첫 화면으로)");
    }

    /** 연결 상태의 색. 정상은 초록, 지연은 노랑, 그보다 나쁘면 빨강 */
    static Color connectionColor(ConnectionState connection) {
        return switch (connection) {
            case NORMAL -> SUCCESS;
            case LAGGING -> SELECTED;
            case CHECKING, DISCONNECTED -> ERROR;
        };
    }

    private void row(Graphics2D g2, String label, String value, int y) {
        g2.setFont(new Font(Font.DIALOG, Font.BOLD, 14));
        g2.setColor(MUTED);
        g2.drawString(label, FIELD_LEFT + 24, y);
        g2.setFont(new Font(Font.DIALOG, Font.BOLD, 17));
        g2.setColor(Color.WHITE);
        g2.drawString(value, FIELD_LEFT + 120, y);
    }

    // ------------------------------------------------------------------
    // 공통
    // ------------------------------------------------------------------

    private void title(Graphics2D g2, String title, String subtitle) {
        g2.setFont(new Font(Font.DIALOG, Font.BOLD, 32));
        g2.setColor(Color.CYAN);
        drawCentered(g2, title, 80);
        if (!subtitle.isEmpty()) {
            g2.setFont(new Font(Font.DIALOG, Font.PLAIN, 15));
            g2.setColor(Color.WHITE);
            drawCentered(g2, subtitle, 112);
        }
    }

    private void hint(Graphics2D g2, String text) {
        g2.setFont(new Font(Font.DIALOG, Font.PLAIN, 13));
        g2.setColor(MUTED);
        drawCentered(g2, text, getHeight() - 24);
    }

    private static void box(Graphics2D g2, int x, int y, int width, int height, Color border) {
        g2.setColor(SURFACE);
        g2.fillRoundRect(x, y, width, height, 10, 10);
        g2.setStroke(new BasicStroke(border == SELECTED ? 2f : 1f));
        g2.setColor(border);
        g2.drawRoundRect(x, y, width, height, 10, 10);
    }

    private void drawCentered(Graphics2D g2, String text, int y) {
        FontMetrics metrics = g2.getFontMetrics();
        g2.drawString(text, (getWidth() - metrics.stringWidth(text)) / 2, y);
    }
}
