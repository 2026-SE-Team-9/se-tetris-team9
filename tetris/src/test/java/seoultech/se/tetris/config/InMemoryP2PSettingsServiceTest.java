package seoultech.se.tetris.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import seoultech.se.tetris.config.P2PSettingsService.RecentPeer;

class InMemoryP2PSettingsServiceTest {

    @Test
    void 처음에는_기본_포트와_빈_접속_기록이다() {
        InMemoryP2PSettingsService service = new InMemoryP2PSettingsService();

        assertEquals(24700, service.port());
        assertEquals(P2PSettingsService.DEFAULT_PORT, service.port());
        assertTrue(service.recentPeers().isEmpty());
    }

    @Test
    void 포트를_바꿀_수_있고_범위를_벗어나면_거부한다() {
        InMemoryP2PSettingsService service = new InMemoryP2PSettingsService();

        service.setPort(30000);
        assertEquals(30000, service.port());

        assertThrows(IllegalArgumentException.class, () -> service.setPort(0));
        assertThrows(IllegalArgumentException.class, () -> service.setPort(65536));
        assertEquals(30000, service.port());
    }

    @Test
    void 미리_채운_접속_기록을_순서대로_돌려주고_지울_수_있다() {
        List<RecentPeer> peers = new ArrayList<>(List.of(
                new RecentPeer("192.168.0.2", 24700), new RecentPeer("10.0.0.5", 30000)));
        InMemoryP2PSettingsService service = new InMemoryP2PSettingsService(peers);
        peers.clear();

        assertEquals(List.of(new RecentPeer("192.168.0.2", 24700), new RecentPeer("10.0.0.5", 30000)),
                service.recentPeers());

        service.clearRecentPeers();
        assertTrue(service.recentPeers().isEmpty());
    }

    @Test
    void 돌려준_접속_기록은_바꿀_수_없다() {
        InMemoryP2PSettingsService service = new InMemoryP2PSettingsService(
                List.of(new RecentPeer("192.168.0.2", 24700)));

        assertThrows(UnsupportedOperationException.class, () -> service.recentPeers().clear());
        assertThrows(NullPointerException.class, () -> new InMemoryP2PSettingsService(null));
    }

    @Test
    void 접속_기록은_IP와_포트로_표시한다() {
        assertEquals("192.168.0.12:24700", new RecentPeer("192.168.0.12", 24700).text());
        assertThrows(NullPointerException.class, () -> new RecentPeer(null, 24700));
        assertThrows(IllegalArgumentException.class, () -> new RecentPeer("1.2.3.4", 0));
    }

    @Test
    void 포트_범위는_1부터_65535까지다() {
        assertTrue(P2PSettingsService.isValidPort(1));
        assertTrue(P2PSettingsService.isValidPort(65535));
        assertFalse(P2PSettingsService.isValidPort(0));
        assertFalse(P2PSettingsService.isValidPort(65536));
    }
}
