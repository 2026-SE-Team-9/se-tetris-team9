package seoultech.se.tetris.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.EnumMap;
import java.util.OptionalInt;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import seoultech.se.tetris.config.KeyConfigService.GameAction;
import seoultech.se.tetris.config.KeyConfigService.KeyBinding;

class PropertiesKeyConfigServiceTest {
    @TempDir
    Path tempDir;

    /** 기본 조작 키를 제공하고, 변경한 설정을 기본값으로 되돌린다. */
    @Test
    void providesDefaultsAndResetsChangedKey() {
        PropertiesKeyConfigService service = new PropertiesKeyConfigService(tempDir.resolve("keys.properties"));

        assertEquals(KeyEvent.VK_LEFT, service.getKeyBinding(GameAction.MOVE_LEFT).mainKeyCode());
        assertEquals(KeyEvent.VK_RIGHT, service.getKeyBinding(GameAction.MOVE_RIGHT).mainKeyCode());
        assertEquals(KeyEvent.VK_DOWN, service.getKeyBinding(GameAction.SOFT_DROP).mainKeyCode());
        assertEquals(KeyEvent.VK_SPACE, service.getKeyBinding(GameAction.HARD_DROP).mainKeyCode());
        assertEquals(KeyEvent.VK_UP, service.getKeyBinding(GameAction.ROTATE).mainKeyCode());
        assertEquals(List.of(KeyEvent.VK_UP, KeyEvent.VK_X), service.getKeyCodes(GameAction.ROTATE));
        assertEquals(new KeyBinding(KeyEvent.VK_UP, KeyEvent.VK_X), service.getKeyBinding(GameAction.ROTATE));
        assertEquals(OptionalInt.empty(), service.getKeyBinding(GameAction.MOVE_LEFT).subKeyCode());
        assertEquals(KeyEvent.VK_ESCAPE, service.getKeyBinding(GameAction.PAUSE_MENU).mainKeyCode());
        assertEquals(KeyEvent.VK_Z, service.getKeyBinding(GameAction.ROTATE_COUNTERCLOCKWISE).mainKeyCode());

        saveBinding(service, GameAction.MOVE_LEFT, new KeyBinding(KeyEvent.VK_A, service.getKeyBinding(GameAction.MOVE_LEFT).subKeyCode()));
        assertEquals(KeyEvent.VK_A, service.getKeyBinding(GameAction.MOVE_LEFT).mainKeyCode());
        service.resetBindingsToDefault();
        assertEquals(KeyEvent.VK_LEFT, service.getKeyBinding(GameAction.MOVE_LEFT).mainKeyCode());
        assertThrows(IllegalArgumentException.class,
                () -> saveBinding(service, GameAction.MOVE_LEFT, new KeyBinding(KeyEvent.VK_UNDEFINED, service.getKeyBinding(GameAction.MOVE_LEFT).subKeyCode())));
        assertThrows(IllegalArgumentException.class,
                () -> saveBinding(service, GameAction.MOVE_LEFT, new KeyBinding(KeyEvent.VK_RIGHT, service.getKeyBinding(GameAction.MOVE_LEFT).subKeyCode())));
    }

