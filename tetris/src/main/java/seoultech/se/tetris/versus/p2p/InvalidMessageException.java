package seoultech.se.tetris.versus.p2p;

import java.io.IOException;

/**
 * 받은 데이터를 메시지로 받아들일 수 없다. 상대가 같은 프로그램이 아니거나 데이터가 손상된 경우이며,
 * 받은 쪽은 통신 오류로 보고 연결을 닫는다.
 */
final class InvalidMessageException extends IOException {

    private static final long serialVersionUID = 1L;

    InvalidMessageException(String message) {
        super(message);
    }

    InvalidMessageException(String message, Throwable cause) {
        super(message, cause);
    }
}
