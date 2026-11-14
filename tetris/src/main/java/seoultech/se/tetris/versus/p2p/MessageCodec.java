package seoultech.se.tetris.versus.p2p;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.util.Objects;

/**
 * 메시지를 바이트로 바꾸고 되돌린다.
 *
 * <p>통로에는 메시지마다 [길이(4바이트) + 직렬화한 내용]을 하나씩 쓴다.
 * 메시지마다 직렬화 스트림을 새로 만드는 이유는 다음과 같다.
 * <ul>
 *   <li>하나의 직렬화 스트림을 계속 쓰면, 이미 보낸 객체는 내용이 바뀌어도 다시 보내지 않는다.
 *       메시지마다 새로 만들면 항상 지금 내용을 보낸다</li>
 *   <li>받은 데이터 검사({@link MessageFilter})의 크기·개수 한도가 스트림 전체에 누적되므로,
 *       메시지마다 새로 만들어야 한도를 메시지 하나 기준으로 적용할 수 있다</li>
 *   <li>길이를 먼저 읽으므로, 너무 큰 데이터는 객체로 되돌리기 전에 거절한다</li>
 * </ul>
 */
final class MessageCodec {

    /** 메시지 하나의 최대 크기(바이트). 검사 한도와 같다. */
    static final int MAX_FRAME_BYTES = (int) MessageFilter.MAX_BYTES;

    private MessageCodec() {
    }

    /** 메시지를 직렬화한 바이트로 바꾼다. */
    static byte[] encode(P2PMessage message) throws IOException {
        Objects.requireNonNull(message, "message");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(message);
        }
        return bytes.toByteArray();
    }

    /**
     * 받은 바이트를 메시지로 되돌린다.
     *
     * @throws InvalidMessageException 허용하지 않은 클래스이거나, 한도를 넘었거나, 값이 올바르지 않거나, 메시지가 아닌 경우
     */
    static P2PMessage decode(byte[] bytes) throws InvalidMessageException {
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            in.setObjectInputFilter(new MessageFilter());
            Object value = in.readObject();
            if (value instanceof P2PMessage message) {
                return message;
            }
            throw new InvalidMessageException("메시지가 아닙니다: " + value.getClass().getName());
        } catch (InvalidMessageException e) {
            throw e;
        } catch (InvalidClassException e) {
            throw new InvalidMessageException("허용하지 않은 데이터입니다", e);
        } catch (ObjectStreamException e) {
            throw new InvalidMessageException("올바르지 않은 값입니다", e);
        } catch (IOException | ClassNotFoundException | RuntimeException e) {
            throw new InvalidMessageException("메시지를 읽을 수 없습니다", e);
        }
    }

    /** 메시지 하나를 [길이 + 내용]으로 쓴다. */
    static void write(DataOutput out, P2PMessage message) throws IOException {
        byte[] bytes = encode(message);
        if (bytes.length > MAX_FRAME_BYTES) {
            throw new InvalidMessageException("메시지가 너무 큽니다: " + bytes.length + "바이트");
        }
        out.writeInt(bytes.length);
        out.write(bytes);
    }

    /**
     * 메시지 하나를 읽는다. 다음 메시지가 올 때까지 기다린다.
     *
     * @throws java.io.EOFException 상대가 연결을 닫아 더 읽을 것이 없는 경우
     * @throws InvalidMessageException 길이가 올바르지 않거나 내용을 메시지로 되돌릴 수 없는 경우
     */
    static P2PMessage read(DataInput in) throws IOException {
        int length = in.readInt();
        if (length <= 0 || length > MAX_FRAME_BYTES) {
            throw new InvalidMessageException("메시지 길이가 올바르지 않습니다: " + length);
        }
        byte[] bytes = new byte[length];
        in.readFully(bytes);
        return decode(bytes);
    }
}
