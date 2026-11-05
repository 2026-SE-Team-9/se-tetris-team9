package seoultech.se.tetris.versus;

import java.io.Serializable;
import java.util.Objects;

/**
 * 대전 한 판의 결과. 대전 모드의 종료 화면에 승패 여부, 최종 점수, 종료 사유를 보여줄 때 쓴다.
 *
 * <p>{@code winner}가 {@code null}이면 무승부다. 무승부는 다음 두 경우에만 생긴다. (3차 팀 결정)
 * <ul>
 *   <li>시간제한 모드에서 제한 시간이 끝났을 때 점수가 같은 경우 ({@link EndReason#TIME_UP})</li>
 *   <li>P2P 대전 모드에서 두 플레이어 모두 상대의 종료 신호를 받기 전에 종료된 경우 ({@link EndReason#TOP_OUT})</li>
 * </ul>
 * 상대가 나간 경우({@link EndReason#FORFEIT})는 항상 남은 플레이어가 이긴다.
 *
 * <p>직접 생성자를 부르기보다 {@link #win}과 {@link #draw}를 쓴다.
 *
 * @param winner  이긴 쪽. 무승부면 {@code null}
 * @param reason  끝난 사유
 * @param p1Score P1의 최종 점수
 * @param p2Score P2의 최종 점수
 */
public record MatchResult(Side winner, EndReason reason, int p1Score, int p2Score) implements Serializable {

    /**
     * @throws IllegalArgumentException 점수가 음수이거나, 상대가 나갔는데 이긴 쪽이 없는 경우
     */
    public MatchResult {
        Objects.requireNonNull(reason, "reason");
        if (p1Score < 0 || p2Score < 0) {
            throw new IllegalArgumentException("점수는 음수일 수 없습니다: " + p1Score + ", " + p2Score);
        }
        if (winner == null && reason == EndReason.FORFEIT) {
            throw new IllegalArgumentException("상대가 나간 경우는 무승부가 될 수 없습니다.");
        }
    }

    /** {@code winner}가 이긴 결과 */
    public static MatchResult win(Side winner, EndReason reason, int p1Score, int p2Score) {
        return new MatchResult(Objects.requireNonNull(winner, "winner"), reason, p1Score, p2Score);
    }

    /** 무승부 결과 */
    public static MatchResult draw(EndReason reason, int p1Score, int p2Score) {
        return new MatchResult(null, reason, p1Score, p2Score);
    }

    public boolean isDraw() {
        return winner == null;
    }

    /** {@code side}가 이겼는지. 무승부면 양쪽 모두 {@code false} */
    public boolean isWinner(Side side) {
        return winner == Objects.requireNonNull(side, "side");
    }

    /** {@code side}의 최종 점수 */
    public int score(Side side) {
        return Objects.requireNonNull(side, "side") == Side.P1 ? p1Score : p2Score;
    }
}
