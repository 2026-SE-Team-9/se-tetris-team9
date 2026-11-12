package seoultech.se.tetris.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seoultech.se.tetris.component.model.GarbageLineFixtures.line;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import seoultech.se.tetris.component.model.Attack;
import seoultech.se.tetris.component.model.GarbageLine;

@DisplayName("GarbageQueue")
class GarbageQueueTest {

    private final GarbageQueue queue = new GarbageQueue();

    @Test
    @DisplayName("최대 줄 수는 10줄이다")
    void maxLines() {
        assertEquals(10, GarbageQueue.MAX_LINES);
    }

    @Test
    @DisplayName("빈 대기열에 공격을 넣으면 모든 줄이 순서대로 붙는다")
    void addsAllLines() {
        Attack attack = attack(3, 'a');

        assertEquals(3, queue.add(attack));
        assertEquals(attack.lines(), queue.lines());
    }

    @Test
    @DisplayName("여러 번의 공격은 기존 대기 줄의 아래에 붙는다")
    void laterAttackGoesBelow() {
        Attack first = attack(2, 'a');
        Attack second = attack(3, 'b');

        queue.add(first);
        queue.add(second);

        List<GarbageLine> expected = new ArrayList<>(first.lines());
        expected.addAll(second.lines());
        assertEquals(expected, queue.lines());
    }

    @Test
    @DisplayName("공간이 모자라면 공격의 위쪽 줄부터 남은 줄 수만큼만 붙는다 (대기 8줄 + 공격 3줄 → 위쪽 2줄)")
    void keepsUpperLinesWhenOverflowing() {
        queue.add(attack(4, 'a'));
        queue.add(attack(4, 'b'));
        Attack third = attack(3, 'c');

        assertEquals(2, queue.add(third));

        assertEquals(10, queue.size());
        assertEquals(third.lines().subList(0, 2), queue.lines().subList(8, 10));
    }

    @Test
    @DisplayName("이미 10줄이 차 있으면 공격은 무시된다")
    void ignoresWhenFull() {
        queue.add(attack(4, 'a'));
        queue.add(attack(4, 'b'));
        queue.add(attack(2, 'c'));
        List<GarbageLine> before = queue.lines();

        assertEquals(0, queue.add(attack(4, 'd')));
        assertEquals(before, queue.lines());
    }

    @Test
    @DisplayName("꺼내면 대기 줄 전체를 위에서 아래 순서로 돌려주고 대기열을 비운다")
    void drainTakesEverything() {
        queue.add(attack(2, 'a'));
        queue.add(attack(3, 'b'));
        List<GarbageLine> before = queue.lines();

        assertEquals(before, queue.drain());
        assertTrue(queue.isEmpty());
        assertEquals(0, queue.size());
    }

    @Test
    @DisplayName("돌려받은 대기 줄 목록은 고칠 수 없다")
    void linesAreUnmodifiable() {
        queue.add(attack(2, 'a'));

        assertThrows(UnsupportedOperationException.class, () -> queue.lines().clear());
        assertEquals(2, queue.size());
    }

    @Test
    @DisplayName("공격 줄의 폭이 보드 폭과 다르면 예외를 던지고 아무 줄도 붙이지 않는다")
    void rejectsWrongWidth() {
        Attack wrong = new Attack(List.of(line("XXX.XXXXXX"), line("XXX.XXXXX")));

        assertThrows(IllegalArgumentException.class, () -> queue.add(wrong));
        assertTrue(queue.isEmpty());
    }

    @Test
    @DisplayName("공격이 없으면 예외를 던진다")
    void rejectsNull() {
        assertThrows(NullPointerException.class, () -> queue.add(null));
    }

    @Test
    @DisplayName("비우면 대기 줄이 모두 사라진다")
    void clearRemovesEverything() {
        queue.add(attack(3, 'a'));

        queue.clear();

        assertTrue(queue.isEmpty());
    }

    /**
     * 줄마다 빈 칸 위치가 다른 공격. 순서를 확인할 수 있도록 공격마다 빈 칸 위치의 시작을 다르게 한다.
     *
     * @param count 줄 수
     * @param tag   공격 구분용 문자 ('a'부터). 빈 칸 열의 시작 위치를 정한다
     */
    private static Attack attack(int count, char tag) {
        List<GarbageLine> lines = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            int hole = ((tag - 'a') * 3 + i) % Board.WIDTH;
            StringBuilder text = new StringBuilder("XXXXXXXXXX");
            text.setCharAt(hole, '.');
            lines.add(line(text.toString()));
        }
        return new Attack(lines);
    }
}
