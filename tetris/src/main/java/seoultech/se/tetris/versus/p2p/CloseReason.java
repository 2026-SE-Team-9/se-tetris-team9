package seoultech.se.tetris.versus.p2p;

/**
 * 연결이 닫힌 이유. 화면은 이유에 맞는 메시지를 표시하고 P2P 대전 모드 첫 화면으로 돌아간다.
 */
public enum CloseReason {

    /** 이 PC의 플레이어가 직접 끊음 */
    LOCAL,

    /** 상대가 직접 끊음 ({@link P2PMessage.Bye}를 받음) */
    PEER_LEFT,

    /** 마지막 신호를 받은 지 10초가 지남 */
    TIMED_OUT,

    /** 상대가 알림 없이 끊었거나, 통신 중 오류가 남. 에러 메시지를 표시한다 (3차 팀 결정) */
    FAILED
}
