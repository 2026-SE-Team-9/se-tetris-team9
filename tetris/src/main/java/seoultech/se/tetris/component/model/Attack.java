package seoultech.se.tetris.component.model;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/**
 * 한 번의 줄 공격. (3차 「줄 공격」)
 *
 * <p>줄은 보내는 쪽 보드에 있던 순서대로 <b>위에서 아래로</b> 담는다.
 * 받는 쪽은 이 순서 그대로 대기 줄 아래에 붙이고, 넘치면 위쪽 줄부터 남는 만큼만 붙인다.
 */
public record Attack(List<GarbageLine> lines) implements Serializable {

    /** 공격이 되기 위한 최소 줄 수 */
    public static final int MIN_LINES = 2;

    /**
     * @param lines 위에서 아래 순서의 공격 줄
     * @throws IllegalArgumentException 줄이 {@link #MIN_LINES}개보다 적은 경우
     */
    public Attack {
        lines = List.copyOf(Objects.requireNonNull(lines, "lines"));
        if (lines.size() < MIN_LINES) {
            throw new IllegalArgumentException("공격은 " + MIN_LINES + "줄 이상이어야 합니다: " + lines.size());
        }
    }

    /** 공격 줄 수 */
    public int size() {
        return lines.size();
    }
}
