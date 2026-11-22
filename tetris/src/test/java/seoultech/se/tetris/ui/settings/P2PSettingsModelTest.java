package seoultech.se.tetris.ui.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.OptionalInt;

import org.junit.jupiter.api.Test;

import seoultech.se.tetris.config.InMemoryP2PSettingsService;
import seoultech.se.tetris.config.P2PSettingsService.RecentPeer;

class P2PSettingsModelTest {

    private final InMemoryP2PSettingsService service = new InMemoryP2PSettingsService(List.of(
            new RecentPeer("192.168.0.12", 24700), new RecentPeer("10.0.0.5", 30000)));
    private final P2PSettingsModel model = new P2PSettingsModel(service);

    @Test
    void 저장된_포트를_입력칸에_보여준다() {
        service.setPort(31000);

        assertEquals("31000", new P2PSettingsModel(service).portText());
        assertTrue(new P2PSettingsModel(service).isPortValid());
    }

    @Test
    void 숫자로_된_범위_안의_포트만_올바르다() {
        model.setPortText(" 8080 ");
        assertEquals(OptionalInt.of(8080), model.parsedPort());
        assertNull(model.portError());

        for (String wrong : new String[] {"", "abc", "0", "65536", "-1", "12a", "999999", "1.5"}) {
            model.setPortText(wrong);
            assertFalse(model.isPortValid(), wrong);
            assertEquals(P2PSettingsModel.INVALID_PORT, model.portError(), wrong);
        }
    }

    @Test
    void 입력칸이_null이면_빈_입력으로_본다() {
        model.setPortText(null);

        assertEquals("", model.portText());
        assertFalse(model.isPortValid());
    }

    @Test
    void 초기화하면_입력칸이_기본_포트가_되고_저장하기_전에는_반영하지_않는다() {
        service.setPort(31000);
        P2PSettingsModel fresh = new P2PSettingsModel(service);

        fresh.resetPort();

        assertEquals("24700", fresh.portText());
        assertEquals(31000, service.port());
    }

    @Test
    void 올바른_포트를_반영하면_설정이_바뀐다() {
        model.setPortText("40000");

        assertTrue(model.applyPort());
        assertEquals(40000, service.port());
    }

    @Test
    void 잘못된_포트는_반영하지_않는다() {
        model.setPortText("70000");

        assertFalse(model.applyPort());
        assertEquals(24700, service.port());
    }

    @Test
    void 최근_접속_기록을_IP와_포트로_최신순_표시한다() {
        assertTrue(model.hasRecentPeers());
        assertEquals(List.of("192.168.0.12:24700", "10.0.0.5:30000"), model.recentPeerTexts());
    }

    @Test
    void 최근_접속_기록을_지우면_바로_반영된다() {
        model.clearRecentPeers();

        assertFalse(model.hasRecentPeers());
        assertTrue(service.recentPeers().isEmpty());
        assertEquals(List.of(), model.recentPeerTexts());
    }

    @Test
    void 기본_포트_안내에_기본값을_보여준다() {
        assertTrue(model.defaultPortHint().contains("24700"));
        assertThrows(NullPointerException.class, () -> new P2PSettingsModel(null));
    }
}
