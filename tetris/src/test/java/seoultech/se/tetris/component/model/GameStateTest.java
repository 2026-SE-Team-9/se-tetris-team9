package seoultech.se.tetris.component.model;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import seoultech.se.tetris.blocks.Block;

@DisplayName("GameState")
class GameStateTest {

    @Test
    @DisplayName("대전이 아닌 구현은 공격 대기 줄이 없다")
    void noPendingGarbageByDefault() {
        GameState state = new MinimalState();

        assertTrue(state.getPendingGarbage().isEmpty());
    }

    /** 기본 메서드를 하나도 다시 정의하지 않은 구현 */
    private static final class MinimalState implements GameState {
        @Override public int[][] getBoard() { return new int[20][10]; }
        @Override public Block getCurrentBlock() { return null; }
        @Override public int getCurrentX() { return 0; }
        @Override public int getCurrentY() { return 0; }
        @Override public Block getNextBlock() { return null; }
        @Override public int getScore() { return 0; }
        @Override public int getLevel() { return 1; }
        @Override public int getLinesCleared() { return 0; }
        @Override public boolean isPaused() { return false; }
        @Override public boolean isGameOver() { return false; }
        @Override public int getGhostY() { return 0; }
    }
}
