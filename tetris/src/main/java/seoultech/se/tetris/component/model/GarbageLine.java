package seoultech.se.tetris.component.model;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;

/**
 * 대전 모드에서 상대에게 넘어가는 공격 줄 하나.
 *
 * <p>칸마다 채워졌는지만 가진다. 넘어간 칸은 원래 블럭이나 아이템과 관계없이 모두
 * 공격 칸({@link seoultech.se.tetris.blocks.CellCode#GARBAGE})이 되므로 칸 번호는 보관하지 않는다. (3차 팀 결정)
 *
 * <p>줄을 지운 블럭의 칸이 비어서 넘어가므로 빈 칸이 하나 이상 있어야 한다. 꽉 찬 줄은 공격 줄이 될 수 없다.
 *
 * <p>엔진과 화면·대전 진행이 함께 쓰는 값이므로 엔진({@code engine} 패키지)을 모른다.
 * 그래서 줄의 폭이 보드 폭과 같은지는 이 객체가 아니라 공격을 받는 쪽이 검사한다.
 *
 * <p>배열을 받을 때와 돌려줄 때 모두 복사하므로 이 객체는 불변이다.
 */
public record GarbageLine(boolean[] filled) implements Serializable {

    /**
     * @param filled 칸마다 채워졌는지. 왼쪽 열부터
     * @throws IllegalArgumentException 빈 칸이 없는 경우
     */
    public GarbageLine {
        Objects.requireNonNull(filled, "filled");
        filled = filled.clone();
        boolean hasHole = false;
        for (boolean cell : filled) {
            if (!cell) {
                hasHole = true;
                break;
            }
        }
        if (!hasHole) {
            throw new IllegalArgumentException("빈 칸이 없는 줄은 공격 줄이 될 수 없습니다.");
        }
    }

    /** 칸마다 채워졌는지. 복사본을 돌려준다. */
    @Override
    public boolean[] filled() {
        return filled.clone();
    }

    /** 칸 수 */
    public int width() {
        return filled.length;
    }

    /** 지정한 열이 채워졌는지 */
    public boolean isFilled(int col) {
        return filled[col];
    }

    // record의 기본 equals·hashCode는 배열을 참조로 비교하므로 내용으로 비교하도록 바꾼다.

    @Override
    public boolean equals(Object other) {
        return other instanceof GarbageLine line && Arrays.equals(filled, line.filled);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(filled);
    }

    @Override
    public String toString() {
        StringBuilder text = new StringBuilder(filled.length);
        for (boolean cell : filled) {
            text.append(cell ? 'X' : '.');
        }
        return "GarbageLine[" + text + "]";
    }
}
