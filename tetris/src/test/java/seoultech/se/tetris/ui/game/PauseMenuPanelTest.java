package seoultech.se.tetris.ui.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import seoultech.se.tetris.ui.theme.RetroTheme;

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

    /** 가운데 하늘색 틀 안에 항목을 그리고, 선택한 항목만 노란 상자로 강조한다. */
    @Test
    void drawsRetroFrameAndHighlightsSelectedItem() {
        PauseMenuPanel panel = new PauseMenuPanel(() -> { }, () -> { }, () -> { });
        panel.setSize(500, 600);
        BufferedImage image = new BufferedImage(500, 600, BufferedImage.TYPE_INT_RGB);
        panel.paint(image.getGraphics());

        Rectangle frame = panel.menuFrame();
        assertEquals(RetroTheme.CYAN.getRGB(), image.getRGB(frame.x + frame.width / 2, frame.y));
        Rectangle first = panel.selectionBounds(0);
        Rectangle second = panel.selectionBounds(1);
        assertEquals(RetroTheme.YELLOW.getRGB(), image.getRGB(first.x + first.width / 2, first.y));
        assertNotEquals(RetroTheme.YELLOW.getRGB(), image.getRGB(second.x + second.width / 2, second.y));
        assertTrue(second.y + second.height < frame.y + frame.height);

        // 사용하지 않는 키를 누르면 안내가 밝아진다
        press(panel, KeyEvent.VK_A);
        panel.paint(image.getGraphics());
        press(panel, KeyEvent.VK_DOWN);
        panel.paint(image.getGraphics());
        assertEquals(RetroTheme.YELLOW.getRGB(), image.getRGB(second.x + second.width / 2, second.y));
    }

    private static void press(PauseMenuPanel panel, int keyCode) {
        KeyEvent event = new KeyEvent(panel, KeyEvent.KEY_PRESSED, System.currentTimeMillis(),
                0, keyCode, KeyEvent.CHAR_UNDEFINED);
        for (var listener : panel.getKeyListeners()) {
            listener.keyPressed(event);
        }
    }
}
