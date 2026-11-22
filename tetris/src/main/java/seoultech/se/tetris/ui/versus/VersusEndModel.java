package seoultech.se.tetris.ui.versus;

import java.util.List;
import java.util.Objects;

import seoultech.se.tetris.ui.menu.MenuItem;
import seoultech.se.tetris.ui.menu.StartMenuModel;
import seoultech.se.tetris.versus.MatchResult;
import seoultech.se.tetris.versus.Side;
import seoultech.se.tetris.versus.VersusMode;

/**
 * 로컬 대전 종료 화면의 표시 내용과 메뉴 선택 상태. Swing/AWT에 의존하지 않는다.
 *
 * <p>승패 여부, 두 플레이어의 최종 점수, 종료 사유와 메뉴(게임 다시하기 / 시작 메뉴로 돌아가기)를 제공한다. (3차 팀 결정)
 * 대전 모드는 스코어보드로 기록을 관리하지 않으므로 이름 입력과 기록 등록은 없다.
 * 위·아래 이동과 Enter 선택은 시작 메뉴와 같은 리스트 메뉴를 재사용한다.
 */
public final class VersusEndModel {

    static final String TITLE = "대전 종료";
    static final String RESTART_LABEL = "게임 다시하기";
    static final String MENU_LABEL = "시작 메뉴로 돌아가기";

    private final VersusMode mode;
    private final MatchResult result;
    private final StartMenuModel menu;

    /**
     * @param onRestart 같은 모드로 새 대전을 시작하는 동작
     * @param onMenu    시작 메뉴로 돌아가는 동작
     */
    public VersusEndModel(VersusMode mode, MatchResult result, Runnable onRestart, Runnable onMenu) {
        this.mode = Objects.requireNonNull(mode, "mode");
        this.result = Objects.requireNonNull(result, "result");
        this.menu = new StartMenuModel(List.of(
                new MenuItem(RESTART_LABEL, Objects.requireNonNull(onRestart, "onRestart")),
                new MenuItem(MENU_LABEL, Objects.requireNonNull(onMenu, "onMenu"))));
    }

    public VersusMode mode() {
        return mode;
    }

    public MatchResult result() {
        return result;
    }

    /** 화면 맨 위 제목 */
    public String title() {
        return TITLE;
    }

    /** 대전한 모드. 예: "로컬 대전 · 시간제한 모드" */
    public String modeText() {
        return "로컬 대전 · " + VersusModeLabels.of(mode);
    }

    /** 승패 요약. 예: "P1 승리", "무승부" */
    public String outcomeText() {
        return result.isDraw() ? "무승부" : VersusScreenModel.playerLabel(result.winner()) + " 승리";
    }

    /** 종료 사유. 예: "P2가 더 이상 블럭을 쌓을 수 없음", "제한 시간 종료" */
    public String reasonText() {
        return reasonOf(result);
    }

    /** 대전 결과의 종료 사유 문구. 대전 화면과 종료 화면이 같은 문구를 쓴다. */
    static String reasonOf(MatchResult result) {
        return switch (result.reason()) {
            case TOP_OUT -> VersusScreenModel.playerLabel(result.winner().opponent()) + "가 더 이상 블럭을 쌓을 수 없음";
            case TIME_UP -> "제한 시간 종료";
            case FORFEIT -> VersusScreenModel.playerLabel(result.winner().opponent()) + " 기권";
        };
    }

    /** 승리한 플레이어. 무승부이면 {@code null} */
    public Side winner() {
        return result.winner();
    }

    /** 각 플레이어의 승패. "승리" / "패배" / "무승부" */
    public String outcomeOf(Side side) {
        Objects.requireNonNull(side, "side");
        if (result.isDraw()) {
            return "무승부";
        }
        return result.isWinner(side) ? "승리" : "패배";
    }

    /** 각 플레이어의 최종 점수 */
    public int scoreOf(Side side) {
        return result.score(Objects.requireNonNull(side, "side"));
    }

    // ------------------------------------------------------------------
    // 메뉴
    // ------------------------------------------------------------------

    public List<MenuItem> getItems() {
        return menu.getItems();
    }

    public int getSelectedIndex() {
        return menu.getSelectedIndex();
    }

    public boolean isKeyHintVisible() {
        return menu.isKeyHintVisible();
    }

    public void moveUp() {
        menu.moveUp();
    }

    public void moveDown() {
        menu.moveDown();
    }

    /** 선택한 항목의 동작을 실행한다. */
    public void select() {
        menu.select();
    }

    /** 사용하지 않는 키가 입력되었을 때 호출한다. 사용 가능한 키 안내를 표시한다. */
    public void rejectInput() {
        menu.rejectInput();
    }
}
