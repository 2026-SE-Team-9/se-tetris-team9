package seoultech.se.tetris.config;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import seoultech.se.tetris.config.KeyConfigService.GameAction;
import seoultech.se.tetris.config.KeyConfigService.KeyBinding;

class KeyBindingEditorTest {
    @TempDir
    Path tempDir;

    /** 모든 설정 기본값은 저장 전 임시 키에만 적용되고 Esc 기본 키도 복원한다. */
    @Test
    void restoresDefaultDraftWithoutSaving() {
        var config = new PropertiesKeyConfigService(tempDir.resolve("keys.properties"));
        var editor = new KeyBindingEditor(config, () -> {});
        editor.captureMainKey(GameAction.MOVE_LEFT, KeyEvent.VK_A);
        editor.restoreDefaults();
        assertEquals(KeyEvent.VK_LEFT,
                editor.getDraftBinding(GameAction.MOVE_LEFT).mainKeyCode().getAsInt());
        assertEquals(KeyEvent.VK_ESCAPE,
                editor.getDraftBinding(GameAction.PAUSE_MENU).mainKeyCode().getAsInt());
        assertEquals(KeyEvent.VK_LEFT, config.getKeyBinding(GameAction.MOVE_LEFT).mainKeyCode());
    }

    /** 일시정지 키를 바꾸려는 요청은 거부하고 다른 임시 키는 유지한다. */
    @Test
    void pauseKeyIsFixedWithoutResettingOtherDraftBindings() {
        var config = new PropertiesKeyConfigService(tempDir.resolve("keys.properties"));
        var editor = new KeyBindingEditor(config, () -> {});
        editor.captureMainKey(GameAction.MOVE_LEFT, KeyEvent.VK_A);
        assertThrows(IllegalArgumentException.class,
                () -> editor.captureMainKey(GameAction.PAUSE_MENU, KeyEvent.VK_P));
        assertEquals(OptionalInt.of(KeyEvent.VK_ESCAPE),
                editor.getDraftBinding(GameAction.PAUSE_MENU).mainKeyCode());
        assertEquals(OptionalInt.of(KeyEvent.VK_A),
                editor.getDraftBinding(GameAction.MOVE_LEFT).mainKeyCode());
        assertTrue(editor.canSave());
    }

    /** Esc로 임시 메인 키를 비우면 저장을 막고 실제 설정은 유지한다. */
    @Test
    void escapeClearsMainAndBlocksSaveWithoutChangingStoredBinding() {
        var config = new PropertiesKeyConfigService(tempDir.resolve("keys.properties"));
        var editor = new KeyBindingEditor(config, () -> {});
        editor.captureMainKey(GameAction.ROTATE, KeyEvent.VK_ESCAPE);
        assertEquals(OptionalInt.empty(), editor.getDraftBinding(GameAction.ROTATE).mainKeyCode());
        assertEquals(OptionalInt.of(KeyEvent.VK_X), editor.getDraftBinding(GameAction.ROTATE).subKeyCode());
        assertFalse(editor.canSave());
        assertThrows(IllegalStateException.class, editor::save);
        assertEquals(new KeyBinding(KeyEvent.VK_UP, KeyEvent.VK_X), config.getKeyBinding(GameAction.ROTATE));
    }

    /** Esc로 임시 서브키를 비운 뒤 저장하면 메인 키만 복원된다. */
    @Test
    void escapeClearsSubAndPersistsMainOnly() {
        Path file = tempDir.resolve("keys.properties");
        var config = new PropertiesKeyConfigService(file);
        var editor = new KeyBindingEditor(config, () -> {});
        editor.captureSubKey(GameAction.ROTATE, KeyEvent.VK_ESCAPE);
        assertEquals(OptionalInt.empty(), editor.getDraftBinding(GameAction.ROTATE).subKeyCode());
        assertTrue(editor.canSave());
        editor.save();
        var restored = new PropertiesKeyConfigService(file);
        restored.load();
        assertEquals(new KeyBinding(KeyEvent.VK_UP), restored.getKeyBinding(GameAction.ROTATE));
    }

