package seoultech.se.tetris.ui.versus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import seoultech.se.tetris.versus.EndReason;
import seoultech.se.tetris.versus.MatchResult;
import seoultech.se.tetris.versus.Side;
import seoultech.se.tetris.versus.VersusMode;

class VersusEndModelTest {

    private final List<String> log = new ArrayList<>();

    private VersusEndModel model(VersusMode mode, MatchResult result) {
        return new VersusEndModel(mode, result, () -> log.add("restart"), () -> log.add("menu"));
    }

    @Test
    void 블럭을_쌓지_못해_끝나면_상대의_승리와_사유를_보여준다() {
        VersusEndModel model = model(VersusMode.NORMAL, MatchResult.win(Side.P1, EndReason.TOP_OUT, 1200, 800));

        assertEquals("대전 종료", model.title());
        assertEquals("로컬 대전 · 일반 모드", model.modeText());
        assertEquals("P1 승리", model.outcomeText());
        assertEquals("P2가 더 이상 블럭을 쌓을 수 없음", model.reasonText());
        assertSame(Side.P1, model.winner());
        assertEquals("승리", model.outcomeOf(Side.P1));
        assertEquals("패배", model.outcomeOf(Side.P2));
    }

    @Test
    void 두_플레이어의_최종_점수를_보여준다() {
        VersusEndModel model = model(VersusMode.ITEM, MatchResult.win(Side.P2, EndReason.TOP_OUT, 300, 4500));

        assertEquals(300, model.scoreOf(Side.P1));
        assertEquals(4500, model.scoreOf(Side.P2));
        assertEquals("로컬 대전 · 아이템 모드", model.modeText());
    }

    @Test
    void 제한_시간이_끝나면_시간_종료를_사유로_보여준다() {
        VersusEndModel model = model(VersusMode.TIME_LIMIT, MatchResult.win(Side.P2, EndReason.TIME_UP, 500, 900));

        assertEquals("P2 승리", model.outcomeText());
        assertEquals("제한 시간 종료", model.reasonText());
    }

    @Test
    void 점수가_같으면_두_플레이어_모두_무승부다() {
        VersusEndModel model = model(VersusMode.TIME_LIMIT, MatchResult.draw(EndReason.TIME_UP, 700, 700));

        assertEquals("무승부", model.outcomeText());
        assertNull(model.winner());
        assertEquals("무승부", model.outcomeOf(Side.P1));
        assertEquals("무승부", model.outcomeOf(Side.P2));
    }

    @Test
    void 기권은_기권한_플레이어를_사유로_보여준다() {
        VersusEndModel model = model(VersusMode.NORMAL, MatchResult.win(Side.P1, EndReason.FORFEIT, 0, 0));

        assertEquals("P2 기권", model.reasonText());
    }

    @Test
    void 메뉴는_게임_다시하기와_시작_메뉴로_돌아가기다() {
        VersusEndModel model = model(VersusMode.NORMAL, MatchResult.win(Side.P1, EndReason.TOP_OUT, 0, 0));

        assertEquals(2, model.getItems().size());
        assertEquals("게임 다시하기", model.getItems().get(0).label());
        assertEquals("시작 메뉴로 돌아가기", model.getItems().get(1).label());
        assertEquals(0, model.getSelectedIndex());
    }

    @Test
    void 첫_항목을_선택하면_게임을_다시_한다() {
        VersusEndModel model = model(VersusMode.NORMAL, MatchResult.win(Side.P1, EndReason.TOP_OUT, 0, 0));

        model.select();

        assertEquals(List.of("restart"), log);
    }

    @Test
    void 아래로_이동해_선택하면_시작_메뉴로_돌아가고_위로_순환한다() {
        VersusEndModel model = model(VersusMode.NORMAL, MatchResult.win(Side.P1, EndReason.TOP_OUT, 0, 0));

        model.moveDown();
        assertEquals(1, model.getSelectedIndex());
        model.select();
        assertEquals(List.of("menu"), log);

        model.moveDown();
        assertEquals(0, model.getSelectedIndex());
        model.moveUp();
        assertEquals(1, model.getSelectedIndex());
    }

    @Test
    void 사용하지_않는_키를_누르면_키_안내를_보여주고_이동하면_숨긴다() {
        VersusEndModel model = model(VersusMode.NORMAL, MatchResult.win(Side.P1, EndReason.TOP_OUT, 0, 0));

        model.rejectInput();
        assertTrue(model.isKeyHintVisible());

        model.moveDown();
        assertFalse(model.isKeyHintVisible());
    }

    @Test
    void 모드와_결과를_그대로_가진다() {
        MatchResult result = MatchResult.win(Side.P1, EndReason.TOP_OUT, 10, 0);
        VersusEndModel model = model(VersusMode.ITEM, result);

        assertSame(VersusMode.ITEM, model.mode());
        assertSame(result, model.result());
    }

    @Test
    void 필요한_값이_없으면_만들_수_없다() {
        MatchResult result = MatchResult.win(Side.P1, EndReason.TOP_OUT, 0, 0);
        Runnable none = () -> { };

        assertThrows(NullPointerException.class, () -> new VersusEndModel(null, result, none, none));
        assertThrows(NullPointerException.class, () -> new VersusEndModel(VersusMode.NORMAL, null, none, none));
        assertThrows(NullPointerException.class, () -> new VersusEndModel(VersusMode.NORMAL, result, null, none));
        assertThrows(NullPointerException.class, () -> new VersusEndModel(VersusMode.NORMAL, result, none, null));
        assertThrows(NullPointerException.class,
                () -> model(VersusMode.NORMAL, result).outcomeOf(null));
    }
}
