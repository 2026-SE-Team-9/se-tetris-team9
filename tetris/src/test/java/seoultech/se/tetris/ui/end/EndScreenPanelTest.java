package seoultech.se.tetris.ui.end;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seoultech.se.tetris.engine.Difficulty;
import seoultech.se.tetris.engine.GameMode;
import seoultech.se.tetris.score.CsvScoreRepository;
import seoultech.se.tetris.ui.end.EndScreenModel.Phase;
import seoultech.se.tetris.ui.menu.MenuItem;
import seoultech.se.tetris.ui.theme.RetroTheme;

/** 1인 게임 종료 화면의 레트로 디자인 */
class EndScreenPanelTest {

    @TempDir
    Path dir;

    private final List<String> calls = new ArrayList<>();

    private EndScreenModel model(int score) {
        return new EndScreenModel(score, GameMode.NORMAL, Difficulty.NORMAL,
                new CsvScoreRepository(dir.resolve("scores.csv")),
                List.of(new MenuItem("메인 메뉴", () -> calls.add("menu")),
                        new MenuItem("종료", () -> calls.add("exit"))));
    }

    private static BufferedImage render(EndScreenPanel panel) {
        Dimension size = panel.getPreferredSize();
        panel.setSize(size);
        BufferedImage image = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_RGB);
        panel.paint(image.getGraphics());
        return image;
    }

    private static void type(EndScreenPanel panel, char c) {
        for (var listener : panel.getKeyListeners()) {
            listener.keyTyped(new KeyEvent(new JPanel(), KeyEvent.KEY_TYPED, 0, 0, KeyEvent.VK_UNDEFINED, c));
        }
    }

    @Test
    void 화면_크기는_기존과_같다() {
        assertEquals(new Dimension(480, 560), new EndScreenPanel(model(100)).getPreferredSize());
    }

    @Test
    void 이름_입력은_분홍_틀_안의_노란_입력칸에_그린다() {
        EndScreenModel model = model(500);
        EndScreenPanel panel = new EndScreenPanel(model);
        assertSame(Phase.NAME_ENTRY, model.getPhase());

        type(panel, 'A');
        BufferedImage image = render(panel);

        assertEquals("A", model.getName());
        Rectangle frame = EndScreenPanel.NAME_FRAME;
        Rectangle field = EndScreenPanel.NAME_FIELD;
        assertEquals(RetroTheme.PINK.getRGB(), image.getRGB(frame.x + frame.width / 2, frame.y));
        assertEquals(RetroTheme.YELLOW.getRGB(), image.getRGB(field.x + field.width / 2, field.y));
    }

    @Test
    void 이름을_등록하면_하늘색_틀_안의_표와_메뉴를_그린다() {
        EndScreenModel model = model(500);
        EndScreenPanel panel = new EndScreenPanel(model);
        type(panel, 'A');
        panel.handleKey(KeyEvent.VK_ENTER);
        assertSame(Phase.SCOREBOARD, model.getPhase());

        BufferedImage image = render(panel);
        Rectangle table = EndScreenPanel.TABLE_FRAME;
        Rectangle first = EndScreenPanel.selectionBounds(0);
        Rectangle second = EndScreenPanel.selectionBounds(1);
        assertEquals(RetroTheme.CYAN.getRGB(), image.getRGB(table.x + table.width / 2, table.y));
        assertEquals(RetroTheme.YELLOW.getRGB(), image.getRGB(first.x + first.width / 2, first.y));
        assertNotEquals(RetroTheme.YELLOW.getRGB(), image.getRGB(second.x + second.width / 2, second.y));
    }

    @Test
    void 메뉴_조작과_키_안내는_기존과_같다() {
        EndScreenModel model = model(500);
        EndScreenPanel panel = new EndScreenPanel(model);
        type(panel, 'A');
        panel.handleKey(KeyEvent.VK_ENTER);
        assertSame(Phase.SCOREBOARD, model.getPhase());
        panel.handleKey(KeyEvent.VK_A);
        render(panel);

        panel.handleKey(KeyEvent.VK_DOWN);
        BufferedImage image = render(panel);
        Rectangle second = EndScreenPanel.selectionBounds(1);
        assertEquals(RetroTheme.YELLOW.getRGB(), image.getRGB(second.x + second.width / 2, second.y));

        panel.handleKey(KeyEvent.VK_UP);
        panel.handleKey(KeyEvent.VK_ENTER);
        assertEquals(List.of("menu"), calls);
    }

    @Test
    void 이름_입력_중에는_지우기와_등록만_쓴다() {
        EndScreenModel model = model(500);
        EndScreenPanel panel = new EndScreenPanel(model);
        type(panel, 'A');
        type(panel, 'B');
        panel.handleKey(KeyEvent.VK_BACK_SPACE);
        panel.handleKey(KeyEvent.VK_DOWN);

        assertEquals("A", model.getName());
        assertSame(Phase.NAME_ENTRY, model.getPhase());
    }
}