    /** 전체 설정을 저장하고 복원하며, 저장 후 외부 맵을 변경해도 적용된 설정은 유지한다. */
    @Test
    void savesAndLoadsKeysFromFile() {
        Path file = tempDir.resolve("settings").resolve("keys.properties");
        PropertiesKeyConfigService original = new PropertiesKeyConfigService(file);
        var bindings = currentBindings(original);
        bindings.put(GameAction.MOVE_LEFT, new KeyBinding(KeyEvent.VK_A));
        bindings.put(GameAction.ROTATE, new KeyBinding(KeyEvent.VK_UP, KeyEvent.VK_R));
        original.saveBindings(bindings);
        bindings.put(GameAction.MOVE_LEFT, new KeyBinding(KeyEvent.VK_B));
        assertEquals(KeyEvent.VK_A, original.getKeyBinding(GameAction.MOVE_LEFT).mainKeyCode());

        PropertiesKeyConfigService restored = new PropertiesKeyConfigService(file);
        restored.load();

        assertEquals(KeyEvent.VK_A, restored.getKeyBinding(GameAction.MOVE_LEFT).mainKeyCode());
        assertEquals(KeyEvent.VK_ESCAPE, restored.getKeyBinding(GameAction.PAUSE_MENU).mainKeyCode());
        assertEquals(KeyEvent.VK_RIGHT, restored.getKeyBinding(GameAction.MOVE_RIGHT).mainKeyCode());
        assertEquals(List.of(KeyEvent.VK_UP, KeyEvent.VK_R), restored.getKeyCodes(GameAction.ROTATE));
        assertEquals(new KeyBinding(KeyEvent.VK_UP, KeyEvent.VK_R), restored.getKeyBinding(GameAction.ROTATE));
        assertEquals(KeyEvent.VK_Z, restored.getKeyBinding(GameAction.ROTATE_COUNTERCLOCKWISE).mainKeyCode());
    }

    /** 저장된 예전 일시정지 키는 Esc로 정규화하고 다른 사용자 키는 유지한다. */
    @Test
    void loadsOldCustomPauseAsFixedEscape() throws IOException {
        Path file = tempDir.resolve("keys.properties");
        var service = new PropertiesKeyConfigService(file);
        var bindings = currentBindings(service);
        bindings.put(GameAction.MOVE_LEFT, new KeyBinding(KeyEvent.VK_A));
        service.saveBindings(bindings);
        rewriteProperty(file, "PAUSE_MENU", Integer.toString(KeyEvent.VK_P));

        var restored = new PropertiesKeyConfigService(file);
        restored.load();
        assertEquals(KeyEvent.VK_ESCAPE, restored.getKeyBinding(GameAction.PAUSE_MENU).mainKeyCode());
        assertEquals(KeyEvent.VK_A, restored.getKeyBinding(GameAction.MOVE_LEFT).mainKeyCode());
    }

    /** Esc를 다른 조작에 쓰거나 일시정지 키를 바꾸는 저장은 거부한다. */
    @Test
    void rejectsChangingFixedPauseKey() {
        var service = new PropertiesKeyConfigService(tempDir.resolve("keys.properties"));
        var bindings = currentBindings(service);
        bindings.put(GameAction.PAUSE_MENU, new KeyBinding(KeyEvent.VK_P));
        assertThrows(IllegalArgumentException.class, () -> service.saveBindings(bindings));
        bindings.put(GameAction.PAUSE_MENU, new KeyBinding(KeyEvent.VK_ESCAPE));
        bindings.put(GameAction.MOVE_LEFT, new KeyBinding(KeyEvent.VK_ESCAPE));
        assertThrows(IllegalArgumentException.class, () -> service.saveBindings(bindings));
    }

    /** 기존 설정 파일을 새 설정으로 교체하고 저장용 임시 파일을 남기지 않는다. */
    @Test
    void replacesExistingSettingsFileAndRemovesTemporaryFile() throws IOException {
        Path file = tempDir.resolve("keys.properties");
        var service = new PropertiesKeyConfigService(file);
        service.saveBindings(currentBindings(service));

        var bindings = currentBindings(service);
        bindings.put(GameAction.ROTATE, new KeyBinding(KeyEvent.VK_K, KeyEvent.VK_N));
        service.saveBindings(bindings);

        var restored = new PropertiesKeyConfigService(file);
        restored.load();
        assertEquals(bindings, currentBindings(restored));
        assertEquals(bindings, currentBindings(service));
        try (var files = Files.list(tempDir)) {
            assertEquals(List.of(file), files.toList());
        }
    }

