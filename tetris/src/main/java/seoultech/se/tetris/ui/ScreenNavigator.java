package seoultech.se.tetris.ui;

import seoultech.se.tetris.engine.GameMode;
import seoultech.se.tetris.versus.VersusMode;

public interface ScreenNavigator {
    /** 시작 메뉴 화면으로 이동 */
    void navigateToMenu();

    /** 선택한 모드로 게임 시작 (인게임 화면으로 전환). 게임 종료 후의 점수 기록도 이 모드로 남는다. */
    void navigateToGame(GameMode mode);

    /** 로컬 대전 모드 진입. 대전에서 사용할 모드(일반 / 아이템 / 시간제한) 선택 화면으로 이동한다. */
    void navigateToVersus();

    /** 선택한 대전 모드(일반 / 아이템 / 시간제한)로 로컬 대전을 시작한다. */
    void navigateToVersusGame(VersusMode mode);

    /** P2P 대전 모드 진입. P2P 대전 모드 첫 화면(방 생성 / 참여)으로 이동한다. */
    void navigateToP2P();

    /** 설정 화면으로 이동 */
    void navigateToSettings();

    /** 스코어보드 화면으로 이동 */
    void navigateToScoreboard();

    /** 게임을 더 진행할 수 없을 때 종료 화면으로 이동. 인자는 최종 점수다. */
    void navigateToGameOver(int finalScore);

    /** 프로그램 전체 종료 */
    void exitProgram();
}
