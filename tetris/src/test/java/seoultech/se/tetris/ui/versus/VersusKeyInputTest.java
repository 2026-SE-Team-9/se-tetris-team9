package seoultech.se.tetris.ui.versus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.KeyEvent;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.swing.JPanel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seoultech.se.tetris.component.controller.GameController;
import seoultech.se.tetris.config.PropertiesBattleKeyConfigService;
import seoultech.se.tetris.versus.EndReason;
import seoultech.se.tetris.versus.MatchResult;
import seoultech.se.tetris.versus.Side;
import seoultech.se.tetris.versus.VersusMode;

class VersusKeyInputTest {

    @TempDir
    Path dir;

    private final List<String> log = new ArrayList<>();
    private VersusScreenModel model;
    private VersusKeyInput input;

    @BeforeEach
    void setUp() {
        VersusKeyMap keyMap = new VersusKeyMap(
                new PropertiesBattleKeyConfigService(dir.resolve("battle_keybindings.properties")));
        model = new VersusScreenModel(VersusMode.NORMAL);
        input = new VersusKeyInput(keyMap, this::controller, model,
                () -> log.add("quit?"), () -> log.add("menu"));
    }

    private Optional<GameController> controller(Side side) {
        return Optional.of(new GameController() {
            @Override public void moveLeft() { log.add(side + ":left"); }
            @Override public void moveRight() { log.add(side + ":right"); }
            @Override public void moveDown() { log.add(side + ":down"); }
            @Override public void dropToBottom() { log.add(side + ":drop"); }
            @Override public void rotate() { log.add(side + ":rotate"); }
            @Override public void rotateCounterclockwise() { }
            @Override public void togglePause() { }
            @Override public void pauseGame() { }
            @Override public void resumeGame() { }
            @Override public void restartGame() { }
            @Override public void exitGame() { }
        });
    }

    private void press(int keyCode) {
        input.keyPressed(new KeyEvent(new JPanel(), KeyEvent.KEY_PRESSED, 0, 0, keyCode, KeyEvent.CHAR_UNDEFINED));
    }

    @Test
    void 대전_중에는_두_플레이어의_키를_함께_처리한다() {
        press(KeyEvent.VK_A);
        press(KeyEvent.VK_LEFT);
        press(KeyEvent.VK_W);
        press(KeyEvent.VK_CONTROL);

        assertEquals(List.of("P1:left", "P2:left", "P1:rotate", "P2:drop"), log);
    }

    @Test
    void 대전_중_Esc는_나가기_확인을_요청한다() {
        press(KeyEvent.VK_ESCAPE);

        assertEquals(List.of("quit?"), log);
    }

    @Test
    void 대전_중_Enter와_대전_키가_아닌_키는_무시한다() {
        press(KeyEvent.VK_ENTER);
        press(KeyEvent.VK_Q);

        assertTrue(log.isEmpty());
    }

    @Test
    void 대전이_끝나면_Enter나_Esc로_시작_메뉴로_돌아간다() {
        model.matchEnded(MatchResult.win(Side.P1, EndReason.TOP_OUT, 100, 0));

        press(KeyEvent.VK_ENTER);
        press(KeyEvent.VK_ESCAPE);

        assertEquals(List.of("menu", "menu"), log);
    }

    @Test
    void 대전이_끝나면_조작_키는_무시한다() {
        model.matchEnded(MatchResult.win(Side.P1, EndReason.TOP_OUT, 100, 0));

        press(KeyEvent.VK_A);
        press(KeyEvent.VK_LEFT);

        assertTrue(log.isEmpty());
    }

    @Test
    void 필요한_값이_없으면_만들_수_없다() {
        VersusKeyMap keyMap = new VersusKeyMap(
                new PropertiesBattleKeyConfigService(dir.resolve("other.properties")));
        Runnable none = () -> { };

        assertThrows(NullPointerException.class, () -> new VersusKeyInput(null, this::controller, model, none, none));
        assertThrows(NullPointerException.class, () -> new VersusKeyInput(keyMap, null, model, none, none));
        assertThrows(NullPointerException.class, () -> new VersusKeyInput(keyMap, this::controller, null, none, none));
        assertThrows(NullPointerException.class, () -> new VersusKeyInput(keyMap, this::controller, model, null, none));
        assertThrows(NullPointerException.class, () -> new VersusKeyInput(keyMap, this::controller, model, none, null));
    }
}