    /** 파일 교체 실패 시 메모리 설정과 교체 대상의 데이터를 유지하고 임시 파일을 정리한다. */
    @Test
    void failedReplacementKeepsMemoryAndTargetAndRemovesTemporaryFile() throws IOException {
        // 내용이 있는 디렉터리를 교체 대상으로 두어 파일 이동 단계에서 실패하도록 한다.
        Path target = Files.createDirectory(tempDir.resolve("keys.properties"));
        Path marker = target.resolve("original");
        Files.writeString(marker, "기존 데이터");
        var service = new PropertiesKeyConfigService(target);
        var original = currentBindings(service);
        var replacement = currentBindings(service);
        replacement.put(GameAction.MOVE_LEFT, new KeyBinding(KeyEvent.VK_A));

        assertThrows(UncheckedIOException.class, () -> service.saveBindings(replacement));
        assertEquals(original, currentBindings(service));
        assertEquals("기존 데이터", Files.readString(marker));
        try (var files = Files.list(tempDir)) {
            assertEquals(List.of(target), files.toList());
        }
    }

    /** 설정 파일이 없으면 실행 중 변경된 키 설정을 기본값으로 복구한다. */
    @Test
    void missingFileRestoresDefaults() throws IOException {
        PropertiesKeyConfigService service = new PropertiesKeyConfigService(tempDir.resolve("missing.properties"));
        saveBinding(service, GameAction.MOVE_LEFT, new KeyBinding(KeyEvent.VK_A, service.getKeyBinding(GameAction.MOVE_LEFT).subKeyCode()));

        Files.delete(tempDir.resolve("missing.properties"));
        service.load();

        assertEquals(KeyEvent.VK_LEFT, service.getKeyBinding(GameAction.MOVE_LEFT).mainKeyCode());
    }

    /** 키 코드가 손상된 파일을 읽으면 일부만 적용하지 않고 기본값으로 복구한다. */
    @Test
    void invalidFileRestoresAllDefaults() throws IOException {
        Path file = tempDir.resolve("invalid.properties");
        PropertiesKeyConfigService service = new PropertiesKeyConfigService(file);
        saveBinding(service, GameAction.MOVE_LEFT, new KeyBinding(KeyEvent.VK_A));

        Files.writeString(file, "MOVE_LEFT=65\nMOVE_RIGHT=not-a-key\n");
        service.load();

        assertEquals(KeyEvent.VK_LEFT, service.getKeyBinding(GameAction.MOVE_LEFT).mainKeyCode());
        assertEquals(KeyEvent.VK_ESCAPE, service.getKeyBinding(GameAction.PAUSE_MENU).mainKeyCode());
    }

    /** 필수 액션이 누락된 설정 파일은 기본값으로 복구한다. */
    @Test
    void incompleteFileRestoresDefaults() throws IOException {
        Path file = tempDir.resolve("incomplete.properties");
        Files.writeString(file, "MOVE_LEFT=65\n");
        PropertiesKeyConfigService service = new PropertiesKeyConfigService(file);

        service.load();

        assertEquals(KeyEvent.VK_LEFT, service.getKeyBinding(GameAction.MOVE_LEFT).mainKeyCode());
    }

    /** 서로 다른 액션에 같은 키가 저장되어 있으면 기본값으로 복구한다. */
    @Test
    void duplicateKeyInFileRestoresDefaults() throws IOException {
        Path file = tempDir.resolve("duplicate.properties");
        PropertiesKeyConfigService service = new PropertiesKeyConfigService(file);
        service.saveBindings(currentBindings(service));
        rewriteProperty(file, "MOVE_LEFT", Integer.toString(KeyEvent.VK_X));

        service.load();

        assertEquals(KeyEvent.VK_LEFT, service.getKeyBinding(GameAction.MOVE_LEFT).mainKeyCode());
        assertEquals(KeyEvent.VK_RIGHT, service.getKeyBinding(GameAction.MOVE_RIGHT).mainKeyCode());
    }

