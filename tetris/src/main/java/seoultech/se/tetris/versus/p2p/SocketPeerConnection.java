package seoultech.se.tetris.versus.p2p;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

/**
 * TCP 소켓으로 구현한 상대 PC와의 통로.
 *
 * <p>스레드 세 개를 쓴다.
 * <ul>
 *   <li>받는 스레드: 메시지를 하나씩 읽어 확인 신호는 직접 처리하고, 나머지는 리스너에 전달한다</li>
 *   <li>보내는 스레드: {@link OutboundQueue}에서 꺼내 순서대로 보낸다</li>
 *   <li>확인 신호 스레드: 0.5초마다 확인 신호를 보내고 연결 상태를 다시 계산한다</li>
 * </ul>
 *
 * <p>리스너 호출은 모두 만들 때 받은 실행기에서 한다. 받기는 연결 완료 알림을 전달한 뒤에 시작하므로,
 * 그 알림 안에서 리스너를 등록하면 연결 직후에 도착한 메시지나 끊김도 놓치지 않는다.
 */
final class SocketPeerConnection implements PeerConnection {

    /**
     * 직접 끊을 때, 보내기를 닫은 뒤 소켓 전체를 닫기까지 기다리는 시간.
     *  보내기가 막혀 있어도 이 시간 뒤에는 닫힌다
     */
    static final long CLOSE_GRACE_MILLIS = 1_000;

    private final Socket socket;
    private final Role role;
    private final Executor executor;
    private final LongSupplier clock;
    private final DataInputStream in;
    private final DataOutputStream out;

    private final OutboundQueue outbound = new OutboundQueue();
    private final ConnectionMonitor monitor;
    private final List<PeerListener> listeners = new CopyOnWriteArrayList<>();
    private final AtomicBoolean closed = new AtomicBoolean();

    private volatile ConnectionState state = ConnectionState.NORMAL;
    /** 닫힘을 리스너에 알렸는지. 실행기 안에서 읽고 쓴다. */
    private volatile boolean closeNotified;
    /** 확인 신호 스레드. 다른 스레드에서 닫을 수 있으므로 {@code volatile}이다. */
    private volatile ScheduledExecutorService heartbeat;

    /**
     * @param socket   연결과 버전 확인을 마친 소켓
     * @param clock    되돌아가지 않는 시계(ms)
     */
    SocketPeerConnection(Socket socket, Role role, Executor executor, LongSupplier clock) throws IOException {
        this.socket = Objects.requireNonNull(socket, "socket");
        this.role = Objects.requireNonNull(role, "role");
        this.executor = Objects.requireNonNull(executor, "executor");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        this.out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        this.monitor = new ConnectionMonitor(clock.getAsLong());
    }

    /**
     * 받기와 보내기를 시작한다.
     *
     * @param autoHeartbeat 확인 신호를 0.5초마다 자동으로 보낼지. 테스트에서는 끄고 {@link #tick()}을 직접 부른다
     */
    void start(boolean autoHeartbeat) {
        // 받는 스레드가 곧바로 연결을 닫을 수도 있으므로, 확인 신호는 받는 스레드를 시작하기 전에 일정까지 등록한다.
        // 닫힌 뒤에 등록하려 하면 이미 멈춘 실행기라 예외가 난다
        ScheduledExecutorService scheduler = null;
        if (autoHeartbeat) {
            scheduler = Executors.newSingleThreadScheduledExecutor(r -> thread(r, "heartbeat"));
            scheduler.scheduleAtFixedRate(this::tick, ConnectionMonitor.PING_INTERVAL_MILLIS,
                    ConnectionMonitor.PING_INTERVAL_MILLIS, TimeUnit.MILLISECONDS);
            heartbeat = scheduler;
        }
        thread(this::readLoop, "reader").start();
        thread(this::writeLoop, "writer").start();
        if (scheduler != null && closed.get()) {
            scheduler.shutdownNow();   // 시작 전에 이미 닫혔으면(연결 알림 안에서 바로 닫은 경우 등) 남지 않게 한다
        }
    }

    private Thread thread(Runnable task, String name) {
        Thread thread = new Thread(task, "p2p-" + role.name().toLowerCase() + "-" + name);
        thread.setDaemon(true);
        return thread;
    }

    // ------------------------------------------------------------------
    // PeerConnection
    // ------------------------------------------------------------------

    @Override
    public Role role() {
        return role;
    }

    @Override
    public void send(P2PMessage message) {
        Objects.requireNonNull(message, "message");
        if (!closed.get()) {
            outbound.offer(message);
        }
    }

    @Override
    public void addListener(PeerListener listener) {
        listeners.add(Objects.requireNonNull(listener, "listener"));
    }

    @Override
    public void removeListener(PeerListener listener) {
        listeners.remove(listener);
    }

    @Override
    public ConnectionState state() {
        return closed.get() ? ConnectionState.DISCONNECTED : state;
    }

    @Override
    public int roundTripMillis() {
        synchronized (monitor) {
            return monitor.roundTripMillis();
        }
    }

    @Override
    public void close() {
        shutdown(CloseReason.LOCAL);
    }

