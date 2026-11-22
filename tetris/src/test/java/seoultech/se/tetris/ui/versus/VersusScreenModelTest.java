package seoultech.se.tetris.ui.versus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import seoultech.se.tetris.versus.EndReason;
import seoultech.se.tetris.versus.MatchResult;
import seoultech.se.tetris.versus.Side;
import seoultech.se.tetris.versus.VersusMode;

class VersusScreenModelTest {

    @Test
    void 제목에_모드_이름이_들어간다() {
        assertEquals("로컬 대전 · 일반 모드", new VersusScreenModel(VersusMode.NORMAL).title());
        assertEquals("로컬 대전 · 아이템 모드", new VersusScreenModel(VersusMode.ITEM).title());
        assertEquals("로컬 대전 · 시간제한 모드", new VersusScreenModel(VersusMode.TIME_LIMIT).title());
    }

    @Test
    void 플레이어_이름은_자리와_같다() {
        assertEquals("P1", VersusScreenModel.playerLabel(Side.P1));
        assertEquals("P2", VersusScreenModel.playerLabel(Side.P2));
    }

    @Test
    void 대전_중에는_승패와_결과를_표시하지_않는다() {
        VersusScreenModel model = new VersusScreenModel(VersusMode.NORMAL);

        assertFalse(model.isEnded());
        assertNull(model.overlayOf(Side.P1));
        assertNull(model.overlayOf(Side.P2));
        assertEquals("", model.resultText());
        assertEquals(VersusScreenModel.PLAYING_HINT, model.hintText());
    }

    @Test
    void 블럭을_쌓지_못해_끝나면_상대가_승리한다() {
        VersusScreenModel model = new VersusScreenModel(VersusMode.NORMAL);

        model.matchEnded(MatchResult.win(Side.P1, EndReason.TOP_OUT, 1200, 800));

        assertTrue(model.isEnded());
        assertEquals("승리", model.overlayOf(Side.P1));
        assertEquals("패배", model.overlayOf(Side.P2));
        assertEquals("P1 승리 (P2가 더 이상 블럭을 쌓을 수 없음)", model.resultText());
        assertEquals(VersusScreenModel.ENDED_HINT, model.hintText());
    }

    @Test
    void P2가_이기면_P2에_승리를_표시한다() {
        VersusScreenModel model = new VersusScreenModel(VersusMode.ITEM);

        model.matchEnded(MatchResult.win(Side.P2, EndReason.TOP_OUT, 0, 300));

        assertEquals("패배", model.overlayOf(Side.P1));
        assertEquals("승리", model.overlayOf(Side.P2));
        assertEquals("P2 승리 (P1가 더 이상 블럭을 쌓을 수 없음)", model.resultText());
    }

    @Test
    void 제한_시간이_끝나면_점수로_정한_결과를_표시한다() {
        VersusScreenModel model = new VersusScreenModel(VersusMode.TIME_LIMIT);

        model.matchEnded(MatchResult.win(Side.P2, EndReason.TIME_UP, 500, 900));

        assertEquals("P2 승리 (제한 시간 종료 · 500 : 900)", model.resultText());
    }

    @Test
    void 무승부는_두_보드에_모두_무승부를_표시한다() {
        VersusScreenModel model = new VersusScreenModel(VersusMode.TIME_LIMIT);

        model.matchEnded(MatchResult.draw(EndReason.TIME_UP, 700, 700));

        assertEquals("무승부", model.overlayOf(Side.P1));
        assertEquals("무승부", model.overlayOf(Side.P2));
        assertEquals("무승부 (제한 시간 종료 · 700 : 700)", model.resultText());
    }

    @Test
    void 상대가_나가면_기권으로_표시한다() {
        VersusScreenModel model = new VersusScreenModel(VersusMode.NORMAL);

        model.matchEnded(MatchResult.win(Side.P1, EndReason.FORFEIT, 0, 0));

        assertEquals("P1 승리 (P2 기권)", model.resultText());
    }

    @Test
    void 모드와_결과는_null일_수_없다() {
        assertThrows(NullPointerException.class, () -> new VersusScreenModel(null));
        VersusScreenModel model = new VersusScreenModel(VersusMode.NORMAL);
        assertThrows(NullPointerException.class, () -> model.matchEnded(null));
        assertThrows(NullPointerException.class, () -> model.overlayOf(null));
    }

    @Test
    void 모드를_돌려준다() {
        assertEquals(VersusMode.ITEM, new VersusScreenModel(VersusMode.ITEM).mode());
    }

    // ------------------------------------------------------------------
    // 남은 시간 (시간제한 모드)
    // ------------------------------------------------------------------

    @Test
    void 시계가_없으면_남은_시간을_표시하지_않는다() {
        VersusScreenModel model = new VersusScreenModel(VersusMode.NORMAL);

        assertFalse(model.hasTimer());
        assertEquals("", model.timerText());
        assertFalse(model.isTimerUrgent());
    }

    @Test
    void 시계가_있으면_남은_시간과_강조_여부를_표시한다() {
        long[] now = {0};
        CountdownClock clock = new CountdownClock(Duration.ofMinutes(3), () -> now[0]);
        VersusScreenModel model = new VersusScreenModel(VersusMode.TIME_LIMIT, clock);

        assertTrue(model.hasTimer());
        assertEquals("03:00", model.timerText());
        assertFalse(model.isTimerUrgent());

        clock.start();
        now[0] = 175_000;

        assertEquals("00:05", model.timerText());
        assertTrue(model.isTimerUrgent());
    }
}
