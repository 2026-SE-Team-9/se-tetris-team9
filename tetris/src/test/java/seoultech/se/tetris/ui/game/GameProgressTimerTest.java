package seoultech.se.tetris.ui.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import seoultech.se.tetris.blocks.BlockFactory;
import seoultech.se.tetris.engine.LevelPolicy;
import seoultech.se.tetris.engine.TetrisEngine;

class GameProgressTimerTest {
    /** 자동 낙하가 엔진에 전달되고 일시정지·재개·종료 상태에 맞춰 멈추거나 다시 시작한다. */
    @Test
    void followsGameLifecycle() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            TetrisEngine engine = new TetrisEngine();
            GameProgressTimer timer = new GameProgressTimer(engine, engine);
            engine.addListener(timer);
            try {
                assertFalse(timer.isRunning());
                timer.start();
                assertTrue(timer.isRunning());

                int initialRow = engine.getCurrentY();
                timer.tick();
                assertEquals(initialRow + 1, engine.getCurrentY());

                engine.pauseGame();
                assertFalse(timer.isRunning());
                timer.tick();
                assertEquals(initialRow + 1, engine.getCurrentY());

                engine.resumeGame();
                assertTrue(timer.isRunning());
                timer.tick();
                assertEquals(initialRow + 2, engine.getCurrentY());

                engine.exitGame();
                assertFalse(timer.isRunning());
                engine.restartGame();
                assertTrue(timer.isRunning());
                assertEquals(LevelPolicy.INITIAL_INTERVAL_MS, timer.intervalMs());
            } finally {
                timer.stop();
            }
            assertFalse(timer.isRunning());
            engine.restartGame();
            assertFalse(timer.isRunning());
        });
    }

    /** 레벨 변경과 재시작 시 간격을 갱신하고 실제 게임 오버에서 자동 낙하를 멈춘다. */
    @Test
    void updatesSpeedAndStopsOnGameOver() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            TetrisEngine engine = new TetrisEngine(new BlockFactory(new Random(42)));
            GameProgressTimer timer = new GameProgressTimer(engine, engine);
            engine.addListener(timer);
            try {
                timer.start();
                timer.onSpeedIncreased(250);
                assertEquals(250, timer.intervalMs());

                engine.restartGame();
                assertEquals(LevelPolicy.INITIAL_INTERVAL_MS, timer.intervalMs());

                for (int i = 0; i < 200 && !engine.isGameOver(); i++) {
                    engine.dropToBottom();
                }
                assertTrue(engine.isGameOver());
                assertFalse(timer.isRunning());

                engine.restartGame();
                assertTrue(timer.isRunning());
                assertEquals(LevelPolicy.INITIAL_INTERVAL_MS, timer.intervalMs());
            } finally {
                timer.stop();
            }
        });
    }
}