    /** 빈 메인 키를 다시 지정하고 메인·서브키의 위치를 바꿔 저장할 수 있다. */
    @Test
    void mainCanBeFilledAgainAndMainAndSubCanExchangePlaces() {
        var config = new PropertiesKeyConfigService(tempDir.resolve("keys.properties"));
        var editor = new KeyBindingEditor(config, () -> {});
        editor.captureMainKey(GameAction.ROTATE, KeyEvent.VK_ESCAPE);
        editor.captureMainKey(GameAction.ROTATE, KeyEvent.VK_X);
        editor.captureSubKey(GameAction.ROTATE, KeyEvent.VK_UP);
        assertTrue(editor.canSave());
        editor.save();
        assertEquals(new KeyBinding(KeyEvent.VK_X, KeyEvent.VK_UP), config.getKeyBinding(GameAction.ROTATE));
    }

    /** 메인·서브키를 각각 편집할 때 다른 칸을 유지하고 저장 전에는 실제 설정을 바꾸지 않는다. */
    @Test
    void editsMainAndSubKeysIndependently() {
        var config = new PropertiesKeyConfigService(tempDir.resolve("keys.properties"));
        var original = config.getKeyBinding(GameAction.ROTATE);
        var editor = new KeyBindingEditor(config, () -> {});

        editor.captureMainKey(GameAction.ROTATE, KeyEvent.VK_R);
        assertEquals(OptionalInt.of(KeyEvent.VK_R), editor.getDraftBinding(GameAction.ROTATE).mainKeyCode());
        assertEquals(OptionalInt.of(KeyEvent.VK_X), editor.getDraftBinding(GameAction.ROTATE).subKeyCode());

        editor.captureSubKey(GameAction.ROTATE, KeyEvent.VK_N);
        assertEquals(OptionalInt.of(KeyEvent.VK_R), editor.getDraftBinding(GameAction.ROTATE).mainKeyCode());
        assertEquals(OptionalInt.of(KeyEvent.VK_N), editor.getDraftBinding(GameAction.ROTATE).subKeyCode());

        editor.captureSubKey(GameAction.ROTATE, KeyEvent.VK_ESCAPE);
        assertEquals(OptionalInt.of(KeyEvent.VK_R), editor.getDraftBinding(GameAction.ROTATE).mainKeyCode());
        assertEquals(OptionalInt.empty(), editor.getDraftBinding(GameAction.ROTATE).subKeyCode());

        editor.captureSubKey(GameAction.ROTATE, KeyEvent.VK_UP);
        assertEquals(original, config.getKeyBinding(GameAction.ROTATE));
        editor.save();
        assertEquals(new KeyBinding(KeyEvent.VK_R, KeyEvent.VK_UP), config.getKeyBinding(GameAction.ROTATE));
    }

    /** 새 편집기는 저장된 설정으로 시작하며 이전 편집기의 미저장 변경을 이어받지 않는다. */
    @Test
    void newEditorStartsFromSavedBindingsWithoutPreviousUnsavedChanges() throws IOException {
        Path file = tempDir.resolve("keys.properties");
        var config = new PropertiesKeyConfigService(file);
        var initialEditor = new KeyBindingEditor(config, () -> {});
        initialEditor.captureMainKey(GameAction.ROTATE, KeyEvent.VK_R);
        initialEditor.save();
        byte[] savedFile = Files.readAllBytes(file);
        int[] savedCallbacks = {0};

        var savedDraft = initialEditor.getDraftBindings();
        // 저장하지 않고 편집을 끝낸 뒤 새 편집기로 설정 화면을 다시 여는 흐름을 확인한다.
        {
            var editor = new KeyBindingEditor(config, () -> savedCallbacks[0]++);
            editor.captureMainKey(GameAction.ROTATE, KeyEvent.VK_LEFT);
            editor.captureSubKey(GameAction.ROTATE, KeyEvent.VK_N);
            editor.captureMainKey(GameAction.MOVE_RIGHT, KeyEvent.VK_ESCAPE);
            assertFalse(editor.canSave());
            assertNotEquals(savedDraft, editor.getDraftBindings());
        }
        var reopened = new KeyBindingEditor(config, () -> savedCallbacks[0]++);
        assertEquals(savedDraft, reopened.getDraftBindings());
        assertTrue(reopened.canSave());
        assertEquals(0, savedCallbacks[0]);
        assertArrayEquals(savedFile, Files.readAllBytes(file));
        assertEquals(new KeyBinding(KeyEvent.VK_R, KeyEvent.VK_X), config.getKeyBinding(GameAction.ROTATE));
    }

