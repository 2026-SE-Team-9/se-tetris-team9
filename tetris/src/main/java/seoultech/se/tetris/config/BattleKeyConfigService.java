package seoultech.se.tetris.config;

import java.util.Map;
import java.util.Objects;

/**
 * 로컬 대전 모드 2인 조작 키 설정 서비스 명세.
 */
public interface BattleKeyConfigService {

    /** 대전 모드 조작 액션 정의 (5개) */
    enum BattleAction {
        MOVE_LEFT, MOVE_RIGHT, SOFT_DROP, HARD_DROP, ROTATE
    }

    /** 플레이어 구분 */
    enum BattlePlayer {
        P1, P2
    }

    /** 특정 플레이어의 특정 액션 키 슬롯 */
    record BattleKeySlot(BattlePlayer player, BattleAction action) {
        public BattleKeySlot {
            Objects.requireNonNull(player, "player");
            Objects.requireNonNull(action, "action");
        }
    }

    /** 지정한 플레이어와 액션의 키 코드를 조회한다. */
    int getKey(BattlePlayer player, BattleAction action);

    /** 지정한 슬롯의 키 코드를 조회한다. */
    default int getKey(BattleKeySlot slot) {
        Objects.requireNonNull(slot, "slot");
        return getKey(slot.player(), slot.action());
    }

    /** 전체 10개 슬롯의 키 설정을 불변 맵으로 반환한다. */
    Map<BattleKeySlot, Integer> getAllBindings();

    /** 1P, 2P 기본 조작 키로 되돌린다. 파일에는 저장하지 않는다. */
    void resetBindingsToDefault();

    /** 전체 10개 슬롯 설정을 검증하여 파일에 저장하고 적용한다. */
    void saveBindings(Map<BattleKeySlot, Integer> bindings);

    /** 파일에서 키 설정을 로드한다. 파일이 없거나 손상 시 기본값으로 복구한다. */
    void load();
}
