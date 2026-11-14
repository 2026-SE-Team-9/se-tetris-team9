package seoultech.se.tetris.versus.p2p;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

/**
 * 보낼 메시지 대기열. 여러 스레드가 넣고, 보내는 스레드 하나가 꺼낸다.
 *
 * <p>화면 정보({@link P2PMessage.Snapshot})는 가장 최신 것만 의미가 있으므로, 아직 보내지 못한 화면 정보가 있으면
 * 그것을 버리고 새 것을 맨 뒤에 넣는다. 다른 메시지는 넣은 순서대로 꺼낸다.
 */
final class OutboundQueue {

    private final Deque<P2PMessage> messages = new ArrayDeque<>();
    private boolean closed;

    /** 메시지를 넣는다. 닫힌 뒤에는 무시한다. */
    synchronized void offer(P2PMessage message) {
        Objects.requireNonNull(message, "message");
        if (closed) {
            return;
        }
        if (message instanceof P2PMessage.Snapshot) {
            messages.removeIf(P2PMessage.Snapshot.class::isInstance);
        }
        messages.addLast(message);
        notifyAll();
    }

    /**
     * 다음 메시지를 꺼낸다. 비어 있으면 들어올 때까지 기다린다.
     *
     * @return 다음 메시지. 닫혔고 남은 메시지도 없으면 {@code null}
     */
    synchronized P2PMessage take() throws InterruptedException {
        while (messages.isEmpty() && !closed) {
            wait();
        }
        return messages.pollFirst();
    }

    /** 더 받지 않는다. 이미 넣은 메시지는 그대로 꺼낼 수 있다. */
    synchronized void close() {
        closed = true;
        notifyAll();
    }

    /** 남은 메시지를 버리고 닫는다. */
    synchronized void discardAndClose() {
        messages.clear();
        close();
    }

    synchronized int size() {
        return messages.size();
    }
}
