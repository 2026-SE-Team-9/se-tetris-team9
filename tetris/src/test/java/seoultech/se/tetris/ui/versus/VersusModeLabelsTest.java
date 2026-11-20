package seoultech.se.tetris.ui.versus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import seoultech.se.tetris.versus.VersusMode;

class VersusModeLabelsTest {

    @Test
    void 모드마다_화면_이름이_있다() {
        assertEquals("일반 모드", VersusModeLabels.of(VersusMode.NORMAL));
        assertEquals("아이템 모드", VersusModeLabels.of(VersusMode.ITEM));
        assertEquals("시간제한 모드", VersusModeLabels.of(VersusMode.TIME_LIMIT));
    }

    @Test
    void 모드_이름은_서로_다르다() {
        long distinct = Arrays.stream(VersusMode.values()).map(VersusModeLabels::of).distinct().count();

        assertEquals(VersusMode.values().length, distinct);
    }

    @Test
    void 모드는_null일_수_없다() {
        assertThrows(NullPointerException.class, () -> VersusModeLabels.of(null));
    }
}
