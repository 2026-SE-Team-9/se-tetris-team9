package seoultech.se.tetris.ui.p2p;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.Objects;

import javax.swing.JPanel;

import seoultech.se.tetris.config.P2PSettingsService.RecentPeer;
import seoultech.se.tetris.ui.DifficultyLabels;
import seoultech.se.tetris.ui.theme.RetroTheme;
import seoultech.se.tetris.ui.versus.VersusModeLabels;
import seoultech.se.tetris.versus.p2p.CloseReason;
import seoultech.se.tetris.versus.p2p.ConnectionState;
import seoultech.se.tetris.versus.p2p.Role;

/**
 * P2P 대전 모드의 연결 화면. 첫 화면, 방 생성, 참여, 대기실을 그린다.
 *
 * <p>그릴 값은 {@link P2PScreenState}에서 읽고, 눌린 키는 {@link P2PScreenActions}에 알린다.
 * 연결과 화면 흐름은 두 인터페이스를 구현하는 쪽이 맡으며, 상태가 바뀌면 그쪽이 {@link #repaint()}를 호출한다.
 *
 * <p>시작 메뉴와 같은 크기와 레트로 디자인({@link RetroTheme})을 쓴다. 입력칸도 직접 그려 모든 조작을 키보드로 한다.
 */
