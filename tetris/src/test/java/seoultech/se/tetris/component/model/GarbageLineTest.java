package seoultech.se.tetris.component.model;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seoultech.se.tetris.SerializationTestSupport.roundTrip;
import static seoultech.se.tetris.component.model.GarbageLineFixtures.cells;
import static seoultech.se.tetris.component.model.GarbageLineFixtures.line;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("GarbageLine")
class GarbageLineTest {

    @Test
    @DisplayName("칸마다 채워졌는지를 그대로 담는다")
    void keepsCells() {
        GarbageLine line = line("XXX.XXXXXX");

        assertEquals(10, line.width());
        assertTrue(line.isFilled(0));
        assertFalse(line.isFilled(3));
        assertArrayEquals(cells("XXX.XXXXXX"), line.filled());
    }

    @Test
    @DisplayName("빈 칸이 하나도 없는 줄은 공격 줄이 될 수 없다")
    void rejectsFullLine() {
        assertThrows(IllegalArgumentException.class, () -> line("XXXXXXXXXX"));
    }

    @Test
    @DisplayName("칸 정보가 없으면 예외를 던진다")
    void rejectsNull() {
        assertThrows(NullPointerException.class, () -> new GarbageLine(null));
    }

    @Test
    @DisplayName("만들 때 넘긴 배열을 나중에 고쳐도 줄은 바뀌지 않는다")
    void copiesGivenArray() {
        boolean[] source = cells("XXX.XXXXXX");
        GarbageLine line = new GarbageLine(source);

        source[3] = true;

        assertFalse(line.isFilled(3));
    }

    @Test
    @DisplayName("돌려받은 배열을 고쳐도 줄은 바뀌지 않는다")
    void returnsCopy() {
        GarbageLine line = line("XXX.XXXXXX");

        line.filled()[3] = true;

        assertFalse(line.isFilled(3));
    }

    @Test
    @DisplayName("칸 내용이 같으면 같은 줄이다")
    void equalsByContent() {
        assertEquals(line("X.XXXXXXXX"), line("X.XXXXXXXX"));
        assertEquals(line("X.XXXXXXXX").hashCode(), line("X.XXXXXXXX").hashCode());
        assertNotEquals(line("X.XXXXXXXX"), line(".XXXXXXXXX"));
    }

    @Test
    @DisplayName("문자열로 바꾸면 채워진 칸은 X, 빈 칸은 .으로 보인다")
    void readableText() {
        assertEquals("GarbageLine[XX..XXXXXX]", line("XX..XXXXXX").toString());
    }

    @Test
    @DisplayName("직렬화했다가 다시 읽어도 같은 줄이다")
    void survivesSerialization() throws Exception {
        GarbageLine line = line("XXXX.XXXXX");

        assertEquals(line, roundTrip(line));
    }
}
