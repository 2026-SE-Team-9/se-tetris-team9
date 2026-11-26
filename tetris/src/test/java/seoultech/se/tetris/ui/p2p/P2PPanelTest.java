package seoultech.se.tetris.ui.p2p;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

import org.junit.jupiter.api.Test;

import seoultech.se.tetris.config.P2PSettingsService.RecentPeer;
import seoultech.se.tetris.engine.Difficulty;
import seoultech.se.tetris.ui.p2p.P2PScreenState.InputError;
import seoultech.se.tetris.ui.theme.RetroTheme;
import seoultech.se.tetris.ui.p2p.P2PScreenState.Screen;
import seoultech.se.tetris.versus.VersusMode;
import seoultech.se.tetris.versus.p2p.CloseReason;
import seoultech.se.tetris.versus.p2p.ConnectListener.FailReason;
import seoultech.se.tetris.versus.p2p.ConnectListener.Step;
import seoultech.se.tetris.versus.p2p.ConnectionState;
import seoultech.se.tetris.versus.p2p.Role;

class P2PPanelTest {

    /** 테스트가 값을 직접 정하는 상태 */
    private static final class FakeState implements P2PScreenState {
        Screen screen = Screen.MENU;
        int menuIndex;
        CloseReason closeReason;
        Step step;
        FailReason failure;
        List<String> hostAddresses = List.of("192.168.0.12");
        int hostPort = 24700;
        String ipText = "";
        String portText = "24700";
        int joinRow;
        boolean connecting;
        InputError inputError;
        List<RecentPeer> recentPeers = List.of();
        Role role = Role.SERVER;
        VersusMode mode = VersusMode.NORMAL;
        Difficulty difficulty = Difficulty.NORMAL;
        ConnectionState connectionState = ConnectionState.NORMAL;
        int roundTripMillis = -1;
        int roomRow = P2PScreenState.ROOM_ROW_MODE;
        boolean ready;
        boolean opponentReady;

        @Override public Screen screen() { return screen; }
        @Override public int menuIndex() { return menuIndex; }
        @Override public CloseReason closeReason() { return closeReason; }
        @Override public Step step() { return step; }
        @Override public FailReason failure() { return failure; }
        @Override public List<String> hostAddresses() { return hostAddresses; }
        @Override public int hostPort() { return hostPort; }
        @Override public String ipText() { return ipText; }
        @Override public String portText() { return portText; }
        @Override public int joinRow() { return joinRow; }
        @Override public boolean isConnecting() { return connecting; }
        @Override public InputError inputError() { return inputError; }
        @Override public List<RecentPeer> recentPeers() { return recentPeers; }
        @Override public Role role() { return role; }
        @Override public VersusMode mode() { return mode; }
        @Override public Difficulty difficulty() { return difficulty; }
        @Override public ConnectionState connectionState() { return connectionState; }
        @Override public int roundTripMillis() { return roundTripMillis; }
        @Override public int roomRow() { return roomRow; }
        @Override public boolean isReady() { return ready; }
        @Override public boolean isOpponentReady() { return opponentReady; }
    }

    /** 불린 동작을 기록한다. */
    private static final class RecordingActions implements P2PScreenActions {
        final List<String> calls = new ArrayList<>();

        @Override public void up() { calls.add("up"); }
        @Override public void down() { calls.add("down"); }
        @Override public void left() { calls.add("left"); }
        @Override public void right() { calls.add("right"); }
        @Override public void select() { calls.add("select"); }
        @Override public void back() { calls.add("back"); }
        @Override public void type(char c) { calls.add("type:" + c); }
        @Override public void erase() { calls.add("erase"); }
    }

    private final FakeState state = new FakeState();
    private final RecordingActions actions = new RecordingActions();
    private final P2PPanel panel = new P2PPanel(state, actions);

