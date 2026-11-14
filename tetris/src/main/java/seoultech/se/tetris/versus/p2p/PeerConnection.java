package seoultech.se.tetris.versus.p2p;

/**
 * 연결이 완료된 상대 PC와의 통로. 네트워크 계층이 구현하고, P2P 대전 진행이 사용한다.
 *
 * <ul>
 *   <li>확인 신호({@link P2PMessage.Ping}, {@link P2PMessage.Pong})는 이 통로가 알아서 주고받고,
 *       그 결과를 {@link #state()}와 {@link #roundTripMillis()}로 보여준다. 리스너에는 전달하지 않는다</li>
 *   <li>리스너 호출은 연결을 만들 때 지정한 실행기에서 한다. 화면에서는 Swing 이벤트 스레드를 지정하므로,
 *       리스너 안에서 엔진과 화면을 바로 다뤄도 된다</li>
 * </ul>
 */
public interface PeerConnection {

    /** 이 PC의 역할 */
    Role role();

    /**
     * 메시지를 보낸다. 바로 돌아오며, 실제 전송은 네트워크 계층의 스레드가 한다.
     *
     * <p>아직 보내지 못한 {@link P2PMessage.Snapshot}이 있는데 새 {@code Snapshot}을 보내면, 이전 것은 버리고 최신 것만 보낸다.
     * 다른 메시지는 버리지 않고 보낸 순서대로 도착한다. 닫힌 뒤에 보낸 메시지는 무시한다.
     */
    void send(P2PMessage message);

    void addListener(PeerListener listener);

    void removeListener(PeerListener listener);

    /** 현재 연결 상태 */
    ConnectionState state();

    /** 최근 10회 왕복 시간의 평균(ms). 아직 잰 적이 없으면 -1 */
    int roundTripMillis();

    /** {@link P2PMessage.Bye}를 보내고 연결을 닫는다. 리스너에는 {@link CloseReason#LOCAL}로 알린다. 이미 닫혔으면 아무 일도 하지 않는다. */
    void close();
}
