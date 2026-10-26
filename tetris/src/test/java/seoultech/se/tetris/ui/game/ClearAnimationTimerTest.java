package seoultech.se.tetris.ui.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import seoultech.se.tetris.blocks.BlockFactory;
import seoultech.se.tetris.blocks.BlockType;
import seoultech.se.tetris.engine.TetrisEngine;

class ClearAnimationTimerTest {

    /** 항상 I 블럭만 주는 생성기. */
    private static BlockFactory alwaysI() {
        return new BlockFactory(new Random(0)) {
            @Override
            public BlockType next() {
                return BlockType.I;
            }
        };
    }

    /**
     * 애니메이션이 시작되면 한 번만 동작하는 타이머를 켜고, 시간이 되면 엔진의 애니메이션을 끝낸다.
     * 그동안 자동 낙하 타이머는 멈췄다가 애니메이션이 끝나면 다시 시작한다.
     */
    @Test
    void finishesClearingWhenTimeIsUp() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            TetrisEngine engine = new TetrisEngine(alwaysI());
            ClearAnimationTimer timer = new ClearAnimationTimer(engine);
            GameProgressTimer drop = new GameProgressTimer(engine, engine);
            engine.addListener(timer);
            engine.addListener(drop);
            try {
                drop.start();
                assertFalse(timer.repeats());
                assertFalse(timer.isRunning());

                fillBottomRow(engine);
                assertTrue(engine.isClearing());
                assertTrue(timer.isRunning());
                assertFalse(drop.isRunning());

                timer.fire();
                assertFalse(engine.isClearing());
                assertFalse(timer.isRunning());
                assertTrue(drop.isRunning());
                assertEquals(1, engine.getLinesCleared());
            } finally {
                timer.stop();
                drop.stop();
            }
        });
    }

    /** 게임 오버를 받거나 화면을 닫으면 타이머를 멈춘다. */
    @Test
    void stopsOnGameOverAndClose() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            TetrisEngine engine = new TetrisEngine(alwaysI());
            ClearAnimationTimer timer = new ClearAnimationTimer(engine);

            timer.onClearingStarted();
            assertTrue(timer.isRunning());
            timer.onGameOver(0);
            assertFalse(timer.isRunning());

            timer.onClearingStarted();
            timer.stop();
            assertFalse(timer.isRunning());
        });
    }

    /**
     * I 블럭 네 개로 맨 아래 줄(19행)을 채운다. 마지막 블럭이 고정되면 줄 삭제 애니메이션이 시작된다.
     *
     * <p>엔진 밖의 패키지에서는 미리 채운 보드를 넣을 수 없으므로 실제 조작으로 줄을 만든다.
     * 가로 I는 생성 위치에서 3~6열을, 세로 I는 5열을 차지한다.
     */
    private static void fillBottomRow(TetrisEngine engine) {
        moveAndDrop(engine, false, -3);   // 가로 I → 0~3열
        moveAndDrop(engine, false, 1);    // 가로 I → 4~7열
        moveAndDrop(engine, true, 3);     // 세로 I → 8열
        moveAndDrop(engine, true, 4);     // 세로 I → 9열, 19행이 가득 찬다
    }

    private static void moveAndDrop(TetrisEngine engine, boolean vertical, int columns) {
        if (vertical) {
            engine.rotate();
        }
        for (int i = 0; i < Math.abs(columns); i++) {
            if (columns < 0) {
                engine.moveLeft();
            } else {
                engine.moveRight();
            }
        }
        engine.dropToBottom();
    }
}
