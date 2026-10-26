package seoultech.se.tetris.component.model;

import seoultech.se.tetris.blocks.Block;

public interface GameState {
    /**
     * 현재 20x10 보드에 고정된 블럭들의 상태 반환.
     *
     * <p>각 칸의 값은 칸 번호로, 테트로미노 또는 아이템의 번호이며 빈 칸은
     * {@link seoultech.se.tetris.blocks.CellCode#EMPTY}이다.
     * 화면은 번호를 직접 비교하지 않고 {@link seoultech.se.tetris.blocks.CellCode}로 종류와 표시 문자를 얻는다.
     */
    int[][] getBoard();

    /** 현재 떨어지고 있는 블록 객체 반환 */
    Block getCurrentBlock();

    /** 현재 블록의 보드 상 X 좌표 */
    int getCurrentX();

    /** 현재 블록의 보드 상 Y 좌표 */
    int getCurrentY();

    /** 다음에 등장할 블록 객체 반환 */
    Block getNextBlock();

    /** 실시간 점수 반환 */
    int getScore();

    /** 현재 레벨 반환 (1부터 시작) */
    int getLevel();

    /** 현재 삭제한 총 라인 수 반환 */
    int getLinesCleared();

    /** 게임 일시정지 상태 여부 */
    boolean isPaused();

    /** 게임 오버 상태 여부 */
    boolean isGameOver();

    /** (추가 기능) 하드 드롭 착지 예상 Y 좌표 (고스트 피스용) */
    int getGhostY();

    /**
     * 줄 삭제 애니메이션 중인지 여부
     *
     * <p>애니메이션 중에는 현재 블럭이 이미 보드에 고정되어 있으므로 현재 블럭과 고스트를 그리지 않는다.
     * 일시정지와는 별개의 상태이며, 이 동안에는 일시정지 키를 포함한 모든 입력을 받지 않는다.
     *
     * <p>애니메이션이 없는 구현을 위해 기본값은 {@code false}
     */
    default boolean isClearing() {
        return false;
    }

    /**
     * 애니메이션이 끝나면 지워질 칸
     *
     * <p>{@link #getBoard()}와 같은 크기이며 지워질 칸이 {@code true}이다.
     *
     * <p>애니메이션 중이 아니면 모든 칸이 {@code false}
     */
    default boolean[][] getClearingCells() {
        int[][] board = getBoard();
        return new boolean[board.length][board.length == 0 ? 0 : board[0].length];
    }
}
