package seoultech.se.tetris.versus;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import seoultech.se.tetris.blocks.Block;
import seoultech.se.tetris.component.model.GameState;
import seoultech.se.tetris.engine.Board;
import seoultech.se.tetris.component.model.GarbageLine;

/**
 * 한 플레이어의 보드를 그리는 데 필요한 정보를 한 시점에 담아 둔 것.
 *
 * <p>P2P 대전 모드에서 상대 화면을 그리기 위해 주고받는다.
 * 각자 자신의 보드를 진행하고, 서로의 화면 정보와 공격 정보만 주고받기 때문이다. (3차 팀 결정)
 *
 * <p>값은 {@link GameState}에서 읽은 그대로다. 일시정지는 P2P 대전 모드에 없으므로 담지 않는다.
 *
 * <p>배열은 받을 때와 돌려줄 때 모두 복사하므로 이 객체는 불변이다.
 * 역직렬화할 때도 생성자를 거치므로 받은 값의 크기 검사가 그대로 적용된다.
 *
 * @param board          고정된 블럭들의 칸 번호 ({@link GameState#getBoard()})
 * @param current        현재 블럭. 없으면 {@code null}
 * @param next           다음 블럭. 없으면 {@code null}
 * @param ghostY         현재 블럭이 떨어질 행 ({@link GameState#getGhostY()})
 * @param score          점수
 * @param level          레벨
 * @param linesCleared   지운 줄 수
 * @param gameOver       게임이 끝났는지
 * @param clearing       줄 삭제 애니메이션 중인지
 * @param clearingCells  애니메이션이 끝나면 지워질 칸 ({@link GameState#getClearingCells()})
 * @param pendingGarbage 보드에 들어가기 전의 공격 대기 줄. 위에서 아래 순서
 */
public record BoardSnapshot(
        int[][] board,
        Block current,
        Block next,
        int ghostY,
        int score,
        int level,
        int linesCleared,
        boolean gameOver,
        boolean clearing,
        boolean[][] clearingCells,
        List<GarbageLine> pendingGarbage) implements Serializable {

    /**
     * @throws IllegalArgumentException 보드나 지워질 칸의 크기가 화면에 보이는 보드 크기와 다르거나,
     *         공격 대기 줄의 폭이 보드 폭과 다른 경우
     */
    public BoardSnapshot {
        board = copyBoard(Objects.requireNonNull(board, "board"));
        clearingCells = copyCells(Objects.requireNonNull(clearingCells, "clearingCells"));
        pendingGarbage = List.copyOf(Objects.requireNonNull(pendingGarbage, "pendingGarbage"));
        for (GarbageLine line : pendingGarbage) {
            if (line.width() != Board.WIDTH) {
                throw new IllegalArgumentException("공격 대기 줄은 " + Board.WIDTH + "칸이어야 합니다: " + line.width());
            }
        }
    }

    /** 현재 상태를 담는다. */
    public static BoardSnapshot of(GameState state) {
        Objects.requireNonNull(state, "state");
        return new BoardSnapshot(
                state.getBoard(),
                state.getCurrentBlock(),
                state.getNextBlock(),
                state.getGhostY(),
                state.getScore(),
                state.getLevel(),
                state.getLinesCleared(),
                state.isGameOver(),
                state.isClearing(),
                state.getClearingCells(),
                state.getPendingGarbage());
    }

    /** 고정된 블럭들의 칸 번호. 복사본을 돌려준다. */
    @Override
    public int[][] board() {
        return copyBoard(board);
    }

    /** 애니메이션이 끝나면 지워질 칸. 복사본을 돌려준다. */
    @Override
    public boolean[][] clearingCells() {
        return copyCells(clearingCells);
    }

    private static int[][] copyBoard(int[][] source) {
        if (source.length != Board.VISIBLE_HEIGHT) {
            throw new IllegalArgumentException("보드는 " + Board.VISIBLE_HEIGHT + "줄이어야 합니다: " + source.length);
        }
        int[][] copy = new int[Board.VISIBLE_HEIGHT][];
        for (int r = 0; r < Board.VISIBLE_HEIGHT; r++) {
            if (source[r].length != Board.WIDTH) {
                throw new IllegalArgumentException("각 줄은 " + Board.WIDTH + "칸이어야 합니다: " + source[r].length);
            }
            copy[r] = source[r].clone();
        }
        return copy;
    }

    private static boolean[][] copyCells(boolean[][] source) {
        if (source.length != Board.VISIBLE_HEIGHT) {
            throw new IllegalArgumentException("지워질 칸은 " + Board.VISIBLE_HEIGHT + "줄이어야 합니다: " + source.length);
        }
        boolean[][] copy = new boolean[Board.VISIBLE_HEIGHT][];
        for (int r = 0; r < Board.VISIBLE_HEIGHT; r++) {
            if (source[r].length != Board.WIDTH) {
                throw new IllegalArgumentException("각 줄은 " + Board.WIDTH + "칸이어야 합니다: " + source[r].length);
            }
            copy[r] = source[r].clone();
        }
        return copy;
    }

    // record의 기본 equals·hashCode는 배열을 참조로 비교하므로 내용으로 비교하도록 바꾼다.

    @Override
    public boolean equals(Object other) {
        return other instanceof BoardSnapshot s
                && Arrays.deepEquals(board, s.board)
                && Objects.equals(current, s.current)
                && Objects.equals(next, s.next)
                && ghostY == s.ghostY
                && score == s.score
                && level == s.level
                && linesCleared == s.linesCleared
                && gameOver == s.gameOver
                && clearing == s.clearing
                && Arrays.deepEquals(clearingCells, s.clearingCells)
                && pendingGarbage.equals(s.pendingGarbage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(Arrays.deepHashCode(board), current, next, ghostY, score, level, linesCleared,
                gameOver, clearing, Arrays.deepHashCode(clearingCells), pendingGarbage);
    }

    @Override
    public String toString() {
        return "BoardSnapshot[current=" + current + " score=" + score + " level=" + level
                + " lines=" + linesCleared + " gameOver=" + gameOver + " clearing=" + clearing
                + " pending=" + pendingGarbage.size() + "]";
    }
}
