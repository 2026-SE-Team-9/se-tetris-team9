package seoultech.se.tetris.config;

import java.awt.event.KeyEvent;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;

import seoultech.se.tetris.config.BattleKeyConfigService.BattleAction;
import seoultech.se.tetris.config.BattleKeyConfigService.BattleKeySlot;
import seoultech.se.tetris.config.BattleKeyConfigService.BattlePlayer;

/**
 * 대전 모드 키 설정의 임시 편집 상태를 관리한다.
 * 저장 전까지 실제 설정과 파일에는 영향을 주지 않는다.
 */
public final class BattleKeyBindingEditor {

    private final BattleKeyConfigService config;
    private final Runnable onSaved;
    private Map<BattleKeySlot, OptionalInt> draftBindings;

    public BattleKeyBindingEditor(BattleKeyConfigService config, Runnable onSaved) {
        this.config = Objects.requireNonNull(config, "config");
        this.onSaved = Objects.requireNonNull(onSaved, "onSaved");
        this.draftBindings = createDraftBindings();
    }

    /** 전체 슬롯의 편집 상태 스냅샷을 반환한다. */
    public Map<BattleKeySlot, OptionalInt> getDraftBindings() {
        return Collections.unmodifiableMap(new HashMap<>(draftBindings));
    }

    /** 특정 슬롯의 현재 편집 키 값을 반환한다. */
    public OptionalInt getDraftBinding(BattlePlayer player, BattleAction action) {
        return getDraftBinding(new BattleKeySlot(player, action));
    }

    /** 특정 슬롯의 현재 편집 키 값을 반환한다. */
    public OptionalInt getDraftBinding(BattleKeySlot slot) {
        Objects.requireNonNull(slot, "slot");
        OptionalInt code = draftBindings.get(slot);
        return code != null ? code : OptionalInt.empty();
    }

    /**
     * 특정 슬롯에 새 키를 할당한다.
     * Esc는 해당 슬롯을 비우고, 다른 키는 기존 사용 칸에서 자동으로 비운 뒤 할당한다.
     */
    public void captureKey(BattlePlayer player, BattleAction action, int keyCode) {
        captureKey(new BattleKeySlot(player, action), keyCode);
    }

    /**
     * 특정 슬롯에 새 키를 할당한다.
     */
    public void captureKey(BattleKeySlot slot, int keyCode) {
        Objects.requireNonNull(slot, "slot");
        if (keyCode <= KeyEvent.VK_UNDEFINED) {
            throw new IllegalArgumentException("유효하지 않은 키 코드: " + keyCode);
        }

        if (keyCode == KeyEvent.VK_ESCAPE) {
            // Esc는 슬롯 비우기 동작
            draftBindings.put(slot, OptionalInt.empty());
            return;
        }

        // 다른 슬롯에서 이미 사용 중인 키라면 해당 슬롯을 비움 (10개 슬롯 간 상호 고유 보장)
        draftBindings.replaceAll((s, existing) ->
                existing.isPresent() && existing.getAsInt() == keyCode ? OptionalInt.empty() : existing);

        draftBindings.put(slot, OptionalInt.of(keyCode));
    }

    /** 10개 슬롯이 모두 지정되어 있어야 저장할 수 있다. */
    public boolean canSave() {
        if (draftBindings.size() != BattlePlayer.values().length * BattleAction.values().length) {
            return false;
        }
        return draftBindings.values().stream().allMatch(OptionalInt::isPresent);
    }

    /** 파일 저장 후 설정을 적용하고 onSaved 콜백을 실행한다. */
    public void save() {
        if (!canSave()) {
            throw new IllegalStateException("10개 대전 조작 키가 모두 지정되어야 저장할 수 있습니다");
        }
        Map<BattleKeySlot, Integer> toSave = new HashMap<>();
        draftBindings.forEach((slot, opt) -> toSave.put(slot, opt.getAsInt()));
        config.saveBindings(toSave);
        onSaved.run();
    }

    /** 변경된 임시 편집 내용을 버리고 현재 적용된 설정으로 되돌린다. */
    public void resetChanges() {
        this.draftBindings = createDraftBindings();
    }

    /** 편집 값을 1P, 2P 기본 키로 변경한다. */
    public void restoreDefaults() {
        draftBindings = new HashMap<>();
        PropertiesBattleKeyConfigService.defaultBindings().forEach((slot, code) ->
                draftBindings.put(slot, OptionalInt.of(code)));
    }

    private Map<BattleKeySlot, OptionalInt> createDraftBindings() {
        Map<BattleKeySlot, OptionalInt> bindings = new HashMap<>();
        for (BattlePlayer player : BattlePlayer.values()) {
            for (BattleAction action : BattleAction.values()) {
                BattleKeySlot slot = new BattleKeySlot(player, action);
                bindings.put(slot, OptionalInt.of(config.getKey(slot)));
            }
        }
        return bindings;
    }
}