    /** 저장 실패 시 실제 설정과 콜백을 유지하고 임시 편집 내용을 보존하여 재시도할 수 있다. */
    @Test
    void saveFailureRestoresBothKeysAndKeepsEditsForRetry() throws IOException {
        for (KeyBinding replacement : List.of(new KeyBinding(KeyEvent.VK_R),
                new KeyBinding(KeyEvent.VK_X, KeyEvent.VK_UP))) {
            Path blockedParent = tempDir.resolve("blocked-" + replacement.mainKeyCode());
            Files.writeString(blockedParent, "A file prevents creating the settings directory");
            Path file = blockedParent.resolve("keys.properties");
            var config = new PropertiesKeyConfigService(file);
            KeyBinding previous = config.getKeyBinding(GameAction.ROTATE);
            KeyBinding other = config.getKeyBinding(GameAction.MOVE_LEFT);
            int[] savedCallbacks = {0};
            var editor = new KeyBindingEditor(config, () -> savedCallbacks[0]++);
            editor.captureMainKey(GameAction.ROTATE, replacement.mainKeyCode());
            editor.captureSubKey(GameAction.ROTATE, replacement.subKeyCode().orElse(KeyEvent.VK_ESCAPE));

            assertThrows(UncheckedIOException.class, editor::save);
            assertEquals(0, savedCallbacks[0]);
            assertEquals(previous, config.getKeyBinding(GameAction.ROTATE));
            assertEquals(other, config.getKeyBinding(GameAction.MOVE_LEFT));
            assertEquals(OptionalInt.of(replacement.mainKeyCode()), editor.getDraftBinding(GameAction.ROTATE).mainKeyCode());
            assertEquals(replacement.subKeyCode(), editor.getDraftBinding(GameAction.ROTATE).subKeyCode());
            assertTrue(editor.canSave());

            Files.delete(blockedParent);
            editor.save();
            assertEquals(1, savedCallbacks[0]);
            var restored = new PropertiesKeyConfigService(file);
            restored.load();
            assertEquals(replacement, restored.getKeyBinding(GameAction.ROTATE));
        }
    }

