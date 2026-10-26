package seoultech.se.tetris.ui.scoreboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seoultech.se.tetris.engine.Difficulty;
import seoultech.se.tetris.engine.GameMode;
import seoultech.se.tetris.score.CsvScoreRepository;
import seoultech.se.tetris.score.ScoreRepository;

class ScoreboardPanelTest {

    @TempDir
    Path dir;

    private static void press(ScoreboardPanel panel, int keyCode) {
        KeyEvent event = new KeyEvent(panel, KeyEvent.KEY_PRESSED, 0, 0, keyCode, KeyEvent.CHAR_UNDEFINED);
        for (var listener : panel.getKeyListeners()) {
            listener.keyPressed(event);
        }
    }

    @Test
    void 좌우_키로_모드를_바꾸고_Enter와_Esc로_돌아간다() {
        AtomicInteger backs = new AtomicInteger();
        ScoreboardModel model = new ScoreboardModel(
                new CsvScoreRepository(dir.resolve("scores.csv")), backs::incrementAndGet);
        ScoreboardPanel panel = new ScoreboardPanel(model);

        press(panel, KeyEvent.VK_RIGHT);
        assertEquals(GameMode.ITEM, model.getMode());
        press(panel, KeyEvent.VK_LEFT);
        assertEquals(GameMode.NORMAL, model.getMode());
        press(panel, KeyEvent.VK_ENTER);
        press(panel, KeyEvent.VK_ESCAPE);
        press(panel, KeyEvent.VK_A);

        assertEquals(2, backs.get());
        assertEquals(GameMode.NORMAL, model.getMode());
    }

    @Test
    void 기록이_있어도_없어도_화면을_그릴_수_있다() {
        ScoreRepository empty = new CsvScoreRepository(dir.resolve("empty.csv"));
        ScoreRepository filled = new CsvScoreRepository(dir.resolve("filled.csv"));
        filled.addScore("kim", 300, GameMode.NORMAL, Difficulty.NORMAL);
        for (ScoreRepository repository : new ScoreRepository[] {empty, filled}) {
            ScoreboardPanel panel = new ScoreboardPanel(new ScoreboardModel(repository, () -> { }));
            Dimension size = panel.getPreferredSize();
            panel.setSize(size);
            BufferedImage image = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_RGB);

            panel.paint(image.createGraphics());
        }
    }

    @Test
    void 모든_모드에_화면_표시_이름이_있다() {
        for (GameMode mode : GameMode.values()) {
            assertTrue(!ScoreboardPanel.modeLabel(mode).isBlank());
        }
    }
}
