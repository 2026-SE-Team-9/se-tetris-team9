package seoultech.se.tetris.ui.p2p;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.KeyEvent;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JRootPane;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seoultech.se.tetris.component.controller.GameController;
import seoultech.se.tetris.component.listener.GameEventListener;
import seoultech.se.tetris.component.model.GameState;
import seoultech.se.tetris.config.KeyConfigService.GameAction;
import seoultech.se.tetris.config.PropertiesKeyConfigService;
import seoultech.se.tetris.engine.Difficulty;
import seoultech.se.tetris.engine.TetrisEngine;
import seoultech.se.tetris.versus.MatchListener;
import seoultech.se.tetris.versus.MatchPhase;
import seoultech.se.tetris.versus.MatchResult;
import seoultech.se.tetris.versus.Side;
import seoultech.se.tetris.versus.VersusMatch;
import seoultech.se.tetris.versus.VersusMode;
import seoultech.se.tetris.versus.p2p.ConnectionState;
import seoultech.se.tetris.versus.p2p.P2PSession;
import seoultech.se.tetris.versus.p2p.Role;

class P2PGameSessionTest {

    private static final class StubSession implements P2PSession {
        boolean left;
        final java.util.List<SessionListener> listeners = new java.util.ArrayList<>();

        @Override public Role role() { return Role.SERVER; }
        @Override public VersusMode mode() { return VersusMode.NORMAL; }
        @Override public Difficulty difficulty() { return Difficulty.NORMAL; }
        @Override public void changeSettings(VersusMode mode, Difficulty difficulty) { }
        @Override public void setReady(boolean ready) { }
        @Override public boolean isReady() { return false; }
        @Override public boolean isOpponentReady() { return false; }
        @Override public Optional<VersusMatch> match() { return Optional.empty(); }
        @Override public ConnectionState connectionState() { return ConnectionState.NORMAL; }
        @Override public int roundTripMillis() { return 15; }
        @Override public void leave() { left = true; }
        @Override public void addListener(SessionListener listener) { listeners.add(listener); }
        @Override public void removeListener(SessionListener listener) { listeners.remove(listener); }
    }

    private static final class StubMatch implements VersusMatch {
        final TetrisEngine p1 = new TetrisEngine(Difficulty.NORMAL);
        final TetrisEngine p2 = new TetrisEngine(Difficulty.NORMAL);
        MatchPhase phase = MatchPhase.READY;
        boolean quitCalled;

        @Override public VersusMode mode() { return VersusMode.NORMAL; }
        @Override public MatchPhase phase() { return phase; }
        @Override public GameState state(Side side) { return side == Side.P1 ? p1 : p2; }
        @Override public Optional<GameController> controller(Side side) {
            return side == Side.P1 ? Optional.of(p1) : Optional.empty();
        }
        @Override public Optional<MatchResult> result() { return Optional.empty(); }
        @Override public void addListener(MatchListener listener) { }
        @Override public void removeListener(MatchListener listener) { }
        @Override public void addGameListener(Side side, GameEventListener listener) {
            (side == Side.P1 ? p1 : p2).addListener(listener);
        }
        @Override public void removeGameListener(Side side, GameEventListener listener) {
            (side == Side.P1 ? p1 : p2).removeListener(listener);
        }
        @Override public void begin() { phase = MatchPhase.PLAYING; }
        @Override public boolean supportsPause() { return false; }
        @Override public void pause() { throw new UnsupportedOperationException(); }
        @Override public void resume() { }
        @Override public void timeUp() { }
        @Override public void quit() {
            quitCalled = true;
            phase = MatchPhase.ENDED;
        }
    }

    @Test
    void usesSinglePlayerKeyConfigAndExitMenuWithoutPausing(@TempDir Path tempDir) {
        StubSession p2pSession = new StubSession();
        StubMatch match = new StubMatch();
        PropertiesKeyConfigService keyConfig = new PropertiesKeyConfigService(tempDir.resolve("keys.properties"));
        JRootPane rootPane = new JRootPane();
        AtomicBoolean disposed = new AtomicBoolean();
        AtomicBoolean returned = new AtomicBoolean();

        P2PGameSession session = new P2PGameSession(p2pSession, match, 1.0, false,
                keyConfig, rootPane, () -> disposed.set(true), () -> returned.set(true),
                Duration.ZERO, System::currentTimeMillis);
        session.start();

        assertEquals(MatchPhase.PLAYING, match.phase());
        assertEquals("연결 상태: 정상 · 15ms", session.panel().connectionText());

        // 1인용 키 설정으로 왼쪽 이동
        int beforeX = match.p1.getCurrentX();
        int leftKey = keyConfig.getKeyCodes(GameAction.MOVE_LEFT).get(0);
        session.handleKey(leftKey);
        assertEquals(beforeX - 1, match.p1.getCurrentX());

        // Esc를 누르면 일시정지 없이 나가기 메뉴가 열린다
        session.handleKey(KeyEvent.VK_ESCAPE);
        assertEquals(MatchPhase.PLAYING, match.phase());
        assertTrue(rootPane.getGlassPane().isVisible());

        // 계속하기(Esc)로 닫기
        P2PExitMenuPanel exitMenu = (P2PExitMenuPanel) rootPane.getGlassPane();
        exitMenu.handleKey(KeyEvent.VK_ESCAPE);
        assertFalse(rootPane.getGlassPane().isVisible());

        // 다시 열어 게임 나가기 선택
        session.handleKey(KeyEvent.VK_ESCAPE);
        exitMenu.handleKey(KeyEvent.VK_DOWN);
        exitMenu.handleKey(KeyEvent.VK_ENTER);
        assertTrue(match.quitCalled);
        assertTrue(p2pSession.left);
        assertTrue(disposed.get());
    }
}
