package seoultech.se.tetris.ui.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import seoultech.se.tetris.config.AppSettings.ScreenSize;
import seoultech.se.tetris.engine.Difficulty;

class ModeOptionsPanelTest {

    @Test
    void menuModeOptionsIncludesDifficultyAndManagesValues() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ModeOptionsPanel panel = new ModeOptionsPanel(true);

            assertEquals(ScreenSize.SMALL, panel.getScreenSize());
            assertEquals(Difficulty.EASY, panel.getDifficulty());
            assertFalse(panel.isColorBlind());

            panel.setScreenSize(ScreenSize.LARGE);
            panel.setDifficulty(Difficulty.HARD);
            panel.setColorBlind(true);

            assertEquals(ScreenSize.LARGE, panel.getScreenSize());
            assertEquals(Difficulty.HARD, panel.getDifficulty());
            assertTrue(panel.isColorBlind());

            // allowsDifficulty=true일 때 화면 크기, 난이도, 색맹 3개 행
            assertEquals(3, panel.navigationRows().size());
        });
    }

    @Test
    void inGameModeOptionsExcludesDifficulty() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ModeOptionsPanel panel = new ModeOptionsPanel(false);

            // allowsDifficulty=false일 때 화면 크기, 색맹 2개 행
            assertEquals(2, panel.navigationRows().size());
        });
    }
}
