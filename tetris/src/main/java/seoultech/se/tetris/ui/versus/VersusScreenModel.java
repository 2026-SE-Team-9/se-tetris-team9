package seoultech.se.tetris.ui.versus;

import java.util.Objects;

import seoultech.se.tetris.versus.MatchResult;
import seoultech.se.tetris.versus.Side;
import seoultech.se.tetris.versus.VersusMode;

/**
 * 로컬 대전 화면에 표시할 문구와 대전 결과. Swing/AWT에 의존하지 않는다.
 *
 * <p>대전 진행 중에는 모드 이름과 조작 안내를, 대전이 끝나면 각 보드의 승패와 결과 요약을 제공한다.
 * 대전 종료 화면은 별도 이슈에서 만들며, 그 전까지는 이 문구로 결과를 임시 표시한다.
 */
public class VersusScreenModel {

    static final String PLAYING_HINT = "Esc  시작 메뉴로";
    static final String ENDED_HINT = "Enter / Esc  시작 메뉴로";

    private final VersusMode mode;
    private MatchResult result;

    public VersusScreenModel(VersusMode mode) {
        this.mode = Objects.requireNonNull(mode, "mode");
    }

    public VersusMode mode() {
        return mode;
    }

    /** 화면 맨 위 제목. 예: "로컬 대전 · 아이템 모드" */
    public String title() {
        return "로컬 대전 · " + VersusModeLabels.of(mode);
    }

    /** 각 보드 위에 표시할 플레이어 이름 */
    public static String playerLabel(Side side) {
        return Objects.requireNonNull(side, "side").name();
    }

    /** 대전이 끝났을 때 결과를 기록한다. */
    public void matchEnded(MatchResult matchResult) {
        result = Objects.requireNonNull(matchResult, "matchResult");
    }

    public boolean isEnded() {
        return result != null;
    }

    /** 보드 위에 덮어 표시할 승패. 대전 중이면 {@code null}이라 보드의 기본 표시를 쓴다. */
    public String overlayOf(Side side) {
        Objects.requireNonNull(side, "side");
        if (result == null) {
            return null;
        }
        if (result.isDraw()) {
            return "무승부";
        }
        return result.isWinner(side) ? "승리" : "패배";
    }

    /** 결과 요약. 대전 중이면 빈 문자열이다. 예: "P1 승리 (P2가 더 이상 블럭을 쌓을 수 없음)" */
    public String resultText() {
        if (result == null) {
            return "";
        }
        String outcome = result.isDraw() ? "무승부" : playerLabel(result.winner()) + " 승리";
        return outcome + " (" + reasonText(result) + ")";
    }

    /** 현재 사용할 수 있는 키 안내 */
    public String hintText() {
        return isEnded() ? ENDED_HINT : PLAYING_HINT;
    }

    private static String reasonText(MatchResult result) {
        return switch (result.reason()) {
            case TOP_OUT -> playerLabel(result.winner().opponent()) + "가 더 이상 블럭을 쌓을 수 없음";
            case TIME_UP -> "제한 시간 종료";
            case FORFEIT -> playerLabel(result.winner().opponent()) + " 기권";
        };
    }
}
