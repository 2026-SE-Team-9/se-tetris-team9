package seoultech.se.tetris.ui.versus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.KeyEvent;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JRootPane;
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

    private static final VersusGameSession.WindowHost NO_OP_HOST = new VersusGameSession.WindowHost() {
        @Override public void packAndCenter() { }
        @Override public void disposeWindow() { }
        @Override public boolean confirmQuit() { return true; }
    };

    /** 로컬 대전 중 Esc로 일시정지하면 타이머가 멈추고, 재개하면 다시 시작한다. */
    @Test
    void escPausesTimersAndShowsPauseMenuThenResumes() throws Exception {
        AtomicBoolean returned = new AtomicBoolean(false);
        VersusGameSession[] holder = new VersusGameSession[1];
        LocalMatch match = LocalMatch.create(VersusMode.TIME_LIMIT, Difficulty.DEFAULT, 1L);
        try {
            SwingUtilities.invokeAndWait(() -> {
                PropertiesBattleKeyConfigService keys = new PropertiesBattleKeyConfigService(
                        tempDir.resolve("battle-keys.properties"));
                JRootPane rootPane = new JRootPane();
                VersusGameSession session = new VersusGameSession(match, 1.0, false, keys, rootPane,
                        NO_OP_HOST, () -> returned.set(true), result -> {},
                        () -> new PropertiesAppSettingsService(tempDir.resolve("settings.properties")),
                        Duration.ZERO, () -> 0L);
                holder[0] = session;
                session.start();

                assertEquals(MatchPhase.PLAYING, match.phase());
                assertTrue(session.isEffectTimerRunning());
                assertTrue(session.isClockTimerRunning());
                assertFalse(rootPane.getGlassPane().isVisible());

                // Esc 입력 → 일시정지 메뉴 표시 및 모든 타이머 정지
                pressKey(session.panel(), KeyEvent.VK_ESCAPE);
                assertEquals(MatchPhase.PAUSED, match.phase());
                assertInstanceOf(PauseMenuPanel.class, rootPane.getGlassPane());
                assertTrue(rootPane.getGlassPane().isVisible());
                // 로컬 대전의 마지막 항목은 타이틀이 아니라 시작 메뉴로 돌아간다
                assertEquals("시작 메뉴로 돌아가기",
                        ((PauseMenuPanel) rootPane.getGlassPane()).getItems().get(2).label());
                assertFalse(session.isEffectTimerRunning());
                assertFalse(session.isClockTimerRunning());

                // 일시정지 메뉴에서 Esc → 재개 및 타이머 재시작
                pressKey(rootPane.getGlassPane(), KeyEvent.VK_ESCAPE);
                assertEquals(MatchPhase.PLAYING, match.phase());
                assertFalse(rootPane.getGlassPane().isVisible());
                assertTrue(session.isEffectTimerRunning());
                assertTrue(session.isClockTimerRunning());
                assertFalse(returned.get());
            });
        } finally {
            releaseQuietly(holder[0]);
        }
    }

    /** 일시정지 메뉴에서 설정을 열면 로컬 대전 키 설정도 편집할 수 있고, 저장 후 닫으면 일시정지 메뉴로 복귀한다. */
    @Test
    void settingsFromPauseAllowsKeyEditingAndReturnsToPauseMenu() throws Exception {
        VersusGameSession[] holder = new VersusGameSession[1];
        LocalMatch match = LocalMatch.create(VersusMode.NORMAL, Difficulty.DEFAULT, 2L);
        try {
            SwingUtilities.invokeAndWait(() -> {
                PropertiesBattleKeyConfigService keys = new PropertiesBattleKeyConfigService(
                        tempDir.resolve("battle-keys.properties"));
                Path settingsPath = tempDir.resolve("app-settings.properties");
                JRootPane rootPane = new JRootPane();
                VersusGameSession session = new VersusGameSession(match, 1.0, false, keys, rootPane,
                        NO_OP_HOST, () -> {}, result -> {},
                        () -> new PropertiesAppSettingsService(settingsPath),
                        Duration.ZERO, () -> 0L);
                holder[0] = session;
                session.start();

                session.pauseAndShowMenu();
                assertEquals(MatchPhase.PAUSED, match.phase());

                session.showSettings();
                assertInstanceOf(SettingsPanel.class, rootPane.getContentPane());
                SettingsPanel settingsPanel = (SettingsPanel) rootPane.getContentPane();
                assertFalse(rootPane.getGlassPane().isVisible());
                assertTrue(hasLabel(settingsPanel, "로컬 대전 조작 키 (2인)"));

                // 로컬 대전 탭이 기본 선택되어 있으므로 바로 색맹 모드 변경 후 저장 → 대전 화면 + 일시정지 메뉴로 복귀
                JCheckBox battleColorBlind = findComponent(settingsPanel, JCheckBox.class, 1);
                assertTrue(battleColorBlind.isShowing() || battleColorBlind.getParent().isVisible());
                battleColorBlind.setSelected(true);
                JButton saveButton = findButton(settingsPanel, "저장");
                saveButton.doClick();

                PropertiesAppSettingsService reloaded = new PropertiesAppSettingsService(settingsPath);
                reloaded.load();
                assertTrue(reloaded.current().battleColorBlind());
                assertEquals(session.panel(), rootPane.getContentPane());
                assertTrue(rootPane.getGlassPane().isVisible());
                assertEquals(MatchPhase.PAUSED, match.phase());
                assertFalse(session.isEffectTimerRunning());
            });
        } finally {
            releaseQuietly(holder[0]);
        }
    }

    /** 시작 메뉴로 복귀하면 대전을 정리하고 콜백을 실행한다. */
    @Test
    void returnToMenuQuitsMatchAndInvokesCallback() throws Exception {
        AtomicBoolean returned = new AtomicBoolean(false);
        VersusGameSession[] holder = new VersusGameSession[1];
        LocalMatch match = LocalMatch.create(VersusMode.NORMAL, Difficulty.DEFAULT, 3L);
        try {
            SwingUtilities.invokeAndWait(() -> {
                PropertiesBattleKeyConfigService keys = new PropertiesBattleKeyConfigService(
                        tempDir.resolve("battle-keys.properties"));
                JRootPane rootPane = new JRootPane();
                VersusGameSession session = new VersusGameSession(match, 1.0, false, keys, rootPane,
                        NO_OP_HOST, () -> returned.set(true), result -> {},
                        () -> new PropertiesAppSettingsService(tempDir.resolve("settings.properties")),
                        Duration.ZERO, () -> 0L);
                holder[0] = session;
                session.start();

                session.returnToMenu();
                assertEquals(MatchPhase.ENDED, match.phase());
                assertFalse(session.isEffectTimerRunning());
            });
            SwingUtilities.invokeAndWait(() -> assertTrue(returned.get()));
        } finally {
            releaseQuietly(holder[0]);
        }
    }

    /** 3초 카운트다운 동안 READY를 유지하며 보드 오버레이를 갱신하고, 종료 시 PLAYING으로 전환한다. */
    @Test
    void startWithCountdownShowsOverlayAndBeginsAfterDuration() throws Exception {
        long[] now = {0L};
        VersusGameSession[] holder = new VersusGameSession[1];
        LocalMatch match = LocalMatch.create(VersusMode.TIME_LIMIT, Difficulty.DEFAULT, 4L);
        try {
            SwingUtilities.invokeAndWait(() -> {
                PropertiesBattleKeyConfigService keys = new PropertiesBattleKeyConfigService(
                        tempDir.resolve("battle-keys.properties"));
                JRootPane rootPane = new JRootPane();
                VersusGameSession session = new VersusGameSession(match, 1.0, false, keys, rootPane,
                        NO_OP_HOST, () -> {}, result -> {},
                        () -> new PropertiesAppSettingsService(tempDir.resolve("settings.properties")),
                        Duration.ofSeconds(3), () -> now[0]);
                holder[0] = session;
                session.start();

                assertEquals(MatchPhase.READY, match.phase());
                assertTrue(session.isCountdownTimerRunning());
                assertFalse(session.isEffectTimerRunning());
                assertFalse(session.isClockTimerRunning());
                assertEquals("3", session.panel().board(seoultech.se.tetris.versus.Side.P1).getOverlayText());
                assertEquals("3", session.panel().board(seoultech.se.tetris.versus.Side.P2).getOverlayText());

                now[0] = 1_000L;
                session.tickCountdown();
                assertEquals("2", session.panel().board(seoultech.se.tetris.versus.Side.P1).getOverlayText());
                assertEquals(MatchPhase.READY, match.phase());

                now[0] = 2_000L;
                session.tickCountdown();
                assertEquals("1", session.panel().board(seoultech.se.tetris.versus.Side.P1).getOverlayText());
                assertEquals(MatchPhase.READY, match.phase());

                now[0] = 3_000L;
                session.tickCountdown();
                assertEquals(MatchPhase.PLAYING, match.phase());
                assertFalse(session.isCountdownTimerRunning());
                assertTrue(session.isEffectTimerRunning());
                assertTrue(session.isClockTimerRunning());
                assertNull(session.panel().board(seoultech.se.tetris.versus.Side.P1).getOverlayText());
                assertNull(session.panel().board(seoultech.se.tetris.versus.Side.P2).getOverlayText());
            });
        } finally {
            releaseQuietly(holder[0]);
        }
    }

    /** 카운트다운 중 Esc를 누르면 일시정지 대신 나가기 확인을 요청하고, 취소하면 카운트다운을 이어간다. */
    @Test
    void escDuringCountdownRequestsQuitConfirmation() throws Exception {
        AtomicBoolean confirmResult = new AtomicBoolean(false);
        AtomicBoolean confirmCalled = new AtomicBoolean(false);
        AtomicBoolean returned = new AtomicBoolean(false);
        VersusGameSession.WindowHost host = new VersusGameSession.WindowHost() {
            @Override public void packAndCenter() { }
            @Override public void disposeWindow() { }
            @Override public boolean confirmQuit() {
                confirmCalled.set(true);
                return confirmResult.get();
            }
        };
        VersusGameSession[] holder = new VersusGameSession[1];
        LocalMatch match = LocalMatch.create(VersusMode.NORMAL, Difficulty.DEFAULT, 5L);
        try {
            SwingUtilities.invokeAndWait(() -> {
                PropertiesBattleKeyConfigService keys = new PropertiesBattleKeyConfigService(
                        tempDir.resolve("battle-keys.properties"));
                JRootPane rootPane = new JRootPane();
                VersusGameSession session = new VersusGameSession(match, 1.0, false, keys, rootPane,
                        host, () -> returned.set(true), result -> {},
                        () -> new PropertiesAppSettingsService(tempDir.resolve("settings.properties")),
                        Duration.ofSeconds(3), () -> 0L);
                holder[0] = session;
                session.start();

                pressKey(session.panel(), KeyEvent.VK_ESCAPE);
                assertTrue(confirmCalled.get());
                assertEquals(MatchPhase.READY, match.phase());
                assertFalse(rootPane.getGlassPane().isVisible());
                assertTrue(session.isCountdownTimerRunning());

                confirmResult.set(true);
                pressKey(session.panel(), KeyEvent.VK_ESCAPE);
                assertEquals(MatchPhase.ENDED, match.phase());
                assertFalse(session.isCountdownTimerRunning());
            });
            SwingUtilities.invokeAndWait(() -> assertTrue(returned.get()));
        } finally {
            releaseQuietly(holder[0]);
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

    private static <T> T findComponent(Container root, Class<T> type, int index) {
        int[] current = {0};
        return findComponent(root, type, index, current);
    }

    private static <T> T findComponent(Container root, Class<T> type, int targetIndex, int[] current) {
        for (Component child : root.getComponents()) {
            if (type.isInstance(child)) {
                if (current[0] == targetIndex) {
                    return type.cast(child);
                }
                current[0]++;
            }
            if (child instanceof Container container) {
                T found = findComponent(container, type, targetIndex, current);
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

    private static void releaseQuietly(VersusGameSession session) throws Exception {
        if (session != null) {
            SwingUtilities.invokeAndWait(session::release);
        }
    }
}
