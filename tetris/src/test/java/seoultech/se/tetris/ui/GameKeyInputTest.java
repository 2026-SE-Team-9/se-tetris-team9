package seoultech.se.tetris.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.awt.event.KeyEvent;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import seoultech.se.tetris.blocks.Block;
import seoultech.se.tetris.component.controller.GameController;
import seoultech.se.tetris.component.model.GameState;
import seoultech.se.tetris.config.KeyBindingEditor;
import seoultech.se.tetris.config.KeyConfigService.GameAction;
import seoultech.se.tetris.config.PropertiesKeyConfigService;

class GameKeyInputTest {
    @TempDir
    Path tempDir;

    /** 기본 메인·서브키를 해당 조작으로 전달하고 등록되지 않은 키는 무시한다. */
    @Test
    void routesDefaultKeysAndIgnoresUnmappedKeys() {
        TestState state = new TestState();
        RecordingController controller = new RecordingController(state);
        GameKeyInput input = new GameKeyInput(config(), controller, state, () -> {});
        JPanel panel = new JPanel();
        input.attachTo(panel);

        press(input, panel, KeyEvent.VK_LEFT);
        press(input, panel, KeyEvent.VK_RIGHT);
        press(input, panel, KeyEvent.VK_DOWN);
        press(input, panel, KeyEvent.VK_SPACE);
        press(input, panel, KeyEvent.VK_UP);
        press(input, panel, KeyEvent.VK_X);
        press(input, panel, KeyEvent.VK_Z);
        press(input, panel, KeyEvent.VK_P);

        assertEquals(List.of("left", "right", "down", "drop", "rotate", "rotate", "counterclockwise"),
                controller.calls);
    }

    /** 설정 저장 후 입력 반영 함수를 호출해야 이전 키가 해제되고 새 키가 작동한다. */
    @Test
    void appliesChangedBindingsAfterRefresh() {
        PropertiesKeyConfigService config = config();
        TestState state = new TestState();
        RecordingController controller = new RecordingController(state);
        GameKeyInput input = new GameKeyInput(config, controller, state, () -> {});
        JPanel panel = new JPanel();
        input.attachTo(panel);

        var editor = new KeyBindingEditor(config, () -> {});
        editor.captureMainKey(GameAction.MOVE_LEFT, KeyEvent.VK_A);
        editor.save();
        assertKeyCalls(input, panel, controller, KeyEvent.VK_LEFT, "left");
        assertKeyCalls(input, panel, controller, KeyEvent.VK_A);
        input.applyKeyBindings();
        assertKeyCalls(input, panel, controller, KeyEvent.VK_LEFT);
        assertKeyCalls(input, panel, controller, KeyEvent.VK_A, "left");
    }

    /** 저장 전에는 기존 입력을 유지하고 저장 후에는 변경·삭제·재할당한 키를 정확히 반영한다. */
    @Test
    void savingEditorReplacesOldMappingsAndKeepsEditsPendingUntilSaved() {
        PropertiesKeyConfigService config = config();
        var initialEditor = new KeyBindingEditor(config, () -> {});
        initialEditor.captureMainKey(GameAction.ROTATE, KeyEvent.VK_N);
        initialEditor.save();
        TestState state = new TestState();
        RecordingController controller = new RecordingController(state);
        GameKeyInput input = new GameKeyInput(config, controller, state, () -> {});
        JPanel panel = new JPanel();
        input.attachTo(panel);
        KeyBindingEditor editor = input.createKeyBindingEditor();
        editor.captureMainKey(GameAction.ROTATE, KeyEvent.VK_K);
        editor.captureSubKey(GameAction.ROTATE, KeyEvent.VK_R);

        assertKeyCalls(input, panel, controller, KeyEvent.VK_N, "rotate");
        assertKeyCalls(input, panel, controller, KeyEvent.VK_X, "rotate");
        assertKeyCalls(input, panel, controller, KeyEvent.VK_K);
        assertKeyCalls(input, panel, controller, KeyEvent.VK_R);
        assertEquals(KeyEvent.VK_N, config.getKeyBinding(GameAction.ROTATE).mainKeyCode());

        editor.save();
        assertKeyCalls(input, panel, controller, KeyEvent.VK_N);
        assertKeyCalls(input, panel, controller, KeyEvent.VK_X);
        assertKeyCalls(input, panel, controller, KeyEvent.VK_K, "rotate");
        assertKeyCalls(input, panel, controller, KeyEvent.VK_R, "rotate");

        editor.captureSubKey(GameAction.ROTATE, KeyEvent.VK_ESCAPE);
        editor.save();
        assertKeyCalls(input, panel, controller, KeyEvent.VK_R);
        assertKeyCalls(input, panel, controller, KeyEvent.VK_K, "rotate");

        KeyBindingEditor leftEditor = input.createKeyBindingEditor();
        leftEditor.captureMainKey(GameAction.MOVE_LEFT, KeyEvent.VK_N);
        leftEditor.save();
        assertKeyCalls(input, panel, controller, KeyEvent.VK_N, "left");
        assertKeyCalls(input, panel, controller, KeyEvent.VK_LEFT);
        assertKeyCalls(input, panel, controller, KeyEvent.VK_K, "rotate");
    }

