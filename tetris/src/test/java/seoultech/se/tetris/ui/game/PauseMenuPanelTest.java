package seoultech.se.tetris.ui.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import seoultech.se.tetris.ui.menu.MenuItem;

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

    /** 1인 모드는 "타이틀로 돌아가기", 로컬 대전은 정한 이름으로 마지막 항목을 보여준다. */
    @Test
    void leaveItemLabelCanBeChosen() throws Exception {
        List<String> calls = new ArrayList<>();
        SwingUtilities.invokeAndWait(() -> {
            PauseMenuPanel single = new PauseMenuPanel(() -> { }, () -> { }, () -> { });
            assertEquals(List.of("재개", "설정", "타이틀로 돌아가기"),
                    single.getItems().stream().map(MenuItem::label).toList());

            PauseMenuPanel versus = new PauseMenuPanel(() -> { }, () -> { },
                    "시작 메뉴로 돌아가기", () -> calls.add("leave"));
            assertEquals(List.of("재개", "설정", "시작 메뉴로 돌아가기"),
                    versus.getItems().stream().map(MenuItem::label).toList());
            press(versus, KeyEvent.VK_UP);
            press(versus, KeyEvent.VK_ENTER);
        });
        assertEquals(List.of("leave"), calls);
        assertThrows(NullPointerException.class,
                () -> new PauseMenuPanel(() -> { }, () -> { }, null, () -> { }));
    }

    private static void press(PauseMenuPanel panel, int keyCode) {
        KeyEvent event = new KeyEvent(panel, KeyEvent.KEY_PRESSED, System.currentTimeMillis(),
                0, keyCode, KeyEvent.CHAR_UNDEFINED);
        for (var listener : panel.getKeyListeners()) {
            listener.keyPressed(event);
        }
    }
}
