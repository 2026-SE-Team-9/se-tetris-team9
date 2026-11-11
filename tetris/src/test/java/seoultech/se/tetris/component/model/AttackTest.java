package seoultech.se.tetris.component.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static seoultech.se.tetris.SerializationTestSupport.roundTrip;
import static seoultech.se.tetris.component.model.GarbageLineFixtures.line;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Attack")
class AttackTest {

    private static final GarbageLine TOP = line("XXX.XXXXXX");
    private static final GarbageLine MIDDLE = line("XXX..XXXXX");
    private static final GarbageLine BOTTOM = line("XXXX.XXXXX");

    @Test
    @DisplayName("공격이 되려면 2줄 이상이어야 한다")
    void minimumLines() {
        assertEquals(2, Attack.MIN_LINES);
        assertEquals(2, new Attack(List.of(TOP, BOTTOM)).size());
    }

    @Test
    @DisplayName("2줄보다 적으면 공격이 될 수 없다")
    void rejectsTooFewLines() {
        assertThrows(IllegalArgumentException.class, () -> new Attack(List.of(TOP)));
        assertThrows(IllegalArgumentException.class, () -> new Attack(List.of()));
    }

    @Test
    @DisplayName("줄 목록이나 줄이 없으면 예외를 던진다")
    void rejectsNull() {
        assertThrows(NullPointerException.class, () -> new Attack(null));
        assertThrows(NullPointerException.class, () -> new Attack(Arrays.asList(TOP, null)));
    }

    @Test
    @DisplayName("줄을 받은 순서(위에서 아래) 그대로 담는다")
    void keepsOrder() {
        Attack attack = new Attack(List.of(TOP, MIDDLE, BOTTOM));

        assertEquals(List.of(TOP, MIDDLE, BOTTOM), attack.lines());
        assertEquals(3, attack.size());
    }

    @Test
    @DisplayName("만들 때 넘긴 목록을 나중에 고쳐도 공격은 바뀌지 않는다")
    void copiesGivenList() {
        List<GarbageLine> source = new ArrayList<>(List.of(TOP, BOTTOM));
        Attack attack = new Attack(source);

        source.add(MIDDLE);

        assertEquals(2, attack.size());
    }

    @Test
    @DisplayName("돌려받은 목록은 고칠 수 없다")
    void linesAreUnmodifiable() {
        Attack attack = new Attack(List.of(TOP, BOTTOM));

        assertThrows(UnsupportedOperationException.class, () -> attack.lines().add(MIDDLE));
    }

    @Test
    @DisplayName("직렬화했다가 다시 읽어도 같은 공격이다")
    void survivesSerialization() throws Exception {
        Attack attack = new Attack(List.of(TOP, MIDDLE, BOTTOM));

        assertEquals(attack, roundTrip(attack));
    }
}