    /** 편집기 저장이 실패하면 새 키를 적용하지 않고 기존 메인·서브 입력을 유지한다. */
    @Test
    void failedEditorSaveKeepsOriginalInputMappings() {
        PropertiesKeyConfigService config = new PropertiesKeyConfigService(tempDir);
        TestState state = new TestState();
        RecordingController controller = new RecordingController(state);
        GameKeyInput input = new GameKeyInput(config, controller, state, () -> {});
        JPanel panel = new JPanel();
        input.attachTo(panel);
        KeyBindingEditor editor = input.createKeyBindingEditor();
        editor.captureMainKey(GameAction.ROTATE, KeyEvent.VK_K);
        editor.captureSubKey(GameAction.ROTATE, KeyEvent.VK_ESCAPE);

        assertThrows(UncheckedIOException.class, editor::save);
        assertKeyCalls(input, panel, controller, KeyEvent.VK_K);
        assertKeyCalls(input, panel, controller, KeyEvent.VK_UP, "rotate");
        assertKeyCalls(input, panel, controller, KeyEvent.VK_X, "rotate");
        assertEquals(KeyEvent.VK_UP, config.getKeyBinding(GameAction.ROTATE).mainKeyCode());
    }

    /** 일시정지 중 조작과 메뉴 재요청을 막고 게임 종료 후 입력도 무시한다. */
    @Test
    void blocksMovementWhilePausedOrGameOver() {
        TestState state = new TestState();
        RecordingController controller = new RecordingController(state);
        GameKeyInput input = new GameKeyInput(config(), controller, state, () -> {});
        JPanel panel = new JPanel();
        input.attachTo(panel);

        state.paused = true;
        press(input, panel, KeyEvent.VK_LEFT);
        press(input, panel, KeyEvent.VK_ESCAPE);
        state.paused = false;
        state.gameOver = true;
        press(input, panel, KeyEvent.VK_SPACE);
        press(input, panel, KeyEvent.VK_ESCAPE);

        assertEquals(List.of(), controller.calls);
    }

    /** 줄 삭제 애니메이션 중에는 조작과 일시정지 메뉴 요청을 모두 무시한다. */
    @Test
    void blocksAllInputWhileClearing() {
        TestState state = new TestState();
        RecordingController controller = new RecordingController(state);
        int[] menuRequests = {0};
        GameKeyInput input = new GameKeyInput(config(), controller, state, () -> menuRequests[0]++);
        JPanel panel = new JPanel();
        input.attachTo(panel);

        state.clearing = true;
        press(input, panel, KeyEvent.VK_LEFT);
        press(input, panel, KeyEvent.VK_SPACE);
        press(input, panel, KeyEvent.VK_UP);
        press(input, panel, KeyEvent.VK_ESCAPE);

        assertEquals(List.of(), controller.calls);
        assertEquals(0, menuRequests[0]);

        state.clearing = false;
        press(input, panel, KeyEvent.VK_LEFT);
        assertEquals(List.of("left"), controller.calls);
    }

