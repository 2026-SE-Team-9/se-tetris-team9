package seoultech.se.tetris.versus.p2p;

/**
 * 연결을 만드는 과정을 받는 쪽. 접속 과정의 각 단계마다 그 단계를 나타내는 메시지를 표시한다. (3차 팀 결정)
 */
public interface ConnectListener {

    /** 연결 과정의 단계 */
    enum Step {

        /** 서버: 포트를 열고 있음 */
        OPENING,

        /** 서버: 클라이언트의 접속을 기다리는 중 */
        WAITING,

        /** 클라이언트: 서버에 접속하는 중 */
        CONNECTING,

        /** 양쪽: 같은 판의 프로그램인지 확인하는 중 ({@link P2PMessage.Hello}) */
        CHECKING_VERSION
    }

    /** 다음 단계로 넘어갔을 때 */
    void onStep(Step step);

    /** 연결이 완료됐을 때. 이후로는 받은 통로로 주고받는다. */
    void onConnected(PeerConnection connection);

    /**
     * 연결하지 못했을 때. 이후로는 호출되지 않는다.
     *
     * @param reason 화면에 표시할 실패 이유
     */
    void onFailed(FailReason reason);

    /** 연결하지 못한 이유 */
    enum FailReason {

        /** 서버: 포트가 이미 사용 중이거나 열 수 없음 */
        PORT_UNAVAILABLE,

        /** 클라이언트: 주소 형식이 잘못됨 */
        INVALID_ADDRESS,

        /** 클라이언트: 서버가 없거나 응답하지 않음 */
        UNREACHABLE,

        /** 양쪽: 프로그램 판이 다름 */
        VERSION_MISMATCH,

        /** 그 밖의 통신 오류 */
        ERROR
    }
}