    // ------------------------------------------------------------------
    // 확인 신호
    // ------------------------------------------------------------------

    /** 확인 신호를 보내고 연결 상태를 다시 계산한다. 10초 동안 아무것도 받지 못했으면 닫는다. */
    void tick() {
        if (closed.get()) {
            return;
        }
        long now = clock.getAsLong();
        outbound.offer(new P2PMessage.Ping(now));
        boolean changed;
        ConnectionState current;
        int average;
        synchronized (monitor) {
            changed = monitor.refresh(now);
            current = monitor.state(now);
            average = monitor.roundTripMillis();
        }
        state = current;
        if (current == ConnectionState.DISCONNECTED) {
            shutdown(CloseReason.TIMED_OUT);
        } else if (changed) {
            deliver(listener -> listener.onStateChanged(current, average));
        }
    }

    // ------------------------------------------------------------------
    // 받기, 보내기
    // ------------------------------------------------------------------

    private void readLoop() {
        try {
            while (!closed.get()) {
                P2PMessage message = MessageCodec.read(in);
                long now = clock.getAsLong();
                if (message instanceof P2PMessage.Pong pong) {
                    synchronized (monitor) {
                        monitor.roundTrip(pong.sentAtMillis(), now);
                    }
                    continue;
                }
                synchronized (monitor) {
                    monitor.received(now);
                }
                if (message instanceof P2PMessage.Ping ping) {
                    outbound.offer(new P2PMessage.Pong(ping.sentAtMillis()));
                } else if (message instanceof P2PMessage.Bye) {
                    synchronized (monitor) {
                        monitor.peerLeft();
                    }
                    shutdown(CloseReason.PEER_LEFT);
                } else {
                    deliver(listener -> listener.onMessage(message));
                }
            }
        } catch (IOException e) {
            // 상대가 알림 없이 끊었거나(EOF), 받은 데이터가 잘못됐거나, 직접 닫아서 소켓이 닫힘
            shutdown(CloseReason.FAILED);
        }
    }

    /**
     * 대기열의 메시지를 순서대로 보낸다. 직접 끊을 때는 끊는다는 신호까지 다 보낸 뒤 보내기만 닫는다.
     *
     * <p>소켓 전체를 바로 닫으면, 이쪽에 아직 읽지 않은 데이터가 남아 있을 때 정상 종료(FIN) 대신
     * 강제 종료(RST)가 나간다. 강제 종료를 받은 쪽은 아직 읽지 않은 데이터를 버릴 수 있어서,
     * 상대가 마지막 메시지(기권, 끊는다는 신호)를 받지 못하고 통신 오류로 처리할 수 있다.
     * 소켓 전체는 {@link #shutdown}이 맡긴 {@value #CLOSE_GRACE_MILLIS}ms 뒤 닫기가 닫는다.
     */
    private void writeLoop() {
        try {
            P2PMessage message;
            while ((message = outbound.take()) != null) {
                MessageCodec.write(out, message);
                out.flush();
            }
            if (!socket.isClosed()) {
                socket.shutdownOutput();
            }
        } catch (IOException e) {
            shutdown(CloseReason.FAILED);
            closeSocket();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            closeSocket();
        }
    }

    // ------------------------------------------------------------------
    // 닫기, 알림
    // ------------------------------------------------------------------

    /**
     * 연결을 닫는다. 처음 한 번만 동작한다.
     *
     * <p>직접 끊을 때는 보내는 스레드가 이미 넣은 메시지와 끊는다는 신호({@link P2PMessage.Bye})를 마저 보낸 뒤
     * 보내기만 닫고, 소켓 전체는 {@value #CLOSE_GRACE_MILLIS}ms 뒤에 닫는다. 그래야 상대가 마지막 메시지를 받는다
     * ({@link #writeLoop} 참고). 그 밖의 이유로 닫을 때는 남은 메시지를 버리고 바로 소켓을 닫는다.
     */
    private void shutdown(CloseReason reason) {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        state = ConnectionState.DISCONNECTED;
        ScheduledExecutorService scheduler = heartbeat;
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
        if (reason == CloseReason.LOCAL) {
            outbound.offer(new P2PMessage.Bye());
            outbound.close();   // 보내는 스레드가 마저 보낸 뒤 보내기만 닫는다
            CompletableFuture.delayedExecutor(CLOSE_GRACE_MILLIS, TimeUnit.MILLISECONDS).execute(this::closeSocket);
        } else {
            outbound.discardAndClose();
            closeSocket();
        }
        executor.execute(() -> {
            closeNotified = true;
            for (PeerListener listener : listeners) {
                listener.onClosed(reason);
            }
        });
    }

    private void closeSocket() {
        try {
            socket.close();
        } catch (IOException ignored) {
            // 닫는 중의 오류는 할 수 있는 일이 없다
        }
    }

    /** 리스너에 알린다. 닫힘을 알린 뒤에는 더 알리지 않는다. */
    private void deliver(Consumer<PeerListener> event) {
        executor.execute(() -> {
            if (closeNotified) {
                return;
            }
            for (PeerListener listener : listeners) {
                event.accept(listener);
            }
        });
    }
}