    /** Esc는 종료 대신 일시정지 메뉴를 한 번 요청하며 반복 입력으로 재요청하지 않는다. */
    @Test
    void escapePausesAndRequestsMenuWithoutExiting() {
        TestState state = new TestState();
        RecordingController controller = new RecordingController(state);
        int[] menuRequests = {0};
        GameKeyInput input = new GameKeyInput(config(), controller, state, () -> menuRequests[0]++);
        JPanel panel = new JPanel();
        input.attachTo(panel);

        press(input, panel, KeyEvent.VK_ESCAPE);
        press(input, panel, KeyEvent.VK_ESCAPE);

        assertEquals(List.of("pause"), controller.calls);
        assertEquals(1, menuRequests[0]);
    }

    /** 같은 화면에 재연결하면 포커스를 다시 요청하고 리스너 중복 없이 화면 이동·연결 해제를 처리한다. */
    @Test
    void attachingAgainDoesNotDuplicateListenerAndDetachingStopsInput() {
        TestState state = new TestState();
        RecordingController controller = new RecordingController(state);
        GameKeyInput input = new GameKeyInput(config(), controller, state, () -> {});
        FocusTrackingPanel first = new FocusTrackingPanel();
        JPanel second = new JPanel();

        input.attachTo(first);
        input.attachTo(first);
        assertEquals(1, first.getKeyListeners().length);
        assertEquals(2, first.focusRequests);
        input.attachTo(second);
        assertEquals(0, first.getKeyListeners().length);
        assertEquals(1, second.getKeyListeners().length);
        press(input, first, KeyEvent.VK_LEFT);
        press(input, second, KeyEvent.VK_LEFT);
        input.detach();
        press(input, second, KeyEvent.VK_LEFT);
        assertEquals(0, second.getKeyListeners().length);
        assertEquals(List.of("left"), controller.calls);
    }

    private PropertiesKeyConfigService config() {
        return new PropertiesKeyConfigService(tempDir.resolve("keys.properties"));
    }

    /** 키마다 호출 기록을 분리하여 이전 키와 새 키의 동작이 서로 상쇄되지 않도록 한다. */
    private static void assertKeyCalls(GameKeyInput input, JPanel panel, RecordingController controller,
            int keyCode, String... expectedCalls) {
        controller.calls.clear();
        press(input, panel, keyCode);
        assertEquals(List.of(expectedCalls), controller.calls, "입력 키: " + KeyEvent.getKeyText(keyCode));
    }

    private static void press(GameKeyInput input, JPanel panel, int keyCode) {
        input.keyPressed(new KeyEvent(panel, KeyEvent.KEY_PRESSED, 0, 0, keyCode, KeyEvent.CHAR_UNDEFINED));
    }

    private static final class FocusTrackingPanel extends JPanel {
        int focusRequests;

        @Override
        public boolean requestFocusInWindow() {
            focusRequests++;
            return true;
        }
    }

    private static final class TestState implements GameState {
        boolean paused;
        boolean gameOver;
        boolean clearing;

        @Override public int[][] getBoard() { return new int[0][0]; }
        @Override public Block getCurrentBlock() { return null; }
        @Override public int getCurrentX() { return 0; }
        @Override public int getCurrentY() { return 0; }
        @Override public Block getNextBlock() { return null; }
        @Override public int getScore() { return 0; }
        @Override public int getLevel() { return 0; }
        @Override public int getLinesCleared() { return 0; }
        @Override public boolean isPaused() { return paused; }
        @Override public boolean isGameOver() { return gameOver; }
        @Override public int getGhostY() { return 0; }
        @Override public boolean isClearing() { return clearing; }
    }

    private static final class RecordingController implements GameController {
        final List<String> calls = new ArrayList<>();
        final TestState state;

        RecordingController(TestState state) { this.state = state; }

        @Override public void moveLeft() { calls.add("left"); }
        @Override public void moveRight() { calls.add("right"); }
        @Override public void moveDown() { calls.add("down"); }
        @Override public void dropToBottom() { calls.add("drop"); }
        @Override public void rotate() { calls.add("rotate"); }
        @Override public void rotateCounterclockwise() { calls.add("counterclockwise"); }
        @Override public void togglePause() { calls.add("toggle"); }
        @Override public void pauseGame() { calls.add("pause"); state.paused = true; }
        @Override public void resumeGame() { calls.add("resume"); state.paused = false; }
        @Override public void restartGame() { calls.add("restart"); }
        @Override public void exitGame() { calls.add("exit"); }
    }
}
