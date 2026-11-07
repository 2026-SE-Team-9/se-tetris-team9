package seoultech.se.tetris.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.KeyEvent;
import java.nio.file.Path;
import java.util.OptionalInt;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seoultech.se.tetris.config.BattleKeyConfigService.BattleAction;
import seoultech.se.tetris.config.BattleKeyConfigService.BattleKeySlot;
import seoultech.se.tetris.config.BattleKeyConfigService.BattlePlayer;

class BattleKeyBindingEditorTest {

    @TempDir
    Path tempDir;

    private PropertiesBattleKeyConfigService config;
    private AtomicBoolean onSavedCalled;
    private BattleKeyBindingEditor editor;

    @BeforeEach
    void setUp() {
        config = new PropertiesBattleKeyConfigService(tempDir.resolve("keys.properties"));
        onSavedCalled = new AtomicBoolean(false);
        editor = new BattleKeyBindingEditor(config, () -> onSavedCalled.set(true));
    }

    @Test
    void initialState_canSaveIsTrue() {
        assertTrue(editor.canSave());
        assertEquals(OptionalInt.of(KeyEvent.VK_A),
                editor.getDraftBinding(BattlePlayer.P1, BattleAction.MOVE_LEFT));
    }

    @Test
    void captureKey_clearsPreviousHolderWhenKeyIsAlreadyUsed() {
        // 1P의 MOVE_LEFT(A)를 2P의 MOVE_LEFT(←) 키로 변경 시도
        BattleKeySlot p1Left = new BattleKeySlot(BattlePlayer.P1, BattleAction.MOVE_LEFT);
        BattleKeySlot p2Left = new BattleKeySlot(BattlePlayer.P2, BattleAction.MOVE_LEFT);

        assertEquals(OptionalInt.of(KeyEvent.VK_LEFT), editor.getDraftBinding(p2Left));

        editor.captureKey(p1Left, KeyEvent.VK_LEFT);

        // 1P 왼쪽 이동에 VK_LEFT가 들어가고, 기존 점유자였던 2P 왼쪽 이동은 비워져야 함
        assertEquals(OptionalInt.of(KeyEvent.VK_LEFT), editor.getDraftBinding(p1Left));
        assertTrue(editor.getDraftBinding(p2Left).isEmpty());

        // 빈 슬롯이 생겼으므로 canSave는 false가 되어야 함
        assertFalse(editor.canSave());
    }

    @Test
    void captureKey_escapeKeyClearsSlot() {
        BattleKeySlot p1Rotate = new BattleKeySlot(BattlePlayer.P1, BattleAction.ROTATE);

        editor.captureKey(p1Rotate, KeyEvent.VK_ESCAPE);

        assertTrue(editor.getDraftBinding(p1Rotate).isEmpty());
        assertFalse(editor.canSave());
    }

    @Test
    void save_throwsWhenCannotSave() {
        BattleKeySlot p1Rotate = new BattleKeySlot(BattlePlayer.P1, BattleAction.ROTATE);
        editor.captureKey(p1Rotate, KeyEvent.VK_ESCAPE);

        assertThrows(IllegalStateException.class, () -> editor.save());
        assertFalse(onSavedCalled.get());
    }

    @Test
    void save_persistsAndCallsCallback() {
        BattleKeySlot p1Left = new BattleKeySlot(BattlePlayer.P1, BattleAction.MOVE_LEFT);
        editor.captureKey(p1Left, KeyEvent.VK_Q); // 미사용 키

        assertTrue(editor.canSave());
        editor.save();

        assertTrue(onSavedCalled.get());
        assertEquals(KeyEvent.VK_Q, config.getKey(p1Left));
    }

    @Test
    void restoreDefaults_resetsDraftsToDefault() {
        BattleKeySlot p1Left = new BattleKeySlot(BattlePlayer.P1, BattleAction.MOVE_LEFT);
        editor.captureKey(p1Left, KeyEvent.VK_ESCAPE);
        assertFalse(editor.canSave());

        editor.restoreDefaults();

        assertTrue(editor.canSave());
        assertEquals(OptionalInt.of(KeyEvent.VK_A), editor.getDraftBinding(p1Left));
    }

    @Test
    void resetChanges_discardsDraftChanges() {
        BattleKeySlot p1Left = new BattleKeySlot(BattlePlayer.P1, BattleAction.MOVE_LEFT);
        editor.captureKey(p1Left, KeyEvent.VK_ESCAPE);
        assertFalse(editor.canSave());

        editor.resetChanges();

        assertTrue(editor.canSave());
        assertEquals(OptionalInt.of(KeyEvent.VK_A), editor.getDraftBinding(p1Left));
    }
}
