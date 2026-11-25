package seoultech.se.tetris.ui.p2p;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import seoultech.se.tetris.ui.p2p.P2PScreenState.InputError;
import seoultech.se.tetris.versus.p2p.CloseReason;
import seoultech.se.tetris.versus.p2p.ConnectListener.FailReason;
import seoultech.se.tetris.versus.p2p.ConnectListener.Step;
import seoultech.se.tetris.versus.p2p.ConnectionState;
import seoultech.se.tetris.versus.p2p.Role;

class P2PScreenTextsTest {

    @Test
    void 첫_화면_항목은_방_생성_참여_뒤로_가기다() {
        assertEquals(List.of("방 생성", "참여", "뒤로 가기"), P2PScreenTexts.MENU_ITEMS);
        assertEquals(P2PScreenState.MENU_ITEM_COUNT, P2PScreenTexts.MENU_ITEMS.size());
        assertEquals(P2PScreenTexts.MENU_ITEMS.size(), P2PScreenTexts.MENU_DESCRIPTIONS.size());
    }

    @Test
    void 진행_단계마다_문구가_있다() {
        assertEquals("포트를 여는 중...", P2PScreenTexts.step(Step.OPENING));
        assertEquals("상대가 참여하기를 기다리는 중...", P2PScreenTexts.step(Step.WAITING));
        assertEquals("서버에 접속하는 중...", P2PScreenTexts.step(Step.CONNECTING));
        assertEquals("프로그램 버전을 확인하는 중...", P2PScreenTexts.step(Step.CHECKING_VERSION));
    }

    @Test
    void 실패_이유마다_안내가_있고_포트를_열_수_없으면_포트를_보여준다() {
        for (FailReason reason : FailReason.values()) {
            assertFalse(P2PScreenTexts.failure(reason, 24700).isBlank(), reason.name());
        }
        assertTrue(P2PScreenTexts.failure(FailReason.PORT_UNAVAILABLE, 31000).contains("31000"));
        assertTrue(P2PScreenTexts.failure(FailReason.UNREACHABLE, 0).contains("다시 입력"));
        assertTrue(P2PScreenTexts.failure(FailReason.VERSION_MISMATCH, 0).contains("버전"));
    }

    @Test
    void 잘못된_입력은_다시_입력할_형식을_알려준다() {
        assertTrue(P2PScreenTexts.inputError(InputError.INVALID_IP).contains("IPv4"));
        assertTrue(P2PScreenTexts.inputError(InputError.INVALID_PORT).contains("1~65535"));
    }

    @Test
    void 끊긴_이유를_안내하고_직접_나간_경우는_안내하지_않는다() {
        assertNull(P2PScreenTexts.close(CloseReason.LOCAL));
        assertEquals("상대가 대기실을 나갔습니다", P2PScreenTexts.close(CloseReason.PEER_LEFT));
        assertTrue(P2PScreenTexts.close(CloseReason.TIMED_OUT).contains("응답"));
        assertEquals("오류로 연결이 끊어졌습니다", P2PScreenTexts.close(CloseReason.FAILED));

        assertFalse(P2PScreenTexts.isCloseError(CloseReason.PEER_LEFT));
        assertFalse(P2PScreenTexts.isCloseError(CloseReason.LOCAL));
        assertTrue(P2PScreenTexts.isCloseError(CloseReason.TIMED_OUT));
        assertTrue(P2PScreenTexts.isCloseError(CloseReason.FAILED));
    }

    @Test
    void 방_생성_주소는_주소마다_포트와_함께_한_줄이다() {
        assertEquals(List.of("192.168.0.12 : 24700", "10.0.0.4 : 24700"),
                P2PScreenTexts.hostAddressLines(List.of("192.168.0.12", "10.0.0.4"), 24700));
        assertEquals(List.of(P2PScreenTexts.NO_ADDRESS), P2PScreenTexts.hostAddressLines(List.of(), 24700));
    }


    @Test
    void 대기실의_준비_상태와_게임_시작_버튼을_나타낸다() {
        assertEquals("준비 완료", P2PScreenTexts.readyStatus(true));
        assertEquals("대기 중", P2PScreenTexts.readyStatus(false));
        assertEquals("게임 시작", P2PScreenTexts.startButton(false));
        assertEquals("준비 취소", P2PScreenTexts.startButton(true));
    }

    @Test
    void 방장이_바꿀_수_있는_설정은_양옆에_화살표를_붙인다() {
        assertEquals("◀  일반 모드  ▶", P2PScreenTexts.choice("일반 모드", true));
        assertEquals("일반 모드", P2PScreenTexts.choice("일반 모드", false));
    }

    @Test
    void 대기실_키_안내는_역할마다_다르다() {
        assertTrue(P2PScreenTexts.roomHint(Role.SERVER).contains("← → 변경"));
        assertFalse(P2PScreenTexts.roomHint(Role.CLIENT).contains("변경"));
    }


    @Test
    void 게임_나가기_메뉴는_계속하기와_나가기이고_게임이_멈추지_않는다고_알린다() {
        assertEquals(List.of("계속하기", "게임 나가기 (기권패)"), P2PScreenTexts.EXIT_ITEMS);
        assertTrue(P2PScreenTexts.EXIT_NOTE.contains("계속 진행"));
    }

    @Test
    void 연결_상태와_왕복_시간을_나타낸다() {
        assertEquals("연결 상태: 정상", P2PScreenTexts.connection(ConnectionState.NORMAL, -1));
        assertEquals("연결 상태: 지연 · 320ms", P2PScreenTexts.connection(ConnectionState.LAGGING, 320));
        assertEquals("연결 상태: 확인 중 · 5ms", P2PScreenTexts.connection(ConnectionState.CHECKING, 5));
        assertEquals("연결 상태: 끊김", P2PScreenTexts.connection(ConnectionState.DISCONNECTED, -1));
    }
}