    /** 액션 간 키 이동은 임시 설정에만 반영하고 모든 메인이 채워지면 전체 설정을 함께 저장한다. */
    @Test
    void movingKeysBetweenActionsChangesOnlyDraftAndSavesAllActionsTogether() {
        Path file = tempDir.resolve("keys.properties");
        var config = new PropertiesKeyConfigService(file);
        var initialEditor = new KeyBindingEditor(config, () -> {});
        initialEditor.captureMainKey(GameAction.MOVE_LEFT, KeyEvent.VK_N);
        initialEditor.save();
        var editor = new KeyBindingEditor(config, () -> {});

        editor.captureMainKey(GameAction.ROTATE, KeyEvent.VK_N);
        assertEquals(OptionalInt.empty(), editor.getDraftBinding(GameAction.MOVE_LEFT).mainKeyCode());
        assertEquals(OptionalInt.of(KeyEvent.VK_N), editor.getDraftBinding(GameAction.ROTATE).mainKeyCode());
        assertFalse(editor.canSave());
        assertThrows(IllegalStateException.class, editor::save);
        assertEquals(KeyEvent.VK_N, config.getKeyBinding(GameAction.MOVE_LEFT).mainKeyCode());
        assertEquals(KeyEvent.VK_UP, config.getKeyBinding(GameAction.ROTATE).mainKeyCode());

        editor.captureMainKey(GameAction.MOVE_LEFT, KeyEvent.VK_UP);
        editor.captureSubKey(GameAction.MOVE_RIGHT, KeyEvent.VK_X);
        assertEquals(OptionalInt.empty(), editor.getDraftBinding(GameAction.ROTATE).subKeyCode());
        assertTrue(editor.canSave());
        editor.save();
        var restored = new PropertiesKeyConfigService(file);
        restored.load();
        assertEquals(new KeyBinding(KeyEvent.VK_N), restored.getKeyBinding(GameAction.ROTATE));
        assertEquals(new KeyBinding(KeyEvent.VK_UP), restored.getKeyBinding(GameAction.MOVE_LEFT));
        assertEquals(new KeyBinding(KeyEvent.VK_RIGHT, KeyEvent.VK_X),
                restored.getKeyBinding(GameAction.MOVE_RIGHT));
    }

    /** resetChanges는 저장된 설정으로 임시 변경을 되돌리고 외부 스냅샷의 수정을 막는다. */
    @Test
    void cancelDiscardsWholeDraftAndSnapshotsCannotModifyEditor() {
        Path file = tempDir.resolve("keys.properties");
        var config = new PropertiesKeyConfigService(file);
        var initialEditor = new KeyBindingEditor(config, () -> {});
        initialEditor.captureMainKey(GameAction.ROTATE, KeyEvent.VK_R);
        initialEditor.save();
        int[] savedCallbacks = {0};
        var editor = new KeyBindingEditor(config, () -> savedCallbacks[0]++);
        var original = editor.getDraftBindings();
        assertThrows(UnsupportedOperationException.class, () -> original.clear());

        editor.captureMainKey(GameAction.ROTATE, KeyEvent.VK_LEFT);
        editor.captureMainKey(GameAction.MOVE_RIGHT, KeyEvent.VK_ESCAPE);
        assertFalse(editor.canSave());
        assertEquals(OptionalInt.of(KeyEvent.VK_R), original.get(GameAction.ROTATE).mainKeyCode());
        editor.resetChanges();
        assertEquals(original, editor.getDraftBindings());
        assertTrue(editor.canSave());
        assertEquals(0, savedCallbacks[0]);
        var restored = new PropertiesKeyConfigService(file);
        restored.load();
        assertEquals(new KeyBinding(KeyEvent.VK_R, KeyEvent.VK_X), restored.getKeyBinding(GameAction.ROTATE));
    }

    /** 키 이동으로 메인이 비거나 잘못된 코드를 입력해도 실제 설정은 바뀌지 않는다. */
    @Test
    void duplicateOrInvalidKeysCannotChangeStoredBinding() {
        var config = new PropertiesKeyConfigService(tempDir.resolve("keys.properties"));
        var editor = new KeyBindingEditor(config, () -> {});
        editor.captureSubKey(GameAction.ROTATE, KeyEvent.VK_UP);
        assertFalse(editor.canSave());
        assertThrows(IllegalStateException.class, editor::save);
        editor.captureSubKey(GameAction.ROTATE, KeyEvent.VK_Z);
        assertFalse(editor.canSave());
        assertThrows(IllegalStateException.class, editor::save);
        assertThrows(IllegalArgumentException.class, () -> editor.captureMainKey(GameAction.ROTATE, KeyEvent.VK_UNDEFINED));
        assertEquals(new KeyBinding(KeyEvent.VK_UP, KeyEvent.VK_X), config.getKeyBinding(GameAction.ROTATE));
    }
}
