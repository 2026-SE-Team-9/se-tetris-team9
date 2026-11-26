package seoultech.se.tetris.ui.versus;

import java.util.Objects;

import seoultech.se.tetris.versus.Side;
import seoultech.se.tetris.versus.p2p.Role;

/**
 * 대전 화면의 표시 방식. 로컬 대전과 P2P 대전이 같은 대전 화면({@link VersusGamePanel})을 쓰되,
 * 제목·플레이어 이름·보드 순서·안내 문구만 다르게 보여준다. Swing/AWT에 의존하지 않는다.
 *
 * @param titlePrefix   제목 앞부분. 예: "로컬 대전"
 * @param leftSide      왼쪽에 놓을 보드의 자리
 * @param mySide        이 PC의 플레이어 자리. 결과를 내 관점(승리/패배)으로 보여줄 때 쓴다. 로컬 대전은 {@code null}
 * @param p1Label       P1 보드 위의 이름
 * @param p2Label       P2 보드 위의 이름
 * @param playingHint   대전 중 안내
 * @param countdownHint 카운트다운 중 안내
 * @param endedHint     대전이 끝난 뒤 안내
 */
public record VersusPresentation(String titlePrefix, Side leftSide, Side mySide, String p1Label, String p2Label,
        String playingHint, String countdownHint, String endedHint) {

    /** 로컬 대전. P1이 왼쪽이고, 결과는 "P1 승리"처럼 자리로 보여준다. */
    public static final VersusPresentation LOCAL = new VersusPresentation("로컬 대전", Side.P1, null, "P1", "P2",
            VersusScreenModel.PLAYING_HINT, VersusScreenModel.COUNTDOWN_HINT, VersusScreenModel.ENDED_HINT);

    static final String P2P_PLAYING_HINT = "Esc  게임 나가기";
    static final String P2P_COUNTDOWN_HINT = "잠시 후 시작합니다 · Esc 게임 나가기";
    static final String P2P_ENDED_HINT = "잠시 후 대기실로 돌아갑니다";

    public VersusPresentation {
        Objects.requireNonNull(titlePrefix, "titlePrefix");
        Objects.requireNonNull(leftSide, "leftSide");
        Objects.requireNonNull(p1Label, "p1Label");
        Objects.requireNonNull(p2Label, "p2Label");
        Objects.requireNonNull(playingHint, "playingHint");
        Objects.requireNonNull(countdownHint, "countdownHint");
        Objects.requireNonNull(endedHint, "endedHint");
    }

    /**
     * P2P 대전. 내 보드를 왼쪽에 두고, 이름은 "나 (방장)"과 "상대 (참여자)"처럼 나와 상대의 역할로 보여준다.
     * 결과는 내 관점의 승리 / 패배로 보여준다.
     */
    public static VersusPresentation p2p(Role myRole) {
        Side mine = Objects.requireNonNull(myRole, "myRole").side();
        String myLabel = "나 (" + roleName(myRole) + ")";
        String opponentLabel = "상대 (" + roleName(myRole == Role.SERVER ? Role.CLIENT : Role.SERVER) + ")";
        return new VersusPresentation("P2P 대전", mine, mine,
                mine == Side.P1 ? myLabel : opponentLabel,
                mine == Side.P2 ? myLabel : opponentLabel,
                P2P_PLAYING_HINT, P2P_COUNTDOWN_HINT, P2P_ENDED_HINT);
    }

    private static String roleName(Role role) {
        return role == Role.SERVER ? "방장" : "참여자";
    }

    /** 보드 위에 표시할 이름 */
    public String labelOf(Side side) {
        return Objects.requireNonNull(side, "side") == Side.P1 ? p1Label : p2Label;
    }

    /** 오른쪽에 놓을 보드의 자리 */
    public Side rightSide() {
        return leftSide.opponent();
    }
}
