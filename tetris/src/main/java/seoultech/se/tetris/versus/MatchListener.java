package seoultech.se.tetris.versus;

/**
 * 대전 한 판의 진행을 받는 쪽.
 *
 * <p>각 플레이어 보드의 변화(블럭 이동, 줄 삭제 등)는 지금처럼 엔진의
 * {@link seoultech.se.tetris.component.listener.GameEventListener}로 받고,
 * 여기서는 대전 전체의 진행만 받는다.
 */
public interface MatchListener {

    /**
     * 진행 단계가 바뀌었을 때 호출
     *
     * <p>낙하 타이머를 가진 쪽은 {@link MatchPhase#PLAYING}이 되면 타이머를 시작하고,
     * {@link MatchPhase#PAUSED}나 {@link MatchPhase#ENDED}가 되면 멈춘다.
     * 필요한 리스너만 구현하도록 기본 동작은 아무것도 하지 않는 것이다.
     */
    default void onPhaseChanged(MatchPhase phase) {
    }

    /**
     * 결과가 정해져 대전이 끝났을 때 호출
     *
     * <p>결과 없이 끝나는 경우(로컬 대전 도중 종료, P2P 연결 끊김)에는 호출되지 않고
     * {@link #onPhaseChanged}로 {@link MatchPhase#ENDED}만 알린다.
     */
    void onMatchEnded(MatchResult result);
}
