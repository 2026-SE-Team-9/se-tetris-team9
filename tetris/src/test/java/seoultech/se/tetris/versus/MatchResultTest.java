package seoultech.se.tetris.versus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seoultech.se.tetris.SerializationTestSupport.roundTrip;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MatchResult")
class MatchResultTest {

    @Test
    @DisplayName("이긴 결과는 이긴 쪽만 승자이고 무승부가 아니다")
    void win() {
        MatchResult result = MatchResult.win(Side.P2, EndReason.TOP_OUT, 1200, 800);

        assertEquals(Side.P2, result.winner());
        assertTrue(result.isWinner(Side.P2));
        assertFalse(result.isWinner(Side.P1));
        assertFalse(result.isDraw());
        assertEquals(EndReason.TOP_OUT, result.reason());
    }

    @Test
    @DisplayName("무승부는 양쪽 모두 승자가 아니다")
    void draw() {
        MatchResult result = MatchResult.draw(EndReason.TIME_UP, 500, 500);

        assertNull(result.winner());
        assertTrue(result.isDraw());
        assertFalse(result.isWinner(Side.P1));
        assertFalse(result.isWinner(Side.P2));
    }

    @Test
    @DisplayName("무승부는 제한 시간 종료(동점)와 블럭을 쌓을 수 없게 된 경우(P2P 엇갈림)에 생길 수 있다")
    void drawReasons() {
        assertTrue(MatchResult.draw(EndReason.TIME_UP, 300, 300).isDraw());
        assertTrue(MatchResult.draw(EndReason.TOP_OUT, 100, 400).isDraw());
    }

    @Test
    @DisplayName("상대가 나간 경우는 무승부가 될 수 없다")
    void forfeitNeedsWinner() {
        assertThrows(IllegalArgumentException.class, () -> MatchResult.draw(EndReason.FORFEIT, 0, 0));
        assertTrue(MatchResult.win(Side.P1, EndReason.FORFEIT, 0, 300).isWinner(Side.P1));
    }

    @Test
    @DisplayName("자리마다 그 자리의 최종 점수를 알려준다")
    void scoreBySide() {
        MatchResult result = MatchResult.win(Side.P1, EndReason.TIME_UP, 1500, 900);

        assertEquals(1500, result.score(Side.P1));
        assertEquals(900, result.score(Side.P2));
    }

    @Test
    @DisplayName("점수가 음수면 예외를 던진다")
    void rejectsNegativeScore() {
        assertThrows(IllegalArgumentException.class, () -> MatchResult.win(Side.P1, EndReason.TOP_OUT, -1, 0));
        assertThrows(IllegalArgumentException.class, () -> MatchResult.draw(EndReason.TIME_UP, 0, -1));
    }

    @Test
    @DisplayName("끝난 사유나 승자, 묻는 자리가 없으면 예외를 던진다")
    void rejectsNull() {
        assertThrows(NullPointerException.class, () -> MatchResult.draw(null, 0, 0));
        assertThrows(NullPointerException.class, () -> MatchResult.win(null, EndReason.TOP_OUT, 0, 0));

        MatchResult result = MatchResult.draw(EndReason.TIME_UP, 0, 0);
        assertThrows(NullPointerException.class, () -> result.isWinner(null));
        assertThrows(NullPointerException.class, () -> result.score(null));
    }

    @Test
    @DisplayName("직렬화했다가 다시 읽어도 같은 결과다")
    void survivesSerialization() throws Exception {
        MatchResult win = MatchResult.win(Side.P2, EndReason.FORFEIT, 100, 200);
        MatchResult draw = MatchResult.draw(EndReason.TIME_UP, 300, 300);

        assertEquals(win, roundTrip(win));
        assertEquals(draw, roundTrip(draw));
    }
}
