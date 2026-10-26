package seoultech.se.tetris.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import seoultech.se.tetris.engine.Difficulty;

class DifficultyLabelsTest {

    @Test
    void 난이도마다_한글_이름이_있다() {
        assertEquals("쉬움", DifficultyLabels.of(Difficulty.EASY));
        assertEquals("중간", DifficultyLabels.of(Difficulty.NORMAL));
        assertEquals("어려움", DifficultyLabels.of(Difficulty.HARD));
    }

    @Test
    void 이름은_서로_다르다() {
        Set<String> names = new HashSet<>();
        for (Difficulty difficulty : Difficulty.values()) {
            names.add(DifficultyLabels.of(difficulty));
        }

        assertEquals(Difficulty.values().length, names.size());
    }

    @Test
    void 난이도가_없으면_거부한다() {
        assertThrows(NullPointerException.class, () -> DifficultyLabels.of(null));
    }
}
