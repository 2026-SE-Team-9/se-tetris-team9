package seoultech.se.tetris.versus.p2p;

import java.util.List;
import java.util.Objects;
import seoultech.se.tetris.blocks.Block;
import seoultech.se.tetris.component.model.GameState;
import seoultech.se.tetris.component.model.GarbageLine;
import seoultech.se.tetris.versus.BoardSnapshot;

/**
 * 상대 PC에서 받은 화면 정보로 보여주는 상대의 게임 상태. 읽기만 할 수 있다.
 *
 * <p>가장 최근에 받은 화면 정보 하나만 들고 있으며, 새 화면 정보를 받으면 통째로 바꾼다. (3차 팀 결정)
 */
final class RemoteGameState implements GameState {

    private BoardSnapshot snapshot;

    /** @param initial 첫 화면 정보를 받기 전에 보여줄 상태 */
    RemoteGameState(BoardSnapshot initial) {
        this.snapshot = Objects.requireNonNull(initial, "initial");
    }

    /** 받은 화면 정보로 바꾼다. */
    void update(BoardSnapshot latest) {
        this.snapshot = Objects.requireNonNull(latest, "latest");
    }

    BoardSnapshot snapshot() {
        return snapshot;
    }

    @Override
    public int[][] getBoard() {
        return snapshot.board();
    }

    @Override
    public Block getCurrentBlock() {
        return snapshot.current();
    }

    @Override
    public int getCurrentX() {
        return snapshot.current() == null ? 0 : snapshot.current().col();
    }

    @Override
    public int getCurrentY() {
        return snapshot.current() == null ? 0 : snapshot.current().row();
    }

    @Override
    public Block getNextBlock() {
        return snapshot.next();
    }

    @Override
    public int getScore() {
        return snapshot.score();
    }

    @Override
    public int getLevel() {
        return snapshot.level();
    }

    @Override
    public int getLinesCleared() {
        return snapshot.linesCleared();
    }

    /** P2P 대전에는 일시정지가 없다. */
    @Override
    public boolean isPaused() {
        return false;
    }

    @Override
    public boolean isGameOver() {
        return snapshot.gameOver();
    }

    @Override
    public int getGhostY() {
        return snapshot.ghostY();
    }

    @Override
    public boolean isClearing() {
        return snapshot.clearing();
    }

    @Override
    public boolean[][] getClearingCells() {
        return snapshot.clearingCells();
    }

    @Override
    public List<GarbageLine> getPendingGarbage() {
        return snapshot.pendingGarbage();
    }
}