    private BufferedImage render() {
        Dimension size = panel.getPreferredSize();
        panel.setSize(size);
        BufferedImage image = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_RGB);
        panel.paint(image.getGraphics());
        return image;
    }

    /** 화면 아래쪽 안내 줄(y 430~470)에 주어진 색의 픽셀이 있는지 */
    private static boolean noticeAreaHas(BufferedImage image, Color color) {
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 430; y < 470; y++) {
                if (image.getRGB(x, y) == color.getRGB()) {
                    return true;
                }
            }
        }
        return false;
    }

    private static List<RecentPeer> peers(int count) {
        List<RecentPeer> peers = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            peers.add(new RecentPeer("192.168.0." + (i + 1), 24700 + i));
        }
        return peers;
    }

    @Test
    void 시작_메뉴와_같은_크기다() {
        assertEquals(new Dimension(480, 540), panel.getPreferredSize());
    }

    @Test
    void 화면_이동_키를_동작으로_알린다() {
        for (int key : new int[] {KeyEvent.VK_UP, KeyEvent.VK_DOWN, KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT,
                KeyEvent.VK_ENTER, KeyEvent.VK_ESCAPE, KeyEvent.VK_BACK_SPACE, KeyEvent.VK_F1}) {
            panel.handleKey(key);
        }

        assertEquals(List.of("up", "down", "left", "right", "select", "back", "erase"), actions.calls);
    }

    @Test
    void 입력한_글자를_알리고_제어_문자는_넘기지_않는다() {
        panel.handleTyped('1');
        panel.handleTyped('.');
        panel.handleTyped('\n');
        panel.handleTyped('\b');
        panel.handleTyped(KeyEvent.CHAR_UNDEFINED);

        assertEquals(List.of("type:1", "type:."), actions.calls);
    }

    @Test
    void 실제_키_이벤트도_같은_동작으로_알린다() {
        JPanel source = new JPanel();
        for (var listener : panel.getKeyListeners()) {
            listener.keyPressed(new KeyEvent(source, KeyEvent.KEY_PRESSED, 0, 0, KeyEvent.VK_ENTER,
                    KeyEvent.CHAR_UNDEFINED));
            listener.keyTyped(new KeyEvent(source, KeyEvent.KEY_TYPED, 0, 0, KeyEvent.VK_UNDEFINED, '7'));
        }

        assertEquals(List.of("select", "type:7"), actions.calls);
    }

    @Test
    void 상대가_나가서_돌아오면_첫_화면에_안내를_보여준다() {
        assertTrue(!noticeAreaHas(render(), P2PPanel.ERROR));

        state.closeReason = CloseReason.FAILED;
        assertTrue(noticeAreaHas(render(), P2PPanel.ERROR));

        // 상대가 직접 나간 것은 오류 색으로 보여주지 않는다
        state.closeReason = CloseReason.PEER_LEFT;
        assertTrue(!noticeAreaHas(render(), P2PPanel.ERROR));
    }

    @Test
    void 연결_상태의_색은_정상_지연_그_외로_나뉜다() {
        assertEquals(P2PPanel.SUCCESS, P2PPanel.connectionColor(ConnectionState.NORMAL));
        assertEquals(P2PPanel.SELECTED, P2PPanel.connectionColor(ConnectionState.LAGGING));
        assertEquals(P2PPanel.ERROR, P2PPanel.connectionColor(ConnectionState.CHECKING));
        assertEquals(P2PPanel.ERROR, P2PPanel.connectionColor(ConnectionState.DISCONNECTED));
    }

    @Test
    void 모든_화면과_상태를_그릴_수_있다() {
        assertDoesNotThrow(() -> {
            for (int i = 0; i < P2PScreenState.MENU_ITEM_COUNT; i++) {
                state.menuIndex = i;
                render();
            }
            state.closeReason = CloseReason.LOCAL;
            render();

            state.screen = Screen.HOST;
            state.step = Step.WAITING;
            render();
            state.failure = FailReason.PORT_UNAVAILABLE;
            render();
            state.hostAddresses = List.of();
            render();

            state.screen = Screen.JOIN;
            state.failure = null;
            state.step = null;
            state.inputError = InputError.INVALID_IP;
            render();
            state.inputError = null;
            state.recentPeers = peers(10);
            for (int row = 0; row < 13; row++) {
                state.joinRow = row;
                render();
            }
            state.connecting = true;
            state.step = Step.CONNECTING;
            render();
            state.connecting = false;
            state.failure = FailReason.UNREACHABLE;
            render();

            state.screen = Screen.WAITING_ROOM;
            for (Role role : Role.values()) {
                state.role = role;
                for (ConnectionState connection : ConnectionState.values()) {
                    state.connectionState = connection;
                    state.roundTripMillis = 12;
                    render();
                }
            }
        });
    }

    /** 대기실 대전 모드 줄(y 160~198)의 가운데 오른쪽에 노란 픽셀이 있는지. 선택된 줄은 노란 테두리와 글자로 그린다 */
    private static boolean modeRowHighlighted(BufferedImage image) {
        for (int x = 170; x < 420; x++) {
            for (int y = 160; y < 199; y++) {
                if (image.getRGB(x, y) == P2PPanel.SELECTED.getRGB()) {
                    return true;
                }
            }
        }
        return false;
    }

    @Test
    void 방장은_대기실에서_선택한_설정_줄을_강조한다() {
        state.screen = Screen.WAITING_ROOM;
        state.role = Role.SERVER;

        state.roomRow = P2PScreenState.ROOM_ROW_MODE;
        assertTrue(modeRowHighlighted(render()));

        state.roomRow = P2PScreenState.ROOM_ROW_START;
        assertTrue(!modeRowHighlighted(render()));
    }

    @Test
    void 참여자는_대전_설정을_고를_수_없어_강조하지_않는다() {
        state.screen = Screen.WAITING_ROOM;
        state.role = Role.CLIENT;
        state.roomRow = P2PScreenState.ROOM_ROW_MODE;

        assertTrue(!modeRowHighlighted(render()));
    }

    @Test
    void 대기실의_준비_상태를_역할마다_그릴_수_있다() {
        state.screen = Screen.WAITING_ROOM;
        assertDoesNotThrow(() -> {
            for (Role role : Role.values()) {
                state.role = role;
                for (int row = 0; row <= P2PScreenState.ROOM_ROW_START; row++) {
                    state.roomRow = row;
                    state.ready = row % 2 == 0;
                    state.opponentReady = row == 1;
                    render();
                }
            }
        });
    }

    @Test
    void 첫_화면은_레트로_배경과_하늘색_틀에_선택_항목을_노란_상자로_그린다() {
        state.menuIndex = 1;
        BufferedImage image = render();

        Rectangle frame = P2PPanel.MENU_FRAME;
        Rectangle first = P2PPanel.menuSelectionBounds(0);
        Rectangle second = P2PPanel.menuSelectionBounds(1);
        assertEquals(RetroTheme.CYAN.getRGB(),
                image.getRGB(frame.x + frame.width / 2, frame.y));
        assertEquals(P2PPanel.SELECTED.getRGB(), image.getRGB(second.x + second.width / 2, second.y));
        assertTrue(image.getRGB(first.x + first.width / 2, first.y) != P2PPanel.SELECTED.getRGB());
        Color corner = new Color(image.getRGB(5, 5));
        assertTrue(corner.getBlue() > corner.getRed());
    }

    @Test
    void 방_생성은_하늘색_틀_안에_주소를_그리고_실패는_분홍으로_알린다() {
        state.screen = Screen.HOST;
        state.hostAddresses = List.of("192.168.0.12", "10.0.0.4");
        Rectangle frame = panel.hostFrame();
        BufferedImage image = render();

        assertEquals(RetroTheme.CYAN.getRGB(), image.getRGB(frame.x + frame.width / 2, frame.y));
        // 주소가 두 개면 한 개일 때보다 틀이 길다
        state.hostAddresses = List.of("192.168.0.12");
        assertTrue(panel.hostFrame().height < frame.height);

        state.failure = FailReason.PORT_UNAVAILABLE;
        image = render();
        boolean pink = false;
        for (int x = 0; x < image.getWidth() && !pink; x++) {
            for (int y = frame.y + frame.height; y < image.getHeight() - 40; y++) {
                if (image.getRGB(x, y) == P2PPanel.ERROR.getRGB()) {
                    pink = true;
                    break;
                }
            }
        }
        assertTrue(pink);
        // 안내는 화면 안에 들어온다
        assertTrue(image.getRGB(2, frame.y + frame.height + 46 - 4) != P2PPanel.ERROR.getRGB());
    }

    @Test
    void 참여는_선택한_접속_버튼과_최근_기록을_노란색으로_그린다() {
        state.screen = Screen.JOIN;
        state.recentPeers = peers(3);
        state.joinRow = P2PScreenState.JOIN_ROW_CONNECT;
        BufferedImage image = render();

        Rectangle button = P2PPanel.CONNECT_BUTTON;
        Rectangle frame = P2PPanel.RECENT_FRAME;
        assertEquals(P2PPanel.SELECTED.getRGB(), image.getRGB(button.x + button.width / 2, button.y));
        assertEquals(RetroTheme.CYAN.getRGB(), image.getRGB(frame.x + frame.width / 2, frame.y));
        Rectangle firstPeer = P2PPanel.recentCell(0);
        assertTrue(new Color(image.getRGB(firstPeer.x + 1, firstPeer.y + 1)).getRed() < 100);

        // 첫 기록을 고르면 그 칸에 노란 줄이 깔리고 접속 버튼은 강조가 풀린다
        state.joinRow = P2PScreenState.JOIN_ROW_CONNECT + 1;
        image = render();
        assertTrue(new Color(image.getRGB(firstPeer.x + 1, firstPeer.y + 1)).getRed() > 40);
        assertTrue(image.getRGB(button.x + button.width / 2, button.y) != P2PPanel.SELECTED.getRGB());
        // 기록은 틀 안에 들어온다
        assertTrue(P2PPanel.recentCell(9).y + P2PPanel.recentCell(9).height <= frame.y + frame.height);
    }

    @Test
    void 상태와_동작이_있어야_만들_수_있다() {
        assertThrows(NullPointerException.class, () -> new P2PPanel(null, actions));
        assertThrows(NullPointerException.class, () -> new P2PPanel(state, null));
    }
}