public class P2PPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    /** 선택한 항목과 입력칸 */
    static final Color SELECTED = RetroTheme.YELLOW;
    /** 실패·끊김 안내 */
    static final Color ERROR = RetroTheme.PINK;
    /** 연결 성공·준비 완료 */
    static final Color SUCCESS = RetroTheme.GREEN;
    private static final Color SURFACE = RetroTheme.SURFACE;
    /** 선택하지 않은 입력칸·버튼의 테두리. 배경 위에서 옅게 보이는 남보라 */
    private static final Color OUTLINE = new Color(0x3A, 0x34, 0x6E);
    private static final Color MUTED = RetroTheme.MUTED;

    /** 첫 화면 메뉴를 감싸는 틀과 항목 위치 */
    static final Rectangle MENU_FRAME = new Rectangle(70, 160, 340, 202);
    static final int MENU_FIRST_BASELINE = 200;
    static final int MENU_SPACING = 68;

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
        setBackground(RetroTheme.BACKGROUND_TOP);
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
            case KeyEvent.VK_LEFT -> actions.left();
            case KeyEvent.VK_RIGHT -> actions.right();
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
        RetroTheme.applyRenderingHints(g2);
        RetroTheme.paintBackground(g2, getWidth(), getHeight());
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

    /** {@code index}번째 첫 화면 항목을 선택했을 때 그리는 강조 상자의 위치 */
    static Rectangle menuSelectionBounds(int index) {
        int baseline = MENU_FIRST_BASELINE + index * MENU_SPACING;
        return new Rectangle(MENU_FRAME.x + 20, baseline - 24, MENU_FRAME.width - 40, 34);
    }

    /**
     * 첫 화면. 하늘색 틀 안에 방 생성 / 참여 / 뒤로 가기와 각 설명을 그린다. 선택한 항목은 시작 메뉴와 같은
     * 노란 상자와 픽셀 화살표로 표시하고, 설명은 선택한 항목만 밝게 보인다.
     */
    private void paintMenu(Graphics2D g2) {
        title(g2, "P2P 대전", "방을 만들거나 상대의 방에 참여하세요");

        RetroTheme.drawFrame(g2, MENU_FRAME.x, MENU_FRAME.y, MENU_FRAME.width, MENU_FRAME.height, RetroTheme.CYAN);
        List<String> items = P2PScreenTexts.MENU_ITEMS;
        for (int i = 0; i < items.size(); i++) {
            int y = MENU_FIRST_BASELINE + i * MENU_SPACING;
            boolean selected = i == state.menuIndex();
            if (selected) {
                Rectangle box = menuSelectionBounds(i);
                RetroTheme.drawSelection(g2, box.x, box.y, box.width, box.height);
            }
            g2.setFont(RetroTheme.mono(Font.BOLD, 20));
            g2.setColor(selected ? SELECTED : MUTED);
            drawCentered(g2, items.get(i), y);
            String description = P2PScreenTexts.MENU_DESCRIPTIONS.get(i);
            if (!description.isEmpty()) {
                g2.setFont(RetroTheme.mono(Font.PLAIN, 12));
                g2.setColor(selected ? RetroTheme.TEXT : OUTLINE);
                drawCentered(g2, description, y + 28);
            }
        }

        CloseReason reason = state.closeReason();
        String notice = reason == null ? null : P2PScreenTexts.close(reason);
        if (notice != null) {
            g2.setFont(RetroTheme.mono(Font.BOLD, 14));
            g2.setColor(P2PScreenTexts.isCloseError(reason) ? ERROR : MUTED);
            drawCentered(g2, notice, 450);
        }
        hint(g2, "↑ ↓ 이동   Enter 선택   Esc 뒤로");
    }

    // ------------------------------------------------------------------
    // 방 생성
    // ------------------------------------------------------------------

    /** 방 생성 화면의 주소 틀 위쪽 y 좌표 */
    static final int HOST_FRAME_TOP = 156;

    /** 주소 틀의 위치. 주소가 여러 개면 줄 수만큼 길어진다 */
    Rectangle hostFrame() {
        int lines = Math.max(1, state.hostAddresses().size());
        return new Rectangle(FIELD_LEFT - 20, HOST_FRAME_TOP, FIELD_WIDTH + 40, 52 + lines * 36);
    }

    /**
     * 방 생성. 하늘색 틀 안에 "내 주소"와 이 PC의 IPv4 주소·포트를 크게 그리고, 그 아래에 진행 단계나 실패 안내를 그린다.
     */
    private void paintHost(Graphics2D g2) {
        title(g2, "방 생성", "상대에게 아래 주소와 포트를 알려주세요");

        Rectangle frame = hostFrame();
        RetroTheme.drawFrame(g2, frame.x, frame.y, frame.width, frame.height, RetroTheme.CYAN);
        g2.setFont(RetroTheme.mono(Font.BOLD, 12));
        g2.setColor(MUTED);
        g2.drawString("내 주소 : 포트", frame.x + 16, frame.y + 24);

        List<String> addresses = P2PScreenTexts.hostAddressLines(state.hostAddresses(), state.hostPort());
        boolean noAddress = state.hostAddresses().isEmpty();
        int y = frame.y + 60;
        for (String line : addresses) {
            g2.setFont(noAddress ? RetroTheme.mono(Font.PLAIN, 14) : RetroTheme.mono(Font.BOLD, 24));
            g2.setColor(noAddress ? MUTED : RetroTheme.TEXT);
            drawCentered(g2, line, y);
            y += 36;
        }

        paintProgress(g2, frame.y + frame.height + 46);
        hint(g2, state.failure() != null ? "Enter 다시 시도   Esc 취소" : "Esc 취소");
    }

    /**
     * 실패 안내(분홍)가 있으면 그것을, 없으면 진행 단계(하늘색)를 가운데에 그린다. 긴 안내도 화면 안에 들어오도록
     * 작은 고정폭 글꼴로 쓰고, 앞에 상태 표시 사각형을 둔다.
     */
    private void paintProgress(Graphics2D g2, int y) {
        g2.setFont(RetroTheme.mono(Font.BOLD, 13));
        String error = errorText();
        String text = error != null ? error : state.step() == null ? null : P2PScreenTexts.step(state.step());
        if (text == null) {
            return;
        }
        Color color = error != null ? ERROR : RetroTheme.CYAN;
        int width = g2.getFontMetrics().stringWidth(text);
        int x = (getWidth() - width) / 2 + 7;
        g2.setColor(color);
        g2.fillRect(x - 14, y - 9, 8, 8);
        g2.drawString(text, x, y);
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

    /** 참여 화면의 접속 버튼 위치 */
    static final Rectangle CONNECT_BUTTON = new Rectangle(FIELD_LEFT, 282, FIELD_WIDTH, 34);
    /** 참여 화면의 최근 접속 틀 위치 */
    static final Rectangle RECENT_FRAME = new Rectangle(FIELD_LEFT - 10, 358, FIELD_WIDTH + 20, 128);
    private static final int RECENT_ROW_HEIGHT = 20;

    /**
     * 참여. IP·포트 입력칸, 접속 버튼, 진행·오류 안내, 하늘색 틀 안의 최근 접속 목록을 그린다.
     * 선택한 칸·버튼·기록은 노란색으로 표시한다.
     */
    private void paintJoin(Graphics2D g2) {
        title(g2, "참여", "상대의 IP 주소와 포트를 입력하세요");

        field(g2, "IP 주소", state.ipText(), 160, P2PScreenState.JOIN_ROW_IP);
        field(g2, "포트", state.portText(), 228, P2PScreenState.JOIN_ROW_PORT);
        paintConnectButton(g2);

        paintProgress(g2, 340);
        paintRecent(g2);
        hint(g2, state.isConnecting() ? "Esc 접속 취소" : "↑ ↓ 이동   Enter 접속·선택   Esc 뒤로");
    }

    /** 이름표와 입력칸. 선택된 칸은 노란 테두리와 커서(_)로 표시한다. */
    private void field(Graphics2D g2, String label, String value, int top, int row) {
        boolean selected = state.joinRow() == row && !state.isConnecting();
        g2.setFont(RetroTheme.mono(Font.BOLD, 13));
        g2.setColor(MUTED);
        g2.drawString(label, FIELD_LEFT, top - 8);
        box(g2, FIELD_LEFT, top, FIELD_WIDTH, FIELD_HEIGHT, selected ? SELECTED : OUTLINE);
        g2.setFont(RetroTheme.mono(Font.BOLD, 20));
        g2.setColor(RetroTheme.TEXT);
        g2.drawString(value + (selected ? "_" : ""), FIELD_LEFT + 12, top + 25);
    }

    /** 접속 버튼. 선택하면 시작 메뉴와 같은 노란 상자와 픽셀 화살표로, 접속 중이면 흐리게 그린다. */
    private void paintConnectButton(Graphics2D g2) {
        Rectangle button = CONNECT_BUTTON;
        boolean selected = state.joinRow() == P2PScreenState.JOIN_ROW_CONNECT && !state.isConnecting();
        if (selected) {
            RetroTheme.drawSelection(g2, button.x, button.y, button.width, button.height);
        } else {
            box(g2, button.x, button.y, button.width, button.height, OUTLINE);
        }
        g2.setFont(RetroTheme.mono(Font.BOLD, 17));
        g2.setColor(state.isConnecting() ? MUTED : selected ? SELECTED : RetroTheme.TEXT);
        drawCentered(g2, state.isConnecting() ? "접속 중..." : "접속", button.y + 23);
    }

    /** {@code index}번째 최근 접속 기록이 그려지는 칸. 2열 5행에 위에서 아래로 최신순으로 놓는다 */
    static Rectangle recentCell(int index) {
        int columnWidth = FIELD_WIDTH / RECENT_COLUMNS;
        int x = FIELD_LEFT + (index / RECENT_ROWS) * columnWidth;
        int y = RECENT_FRAME.y + 26 + (index % RECENT_ROWS) * RECENT_ROW_HEIGHT;
        return new Rectangle(x - 4, y, columnWidth - 4, RECENT_ROW_HEIGHT);
    }

    /** 하늘색 틀 안에 최근 접속 기록을 그린다. 선택한 기록은 옅은 노란 줄과 노란 글자로 표시한다. */
    private void paintRecent(Graphics2D g2) {
        Rectangle frame = RECENT_FRAME;
        RetroTheme.drawFrame(g2, frame.x, frame.y, frame.width, frame.height, RetroTheme.CYAN);
        g2.setFont(RetroTheme.mono(Font.BOLD, 12));
        g2.setColor(MUTED);
        g2.drawString("최근 접속", frame.x + 14, frame.y + 20);

        List<RecentPeer> peers = state.recentPeers();
        if (peers.isEmpty()) {
            g2.setFont(RetroTheme.mono(Font.PLAIN, 13));
            g2.setColor(MUTED);
            drawCentered(g2, P2PScreenTexts.NO_RECENT, frame.y + frame.height / 2 + 12);
            return;
        }
        g2.setFont(RetroTheme.mono(Font.PLAIN, 12));
        for (int i = 0; i < Math.min(peers.size(), RECENT_COLUMNS * RECENT_ROWS); i++) {
            Rectangle cell = recentCell(i);
            boolean selected = state.joinRow() == P2PScreenState.JOIN_ROW_CONNECT + 1 + i && !state.isConnecting();
            if (selected) {
                g2.setColor(RetroTheme.withAlpha(SELECTED, 40));
                g2.fillRect(cell.x, cell.y, cell.width, cell.height);
            }
            g2.setColor(selected ? SELECTED : RetroTheme.TEXT);
            g2.drawString(peers.get(i).text(), cell.x + 6, cell.y + 15);
        }
    }

    // ------------------------------------------------------------------
    // 대기실
    // ------------------------------------------------------------------

    private static final int ROOM_ROW_HEIGHT = 38;
    private static final int ROOM_SETTINGS_TOP = 160;
    /** 대전 설정 두 줄을 감싸는 틀 */
    static final Rectangle ROOM_SETTINGS_FRAME = new Rectangle(FIELD_LEFT - 10, 148, FIELD_WIDTH + 20, 110);
    /** 나와 상대의 준비 상태 카드 */
    static final int READY_TOP = 290;
    static final int READY_HEIGHT = 44;
    static final int READY_GAP = 12;
    /** '게임 시작' 버튼 */
    static final Rectangle START_BUTTON = new Rectangle(FIELD_LEFT, 350, FIELD_WIDTH, ROOM_ROW_HEIGHT);

    /**
     * 대기실. 위에서부터 연결 안내, 연결 상태, 대전 설정(모드·난이도), 준비 상태, '게임 시작' 버튼을 그린다.
     * 방장만 대전 설정을 고를 수 있다. (요구사항 「P2P 대전 모드 — 연결」의 대기 상태)
     */
    private void paintWaitingRoom(Graphics2D g2) {
        title(g2, "대기실", "");
        g2.setFont(RetroTheme.mono(Font.BOLD, 17));
        g2.setColor(SUCCESS);
        drawCentered(g2, P2PScreenTexts.CONNECTED, 112);

        ConnectionState connection = state.connectionState();
        g2.setFont(RetroTheme.mono(Font.BOLD, 12));
        g2.setColor(connectionColor(connection));
        drawCentered(g2, P2PScreenTexts.connection(connection, state.roundTripMillis()), 134);

        boolean server = state.role() == Role.SERVER;
        Rectangle frame = ROOM_SETTINGS_FRAME;
        RetroTheme.drawFrame(g2, frame.x, frame.y, frame.width, frame.height, RetroTheme.CYAN);
        settingRow(g2, "대전 모드", VersusModeLabels.of(state.mode()), ROOM_SETTINGS_TOP,
                P2PScreenState.ROOM_ROW_MODE, server);
        settingRow(g2, "난이도", DifficultyLabels.of(state.difficulty()), ROOM_SETTINGS_TOP + ROOM_ROW_HEIGHT + 8,
                P2PScreenState.ROOM_ROW_DIFFICULTY, server);
        if (!server) {
            g2.setFont(RetroTheme.mono(Font.PLAIN, 12));
            g2.setColor(MUTED);
            drawCentered(g2, P2PScreenTexts.CLIENT_SETTINGS_NOTE, frame.y + frame.height + 18);
        }

        paintReadyStatus(g2);
        paintStartButton(g2, server);

        g2.setFont(RetroTheme.mono(Font.PLAIN, 12));
        g2.setColor(MUTED);
        drawCentered(g2, P2PScreenTexts.START_GUIDE, getHeight() - 52);
        hint(g2, P2PScreenTexts.roomHint(state.role()));
    }

    /** 대전 설정 한 줄. 방장이 고를 수 있는 줄이 선택되어 있으면 노란 테두리와 ◀ ▶로 표시한다. */
    private void settingRow(Graphics2D g2, String label, String value, int top, int row, boolean editable) {
        boolean selected = editable && state.roomRow() == row;
        box(g2, FIELD_LEFT, top, FIELD_WIDTH, ROOM_ROW_HEIGHT, selected ? SELECTED : OUTLINE);
        g2.setFont(RetroTheme.mono(Font.BOLD, 13));
        g2.setColor(MUTED);
        g2.drawString(label, FIELD_LEFT + 16, top + 25);
        g2.setFont(RetroTheme.mono(Font.BOLD, 16));
        g2.setColor(selected ? SELECTED : RetroTheme.TEXT);
        String text = P2PScreenTexts.choice(value, selected);
        FontMetrics metrics = g2.getFontMetrics();
        int valueCenter = FIELD_LEFT + 110 + (FIELD_WIDTH - 110) / 2;
        g2.drawString(text, valueCenter - metrics.stringWidth(text) / 2, top + 25);
    }

    /** 나({@code mine})와 상대의 준비 상태 카드 위치. 왼쪽이 나, 오른쪽이 상대다 */
    static Rectangle readyCard(boolean mine) {
        int width = (FIELD_WIDTH - READY_GAP) / 2;
        return new Rectangle(mine ? FIELD_LEFT : FIELD_LEFT + width + READY_GAP, READY_TOP, width, READY_HEIGHT);
    }

    /** 나와 상대의 준비 상태를 카드 두 장에 그린다. 준비를 마친 쪽은 초록 틀과 초록 글자다. */
    private void paintReadyStatus(Graphics2D g2) {
        readyCard(g2, "나", state.isReady(), readyCard(true));
        readyCard(g2, "상대", state.isOpponentReady(), readyCard(false));
    }

    private void readyCard(Graphics2D g2, String who, boolean ready, Rectangle card) {
        RetroTheme.drawFrame(g2, card.x, card.y, card.width, card.height, ready ? SUCCESS : OUTLINE);
        g2.setFont(RetroTheme.mono(Font.BOLD, 12));
        g2.setColor(MUTED);
        g2.drawString(who, card.x + 12, card.y + 27);
        g2.setFont(RetroTheme.mono(Font.BOLD, 15));
        g2.setColor(ready ? SUCCESS : RetroTheme.TEXT);
        String status = P2PScreenTexts.readyStatus(ready);
        FontMetrics metrics = g2.getFontMetrics();
        g2.drawString(status, card.x + card.width - 14 - metrics.stringWidth(status), card.y + 28);
    }

    /**
     * '게임 시작' 버튼. 선택하면 시작 메뉴와 같은 노란 상자와 픽셀 화살표로 그린다. 참여자는 이 버튼만 고르므로
     * 항상 선택된 것으로 그린다. 이미 눌렀으면 "준비 취소"를 초록으로 그린다.
     */
    private void paintStartButton(Graphics2D g2, boolean server) {
        Rectangle button = START_BUTTON;
        boolean selected = !server || state.roomRow() == P2PScreenState.ROOM_ROW_START;
        if (selected) {
            RetroTheme.drawSelection(g2, button.x, button.y, button.width, button.height);
        } else {
            box(g2, button.x, button.y, button.width, button.height, OUTLINE);
        }
        g2.setFont(RetroTheme.mono(Font.BOLD, 18));
        g2.setColor(state.isReady() ? SUCCESS : selected ? SELECTED : RetroTheme.TEXT);
        drawCentered(g2, P2PScreenTexts.startButton(state.isReady()), button.y + 26);
    }

    /** 연결 상태의 색. 정상은 초록, 지연은 노랑, 그보다 나쁘면 빨강 */
    static Color connectionColor(ConnectionState connection) {
        return switch (connection) {
            case NORMAL -> SUCCESS;
            case LAGGING -> SELECTED;
            case CHECKING, DISCONNECTED -> ERROR;
        };
    }

    // ------------------------------------------------------------------
    // 공통
    // ------------------------------------------------------------------

    /** 분홍 제목과 하늘색 그림자, 그 아래 부제. 네 화면이 같은 위치에 그린다. */
    private void title(Graphics2D g2, String title, String subtitle) {
        g2.setFont(RetroTheme.mono(Font.BOLD, 32));
        RetroTheme.drawShadowText(g2, title, 0, getWidth(), 80, 3);
        if (!subtitle.isEmpty()) {
            g2.setFont(RetroTheme.mono(Font.PLAIN, 14));
            g2.setColor(RetroTheme.TEXT);
            drawCentered(g2, subtitle, 114);
        }
    }

    private void hint(Graphics2D g2, String text) {
        g2.setFont(RetroTheme.mono(Font.PLAIN, 12));
        g2.setColor(MUTED);
        drawCentered(g2, text, getHeight() - 24);
    }

    /** 각진 상자. 선택한 칸은 노란 테두리와 옅은 노란 바탕으로 그린다. */
    private static void box(Graphics2D g2, int x, int y, int width, int height, Color border) {
        boolean selected = border == SELECTED;
        g2.setColor(selected ? RetroTheme.withAlpha(SELECTED, 28) : SURFACE);
        g2.fillRect(x, y, width, height);
        g2.setStroke(new BasicStroke(selected ? 2f : 1.5f));
        g2.setColor(border);
        g2.drawRect(x, y, width, height);
    }

    private void drawCentered(Graphics2D g2, String text, int y) {
        FontMetrics metrics = g2.getFontMetrics();
        g2.drawString(text, (getWidth() - metrics.stringWidth(text)) / 2, y);
    }
}
