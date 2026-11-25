package seoultech.se.tetris.ui.p2p;

import java.util.List;

import seoultech.se.tetris.config.P2PSettingsService.RecentPeer;
import seoultech.se.tetris.engine.Difficulty;
import seoultech.se.tetris.versus.MatchResult;
import seoultech.se.tetris.versus.VersusMode;
import seoultech.se.tetris.versus.p2p.CloseReason;
import seoultech.se.tetris.versus.p2p.ConnectListener.FailReason;
import seoultech.se.tetris.versus.p2p.ConnectListener.Step;
import seoultech.se.tetris.versus.p2p.ConnectionState;
import seoultech.se.tetris.versus.p2p.Role;

/**
 * P2P 대전 모드 연결 화면({@link P2PPanel})이 그릴 때 읽는 상태.
 *
 * <p>연결과 화면 흐름을 맡는 쪽이 구현한다. 화면은 값을 읽기만 하며, 값이 바뀌면 구현 쪽이
 * {@link P2PPanel#repaint()}를 호출한다. 화면에 보일 문구는 화면이 {@link P2PScreenTexts}로 만든다.
 *
 * <p>각 값은 지금 화면({@link #screen()})에 해당할 때만 읽는다. 해당하지 않는 값은 아무 값이어도 된다.
 */
public interface P2PScreenState {

    /** 화면 종류 */
    enum Screen {
        /** 첫 화면: 방 생성 / 참여 / 뒤로 가기 */
        MENU,
        /** 방 생성: 접속 대기 */
        HOST,
        /** 참여: IP·포트 입력 */
        JOIN,
        /** 대기실: 연결 완료 */
        WAITING_ROOM
    }

    /** 참여 화면의 잘못된 입력 */
    enum InputError {
        /** IPv4 형식이 아님 */
        INVALID_IP,
        /** 포트가 숫자가 아니거나 범위를 벗어남 */
        INVALID_PORT
    }

    /** 첫 화면의 항목 수. 위에서부터 방 생성, 참여, 뒤로 가기 */
    int MENU_ITEM_COUNT = 3;

    /** 참여 화면의 줄 번호. 최근 접속 기록은 {@link #JOIN_ROW_CONNECT} 다음 줄부터 최신순으로 이어진다. */
    int JOIN_ROW_IP = 0;
    int JOIN_ROW_PORT = 1;
    int JOIN_ROW_CONNECT = 2;

    Screen screen();

    // ---------- 첫 화면 ----------

    /** 선택된 항목. 0부터 {@link #MENU_ITEM_COUNT} - 1 */
    int menuIndex();

    /** 연결이 끊겨 첫 화면으로 돌아온 이유. 없으면 {@code null} */
    CloseReason closeReason();

    // ---------- 방 생성 · 참여 공통 ----------

    /** 진행 중인 단계. 없으면 {@code null} */
    Step step();

    /** 연결하지 못한 이유. 없으면 {@code null} */
    FailReason failure();

    // ---------- 방 생성 ----------

    /** 클라이언트가 입력할 이 PC의 IPv4 주소들. 찾지 못했으면 빈 목록 */
    List<String> hostAddresses();

    /** 기다리는 포트 */
    int hostPort();

    // ---------- 참여 ----------

    String ipText();

    String portText();

    /** 선택된 줄. {@link #JOIN_ROW_IP}, {@link #JOIN_ROW_PORT}, {@link #JOIN_ROW_CONNECT}, 그 다음부터 최근 접속 기록 */
    int joinRow();

    /** 접속을 시도하는 중인지 */
    boolean isConnecting();

    /** 잘못된 입력. 없으면 {@code null} */
    InputError inputError();

    /** 최근 접속 기록. 최신순 */
    List<RecentPeer> recentPeers();

    // ---------- 대기실 ----------

    /** 대기실의 줄 번호. 방장은 모드·난이도·게임 시작 줄을 고르고, 참여자는 게임 시작 줄만 있다. */
    int ROOM_ROW_MODE = 0;
    int ROOM_ROW_DIFFICULTY = 1;
    int ROOM_ROW_START = 2;

    /** 대기실에서 선택된 줄. 참여자는 항상 {@link #ROOM_ROW_START}로 본다 */
    int roomRow();

    /** 내가 '게임 시작'을 눌렀는지 */
    boolean isReady();

    /** 상대가 '게임 시작'을 눌렀는지 */
    boolean isOpponentReady();

    /** 방장이 대전 설정을 바꿔 준비가 풀렸음을 알릴지 */
    boolean isReadyReset();

    /** 지난 대전의 결과. 대기실에 처음 들어왔으면 {@code null} */
    MatchResult lastResult();

    Role role();

    VersusMode mode();

    Difficulty difficulty();

    ConnectionState connectionState();

    /** 최근 왕복 시간의 평균(ms). 아직 잰 적이 없으면 -1 */
    int roundTripMillis();
}
