package seoultech.se.tetris.versus.p2p;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import seoultech.se.tetris.blocks.Block;
import seoultech.se.tetris.blocks.BlockType;
import seoultech.se.tetris.engine.Difficulty;
import seoultech.se.tetris.engine.GameMode;
import seoultech.se.tetris.engine.TetrisEngine;
import seoultech.se.tetris.versus.BoardSnapshot;

@DisplayName("RemoteGameState")
class RemoteGameStateTest {

    private static BoardSnapshot snapshotWith(Block current) {
        return new BoardSnapshot(new int[20][10], current, null, 0, 0, 1, 0, false, false, new boolean[20][10], List.of());
    }

    @Test
    @DisplayName("받은 화면 정보를 그대로 보여준다")
    void showsSnapshot() {
        TetrisEngine engine = TetrisEngine.seeded(5L, Difficulty.NORMAL, GameMode.NORMAL);
        engine.dropToBottom();
        RemoteGameState remote = new RemoteGameState(BoardSnapshot.of(engine));

        assertEquals(BoardSnapshot.of(engine), BoardSnapshot.of(remote));
        assertEquals(engine.getCurrentX(), remote.getCurrentX());
        assertEquals(engine.getCurrentY(), remote.getCurrentY());
    }

    @Test
    @DisplayName("새 화면 정보를 받으면 통째로 바꾼다")
    void replacesWithLatest() {
        RemoteGameState remote = new RemoteGameState(snapshotWith(null));
        BoardSnapshot latest = snapshotWith(Block.of(BlockType.O, 3, 4));

        remote.update(latest);

        assertEquals(latest, remote.snapshot());
        assertEquals(4, remote.getCurrentX());
        assertEquals(3, remote.getCurrentY());
    }

    @Test
    @DisplayName("현재 블럭이 없으면 위치는 0이다")
    void noCurrentBlock() {
        RemoteGameState remote = new RemoteGameState(snapshotWith(null));

        assertEquals(0, remote.getCurrentX());
        assertEquals(0, remote.getCurrentY());
    }

    @Test
    @DisplayName("일시정지 상태가 되는 일은 없다")
    void neverPaused() {
        assertFalse(new RemoteGameState(snapshotWith(null)).isPaused());
    }

    @Test
    @DisplayName("화면 정보가 없으면 예외를 던진다")
    void rejectsNull() {
        assertThrows(NullPointerException.class, () -> new RemoteGameState(null));
        RemoteGameState remote = new RemoteGameState(snapshotWith(null));
        assertThrows(NullPointerException.class, () -> remote.update(null));
    }
}
