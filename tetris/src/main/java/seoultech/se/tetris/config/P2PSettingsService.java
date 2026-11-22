package seoultech.se.tetris.config;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/**
 * P2P 대전 설정. 서버가 사용할 포트와 최근 접속 기록을 다룬다.
 *
 * <p>설정 화면은 이 인터페이스만 사용한다.
 *
 * <p>요구사항 (docs/requirements-3.md)
 * <ul>
 *   <li>포트 기본값은 {@value #DEFAULT_PORT}이며, 초기화하면 기본값으로 돌아간다</li>
 *   <li>최근 접속 기록은 접속에 성공한 IP와 포트만, 중복 없이 최신순으로 {@value #MAX_RECENT_PEERS}개까지 저장한다</li>
 *   <li>저장한 기록은 설정 화면의 삭제 버튼으로 삭제할 수 있다</li>
 * </ul>
 */
public interface P2PSettingsService {

    /** 포트 기본값 */
    int DEFAULT_PORT = 24700;
    /** 사용할 수 있는 가장 작은 포트 번호 */
    int MIN_PORT = 1;
    /** 사용할 수 있는 가장 큰 포트 번호 */
    int MAX_PORT = 65535;
    /** 저장하는 최근 접속 기록의 최대 개수 */
    int MAX_RECENT_PEERS = 10;

    /** 최근에 접속한 상대 하나 */
    record RecentPeer(String address, int port) {
        public RecentPeer {
            Objects.requireNonNull(address, "address");
            address = address.strip();
            if (address.isEmpty()) {
                throw new IllegalArgumentException("접속 주소가 비어 있습니다");
            }
            if (!isValidPort(port)) {
                throw new IllegalArgumentException("포트 번호가 범위를 벗어났습니다: " + port);
            }
        }

        /** 화면에 표시할 형식. 예: "192.168.0.12:24700" */
        public String text() {
            return address + ":" + port;
        }
    }

    /** 포트 번호를 사용할 수 있는 범위인지 */
    static boolean isValidPort(int port) {
        return port >= MIN_PORT && port <= MAX_PORT;
    }

    /** 최근 접속 기록을 중복 없이 최대 {@value #MAX_RECENT_PEERS}개까지 정규화한다. */
    static List<RecentPeer> normalizeRecentPeers(List<RecentPeer> peers) {
        Objects.requireNonNull(peers, "peers");
        LinkedHashSet<RecentPeer> unique = new LinkedHashSet<>();
        for (RecentPeer peer : peers) {
            unique.add(Objects.requireNonNull(peer, "peer"));
            if (unique.size() == MAX_RECENT_PEERS) {
                break;
            }
        }
        return List.copyOf(unique);
    }

    /** 최근 접속 기록 맨 앞에 새 상대를 추가하고 중복을 제거한다. */
    static List<RecentPeer> prependRecentPeer(List<RecentPeer> current, RecentPeer peer) {
        Objects.requireNonNull(current, "current");
        Objects.requireNonNull(peer, "peer");
        List<RecentPeer> updated = new ArrayList<>(Math.min(current.size() + 1, MAX_RECENT_PEERS));
        updated.add(peer);
        for (RecentPeer existing : current) {
            if (updated.size() == MAX_RECENT_PEERS) {
                break;
            }
            if (!existing.equals(peer)) {
                updated.add(existing);
            }
        }
        return List.copyOf(updated);
    }

    /** 최근 접속 기록에서 지정한 상대를 제거한다. */
    static List<RecentPeer> removePeerFromList(List<RecentPeer> current, RecentPeer peer) {
        Objects.requireNonNull(current, "current");
        Objects.requireNonNull(peer, "peer");
        return current.stream()
                .filter(existing -> !existing.equals(peer))
                .toList();
    }

    /** 서버가 사용할 포트 */
    int port();

    /**
     * 서버가 사용할 포트를 바꾼다.
     *
     * @throws IllegalArgumentException 사용할 수 없는 범위의 포트인 경우
     */
    void setPort(int port);

    /** 최근 접속 기록. 최신순이다. */
    List<RecentPeer> recentPeers();

    /** 최근 접속 기록에 상대를 추가한다. 이미 있으면 가장 앞으로 옮긴다. */
    void addRecentPeer(RecentPeer peer);

    /** 최근 접속 기록에 IP와 포트를 추가한다. */
    default void addRecentPeer(String address, int port) {
        addRecentPeer(new RecentPeer(address, port));
    }

    /** 최근 접속 기록에서 지정한 상대를 삭제한다. 삭제했으면 {@code true} */
    boolean removeRecentPeer(RecentPeer peer);

    /** 최근 접속 기록에서 지정한 IP와 포트를 삭제한다. 삭제했으면 {@code true} */
    default boolean removeRecentPeer(String address, int port) {
        return removeRecentPeer(new RecentPeer(address, port));
    }

    /** 최근 접속 기록을 모두 지운다. */
    void clearRecentPeers();
}
