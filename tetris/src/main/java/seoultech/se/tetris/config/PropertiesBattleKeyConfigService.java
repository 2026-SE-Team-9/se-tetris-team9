package seoultech.se.tetris.config;

import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

/**
 * properties 파일 기반의 대전 모드 키 설정 서비스.
 * 기본 경로는 ~/.tetris/battle_keybindings.properties이다.
 */
public class PropertiesBattleKeyConfigService implements BattleKeyConfigService {

    private final Path filePath;
    private final Map<BattleKeySlot, Integer> keyBindings = new HashMap<>();

    public PropertiesBattleKeyConfigService() {
        this(Path.of(System.getProperty("user.home"), ".tetris", "battle_keybindings.properties"));
    }

    public PropertiesBattleKeyConfigService(Path filePath) {
        this.filePath = Objects.requireNonNull(filePath, "filePath");
        resetBindingsToDefault();
    }

    @Override
    public int getKey(BattlePlayer player, BattleAction action) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(action, "action");
        Integer code = keyBindings.get(new BattleKeySlot(player, action));
        if (code == null) {
            throw new IllegalStateException("키 설정이 존재하지 않습니다: " + player + ", " + action);
        }
        return code;
    }

    @Override
    public Map<BattleKeySlot, Integer> getAllBindings() {
        return Collections.unmodifiableMap(new HashMap<>(keyBindings));
    }

    @Override
    public void resetBindingsToDefault() {
        keyBindings.clear();
        keyBindings.putAll(defaultBindings());
    }

    /**
     * 1P(WASD+Shift)와 2P(방향키+Ctrl)의 기본 키 설정을 반환한다.
     */
    public static Map<BattleKeySlot, Integer> defaultBindings() {
        Map<BattleKeySlot, Integer> defaults = new HashMap<>();
        // 1P 기본값: A, D, S, Shift, W
        defaults.put(new BattleKeySlot(BattlePlayer.P1, BattleAction.MOVE_LEFT), KeyEvent.VK_A);
        defaults.put(new BattleKeySlot(BattlePlayer.P1, BattleAction.MOVE_RIGHT), KeyEvent.VK_D);
        defaults.put(new BattleKeySlot(BattlePlayer.P1, BattleAction.SOFT_DROP), KeyEvent.VK_S);
        defaults.put(new BattleKeySlot(BattlePlayer.P1, BattleAction.HARD_DROP), KeyEvent.VK_SHIFT);
        defaults.put(new BattleKeySlot(BattlePlayer.P1, BattleAction.ROTATE), KeyEvent.VK_W);

        // 2P 기본값: ←, →, ↓, Ctrl, ↑
        defaults.put(new BattleKeySlot(BattlePlayer.P2, BattleAction.MOVE_LEFT), KeyEvent.VK_LEFT);
        defaults.put(new BattleKeySlot(BattlePlayer.P2, BattleAction.MOVE_RIGHT), KeyEvent.VK_RIGHT);
        defaults.put(new BattleKeySlot(BattlePlayer.P2, BattleAction.SOFT_DROP), KeyEvent.VK_DOWN);
        defaults.put(new BattleKeySlot(BattlePlayer.P2, BattleAction.HARD_DROP), KeyEvent.VK_CONTROL);
        defaults.put(new BattleKeySlot(BattlePlayer.P2, BattleAction.ROTATE), KeyEvent.VK_UP);

        return Collections.unmodifiableMap(defaults);
    }

    @Override
    public void saveBindings(Map<BattleKeySlot, Integer> bindings) {
        Objects.requireNonNull(bindings, "bindings");
        Map<BattleKeySlot, Integer> validated = validateBindings(bindings);
        writeBindings(validated);
        keyBindings.clear();
        keyBindings.putAll(validated);
    }

    private static Map<BattleKeySlot, Integer> validateBindings(Map<BattleKeySlot, Integer> bindings) {
        Map<BattleKeySlot, Integer> result = new HashMap<>();
        Set<Integer> usedCodes = new HashSet<>();
        usedCodes.add(KeyEvent.VK_ESCAPE); // Esc는 일시정지 전용 고정 키

        for (BattlePlayer player : BattlePlayer.values()) {
            for (BattleAction action : BattleAction.values()) {
                BattleKeySlot slot = new BattleKeySlot(player, action);
                Integer code = bindings.get(slot);
                if (code == null) {
                    throw new IllegalArgumentException("누락된 키 설정: " + slot);
                }
                if (code <= KeyEvent.VK_UNDEFINED) {
                    throw new IllegalArgumentException("유효하지 않은 키 코드: " + code);
                }
                if (code == KeyEvent.VK_ESCAPE) {
                    throw new IllegalArgumentException("일시정지 키(Esc)는 대전 조작 키로 지정할 수 없습니다");
                }
                if (!usedCodes.add(code)) {
                    throw new IllegalArgumentException("중복된 키 코드: " + code + " (슬롯: " + slot + ")");
                }
                result.put(slot, code);
            }
        }
        return result;
    }

    private void writeBindings(Map<BattleKeySlot, Integer> bindings) {
        Properties properties = new Properties();
        for (Map.Entry<BattleKeySlot, Integer> entry : bindings.entrySet()) {
            BattleKeySlot slot = entry.getKey();
            String key = propertyKey(slot.player(), slot.action());
            properties.setProperty(key, String.valueOf(entry.getValue()));
        }

        Path temporaryFile = null;
        try {
            Path target = filePath.toAbsolutePath();
            Path parent = target.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
                temporaryFile = Files.createTempFile(parent, ".battle-keys-", ".tmp");
            } else {
                temporaryFile = Files.createTempFile(".battle-keys-", ".tmp");
            }

            try (Writer writer = Files.newBufferedWriter(temporaryFile)) {
                properties.store(writer, "Tetris Battle 2P Key Bindings");
            }
            Files.move(temporaryFile, target,
                    StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            if (temporaryFile != null) {
                try {
                    Files.deleteIfExists(temporaryFile);
                } catch (IOException cleanupFailure) {
                    e.addSuppressed(cleanupFailure);
                }
            }
            throw new UncheckedIOException("대전 키 설정을 저장할 수 없습니다: " + filePath, e);
        }
    }

    @Override
    public void load() {
        if (Files.notExists(filePath)) {
            resetBindingsToDefault();
            return;
        }

        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(filePath)) {
            properties.load(reader);
            Map<BattleKeySlot, Integer> loaded = new HashMap<>();
            for (BattlePlayer player : BattlePlayer.values()) {
                for (BattleAction action : BattleAction.values()) {
                    String propKey = propertyKey(player, action);
                    String val = properties.getProperty(propKey);
                    if (val == null) {
                        throw new IllegalArgumentException("누락된 키 설정: " + propKey);
                    }
                    int code = Integer.parseInt(val.trim());
                    loaded.put(new BattleKeySlot(player, action), code);
                }
            }
            Map<BattleKeySlot, Integer> validated = validateBindings(loaded);
            keyBindings.clear();
            keyBindings.putAll(validated);
        } catch (IllegalArgumentException e) {
            // 형식이 잘못되었거나 중복/누락된 경우 기본값으로 안전 복구
            resetBindingsToDefault();
        } catch (IOException e) {
            throw new UncheckedIOException("대전 키 설정을 불러올 수 없습니다: " + filePath, e);
        }
    }

    private static String propertyKey(BattlePlayer player, BattleAction action) {
        return player.name() + "." + action.name();
    }
}
