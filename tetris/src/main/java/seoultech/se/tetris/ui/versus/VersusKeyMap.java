package seoultech.se.tetris.ui.versus;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import seoultech.se.tetris.component.controller.GameController;
import seoultech.se.tetris.config.BattleKeyConfigService;
import seoultech.se.tetris.config.BattleKeyConfigService.BattleAction;
import seoultech.se.tetris.config.BattleKeyConfigService.BattlePlayer;
import seoultech.se.tetris.config.BattleKeyConfigService.BattleKeySlot;
import seoultech.se.tetris.versus.Side;

/**
 * 로컬 대전에서 누른 키를 플레이어와 동작으로 바꿔 해당 플레이어의 조작기에 전달한다.
 *
 * <p>한 창에서 두 플레이어의 입력을 함께 받으므로, 키 하나가 어느 플레이어의 어떤 동작인지는
 * 로컬 대전용 2인 키 설정({@link BattleKeyConfigService})으로 정한다. Swing에 의존하지 않는다.
 */
public final class VersusKeyMap {

    private final Map<Integer, BattleKeySlot> slotsByKey;

    /** 키 설정의 현재 값으로 만든다. 키 설정이 바뀌면 새로 만들어야 한다. */
    public VersusKeyMap(BattleKeyConfigService keyConfig) {
        Objects.requireNonNull(keyConfig, "keyConfig");
        Map<Integer, BattleKeySlot> map = new HashMap<>();
        keyConfig.getAllBindings().forEach((slot, keyCode) -> map.put(keyCode, slot));
        slotsByKey = Map.copyOf(map);
    }

    /** 키에 연결된 플레이어와 동작. 대전 키가 아니면 비어 있다. */
    public Optional<BattleKeySlot> slotOf(int keyCode) {
        return Optional.ofNullable(slotsByKey.get(keyCode));
    }

    /**
     * 키에 연결된 동작을 해당 플레이어의 조작기에 전달한다.
     *
     * @param controllers 플레이어 자리별 조작기. 조작할 수 없는 자리는 비어 있다
     * @return 대전 키라서 처리했으면 {@code true}
     */
    public boolean dispatch(int keyCode, Function<Side, Optional<GameController>> controllers) {
        Objects.requireNonNull(controllers, "controllers");
        Optional<BattleKeySlot> slot = slotOf(keyCode);
        slot.ifPresent(s -> controllers.apply(sideOf(s.player()))
                .ifPresent(controller -> perform(s.action(), controller)));
        return slot.isPresent();
    }

    /** 키 설정의 플레이어 구분을 대전 모델의 자리로 바꾼다. */
    static Side sideOf(BattlePlayer player) {
        return switch (Objects.requireNonNull(player, "player")) {
            case P1 -> Side.P1;
            case P2 -> Side.P2;
        };
    }

    static void perform(BattleAction action, GameController controller) {
        switch (action) {
            case MOVE_LEFT -> controller.moveLeft();
            case MOVE_RIGHT -> controller.moveRight();
            case SOFT_DROP -> controller.moveDown();
            case HARD_DROP -> controller.dropToBottom();
            case ROTATE -> controller.rotate();
        }
    }
}
