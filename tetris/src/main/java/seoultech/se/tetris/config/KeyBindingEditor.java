package seoultech.se.tetris.config;

import java.awt.event.KeyEvent;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;

import seoultech.se.tetris.config.KeyConfigService.GameAction;
import seoultech.se.tetris.config.KeyConfigService.KeyBinding;

/** 설정 화면 전체의 임시 키 설정. 저장 전까지 실제 설정과 게임 입력은 바꾸지 않는다. */
public final class KeyBindingEditor {
    /** 편집 중에는 메인·서브 모두 빈 값을 허용한다. */
    public record EditableKeyBinding(OptionalInt mainKeyCode, OptionalInt subKeyCode) {
        public EditableKeyBinding {
            Objects.requireNonNull(mainKeyCode, "mainKeyCode");
            Objects.requireNonNull(subKeyCode, "subKeyCode");
        }
    }

    private final KeyConfigService config;
    private final Runnable onSaved;
    private Map<GameAction, EditableKeyBinding> draftBindings;

    public KeyBindingEditor(KeyConfigService config, Runnable onSaved) {
        this.config = Objects.requireNonNull(config, "config");
        this.onSaved = Objects.requireNonNull(onSaved, "onSaved");
        draftBindings = createDraftBindings();
    }

    /** 화면은 이 편집 값으로 표시한다. 반환된 스냅샷은 외부에서 수정할 수 없다. */
    public Map<GameAction, EditableKeyBinding> getDraftBindings() {
        return Map.copyOf(draftBindings);
    }

    public EditableKeyBinding getDraftBinding(GameAction action) {
        return draftBindings.get(Objects.requireNonNull(action, "action"));
    }

    public void captureMainKey(GameAction action, int keyCode) {
        captureKey(action, keyCode, true);
    }

    public void captureSubKey(GameAction action, int keyCode) {
        captureKey(action, keyCode, false);
    }

    /** 선택한 메인·서브 칸을 변경한다. Esc는 칸을 비우고, 다른 키는 기존 사용 칸에서 옮겨 온다. */
    private void captureKey(GameAction action, int keyCode, boolean main) {
        Objects.requireNonNull(action, "action");
        if (action == GameAction.PAUSE_MENU) {
            throw new IllegalArgumentException("일시정지 키는 Esc로 고정되어 있습니다");
        }
        if (keyCode <= KeyEvent.VK_UNDEFINED) {
            throw new IllegalArgumentException("유효하지 않은 키 코드: " + keyCode);
        }
        OptionalInt captured = keyCode == KeyEvent.VK_ESCAPE
                ? OptionalInt.empty() : OptionalInt.of(keyCode);
        if (captured.isPresent()) {
            // 같은 키는 전체 임시 설정의 한 칸에만 둔다. 기존 메인이 비면 저장을 막는다.
            draftBindings.replaceAll((other, binding) -> new EditableKeyBinding(
                    withoutKey(binding.mainKeyCode(), keyCode),
                    withoutKey(binding.subKeyCode(), keyCode)));
        }
        EditableKeyBinding binding = getDraftBinding(action);
        draftBindings.put(action, main
                ? new EditableKeyBinding(captured, binding.subKeyCode())
                : new EditableKeyBinding(binding.mainKeyCode(), captured));
    }

    /** 키 이동으로 중복은 제거되므로 모든 액션의 필수 메인 유무로 저장 가능 여부를 판단한다. */
    public boolean canSave() {
        return draftBindings.values().stream().allMatch(binding -> binding.mainKeyCode().isPresent());
    }

    /** 파일 저장 성공 후 전체 설정을 적용하고 입력 맵을 교체한다. 실패하면 임시 값만 유지한다. */
    public void save() {
        if (!canSave()) {
            throw new IllegalStateException("모든 액션의 메인 키를 지정해야 저장할 수 있습니다");
        }
        Map<GameAction, KeyBinding> bindings = new EnumMap<>(GameAction.class);
        draftBindings.forEach((action, binding) -> bindings.put(action,
                new KeyBinding(binding.mainKeyCode().getAsInt(), binding.subKeyCode())));
        config.saveBindings(bindings);
        onSaved.run();
    }

    /** 적용된 설정으로 새 임시 설정을 완성한 뒤 교체하여 편집한 변경 사항을 초기화한다. */
    public void resetChanges() {
        draftBindings = createDraftBindings();
    }

    /** 저장 전 편집값을 모든 기본 키로 바꾼다. */
    public void restoreDefaults() {
        draftBindings = new EnumMap<>(GameAction.class);
        PropertiesKeyConfigService.defaultBindings().forEach((action, binding) ->
                draftBindings.put(action, new EditableKeyBinding(
                        OptionalInt.of(binding.mainKeyCode()), binding.subKeyCode())));
    }

    /** 현재 적용된 액션별 키 설정을 복사하여 새 임시 설정 맵을 만든다. */
    private Map<GameAction, EditableKeyBinding> createDraftBindings() {
        Map<GameAction, EditableKeyBinding> bindings = new EnumMap<>(GameAction.class);
        for (GameAction action : GameAction.values()) {
            KeyBinding binding = config.getKeyBinding(action);
            bindings.put(action, new EditableKeyBinding(
                    OptionalInt.of(binding.mainKeyCode()), binding.subKeyCode()));
        }
        return bindings;
    }

    /**
     * 현재 키가 제거할 키와 같으면 빈 값을 반환하고, 다르면 현재 값을 유지한다.
     * 예: N에서 N을 제거하면 빈 값, K에서 N을 제거하면 K가 된다.
     * 키를 다른 칸에 지정할 때 기존 사용 칸을 비우는 데 사용한다.
     */
    private static OptionalInt withoutKey(OptionalInt key, int removed) {
        return key.isPresent() && key.getAsInt() == removed ? OptionalInt.empty() : key;
    }
}
