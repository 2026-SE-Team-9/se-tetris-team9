package seoultech.se.tetris.versus.p2p;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.LongSupplier;
import java.util.regex.Pattern;
import seoultech.se.tetris.versus.p2p.ConnectListener.FailReason;
import seoultech.se.tetris.versus.p2p.ConnectListener.Step;

/**
 * TCP 소켓으로 상대 PC와 연결을 만든다.
 *
 * <p>접속을 기다리거나 접속하는 일은 별도 스레드에서 하고, 진행 상황은 만들 때 받은 실행기로 알린다.
 * 화면에서는 {@code SwingUtilities::invokeLater}를 넘긴다.
 */
public final class SocketPeerConnector implements PeerConnector {

    /** 서버에 접속할 때 기다리는 최대 시간 */
    static final int CONNECT_TIMEOUT_MILLIS = 5_000;

    /** 연결 직후 버전 확인 메시지를 기다리는 최대 시간 */
    static final int HANDSHAKE_TIMEOUT_MILLIS = 5_000;

    /** 점 네 개로 구분한 0~255 숫자 */
    private static final Pattern IPV4 = Pattern.compile(
            "((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)\\.){3}(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)");

    private final Executor executor;
    private final LongSupplier clock;
    private final boolean autoHeartbeat;

    /** @param callbackExecutor 리스너를 호출할 실행기 */
    public SocketPeerConnector(Executor callbackExecutor) {
        this(callbackExecutor, () -> System.nanoTime() / 1_000_000, true);
    }

    /** 테스트용. 시계와 확인 신호 자동 전송 여부를 정한다. */
    SocketPeerConnector(Executor callbackExecutor, LongSupplier clock, boolean autoHeartbeat) {
        this.executor = Objects.requireNonNull(callbackExecutor, "callbackExecutor");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.autoHeartbeat = autoHeartbeat;
    }

    // ------------------------------------------------------------------
    // 서버
    // ------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * @param port 0~65535. 0이면 비어 있는 포트를 고른다 (테스트용)
     * @throws IllegalArgumentException 포트가 범위를 벗어난 경우
     */
    @Override
    public Hosting host(int port, ConnectListener listener) {
        checkPort(port);
        Attempt attempt = new Attempt(listener);
        attempt.step(Step.OPENING);
        ServerSocket server;
        try {
            server = new ServerSocket();
            attempt.hold(server);
            server.bind(new InetSocketAddress(port));
        } catch (IOException e) {
            attempt.fail(FailReason.PORT_UNAVAILABLE);
            return new HostingHandle(attempt, port);
        }
        attempt.step(Step.WAITING);
        startThread("p2p-host", () -> {
            try (server) {
                Socket socket = server.accept();
                attempt.hold(socket);
                server.close();   // 클라이언트 하나만 받는다
                attempt.step(Step.CHECKING_VERSION);
                finish(attempt, socket, Role.SERVER);
            } catch (IOException e) {
                attempt.fail(FailReason.ERROR);   // 취소로 닫힌 경우에는 알리지 않는다
            }
        });
        return new HostingHandle(attempt, server.getLocalPort());
    }

    // ------------------------------------------------------------------
    // 클라이언트
    // ------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException 포트가 1~65535 범위를 벗어난 경우
     */
    @Override
    public Pending connect(String address, int port, ConnectListener listener) {
        checkPort(port);
        if (port == 0) {
            throw new IllegalArgumentException("접속할 포트는 1~65535여야 합니다: " + port);
        }
        Attempt attempt = new Attempt(listener);
        if (address == null || !IPV4.matcher(address.strip()).matches()) {
            attempt.fail(FailReason.INVALID_ADDRESS);
            return attempt::cancel;
        }
        String host = address.strip();
        attempt.step(Step.CONNECTING);
        startThread("p2p-connect", () -> {
            Socket socket = new Socket();
            attempt.hold(socket);
            try {
                socket.connect(new InetSocketAddress(InetAddress.getByName(host), port), CONNECT_TIMEOUT_MILLIS);
            } catch (IOException e) {
                closeQuietly(socket);
                attempt.fail(FailReason.UNREACHABLE);
                return;
            }
            attempt.step(Step.CHECKING_VERSION);
            finish(attempt, socket, Role.CLIENT);
        });
        return attempt::cancel;
    }

    // ------------------------------------------------------------------
    // 공통
    // ------------------------------------------------------------------

    /** 버전을 확인하고 통로를 만들어 알린다. */
    private void finish(Attempt attempt, Socket socket, Role role) {
        try {
            socket.setTcpNoDelay(true);
            if (!handshake(socket)) {
                closeQuietly(socket);
                attempt.fail(FailReason.VERSION_MISMATCH);
                return;
            }
            attempt.connected(new SocketPeerConnection(socket, role, executor, clock));
        } catch (InvalidMessageException e) {
            closeQuietly(socket);
            attempt.fail(FailReason.VERSION_MISMATCH);   // 다른 판의 프로그램은 메시지 형식부터 다를 수 있다
        } catch (IOException e) {
            closeQuietly(socket);
            attempt.fail(FailReason.ERROR);
        }
    }

