package seoultech.se.tetris;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * 직렬화 테스트용 도우미.
 *
 * <p>P2P 대전 모드에서 주고받는 객체가 네트워크를 거쳐도 같은 값으로 복원되는지 확인할 때 쓴다.
 */
public final class SerializationTestSupport {

    private SerializationTestSupport() {
    }

    /** 직렬화했다가 다시 읽은 객체. 네트워크로 보냈다가 받은 것과 같다. */
    @SuppressWarnings("unchecked")
    public static <T> T roundTrip(T value) throws IOException, ClassNotFoundException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(value);
        }
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            return (T) in.readObject();
        }
    }
}
