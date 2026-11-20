package seoultech.se.tetris.ui.versus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.KeyEvent;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seoultech.se.tetris.component.controller.GameController;
import seoultech.se.tetris.config.BattleKeyConfigService.BattleAction;
import seoultech.se.tetris.config.BattleKeyConfigService.BattlePlayer;
import seoultech.se.tetris.config.BattleKeyConfigService.BattleKeySlot;
import seoultech.se.tetris.config.PropertiesBattleKeyConfigService;
import seoultech.se.tetris.engine.Difficulty;
import seoultech.se.tetris.versus.LocalMatch;
import seoultech.se.tetris.versus.Side;
import seoultech.se.tetris.versus.VersusMode;

class VersusKeyMapTest {

    @TempDir
    Path dir;

    /** 저장 파일이 없으므로 기본 2인 키 설정(P1: WASD + Shift, P2: 방향키 + Ctrl)이다. */
    private PropertiesBattleKeyConfigService keyConfig;
    private VersusKeyMap keyMap;
    private final Map<Side, List<String>> calls = new EnumMap<>(Side.class);

    @BeforeEach
    void setUp() {
        keyConfig = new PropertiesBattleKeyConfigService(dir.resolve("battle_keybindings.properties"));
        keyMap = new VersusKeyMap(keyConfig);
        for (Side side : Side.values()) {
            calls.put(side, new ArrayList<>());
        }
    }

    private Optional<GameController> recording(Side side) {
        List<String> log = calls.get(side);
        return Optional.of(new GameController() {
            @Override public void moveLeft() { log.add("left"); }
            @Override public void moveRight() { log.add("right"); }
            @Override public void moveDown() { log.add("down"); }
            @Override public void dropToBottom() { log.add("drop"); }
            @Override public void rotate() { log.add("rotate"); }
            @Override public void rotateCounterclockwise() { log.add("rotateCcw"); }
            @Override public void togglePause() { log.add("togglePause"); }
            @Override public void pauseGame() { log.add("pause"); }
            @Override public void resumeGame() { log.add("resume"); }
            @Override public void restartGame() { log.add("restart"); }
            @Override public void exitGame() { log.add("exit"); }
        });
    }

    @Test
    void 설정의_모든_키가_플레이어와_동작으로_연결된다() {
        keyConfig.getAllBindings().forEach((slot, keyCode) ->
                assertEquals(Optional.of(slot), keyMap.slotOf(keyCode), slot.toString()));
    }

    @Test
    void P1_키는_P1에게만_전달된다() {
        keyMap.dispatch(KeyEvent.VK_A, this::recording);
        keyMap.dispatch(KeyEvent.VK_D, this::recording);
        keyMap.dispatch(KeyEvent.VK_S, this::recording);
        keyMap.dispatch(KeyEvent.VK_SHIFT, this::recording);
        keyMap.dispatch(KeyEvent.VK_W, this::recording);

        assertEquals(List.of("left", "right", "down", "drop", "rotate"), calls.get(Side.P1));
        assertTrue(calls.get(Side.P2).isEmpty());
    }

    @Test
    void P2_키는_P2에게만_전달된다() {
        keyMap.dispatch(KeyEvent.VK_LEFT, this::recording);
        keyMap.dispatch(KeyEvent.VK_RIGHT, this::recording);
        keyMap.dispatch(KeyEvent.VK_DOWN, this::recording);
        keyMap.dispatch(KeyEvent.VK_CONTROL, this::recording);
        keyMap.dispatch(KeyEvent.VK_UP, this::recording);

        assertEquals(List.of("left", "right", "down", "drop", "rotate"), calls.get(Side.P2));
        assertTrue(calls.get(Side.P1).isEmpty());
    }

    @Test
    void 대전_키가_아니면_처리하지_않는다() {
        assertFalse(keyMap.dispatch(KeyEvent.VK_Q, this::recording));
        assertTrue(keyMap.slotOf(KeyEvent.VK_Q).isEmpty());
        assertTrue(calls.get(Side.P1).isEmpty());
        assertTrue(calls.get(Side.P2).isEmpty());
    }

    @Test
    void 조작할_수_없는_자리의_키는_처리하되_아무것도_하지_않는다() {
        assertTrue(keyMap.dispatch(KeyEvent.VK_A, side -> Optional.empty()));
    }

    @Test
    void 바꾼_키_설정을_따른다() {
        Map<BattleKeySlot, Integer> bindings = new HashMap<>(keyConfig.getAllBindings());
        bindings.put(new BattleKeySlot(BattlePlayer.P1, BattleAction.MOVE_LEFT), KeyEvent.VK_J);
        keyConfig.saveBindings(bindings);

        VersusKeyMap changed = new VersusKeyMap(keyConfig);
        changed.dispatch(KeyEvent.VK_J, this::recording);

        assertEquals(List.of("left"), calls.get(Side.P1));
        assertTrue(changed.slotOf(KeyEvent.VK_A).isEmpty());
    }

    @Test
    void 키_설정의_플레이어는_대전의_자리와_같다() {
        assertEquals(Side.P1, VersusKeyMap.sideOf(BattlePlayer.P1));
        assertEquals(Side.P2, VersusKeyMap.sideOf(BattlePlayer.P2));
        assertThrows(NullPointerException.class, () -> VersusKeyMap.sideOf(null));
    }

    @Test
    void 실제_대전에서_P1_키는_P1_블럭만_움직인다() {
        LocalMatch match = LocalMatch.create(VersusMode.NORMAL, Difficulty.NORMAL, 42L);
        match.begin();
        int p1Before = match.state(Side.P1).getCurrentX();
        int p2Before = match.state(Side.P2).getCurrentX();

        keyMap.dispatch(KeyEvent.VK_A, match::controller);

        assertEquals(p1Before - 1, match.state(Side.P1).getCurrentX());
        assertEquals(p2Before, match.state(Side.P2).getCurrentX());
    }

    @Test
    void 대전이_시작되기_전의_입력은_무시된다() {
        LocalMatch match = LocalMatch.create(VersusMode.NORMAL, Difficulty.NORMAL, 42L);
        int before = match.state(Side.P2).getCurrentX();

        keyMap.dispatch(KeyEvent.VK_RIGHT, match::controller);

        assertEquals(before, match.state(Side.P2).getCurrentX());
    }

    @Test
    void 키_설정이_없으면_만들_수_없다() {
        assertThrows(NullPointerException.class, () -> new VersusKeyMap(null));
    }
}