    /** 서로 버전 확인 메시지를 보내고 받는다. 같은 판이면 {@code true} */
    private static boolean handshake(Socket socket) throws IOException {
        socket.setSoTimeout(HANDSHAKE_TIMEOUT_MILLIS);
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());
        MessageCodec.write(out, new P2PMessage.Hello(P2PMessage.PROTOCOL_VERSION));
        out.flush();
        // 버퍼 없이 읽어서, 상대가 이어 보낸 메시지를 여기서 먼저 가져가지 않게 한다
        P2PMessage reply = MessageCodec.read(new DataInputStream(socket.getInputStream()));
        socket.setSoTimeout(0);
        return reply instanceof P2PMessage.Hello hello && hello.version() == P2PMessage.PROTOCOL_VERSION;
    }

    private static void checkPort(int port) {
        if (port < 0 || port > 65_535) {
            throw new IllegalArgumentException("포트는 0~65535여야 합니다: " + port);
        }
    }

    private static void startThread(String name, Runnable task) {
        Thread thread = new Thread(task, name);
        thread.setDaemon(true);
        thread.start();
    }

    private static void closeQuietly(Closeable resource) {
        try {
            resource.close();
        } catch (IOException ignored) {
            // 닫는 중의 오류는 할 수 있는 일이 없다
        }
    }

    /** 이 PC의 IPv4 주소들. 켜져 있는 네트워크 장치만 보며, 하나도 없으면 자기 자신(127.0.0.1)이다. */
    static List<String> localAddresses() {
        List<String> addresses = new ArrayList<>();
        try {
            for (NetworkInterface device : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!device.isUp() || device.isLoopback()) {
                    continue;
                }
                for (InetAddress address : Collections.list(device.getInetAddresses())) {
                    if (address instanceof Inet4Address) {
                        addresses.add(address.getHostAddress());
                    }
                }
            }
        } catch (SocketException e) {
            // 장치 목록을 읽지 못하면 아래의 기본값을 쓴다
        }
        return addresses.isEmpty() ? List.of("127.0.0.1") : List.copyOf(addresses);
    }

    // ------------------------------------------------------------------
    // 접속 시도 하나
    // ------------------------------------------------------------------

    /**
     * 접속 시도 하나의 상태. 취소하면 그 뒤로는 리스너에 알리지 않는다.
     * 알림은 실행기에서 실제로 실행될 때 상태를 다시 확인하므로, 취소와 알림이 엇갈려도 취소가 이긴다.
     */
    private final class Attempt {

        private enum State { PENDING, DONE, CANCELLED }

        private final ConnectListener listener;
        private final AtomicReference<State> state = new AtomicReference<>(State.PENDING);
        private final List<Closeable> resources = new ArrayList<>();

        Attempt(ConnectListener listener) {
            this.listener = Objects.requireNonNull(listener, "listener");
        }

        /** 취소할 때 닫을 자원을 기억한다. 이미 취소됐으면 바로 닫는다. */
        void hold(Closeable resource) {
            synchronized (resources) {
                resources.add(resource);
            }
            if (state.get() == State.CANCELLED) {
                closeQuietly(resource);
            }
        }

        void step(Step step) {
            executor.execute(() -> {
                if (state.get() == State.PENDING) {
                    listener.onStep(step);
                }
            });
        }

        /**
         * 연결 완료를 알린 뒤에 받기를 시작한다. 리스너가 알림 안에서 통로에 리스너를 등록하기 전에
         * 상대의 메시지나 끊김이 먼저 처리되어 사라지는 일을 막기 위해서다. 그동안 도착한 데이터는 소켓에 남아 있다.
         */
        void connected(SocketPeerConnection connection) {
            executor.execute(() -> {
                if (state.compareAndSet(State.PENDING, State.DONE)) {
                    listener.onConnected(connection);
                    connection.start(autoHeartbeat);
                } else {
                    connection.close();
                }
            });
        }

        void fail(FailReason reason) {
            executor.execute(() -> {
                if (state.compareAndSet(State.PENDING, State.DONE)) {
                    listener.onFailed(reason);
                }
            });
        }

        void cancel() {
            if (!state.compareAndSet(State.PENDING, State.CANCELLED)) {
                return;
            }
            synchronized (resources) {
                resources.forEach(SocketPeerConnector::closeQuietly);
            }
        }
    }

    private record HostingHandle(Attempt attempt, int port) implements Hosting {

        @Override
        public List<String> addresses() {
            return localAddresses();
        }

        @Override
        public void cancel() {
            attempt.cancel();
        }
    }
}
