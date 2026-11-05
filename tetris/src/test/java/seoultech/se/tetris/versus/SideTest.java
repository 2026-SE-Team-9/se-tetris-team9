package seoultech.se.tetris.versus;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Side")
class SideTest {

    @Test
    @DisplayName("P1의 상대는 P2, P2의 상대는 P1이다")
    void opponent() {
        assertEquals(Side.P2, Side.P1.opponent());
        assertEquals(Side.P1, Side.P2.opponent());
    }

    @Test
    @DisplayName("상대의 상대는 자기 자신이다")
    void opponentOfOpponent() {
        for (Side side : Side.values()) {
            assertEquals(side, side.opponent().opponent());
        }
    }
}
