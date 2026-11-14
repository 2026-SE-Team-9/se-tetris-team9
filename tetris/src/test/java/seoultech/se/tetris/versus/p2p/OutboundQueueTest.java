package seoultech.se.tetris.versus.p2p;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import seoultech.se.tetris.engine.Difficulty;
import seoultech.se.tetris.engine.GameMode;
import seoultech.se.tetris.engine.TetrisEngine;
import seoultech.se.tetris.versus.BoardSnapshot;

@DisplayName("OutboundQueue")
class OutboundQueueTest {

    private final OutboundQueue queue = new OutboundQueue();

    private static P2PMessage.Snapshot snapshot(long seed) {
        return new P2PMessage.Snapshot(BoardSnapshot.of(TetrisEngine.seeded(seed, Difficulty.NORMAL, GameMode.NORMAL)));
    }

    @Test
    @DisplayName("넣은 순서대로 꺼낸다")
    void firstInFirstOut() throws InterruptedException {
        queue.offer(new P2PMessage.Ready(true));
        queue.offer(new P2PMessage.FinalScore(1));

        assertEquals(new P2PMessage.Ready(true), queue.take());
        assertEquals(new P2PMessage.FinalScore(1), queue.take());
    }

    @Test
    @DisplayName("보내지 못한 화면 정보가 있으면 그것을 버리고 새 화면 정보를 맨 뒤에 넣는다")
    void keepsOnlyLatestSnapshot() throws InterruptedException {
        P2PMessage.Snapshot older = snapshot(1);
        P2PMessage.Snapshot newer = snapshot(2);
        queue.offer(older);
        queue.offer(new P2PMessage.Ready(true));
        queue.offer(newer);

        assertEquals(2, queue.size());
        assertEquals(new P2PMessage.Ready(true), queue.take());
        assertEquals(newer, queue.take());
    }

    @Test
    @DisplayName("화면 정보가 아닌 메시지는 같은 종류라도 버리지 않는다")
    void otherMessagesAreKept() {
        queue.offer(new P2PMessage.Ping(1));
        queue.offer(new P2PMessage.Ping(2));

        assertEquals(2, queue.size());
    }

    @Test
    @DisplayName("비어 있으면 메시지가 들어올 때까지 기다린다")
    void takeWaits() throws InterruptedException {
        AtomicReference<P2PMessage> taken = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);
        Thread taker = new Thread(() -> {
            try {
                taken.set(queue.take());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            done.countDown();
        });
        taker.start();

        queue.offer(new P2PMessage.Bye());

        assertTrue(done.await(5, TimeUnit.SECONDS));
        assertEquals(new P2PMessage.Bye(), taken.get());
    }

    @Test
    @DisplayName("닫아도 이미 넣은 메시지는 꺼낼 수 있고, 다 꺼내면 null을 돌려준다")
    void closeDrainsRemaining() throws InterruptedException {
        queue.offer(new P2PMessage.Forfeit());
        queue.offer(new P2PMessage.Bye());
        queue.close();

        assertEquals(new P2PMessage.Forfeit(), queue.take());
        assertEquals(new P2PMessage.Bye(), queue.take());
        assertNull(queue.take());
    }

    @Test
    @DisplayName("닫힌 뒤에 넣은 메시지는 무시한다")
    void ignoresAfterClose() throws InterruptedException {
        queue.close();
        queue.offer(new P2PMessage.Ready(true));

        assertNull(queue.take());
    }

    @Test
    @DisplayName("버리고 닫으면 남은 메시지가 사라진다")
    void discardAndClose() throws InterruptedException {
        queue.offer(new P2PMessage.Ready(true));
        queue.discardAndClose();

        assertNull(queue.take());
    }

    @Test
    @DisplayName("기다리는 중에 닫히면 null을 돌려준다")
    void closeWakesWaiter() throws InterruptedException {
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<P2PMessage> taken = new AtomicReference<>(new P2PMessage.Bye());
        Thread taker = new Thread(() -> {
            try {
                taken.set(queue.take());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            done.countDown();
        });
        taker.start();

        queue.close();

        assertTrue(done.await(5, TimeUnit.SECONDS));
        assertNull(taken.get());
    }

    @Test
    @DisplayName("null은 넣을 수 없다")
    void rejectsNull() {
        assertThrows(NullPointerException.class, () -> queue.offer(null));
    }
}
