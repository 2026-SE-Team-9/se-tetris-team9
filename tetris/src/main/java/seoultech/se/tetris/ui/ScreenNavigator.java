package seoultech.se.tetris.ui;

import seoultech.se.tetris.engine.GameMode;

public interface ScreenNavigator {
    /** 시작 메뉴 화면으로 이동 */
    void navigateToMenu();

    /** 선택한 모드로 게임 시작 (인게임 화면으로 전환). 게임 종료 후의 점수 기록도 이 모드로 남는다. */
    void navigateToGame(GameMode mode);

    /** 설정 화면으로 이동 */
    void navigateToSettings();

    /** 스코어보드 화면으로 이동 */
    void navigateToScoreboard();

    /** 게임을 더 진행할 수 없을 때 종료 화면으로 이동. 인자는 최종 점수다. */
    void navigateToGameOver(int finalScore);

    /** 프로그램 전체 종료 */
    void exitProgram();
}
