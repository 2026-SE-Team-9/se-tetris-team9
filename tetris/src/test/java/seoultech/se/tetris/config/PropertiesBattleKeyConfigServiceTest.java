package seoultech.se.tetris.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seoultech.se.tetris.config.BattleKeyConfigService.BattleAction;
import seoultech.se.tetris.config.BattleKeyConfigService.BattleKeySlot;
import seoultech.se.tetris.config.BattleKeyConfigService.BattlePlayer;

class PropertiesBattleKeyConfigServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void defaultBindings_hasCorrectInitialKeys() {
        PropertiesBattleKeyConfigService service = new PropertiesBattleKeyConfigService(tempDir.resolve("keys.properties"));

        // 1P 기본값 검증: WASD + Shift
        assertEquals(KeyEvent.VK_A, service.getKey(BattlePlayer.P1, BattleAction.MOVE_LEFT));
        assertEquals(KeyEvent.VK_D, service.getKey(BattlePlayer.P1, BattleAction.MOVE_RIGHT));
        assertEquals(KeyEvent.VK_S, service.getKey(BattlePlayer.P1, BattleAction.SOFT_DROP));
        assertEquals(KeyEvent.VK_SHIFT, service.getKey(BattlePlayer.P1, BattleAction.HARD_DROP));
        assertEquals(KeyEvent.VK_W, service.getKey(BattlePlayer.P1, BattleAction.ROTATE));

        // 2P 기본값 검증: 방향키 + Ctrl
        assertEquals(KeyEvent.VK_LEFT, service.getKey(BattlePlayer.P2, BattleAction.MOVE_LEFT));
        assertEquals(KeyEvent.VK_RIGHT, service.getKey(BattlePlayer.P2, BattleAction.MOVE_RIGHT));
        assertEquals(KeyEvent.VK_DOWN, service.getKey(BattlePlayer.P2, BattleAction.SOFT_DROP));
        assertEquals(KeyEvent.VK_CONTROL, service.getKey(BattlePlayer.P2, BattleAction.HARD_DROP));
        assertEquals(KeyEvent.VK_UP, service.getKey(BattlePlayer.P2, BattleAction.ROTATE));

        // 슬롯 레코드 편의 메서드 검증
        assertEquals(KeyEvent.VK_A, service.getKey(new BattleKeySlot(BattlePlayer.P1, BattleAction.MOVE_LEFT)));
        assertEquals(10, service.getAllBindings().size());
    }

    @Test
    void saveAndLoad_persistsCustomBindings() {
        Path file = tempDir.resolve("sub").resolve("keys.properties");
        PropertiesBattleKeyConfigService service = new PropertiesBattleKeyConfigService(file);

        Map<BattleKeySlot, Integer> custom = new HashMap<>(PropertiesBattleKeyConfigService.defaultBindings());
        // 1P MOVE_LEFT를 J로, 1P MOVE_RIGHT를 L로 변경 (중복 없는 임의 키)
        custom.put(new BattleKeySlot(BattlePlayer.P1, BattleAction.MOVE_LEFT), KeyEvent.VK_J);
        custom.put(new BattleKeySlot(BattlePlayer.P1, BattleAction.MOVE_RIGHT), KeyEvent.VK_L);

        service.saveBindings(custom);

        // 새 인스턴스로 로드 확인
        PropertiesBattleKeyConfigService loadedService = new PropertiesBattleKeyConfigService(file);
        loadedService.load();

        assertEquals(KeyEvent.VK_J, loadedService.getKey(BattlePlayer.P1, BattleAction.MOVE_LEFT));
        assertEquals(KeyEvent.VK_L, loadedService.getKey(BattlePlayer.P1, BattleAction.MOVE_RIGHT));
        assertEquals(KeyEvent.VK_UP, loadedService.getKey(BattlePlayer.P2, BattleAction.ROTATE));
    }

    @Test
    void saveBindings_rejectsDuplicateKeys() {
        PropertiesBattleKeyConfigService service = new PropertiesBattleKeyConfigService(tempDir.resolve("keys.properties"));
        Map<BattleKeySlot, Integer> custom = new HashMap<>(PropertiesBattleKeyConfigService.defaultBindings());

        // 2P의 MOVE_LEFT를 1P의 MOVE_LEFT(A)와 동일하게 지정
        custom.put(new BattleKeySlot(BattlePlayer.P2, BattleAction.MOVE_LEFT), KeyEvent.VK_A);

        assertThrows(IllegalArgumentException.class, () -> service.saveBindings(custom));
    }

    @Test
    void saveBindings_rejectsEscapeKey() {
        PropertiesBattleKeyConfigService service = new PropertiesBattleKeyConfigService(tempDir.resolve("keys.properties"));
        Map<BattleKeySlot, Integer> custom = new HashMap<>(PropertiesBattleKeyConfigService.defaultBindings());

        custom.put(new BattleKeySlot(BattlePlayer.P1, BattleAction.ROTATE), KeyEvent.VK_ESCAPE);

        assertThrows(IllegalArgumentException.class, () -> service.saveBindings(custom));
    }

    @Test
    void saveBindings_rejectsMissingSlot() {
        PropertiesBattleKeyConfigService service = new PropertiesBattleKeyConfigService(tempDir.resolve("keys.properties"));
        Map<BattleKeySlot, Integer> incomplete = new HashMap<>(PropertiesBattleKeyConfigService.defaultBindings());
        incomplete.remove(new BattleKeySlot(BattlePlayer.P1, BattleAction.MOVE_LEFT));

        assertThrows(IllegalArgumentException.class, () -> service.saveBindings(incomplete));
    }

    @Test
    void load_recoversDefaultsWhenFileIsCorrupted() throws IOException {
        Path file = tempDir.resolve("corrupted.properties");
        Files.writeString(file, "P1.MOVE_LEFT=not_a_number\n");

        PropertiesBattleKeyConfigService service = new PropertiesBattleKeyConfigService(file);
        service.load();

        // 파싱 실패 시 기본값으로 복구
        assertEquals(KeyEvent.VK_A, service.getKey(BattlePlayer.P1, BattleAction.MOVE_LEFT));
    }

    @Test
    void load_recoversDefaultsWhenFileHasDuplicateKeys() throws IOException {
        Path file = tempDir.resolve("duplicate.properties");
        StringBuilder sb = new StringBuilder();
        for (BattlePlayer p : BattlePlayer.values()) {
            for (BattleAction a : BattleAction.values()) {
                sb.append(p.name()).append(".").append(a.name()).append("=").append(KeyEvent.VK_A).append("\n");
            }
        }
        Files.writeString(file, sb.toString());

        PropertiesBattleKeyConfigService service = new PropertiesBattleKeyConfigService(file);
        service.load();

        // 중복 시 기본값 복구
        assertEquals(KeyEvent.VK_LEFT, service.getKey(BattlePlayer.P2, BattleAction.MOVE_LEFT));
    }

    @Test
    void load_doesNothingIfFileDoesNotExist() {
        Path nonExistent = tempDir.resolve("nonexistent.properties");
        PropertiesBattleKeyConfigService service = new PropertiesBattleKeyConfigService(nonExistent);
        service.load();

        assertEquals(KeyEvent.VK_A, service.getKey(BattlePlayer.P1, BattleAction.MOVE_LEFT));
    }
}