    /** 유효하지 않거나 중복된 키 설정을 거부하고 기존 설정을 유지한다. */
    @Test
    void rejectsInvalidBindingsWithoutChangingExistingKeys() {
        PropertiesKeyConfigService service = new PropertiesKeyConfigService(tempDir.resolve("keys.properties"));
        List<Integer> original = service.getKeyCodes(GameAction.ROTATE);

        assertThrows(IllegalArgumentException.class,
                () -> saveBinding(service, GameAction.ROTATE, new KeyBinding(KeyEvent.VK_UNDEFINED, service.getKeyBinding(GameAction.ROTATE).subKeyCode())));
        assertThrows(IllegalArgumentException.class,
                () -> saveBinding(service, GameAction.ROTATE, new KeyBinding(KeyEvent.VK_X, service.getKeyBinding(GameAction.ROTATE).subKeyCode())));
        assertThrows(IllegalArgumentException.class,
                () -> saveBinding(service, GameAction.ROTATE, new KeyBinding(service.getKeyBinding(GameAction.ROTATE).mainKeyCode(), OptionalInt.of(KeyEvent.VK_UP))));
        assertThrows(IllegalArgumentException.class,
                () -> saveBinding(service, GameAction.ROTATE, new KeyBinding(service.getKeyBinding(GameAction.ROTATE).mainKeyCode(), OptionalInt.of(KeyEvent.VK_UNDEFINED))));
        assertThrows(IllegalArgumentException.class,
                () -> saveBinding(service, GameAction.ROTATE, new KeyBinding(service.getKeyBinding(GameAction.ROTATE).mainKeyCode(), OptionalInt.of(KeyEvent.VK_Z))));
        assertThrows(IllegalArgumentException.class,
                () -> saveBinding(service, GameAction.MOVE_LEFT, new KeyBinding(KeyEvent.VK_X, service.getKeyBinding(GameAction.MOVE_LEFT).subKeyCode())));
        assertEquals(original, service.getKeyCodes(GameAction.ROTATE));
        assertEquals(KeyEvent.VK_LEFT, service.getKeyBinding(GameAction.MOVE_LEFT).mainKeyCode());
    }

    /** 조회한 키 목록의 외부 수정을 막고, 메인 키만 저장하면 기존 서브키를 제거한다. */
    @Test
    void bindingsCannotBeModifiedOutsideServiceAndSingleKeyReplacesAliases() {
        PropertiesKeyConfigService service = new PropertiesKeyConfigService(tempDir.resolve("keys.properties"));
        saveBinding(service, GameAction.ROTATE, new KeyBinding(service.getKeyBinding(GameAction.ROTATE).mainKeyCode(), OptionalInt.of(KeyEvent.VK_R)));
        assertEquals(List.of(KeyEvent.VK_UP, KeyEvent.VK_R), service.getKeyCodes(GameAction.ROTATE));
        assertEquals(KeyEvent.VK_UP, service.getKeyBinding(GameAction.ROTATE).mainKeyCode());
        assertThrows(UnsupportedOperationException.class,
                () -> service.getKeyCodes(GameAction.ROTATE).clear());

        saveBinding(service, GameAction.ROTATE, new KeyBinding(service.getKeyBinding(GameAction.ROTATE).mainKeyCode(), OptionalInt.empty()));
        saveBinding(service, GameAction.ROTATE, new KeyBinding(KeyEvent.VK_X, service.getKeyBinding(GameAction.ROTATE).subKeyCode()));
        assertEquals(List.of(KeyEvent.VK_X), service.getKeyCodes(GameAction.ROTATE));
        saveBinding(service, GameAction.MOVE_LEFT, new KeyBinding(KeyEvent.VK_UP, service.getKeyBinding(GameAction.MOVE_LEFT).subKeyCode()));
        service.resetBindingsToDefault();
        assertEquals(List.of(KeyEvent.VK_UP, KeyEvent.VK_X), service.getKeyCodes(GameAction.ROTATE));
    }

