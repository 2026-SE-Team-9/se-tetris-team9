package seoultech.se.tetris.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 값을 메모리에만 두는 P2P 설정. 파일에 저장하는 구현이 들어오기 전까지 설정 화면을 연결하기 위해 쓴다.
 *
 * <p>프로그램을 다시 실행하면 포트는 기본값, 최근 접속 기록은 빈 상태로 돌아간다.
 */
public final class InMemoryP2PSettingsService implements P2PSettingsService {

    private int port = DEFAULT_PORT;
    private final List<RecentPeer> recentPeers = new ArrayList<>();

    public InMemoryP2PSettingsService() {
    }

    /** 최근 접속 기록을 미리 채운다. 최신순으로 넘긴다. 화면 확인과 테스트에 쓴다. */
    public InMemoryP2PSettingsService(List<RecentPeer> recentPeers) {
        this.recentPeers.addAll(Objects.requireNonNull(recentPeers, "recentPeers"));
    }

    @Override
    public int port() {
        return port;
    }

    @Override
    public void setPort(int port) {
        if (!P2PSettingsService.isValidPort(port)) {
            throw new IllegalArgumentException("포트 번호가 범위를 벗어났습니다: " + port);
        }
        this.port = port;
    }

    @Override
    public List<RecentPeer> recentPeers() {
        return List.copyOf(recentPeers);
    }

    @Override
    public void clearRecentPeers() {
        recentPeers.clear();
    }
}
