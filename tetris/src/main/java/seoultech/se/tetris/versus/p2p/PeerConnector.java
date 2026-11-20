package seoultech.se.tetris.versus.p2p;

import java.util.List;

/**
 * 상대 PC와 연결을 만든다. 네트워크 계층이 구현하고, P2P 대전 모드 첫 화면이 사용한다.
 *
 * <p>두 메서드 모두 바로 돌아오며, 진행 상황은 {@link ConnectListener}로 알린다.
 * 리스너 호출은 구현을 만들 때 지정한 실행기에서 한다.
 */
public interface PeerConnector {

    /**
     * 서버로서 접속을 기다린다. 클라이언트 하나가 접속하면 더 이상 기다리지 않는다.
     *
     * @param port 기다릴 포트. 설정 화면에서 정한 값을 넘긴다
     * @return 화면에 표시할 주소와 대기 취소 수단
     */
    Hosting host(int port, ConnectListener listener);

    /**
     * 클라이언트로서 서버에 접속한다.
     *
     * @param address 서버의 IPv4 주소
     * @return 접속 취소 수단
     */
    Pending connect(String address, int port, ConnectListener listener);

    /** 진행 중인 접속 시도 */
    interface Pending {

        /** 그만둔다. 리스너에는 더 이상 알리지 않는다. 이미 끝났으면 아무 일도 하지 않는다. */
        void cancel();
    }

    /** 서버의 접속 대기 */
    interface Hosting extends Pending {

        /** 클라이언트가 입력할 이 PC의 IPv4 주소들. 네트워크 장치가 여러 개면 여러 개일 수 있다. */
        List<String> addresses();

        /** 기다리는 포트 */
        int port();
    }
}
