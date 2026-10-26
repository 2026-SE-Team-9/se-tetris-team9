package seoultech.se.tetris.config;

import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

/** 키 설정을 properties 파일에 저장한다. 기본 경로는 사용자 홈의 .tetris/keybindings.properties이다. */
public class PropertiesKeyConfigService implements KeyConfigService {
    private static final int MAX_KEYS_PER_ACTION = 2;
    private final Path filePath;
    private final Map<GameAction, KeyBinding> keyBindings = new EnumMap<>(GameAction.class);

    public PropertiesKeyConfigService() {
        this(Path.of(System.getProperty("user.home"), ".tetris", "keybindings.properties"));
    }

    public PropertiesKeyConfigService(Path filePath) {
        this.filePath = Objects.requireNonNull(filePath, "filePath");
        resetBindingsToDefault();
    }

    @Override
    public KeyBinding getKeyBinding(GameAction action) {
        return keyBindings.get(Objects.requireNonNull(action, "action"));
    }

    /** 액션당 키 개수와 코드 유효성을 검사하고, used에 키를 모아 전체 액션 간 중복을 검사한다. */
    private static void validateKeys(List<Integer> codes, Set<Integer> used) {
        if (codes.isEmpty()) {
            throw new IllegalArgumentException("키를 하나 이상 지정해야 합니다");
        }
        if (codes.size() > MAX_KEYS_PER_ACTION) {
            throw new IllegalArgumentException("액션당 키는 최대 두 개까지 지정할 수 있습니다");
        }
        for (int code : codes) {
            if (code <= KeyEvent.VK_UNDEFINED || !used.add(code)) {
                throw new IllegalArgumentException("유효하지 않거나 중복된 키 코드: " + code);
            }
        }
    }

    /** 현재 키 설정 전체를 기본 조작 키로 되돌린다. 파일에는 저장하지 않는다. */
    @Override
    public void resetBindingsToDefault() {
        keyBindings.clear();
        keyBindings.putAll(defaultBindings());
    }

    /** 설정 화면의 저장 전 기본값 복원에도 사용할 수 있는 기본 키 목록. */
    public static Map<GameAction, KeyBinding> defaultBindings() {
        Map<GameAction, KeyBinding> defaults = new EnumMap<>(GameAction.class);
        defaults.put(GameAction.MOVE_LEFT, new KeyBinding(KeyEvent.VK_LEFT));
        defaults.put(GameAction.MOVE_RIGHT, new KeyBinding(KeyEvent.VK_RIGHT));
        defaults.put(GameAction.SOFT_DROP, new KeyBinding(KeyEvent.VK_DOWN));
        defaults.put(GameAction.HARD_DROP, new KeyBinding(KeyEvent.VK_SPACE));
        defaults.put(GameAction.ROTATE, new KeyBinding(KeyEvent.VK_UP, KeyEvent.VK_X));
        defaults.put(GameAction.ROTATE_COUNTERCLOCKWISE, new KeyBinding(KeyEvent.VK_Z));
        defaults.put(GameAction.PAUSE_MENU, new KeyBinding(KeyEvent.VK_ESCAPE));
        return Map.copyOf(defaults);
    }

    /** 모든 액션의 설정과 키 중복을 검사하고, 파일 저장이 성공한 뒤 실행 중 설정을 교체한다. */
    @Override
    public void saveBindings(Map<GameAction, KeyBinding> bindings) {
        Objects.requireNonNull(bindings, "bindings");
        if (bindings.size() != GameAction.values().length) {
            throw new IllegalArgumentException("모든 액션의 키 설정이 필요합니다");
        }
        Map<GameAction, KeyBinding> replacement = new EnumMap<>(GameAction.class);
        Set<Integer> used = new HashSet<>();
        used.add(KeyEvent.VK_ESCAPE);
        for (GameAction action : GameAction.values()) {
            KeyBinding binding = bindings.get(action);
            if (binding == null) {
                throw new IllegalArgumentException("누락된 키 설정: " + action);
            }
            if (action == GameAction.PAUSE_MENU) {
                if (!binding.equals(new KeyBinding(KeyEvent.VK_ESCAPE))) {
                    throw new IllegalArgumentException("일시정지 키는 Esc로 고정되어 있습니다");
                }
            } else {
                validateKeys(binding.keyCodes(), used);
            }
            replacement.put(action, binding);
        }
        writeBindings(replacement);
        keyBindings.clear();
        keyBindings.putAll(replacement);
    }

    /** 임시 파일에 전체 설정을 기록한 뒤 원자적으로 교체한다. 실패하면 임시 파일을 정리한다. */
    private void writeBindings(Map<GameAction, KeyBinding> bindings) {
        Properties properties = new Properties();
        for (GameAction action : GameAction.values()) {
            properties.setProperty(action.name(), bindings.get(action).keyCodes().stream()
                    .map(String::valueOf).collect(Collectors.joining(",")));
        }

        Path temporaryFile = null;
        try {
            Path target = filePath.toAbsolutePath();
            Path parent = target.getParent();
            Files.createDirectories(parent);
            temporaryFile = Files.createTempFile(parent, ".keybindings-", ".tmp");
            try (Writer writer = Files.newBufferedWriter(temporaryFile)) {
                properties.store(writer, "Tetris key bindings");
            }
            // 원자적 교체를 지원하지 않으면 저장 실패로 처리하여 기존 설정을 유지한다.
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
            throw new UncheckedIOException("키 설정을 저장할 수 없습니다: " + filePath, e);
        }
    }

    /** 파일의 전체 설정을 읽고 검증하여 적용한다. 파일이 없거나 설정이 손상되면 기본값으로 복구한다. */
    @Override
    public void load() {
        if (Files.notExists(filePath)) {
            resetBindingsToDefault();
            return;
        }

        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(filePath)) {
            properties.load(reader);
            // 모든 액션의 값을 검증한 뒤 적용해 일부 설정만 복원되는 일을 막는다.
            Map<GameAction, KeyBinding> loaded = new EnumMap<>(GameAction.class);
            Set<Integer> used = new HashSet<>();
            used.add(KeyEvent.VK_ESCAPE);
            for (GameAction action : GameAction.values()) {
                if (action == GameAction.PAUSE_MENU) {
                    if (properties.getProperty(action.name()) == null) {
                        throw new IllegalArgumentException("누락된 키 설정: " + action);
                    }
                    // 이전 파일의 일시정지 사용자 지정 값은 무시하고 고정 키를 적용한다.
                    loaded.put(action, new KeyBinding(KeyEvent.VK_ESCAPE));
                    continue;
                }
                String value = properties.getProperty(action.name());
                if (value == null) {
                    throw new IllegalArgumentException("누락된 키 설정: " + action);
                }
                List<Integer> codes = new ArrayList<>();
                for (String token : value.split(",", -1)) {
                    codes.add(Integer.valueOf(token.trim()));
                }
                validateKeys(codes, used);
                loaded.put(action, codes.size() == 1
                        ? new KeyBinding(codes.getFirst())
                        : new KeyBinding(codes.getFirst(), codes.get(1)));
            }
            keyBindings.clear();
            keyBindings.putAll(loaded);
        } catch (IllegalArgumentException e) {
            // 구형 PAUSE/EXIT 설정 등 필수 액션이 없거나 손상된 파일은 새 기본값으로 복구한다.
            resetBindingsToDefault();
        } catch (IOException e) {
            throw new UncheckedIOException("키 설정을 불러올 수 없습니다: " + filePath, e);
        }
    }
}
