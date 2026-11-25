package seoultech.se.tetris.ui.p2p;

import java.util.List;

import seoultech.se.tetris.config.P2PSettingsService;
import seoultech.se.tetris.engine.Difficulty;
import seoultech.se.tetris.ui.DifficultyLabels;
import seoultech.se.tetris.ui.p2p.P2PScreenState.InputError;
import seoultech.se.tetris.ui.versus.VersusModeLabels;
import seoultech.se.tetris.ui.versus.VersusScreenModel;
import seoultech.se.tetris.versus.MatchResult;
import seoultech.se.tetris.versus.VersusMode;
import seoultech.se.tetris.versus.p2p.CloseReason;
import seoultech.se.tetris.versus.p2p.ConnectListener.FailReason;
import seoultech.se.tetris.versus.p2p.ConnectListener.Step;
import seoultech.se.tetris.versus.p2p.ConnectionState;
import seoultech.se.tetris.versus.p2p.Role;

/**
 * P2P 대전 모드 연결 화면에 보일 문구. 상태 값을 화면 문구로 바꾼다. Swing/AWT에 의존하지 않는다.
 *
 * <p>접속 과정의 각 단계마다 그 단계를 나타내는 메시지를 표시한다. (3차 팀 결정)
 */
public final class P2PScreenTexts {

    static final List<String> MENU_ITEMS = List.of("방 생성", "참여", "뒤로 가기");
    static final List<String> MENU_DESCRIPTIONS = List.of(
            "내 PC에서 상대가 참여하기를 기다립니다", "상대의 IP와 포트로 접속합니다", "");
    static final String NO_ADDRESS = "이 PC의 IPv4 주소를 찾지 못했습니다";
    static final String NO_RECENT = "저장된 접속 기록이 없습니다";

    static final String CONNECTED = "상대와 연결되었습니다";
    static final String READY_RESET = "방장이 대전 설정을 바꿔 준비가 풀렸습니다. 다시 눌러 주세요";
    static final String CLIENT_SETTINGS_NOTE = "대전 설정은 방장이 고릅니다";
    static final String START_GUIDE = "두 플레이어가 모두 '게임 시작'을 누르면 대전이 시작됩니다";
    static final String SERVER_ROOM_HINT = "↑ ↓ 이동   ← → 변경   Enter 선택   Esc 나가기";
    static final String CLIENT_ROOM_HINT = "Enter 게임 시작·취소   Esc 나가기";

    static final String EXIT_TITLE = "게임 나가기";
    static final List<String> EXIT_ITEMS = List.of("계속하기", "게임 나가기 (기권패)");
    static final String EXIT_NOTE = "메뉴를 여는 동안에도 게임은 계속 진행됩니다";
    static final String EXIT_HINT = "↑ ↓ 이동   Enter 선택   Esc 계속하기";

    private P2PScreenTexts() {
    }

    /** 진행 단계 */
    public static String step(Step step) {
        return switch (step) {
            case OPENING -> "포트를 여는 중...";
            case WAITING -> "상대가 참여하기를 기다리는 중...";
            case CONNECTING -> "서버에 접속하는 중...";
            case CHECKING_VERSION -> "프로그램 버전을 확인하는 중...";
        };
    }

    /** 연결하지 못한 이유 */
    public static String failure(FailReason reason, int port) {
        return switch (reason) {
            case PORT_UNAVAILABLE -> "포트 " + port + "을(를) 열 수 없습니다. 설정에서 다른 포트를 지정하세요";
            case INVALID_ADDRESS -> "주소 형식이 올바르지 않습니다. 다시 입력하세요";
            case UNREACHABLE -> "서버에 접속할 수 없습니다. IP와 포트를 확인하고 다시 입력하세요";
            case VERSION_MISMATCH -> "상대 프로그램의 버전이 달라 연결할 수 없습니다";
            case ERROR -> "통신 오류로 연결하지 못했습니다. 다시 시도하세요";
        };
    }

    /** 참여 화면의 잘못된 입력 */
    public static String inputError(InputError error) {
        return switch (error) {
            case INVALID_IP -> "IPv4 주소 형식으로 입력하세요 (예: 192.168.0.12)";
            case INVALID_PORT -> "포트는 " + P2PSettingsService.MIN_PORT + "~" + P2PSettingsService.MAX_PORT
                    + " 사이의 숫자로 입력하세요";
        };
    }

    /** 연결이 끊긴 이유. 직접 나간 경우에는 안내하지 않으므로 {@code null} */
    public static String close(CloseReason reason) {
        return switch (reason) {
            case LOCAL -> null;
            case PEER_LEFT -> "상대가 대기실을 나갔습니다";
            case TIMED_OUT -> "상대의 응답이 없어 연결이 끊어졌습니다";
            case FAILED -> "오류로 연결이 끊어졌습니다";
        };
    }

    /** 끊긴 이유를 오류(붉은색)로 보여줄지. 상대가 직접 나간 것은 오류가 아니다. */
    public static boolean isCloseError(CloseReason reason) {
        return reason == CloseReason.TIMED_OUT || reason == CloseReason.FAILED;
    }

    /** 방 생성 화면의 주소 줄. 예: "192.168.0.12 : 24700". 주소를 찾지 못했으면 안내 한 줄 */
    public static List<String> hostAddressLines(List<String> addresses, int port) {
        if (addresses.isEmpty()) {
            return List.of(NO_ADDRESS);
        }
        return addresses.stream().map(address -> address + " : " + port).toList();
    }

    /** 대기실의 역할 */
    public static String role(Role role) {
        return role == Role.SERVER ? "방장 (서버)" : "참여자 (클라이언트)";
    }

    /** 대기실의 대전 설정. 예: "일반 모드 · 보통" */
    public static String settings(VersusMode mode, Difficulty difficulty) {
        return VersusModeLabels.of(mode) + " · " + DifficultyLabels.of(difficulty);
    }

    /** 준비 상태. 예: "준비 완료", "대기 중" */
    public static String readyStatus(boolean ready) {
        return ready ? "준비 완료" : "대기 중";
    }

    /** '게임 시작' 버튼의 글자. 이미 눌렀으면 다시 눌러 취소한다 */
    public static String startButton(boolean ready) {
        return ready ? "준비 취소" : "게임 시작";
    }

    /** 대기실의 설정 값. 방장이 바꿀 수 있으면 양옆에 ◀ ▶를 붙인다 */
    public static String choice(String value, boolean editable) {
        return editable ? "◀  " + value + "  ▶" : value;
    }

    /** 대기실 아래 키 안내 */
    public static String roomHint(Role role) {
        return role == Role.SERVER ? SERVER_ROOM_HINT : CLIENT_ROOM_HINT;
    }

    /** 지난 대전 결과. 예: "지난 대전: 승리 (상대 기권)" */
    public static String lastResult(MatchResult result, Role myRole) {
        return "지난 대전: " + VersusScreenModel.perspectiveText(result, myRole.side());
    }

    /** 연결 상태와 왕복 시간. 예: "연결 상태: 정상 · 12ms" */
    public static String connection(ConnectionState state, int roundTripMillis) {
        String name = switch (state) {
            case NORMAL -> "정상";
            case LAGGING -> "지연";
            case CHECKING -> "확인 중";
            case DISCONNECTED -> "끊김";
        };
        return "연결 상태: " + name + (roundTripMillis < 0 ? "" : " · " + roundTripMillis + "ms");
    }
}
