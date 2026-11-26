package seoultech.se.tetris.ui.versus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import seoultech.se.tetris.versus.EndReason;
import seoultech.se.tetris.versus.MatchResult;
import seoultech.se.tetris.versus.Side;
import seoultech.se.tetris.versus.VersusMode;
import seoultech.se.tetris.versus.p2p.Role;

class VersusPresentationTest {

    @Test
    void 로컬_대전은_P1이_왼쪽이고_자리_이름을_쓴다() {
        VersusPresentation local = VersusPresentation.LOCAL;

        assertSame(Side.P1, local.leftSide());
        assertSame(Side.P2, local.rightSide());
        assertNull(local.mySide());
        assertEquals("P1", local.labelOf(Side.P1));
        assertEquals("P2", local.labelOf(Side.P2));
        assertEquals(VersusScreenModel.PLAYING_HINT, local.playingHint());
    }

    @Test
    void 방장의_P2P_대전은_내_보드가_왼쪽이다() {
        VersusPresentation server = VersusPresentation.p2p(Role.SERVER);

        assertSame(Side.P1, server.leftSide());
        assertSame(Side.P1, server.mySide());
        assertEquals("나 (방장)", server.labelOf(Side.P1));
        assertEquals("상대 (참여자)", server.labelOf(Side.P2));
    }

    @Test
    void 참여자의_P2P_대전도_내_보드가_왼쪽이다() {
        VersusPresentation client = VersusPresentation.p2p(Role.CLIENT);

        assertSame(Side.P2, client.leftSide());
        assertSame(Side.P1, client.rightSide());
        assertSame(Side.P2, client.mySide());
        assertEquals("나 (참여자)", client.labelOf(Side.P2));
        assertEquals("상대 (방장)", client.labelOf(Side.P1));
    }

    @Test
    void P2P_대전은_게임_나가기와_대기실_복귀를_안내한다() {
        VersusScreenModel model = new VersusScreenModel(VersusMode.ITEM, null, VersusPresentation.p2p(Role.SERVER));

        assertEquals("P2P 대전 · 아이템 모드", model.title());
        assertEquals(VersusPresentation.P2P_PLAYING_HINT, model.hintText());
        model.setCountdownSeconds(3);
        assertEquals(VersusPresentation.P2P_COUNTDOWN_HINT, model.hintText());
        model.matchEnded(MatchResult.win(Side.P1, EndReason.FORFEIT, 0, 0));
        assertEquals(VersusPresentation.P2P_ENDED_HINT, model.hintText());
    }

    @Test
    void P2P_대전_결과는_내_관점으로_보여준다() {
        VersusScreenModel server = new VersusScreenModel(VersusMode.NORMAL, null,
                VersusPresentation.p2p(Role.SERVER));
        VersusScreenModel client = new VersusScreenModel(VersusMode.NORMAL, null,
                VersusPresentation.p2p(Role.CLIENT));
        MatchResult clientForfeited = MatchResult.win(Side.P1, EndReason.FORFEIT, 100, 50);

        server.matchEnded(clientForfeited);
        client.matchEnded(clientForfeited);

        assertEquals("승리 (상대 기권)", server.resultText());
        assertEquals("패배 (기권)", client.resultText());
        assertEquals("승리", server.overlayOf(Side.P1));
        assertEquals("패배", client.overlayOf(Side.P2));
    }

    @Test
    void 내_관점의_결과는_모든_종료_사유를_나타낸다() {
        assertEquals("승리 (상대가 더 이상 블럭을 쌓을 수 없음)", VersusScreenModel.perspectiveText(
                MatchResult.win(Side.P2, EndReason.TOP_OUT, 0, 0), Side.P2));
        assertEquals("패배 (더 이상 블럭을 쌓을 수 없음)", VersusScreenModel.perspectiveText(
                MatchResult.win(Side.P2, EndReason.TOP_OUT, 0, 0), Side.P1));
        // 엇갈림은 무승부다
        assertEquals("무승부 (두 플레이어 모두 더 이상 블럭을 쌓을 수 없음)", VersusScreenModel.perspectiveText(
                MatchResult.draw(EndReason.TOP_OUT, 0, 0), Side.P1));
        assertEquals("패배 (제한 시간 종료 · 나 300 : 상대 900)", VersusScreenModel.perspectiveText(
                MatchResult.win(Side.P1, EndReason.TIME_UP, 900, 300), Side.P2));
        assertEquals("무승부 (제한 시간 종료 · 나 700 : 상대 700)", VersusScreenModel.perspectiveText(
                MatchResult.draw(EndReason.TIME_UP, 700, 700), Side.P1));
    }

    @Test
    void 필요한_값이_없으면_만들_수_없다() {
        assertThrows(NullPointerException.class, () -> VersusPresentation.p2p(null));
        assertThrows(NullPointerException.class,
                () -> new VersusPresentation(null, Side.P1, null, "a", "b", "", "", ""));
        assertThrows(NullPointerException.class,
                () -> new VersusScreenModel(VersusMode.NORMAL, null, null));
        assertThrows(NullPointerException.class, () -> VersusPresentation.LOCAL.labelOf(null));
        assertThrows(NullPointerException.class,
                () -> VersusScreenModel.perspectiveText(null, Side.P1));
        assertTrue(VersusPresentation.LOCAL.titlePrefix().startsWith("로컬"));
    }
}
