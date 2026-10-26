package seoultech.se.tetris.component.listener;

public interface GameEventListener {
    /** 보드 상태가 변경되어 화면을 다시 그려야 할 때 호출 */
    void onBoardUpdated();

    /** 줄이 삭제되었을 때 호출 (애니메이션, 추가 점수 팝업 등에 사용) */
    void onLinesCleared(int clearedCount, int gainedScore);

    /** 게임 속도(레벨)가 증가했을 때 호출 */
    void onSpeedIncreased(int currentInterval);

    /** 게임 오버가 발생했을 때 호출 (최종 점수 전달) */
    void onGameOver(int finalScore);

    /**
     * 줄 삭제 애니메이션이 시작되었을 때 호출
     *
     * <p>애니메이션 길이를 재는 타이머가 받아서,
     * 시간이 다 되면 {@code GameController#finishClearing()}을 호출한다.
     * 필요한 리스너만 구현하도록 기본 동작은 아무것도 하지 않는 것이다.
     */
    default void onClearingStarted() {
    }
}