    /** 전체 설정 저장 시 누락이나 키 중복을 거부하고 기존 파일과 메모리 설정을 유지한다. */
    @Test
    void savingWholeBindingsRejectsMissingOrConflictingActionsWithoutApplying() throws IOException {
        Path file = tempDir.resolve("keys.properties");
        var service = new PropertiesKeyConfigService(file);
        service.saveBindings(currentBindings(service));
        byte[] previousFile = Files.readAllBytes(file);
        var bindings = new EnumMap<GameAction, KeyBinding>(GameAction.class);
        for (GameAction action : GameAction.values()) {
            bindings.put(action, service.getKeyBinding(action));
        }
        bindings.remove(GameAction.MOVE_LEFT);
        assertThrows(IllegalArgumentException.class, () -> service.saveBindings(bindings));
        bindings.put(GameAction.MOVE_LEFT, new KeyBinding(KeyEvent.VK_X));
        assertThrows(IllegalArgumentException.class, () -> service.saveBindings(bindings));
        assertEquals(KeyEvent.VK_LEFT, service.getKeyBinding(GameAction.MOVE_LEFT).mainKeyCode());
        assertArrayEquals(previousFile, Files.readAllBytes(file));
        var restored = new PropertiesKeyConfigService(file);
        restored.load();
        assertEquals(KeyEvent.VK_LEFT, restored.getKeyBinding(GameAction.MOVE_LEFT).mainKeyCode());
    }

    /** 액션 내 키 중복·빈 값·잘못된 코드·키 개수 초과가 있으면 전체 기본값을 복구한다. */
    @Test
    void invalidMultipleKeysRestoreAllDefaults() throws IOException {
        Path file = tempDir.resolve("keys.properties");
        PropertiesKeyConfigService service = new PropertiesKeyConfigService(file);
        for (String value : List.of("38,38", "38,", "", "38,0", "38,not-a-key", "38,82,84")) {
            saveBinding(service, GameAction.MOVE_LEFT, new KeyBinding(KeyEvent.VK_A));
            rewriteProperty(file, "ROTATE", value);
            service.load();
            assertEquals(KeyEvent.VK_LEFT, service.getKeyBinding(GameAction.MOVE_LEFT).mainKeyCode());
            assertEquals(List.of(KeyEvent.VK_UP, KeyEvent.VK_X), service.getKeyCodes(GameAction.ROTATE));
        }
    }

    /** 구형 PAUSE·EXIT 설정 파일을 읽으면 전체 키 설정을 새 기본값으로 복구한다. */
    @Test
    void oldPauseAndExitFormatRestoresNewDefaults() throws IOException {
        Path file = tempDir.resolve("old.properties");
        Files.writeString(file, "MOVE_LEFT=65\nMOVE_RIGHT=39\nSOFT_DROP=40\nHARD_DROP=32\n"
                + "ROTATE=38\nPAUSE=80\nEXIT=27\n");
        PropertiesKeyConfigService service = new PropertiesKeyConfigService(file);
        service.load();
        assertEquals(KeyEvent.VK_LEFT, service.getKeyBinding(GameAction.MOVE_LEFT).mainKeyCode());
        assertEquals(List.of(KeyEvent.VK_UP, KeyEvent.VK_X), service.getKeyCodes(GameAction.ROTATE));
        assertEquals(KeyEvent.VK_Z, service.getKeyBinding(GameAction.ROTATE_COUNTERCLOCKWISE).mainKeyCode());
        assertEquals(KeyEvent.VK_ESCAPE, service.getKeyBinding(GameAction.PAUSE_MENU).mainKeyCode());
    }

    /** 테스트용 전체 설정 복사. 실제 코드에는 개별 설정 수정 API를 추가하지 않는다. */
    private static EnumMap<GameAction, KeyBinding> currentBindings(PropertiesKeyConfigService service) {
        var bindings = new EnumMap<GameAction, KeyBinding>(GameAction.class);
        for (GameAction action : GameAction.values()) {
            bindings.put(action, service.getKeyBinding(action));
        }
        return bindings;
    }

    private static void saveBinding(PropertiesKeyConfigService service, GameAction action, KeyBinding binding) {
        var bindings = currentBindings(service);
        bindings.put(action, binding);
        service.saveBindings(bindings);
    }

    private static void rewriteProperty(Path file, String name, String value) throws IOException {
        Properties properties = new Properties();
        try (var reader = Files.newBufferedReader(file)) {
            properties.load(reader);
        }
        properties.setProperty(name, value);
        try (Writer writer = Files.newBufferedWriter(file)) {
            properties.store(writer, "Test key bindings");
        }
    }
}
