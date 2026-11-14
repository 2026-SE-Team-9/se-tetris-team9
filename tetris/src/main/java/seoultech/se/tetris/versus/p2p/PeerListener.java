package seoultech.se.tetris.versus.p2p;

/**
 * 상대 PC와의 통로({@link PeerConnection})에서 일어난 일을 받는 쪽.
 */
public interface PeerListener {

    /** 상대가 보낸 메시지를 받았을 때. 확인 신호와 {@link P2PMessage.Bye}는 전달하지 않는다. */
    void onMessage(P2PMessage message);

    /**
     * 연결 상태나 왕복 시간이 바뀌었을 때. 화면의 왕복 시간 표시와 지연 표시에 쓴다.
     * 필요한 리스너만 구현하도록 기본 동작은 아무것도 하지 않는 것이다.
     */
    default void onStateChanged(ConnectionState state, int roundTripMillis) {
    }

    /** 연결이 닫혔을 때. 한 번만 호출되며, 이후에는 메시지가 오지 않는다. */
    void onClosed(CloseReason reason);
}
