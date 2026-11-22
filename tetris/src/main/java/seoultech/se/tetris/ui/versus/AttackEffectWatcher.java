package seoultech.se.tetris.ui.versus;

import java.util.Objects;

import seoultech.se.tetris.component.listener.GameEventListener;
import seoultech.se.tetris.component.model.Attack;
import seoultech.se.tetris.component.model.GameState;

/**
 * 한 플레이어의 게임 이벤트를 보고 줄 공격 애니메이션({@link AttackEffects})을 시작한다. Swing/AWT에 의존하지 않는다.
 *
 * <ul>
 *   <li>이 플레이어가 공격을 보내면({@link #onAttack}) 보낸 줄 수를 표시한다.</li>
 *   <li>보드가 바뀔 때마다 공격 대기 줄 수를 직전 값과 비교해, 늘었으면 공격을 받은 것으로,
 *       줄었으면 대기 줄이 보드에 추가된 것으로 본다.</li>
 * </ul>
 * 공격 처리는 엔진이 하므로, 여기서는 게임 상태를 읽기만 한다.
 * 대기 줄 수만 비교하므로 상대 화면 정보만 받는 P2P 대전에도 쓸 수 있다.
 */
public final class AttackEffectWatcher implements GameEventListener {

    private final GameState state;
    private final AttackEffects effects;
    private int lastPending;

    public AttackEffectWatcher(GameState state, AttackEffects effects) {
        this.state = Objects.requireNonNull(state, "state");
        this.effects = Objects.requireNonNull(effects, "effects");
        this.lastPending = pendingCount();
    }

    private int pendingCount() {
        return state.getPendingGarbage().size();
    }

    @Override
    public void onAttack(Attack attack) {
        effects.attackSent(attack.size());
    }

    @Override
    public void onBoardUpdated() {
        int pending = pendingCount();
        if (pending > lastPending) {
            effects.attackReceived(lastPending, pending);
        } else if (pending < lastPending) {
            effects.garbageInserted(lastPending - pending);
        }
        lastPending = pending;
    }

    @Override
    public void onLinesCleared(int clearedCount, int gainedScore) {
        // 줄 삭제 자체는 기존 줄 삭제 애니메이션이 표시한다
    }

    @Override
    public void onSpeedIncreased(int currentInterval) {
        // 속도 변화는 공격과 관계없다
    }

    @Override
    public void onGameOver(int finalScore) {
        // 게임 종료는 대전 결과 표시가 맡는다
    }
}
