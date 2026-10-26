package seoultech.se.tetris.ui.game;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

class PauseMenuPanelTest {

    /** 방향키와 Enter로 설정과 타이틀을 선택하고 Esc로 재개한다. */
    @Test
    void keyboardSelectsPauseActions() throws Exception {
        List<String> calls = new ArrayList<>();
        SwingUtilities.invokeAndWait(() -> {
            PauseMenuPanel panel = new PauseMenuPanel(
                    () -> calls.add("resume"),
                    () -> calls.add("settings"),
                    () -> calls.add("title"));

            press(panel, KeyEvent.VK_DOWN);
            press(panel, KeyEvent.VK_ENTER);
            press(panel, KeyEvent.VK_DOWN);
            press(panel, KeyEvent.VK_ENTER);
            press(panel, KeyEvent.VK_ESCAPE);
        });
        assertEquals(List.of("settings", "title", "resume"), calls);
    }

    private static void press(PauseMenuPanel panel, int keyCode) {
        KeyEvent event = new KeyEvent(panel, KeyEvent.KEY_PRESSED, System.currentTimeMillis(),
                0, keyCode, KeyEvent.CHAR_UNDEFINED);
        for (var listener : panel.getKeyListeners()) {
            listener.keyPressed(event);
        }
    }
}
