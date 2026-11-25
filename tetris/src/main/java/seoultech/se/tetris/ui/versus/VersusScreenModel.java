package seoultech.se.tetris.ui.versus;

import java.util.Objects;

import seoultech.se.tetris.versus.EndReason;
import seoultech.se.tetris.versus.MatchResult;
import seoultech.se.tetris.versus.Side;
import seoultech.se.tetris.versus.VersusMode;

/**
 * 로컬 대전 화면에 표시할 문구와 대전 결과. Swing/AWT에 의존하지 않는다.
 *
 * <p>대전 진행 중에는 모드 이름과 조작 안내를, 대전이 끝나면 각 보드의 승패와 결과 요약을 제공한다.
 * 대전이 끝나면 이 화면에서 결과를 잠시 보여 준 뒤 대전 종료 화면({@link VersusEndModel})으로 넘어간다.
 */
public class VersusScreenModel {

    static final String COUNTDOWN_HINT = "잠시 후 시작합니다 · Esc 나가기";
    static final String PLAYING_HINT = "Esc  일시정지";
    static final String ENDED_HINT = "Enter  결과 화면으로";

    private final VersusMode mode;
    private final CountdownClock clock;
    private final VersusPresentation presentation;
    private int countdownSeconds;
    private MatchResult result;

    /** 남은 시간을 표시하지 않는 화면 */
    public VersusScreenModel(VersusMode mode) {
        this(mode, null);
    }

    /**
     * @param clock 시간제한 모드의 남은 시간. 표시하지 않으면 {@code null}
     */
    public VersusScreenModel(VersusMode mode, CountdownClock clock) {
        this(mode, clock, VersusPresentation.LOCAL);
    }

    /**
     * @param clock        시간제한 모드의 남은 시간. 표시하지 않으면 {@code null}
     * @param presentation 제목·이름·보드 순서·안내의 표시 방식. 로컬 대전은 {@link VersusPresentation#LOCAL}
     */
    public VersusScreenModel(VersusMode mode, CountdownClock clock, VersusPresentation presentation) {
        this.mode = Objects.requireNonNull(mode, "mode");
        this.clock = clock;
        this.presentation = Objects.requireNonNull(presentation, "presentation");
    }

    public VersusPresentation presentation() {
        return presentation;
    }

    /** 이 화면에서 보드 위에 표시할 이름. 로컬 대전은 "P1", P2P 대전은 "나 (방장)" 등 */
    public String labelOf(Side side) {
        return presentation.labelOf(side);
    }

    public VersusMode mode() {
        return mode;
    }

    /** 화면 맨 위 제목. 예: "로컬 대전 · 아이템 모드", "P2P 대전 · 일반 모드" */
    public String title() {
        return presentation.titlePrefix() + " · " + VersusModeLabels.of(mode);
    }

    /** 각 보드 위에 표시할 플레이어 이름 */
    public static String playerLabel(Side side) {
        return Objects.requireNonNull(side, "side").name();
    }

    /** 시작 전 카운트다운의 남은 초를 설정한다. 0이면 카운트다운 표시를 끈다. */
    public void setCountdownSeconds(int seconds) {
        this.countdownSeconds = Math.max(0, seconds);
    }

    public int countdownSeconds() {
        return countdownSeconds;
    }

    public boolean isCountingDown() {
        return countdownSeconds > 0 && !isEnded();
    }

    /** 대전이 끝났을 때 결과를 기록한다. */
    public void matchEnded(MatchResult matchResult) {
        result = Objects.requireNonNull(matchResult, "matchResult");
    }

    public boolean isEnded() {
        return result != null;
    }

    /** 보드 위에 덮어 표시할 문구. 종료 시 승패, 카운트다운 중이면 남은 초를 반환하고 진행 중이면 {@code null}이다. */
    public String overlayOf(Side side) {
        Objects.requireNonNull(side, "side");
        if (result != null) {
            if (result.isDraw()) {
                return "무승부";
            }
            return result.isWinner(side) ? "승리" : "패배";
        }
        return countdownSeconds > 0 ? Integer.toString(countdownSeconds) : null;
    }

    /**
     * 결과 요약. 대전 중이면 빈 문자열이다.
     * 예: "P1 승리 (P2가 더 이상 블럭을 쌓을 수 없음)", "P2 승리 (제한 시간 종료 · 500 : 900)".
     * P2P 대전은 내 관점으로 보여준다. 예: "승리 (상대 기권)"
     */
    public String resultText() {
        if (result == null) {
            return "";
        }
        if (presentation.mySide() != null) {
            return perspectiveText(result, presentation.mySide());
        }
        String outcome = result.isDraw() ? "무승부" : playerLabel(result.winner()) + " 승리";
        return outcome + " (" + reasonText(result) + ")";
    }

    /**
     * 내 관점의 결과. 예: "승리 (상대가 더 이상 블럭을 쌓을 수 없음)", "패배 (기권)",
     * "무승부 (제한 시간 종료 · 나 700 : 상대 700)"
     *
     * @param mySide 이 PC의 플레이어 자리
     */
    public static String perspectiveText(MatchResult result, Side mySide) {
        Objects.requireNonNull(result, "result");
        Objects.requireNonNull(mySide, "mySide");
        String outcome = result.isDraw() ? "무승부" : result.isWinner(mySide) ? "승리" : "패배";
        return outcome + " (" + perspectiveReason(result, mySide) + ")";
    }

    private static String perspectiveReason(MatchResult result, Side mySide) {
        boolean iLost = !result.isDraw() && !result.isWinner(mySide);
        return switch (result.reason()) {
            // P2P에서는 두 플레이어가 상대의 종료 신호를 받기 전에 끝나면(엇갈림) 무승부가 된다 (3차 팀 결정)
            case TOP_OUT -> result.isDraw() ? "두 플레이어 모두 더 이상 블럭을 쌓을 수 없음"
                    : iLost ? "더 이상 블럭을 쌓을 수 없음" : "상대가 더 이상 블럭을 쌓을 수 없음";
            case TIME_UP -> "제한 시간 종료 · 나 " + result.score(mySide)
                    + " : 상대 " + result.score(mySide.opponent());
            case FORFEIT -> iLost ? "기권" : "상대 기권";
        };
    }

    /** 남은 시간을 표시하는지. 시간제한 모드에서만 표시한다. */
    public boolean hasTimer() {
        return clock != null;
    }

    /** 표시할 남은 시간. 예: "02:59". 표시하지 않는 화면이면 빈 문자열이다. */
    public String timerText() {
        return clock == null ? "" : clock.text();
    }

    /** 남은 시간을 강조할지. 남은 시간이 얼마 없을 때 강조한다. */
    public boolean isTimerUrgent() {
        return clock != null && clock.isUrgent();
    }

    /** 현재 사용할 수 있는 키 안내 */
    public String hintText() {
        if (isEnded()) {
            return presentation.endedHint();
        }
        return isCountingDown() ? presentation.countdownHint() : presentation.playingHint();
    }

    /** 시간 종료는 점수로 승패를 정하므로 두 점수를 함께 보여준다. */
    private static String reasonText(MatchResult result) {
        String reason = VersusEndModel.reasonOf(result);
        if (result.reason() == EndReason.TIME_UP) {
            return reason + " · " + result.p1Score() + " : " + result.p2Score();
        }
        return reason;
    }
}
