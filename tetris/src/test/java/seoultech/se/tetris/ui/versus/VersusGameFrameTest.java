package seoultech.se.tetris.ui.versus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.KeyEvent;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seoultech.se.tetris.config.PropertiesAppSettingsService;
import seoultech.se.tetris.config.PropertiesBattleKeyConfigService;
import seoultech.se.tetris.engine.Difficulty;
import seoultech.se.tetris.ui.game.PauseMenuPanel;
import seoultech.se.tetris.ui.settings.SettingsPanel;
import seoultech.se.tetris.versus.LocalMatch;
import seoultech.se.tetris.versus.MatchPhase;
import seoultech.se.tetris.versus.VersusMode;

class VersusGameFrameTest {

    @TempDir
    Path tempDir;

    /** 로컬 대전 중 Esc로 일시정지하면 타이머가 멈추고, 재개하면 다시 시작한다. */
    @Test
    void escPausesTimersAndShowsPauseMenuThenResumes() throws Exception {
        AtomicBoolean returned = new AtomicBoolean(false);
        VersusGameFrame[] holder = new VersusGameFrame[1];
        LocalMatch match = LocalMatch.create(VersusMode.TIME_LIMIT, Difficulty.DEFAULT, 1L);
        try {
            SwingUtilities.invokeAndWait(() -> {
                PropertiesBattleKeyConfigService keys = new PropertiesBattleKeyConfigService(
                        tempDir.resolve("battle-keys.properties"));
                VersusGameFrame frame = VersusGameFrame.launch(match, 1.0, false, keys,
                        () -> returned.set(true), result -> {},
                        () -> new PropertiesAppSettingsService(tempDir.resolve("settings.properties")));
                holder[0] = frame;

                assertEquals(MatchPhase.PLAYING, match.phase());
                assertTrue(frame.isEffectTimerRunning());
                assertTrue(frame.isClockTimerRunning());
                assertFalse(frame.getGlassPane().isVisible());

                // Esc 입력 → 일시정지 메뉴 표시 및 모든 타이머 정지
                pressKey(frame.panel(), KeyEvent.VK_ESCAPE);
                assertEquals(MatchPhase.PAUSED, match.phase());
                assertInstanceOf(PauseMenuPanel.class, frame.getGlassPane());
                assertTrue(frame.getGlassPane().isVisible());
                assertFalse(frame.isEffectTimerRunning());
                assertFalse(frame.isClockTimerRunning());

                // 일시정지 메뉴에서 Esc → 재개 및 타이머 재시작
                pressKey(frame.getGlassPane(), KeyEvent.VK_ESCAPE);
                assertEquals(MatchPhase.PLAYING, match.phase());
                assertFalse(frame.getGlassPane().isVisible());
                assertTrue(frame.isEffectTimerRunning());
                assertTrue(frame.isClockTimerRunning());
                assertFalse(returned.get());
            });
        } finally {
            disposeQuietly(holder[0]);
        }
    }

    /** 일시정지 메뉴에서 설정을 열면 키 설정이 제외되고, 저장 후 닫으면 일시정지 메뉴로 복귀한다. */
    @Test
    void settingsFromPauseHidesKeyEditingAndReturnsToPauseMenu() throws Exception {
        VersusGameFrame[] holder = new VersusGameFrame[1];
        LocalMatch match = LocalMatch.create(VersusMode.NORMAL, Difficulty.DEFAULT, 2L);
        try {
            SwingUtilities.invokeAndWait(() -> {
                PropertiesBattleKeyConfigService keys = new PropertiesBattleKeyConfigService(
                        tempDir.resolve("battle-keys.properties"));
                Path settingsPath = tempDir.resolve("app-settings.properties");
                VersusGameFrame frame = VersusGameFrame.launch(match, 1.0, false, keys,
                        () -> {}, result -> {},
                        () -> new PropertiesAppSettingsService(settingsPath));
                holder[0] = frame;

                frame.pauseAndShowMenu();
                assertEquals(MatchPhase.PAUSED, match.phase());

                frame.showSettings();
                assertInstanceOf(SettingsPanel.class, frame.getContentPane());
                SettingsPanel settingsPanel = (SettingsPanel) frame.getContentPane();
                assertFalse(frame.getGlassPane().isVisible());
                assertFalse(hasLabel(settingsPanel, "1인 조작 키"));
                assertFalse(hasLabel(settingsPanel, "대전 조작 키 (2인)"));

                // 색맹 모드 변경 후 저장 → 대전 화면 + 일시정지 메뉴로 복귀
                JCheckBox colorBlind = findComponent(settingsPanel, JCheckBox.class);
                colorBlind.setSelected(true);
                JButton saveButton = findButton(settingsPanel, "저장");
                saveButton.doClick();

                assertEquals(frame.panel(), frame.getContentPane());
                assertTrue(frame.getGlassPane().isVisible());
                assertEquals(MatchPhase.PAUSED, match.phase());
                assertFalse(frame.isEffectTimerRunning());
            });
        } finally {
            disposeQuietly(holder[0]);
        }
    }

    /** 시작 메뉴로 복귀하면 대전을 정리하고 콜백을 실행한다. */
    @Test
    void returnToMenuQuitsMatchAndInvokesCallback() throws Exception {
        AtomicBoolean returned = new AtomicBoolean(false);
        VersusGameFrame[] holder = new VersusGameFrame[1];
        LocalMatch match = LocalMatch.create(VersusMode.NORMAL, Difficulty.DEFAULT, 3L);
        try {
            SwingUtilities.invokeAndWait(() -> {
                PropertiesBattleKeyConfigService keys = new PropertiesBattleKeyConfigService(
                        tempDir.resolve("battle-keys.properties"));
                VersusGameFrame frame = VersusGameFrame.launch(match, 1.0, false, keys,
                        () -> returned.set(true), result -> {},
                        () -> new PropertiesAppSettingsService(tempDir.resolve("settings.properties")));
                holder[0] = frame;

                frame.returnToMenu();
                assertEquals(MatchPhase.ENDED, match.phase());
                assertFalse(frame.isEffectTimerRunning());
            });
            SwingUtilities.invokeAndWait(() -> assertTrue(returned.get()));
        } finally {
            disposeQuietly(holder[0]);
        }
    }

    private static void pressKey(Component target, int keyCode) {
        KeyEvent event = new KeyEvent(target, KeyEvent.KEY_PRESSED, System.currentTimeMillis(),
                0, keyCode, KeyEvent.CHAR_UNDEFINED);
        for (var listener : target.getKeyListeners()) {
            listener.keyPressed(event);
        }
    }

    private static boolean hasLabel(Container root, String text) {
        for (Component child : root.getComponents()) {
            if (child instanceof JLabel label && text.equals(label.getText())) {
                return true;
            }
            if (child instanceof Container container && hasLabel(container, text)) {
                return true;
            }
        }
        return false;
    }

    private static <T> T findComponent(Container root, Class<T> type) {
        for (Component child : root.getComponents()) {
            if (type.isInstance(child)) {
                return type.cast(child);
            }
            if (child instanceof Container container) {
                T found = findComponent(container, type);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static JButton findButton(Container root, String text) {
        for (Component child : root.getComponents()) {
            if (child instanceof JButton button && text.equals(button.getText())) {
                return button;
            }
            if (child instanceof Container container) {
                JButton found = findButton(container, text);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static void disposeQuietly(VersusGameFrame frame) throws Exception {
        if (frame != null) {
            SwingUtilities.invokeAndWait(frame::dispose);
        }
    }
}
