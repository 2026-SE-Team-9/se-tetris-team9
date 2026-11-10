package seoultech.se.tetris.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import seoultech.se.tetris.component.model.Attack;
import seoultech.se.tetris.component.model.GarbageLine;

/**
 * 보드에 들어가기 전의 공격 대기 줄. (3차 「공격 대기열」)
 *
 * <p>줄은 위에서 아래 순서로 보관하며 최대 {@link #MAX_LINES}줄이다.
 * <ul>
 *   <li>새 공격은 기존 대기 줄의 아래에 붙는다</li>
 *   <li>공간이 모자라면 공격의 위쪽 줄부터 남은 줄 수만큼만 붙이고, 나머지 아래쪽 줄은 버린다</li>
 *   <li>이미 {@link #MAX_LINES}줄이면 공격은 무시된다</li>
 *   <li>보드에 추가할 때는 대기 줄 전체를 한 번에 꺼낸다 (3차 팀 결정)</li>
 * </ul>
 *
 * <p>공격 줄({@link GarbageLine})은 보드 폭을 모르므로 폭이 보드와 같은지는 여기서 검사한다.
 */
final class GarbageQueue {

    /** 대기 줄의 최대 줄 수 */
    static final int MAX_LINES = 10;

    private final List<GarbageLine> lines = new ArrayList<>(MAX_LINES);

    /**
     * 공격을 대기 줄 아래에 붙인다.
     *
     * @return 실제로 붙은 줄 수. 공간이 없으면 0
     * @throws IllegalArgumentException 공격 줄의 폭이 보드 폭과 다른 경우. 이때는 아무 줄도 붙이지 않는다
     */
    int add(Attack attack) {
        Objects.requireNonNull(attack, "attack");
        for (GarbageLine line : attack.lines()) {
            if (line.width() != Board.WIDTH) {
                throw new IllegalArgumentException("공격 줄은 " + Board.WIDTH + "칸이어야 합니다: " + line.width());
            }
        }
        int accepted = Math.min(MAX_LINES - lines.size(), attack.size());
        lines.addAll(attack.lines().subList(0, accepted));
        return accepted;
    }

    /** 대기 줄을 위에서 아래 순서로 모두 꺼내고 대기열을 비운다. */
    List<GarbageLine> drain() {
        List<GarbageLine> drained = List.copyOf(lines);
        lines.clear();
        return drained;
    }

    /** 대기 줄. 위에서 아래 순서이며 고칠 수 없는 복사본이다. */
    List<GarbageLine> lines() {
        return List.copyOf(lines);
    }

    boolean isEmpty() {
        return lines.isEmpty();
    }

    int size() {
        return lines.size();
    }

    /** 대기 줄을 모두 버린다. 게임을 다시 시작할 때 쓴다. */
    void clear() {
        lines.clear();
    }
}
