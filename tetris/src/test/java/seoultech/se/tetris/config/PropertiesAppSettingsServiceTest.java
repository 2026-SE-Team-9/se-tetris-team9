package seoultech.se.tetris.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import seoultech.se.tetris.config.AppSettings.ScreenSize;
import seoultech.se.tetris.config.P2PSettingsService.RecentPeer;
import seoultech.se.tetris.engine.Difficulty;

class PropertiesAppSettingsServiceTest {
    @TempDir Path directory;

    /** 일반 모드·대전 모드·P2P 설정값을 모두 저장하고 새 서비스에서 다시 불러온다. */
    @Test
    void savesAndLoadsAllOptions() {
        Path file = directory.resolve("settings.properties");
        var settings = new PropertiesAppSettingsService(file);
        var selected = new AppSettings(ScreenSize.LARGE, true, Difficulty.HARD,
                ScreenSize.SMALL, false, Difficulty.EASY,
                30000, List.of(new RecentPeer("192.168.0.12", 24700), new RecentPeer("10.0.0.5", 30000)));
        settings.save(selected);

        var restored = new PropertiesAppSettingsService(file);
        restored.load();
        assertEquals(selected, restored.current());
        assertEquals(30000, restored.port());
        assertEquals(List.of(new RecentPeer("192.168.0.12", 24700), new RecentPeer("10.0.0.5", 30000)),
                restored.recentPeers());
    }

    /** 대전 모드 및 P2P 설정이 누락된 이전 형식의 파일은 해당 설정을 기본값으로 불러온다. */
    @Test
    void legacyFormatFillsBattleSettingsWithDefaults() throws Exception {
        Path file = directory.resolve("settings.properties");
        Files.writeString(file, "screenSize=LARGE\ncolorBlind=true\ndifficulty=HARD\n");
        var settings = new PropertiesAppSettingsService(file);
        settings.load();

        var expected = new AppSettings(ScreenSize.LARGE, true, Difficulty.HARD,
                AppSettings.DEFAULT.battleScreenSize(),
                AppSettings.DEFAULT.battleColorBlind(),
                AppSettings.DEFAULT.battleDifficulty(),
                AppSettings.DEFAULT.p2pPort(),
                AppSettings.DEFAULT.recentPeers());
        assertEquals(expected, settings.current());
        assertEquals(P2PSettingsService.DEFAULT_PORT, settings.port());
        assertTrue(settings.recentPeers().isEmpty());
    }

    /** P2P 포트를 변경하면 파일에 저장되어 재실행 후에도 유지되며 잘못된 포트는 거부한다. */
    @Test
    void setPortPersistsAndRejectsInvalidPort() {
        Path file = directory.resolve("settings.properties");
        var settings = new PropertiesAppSettingsService(file);
        assertEquals(P2PSettingsService.DEFAULT_PORT, settings.port());

        settings.setPort(31000);
        assertEquals(31000, settings.port());

        assertThrows(IllegalArgumentException.class, () -> settings.setPort(0));
        assertThrows(IllegalArgumentException.class, () -> settings.setPort(65536));
        assertEquals(31000, settings.port());

        var reloaded = new PropertiesAppSettingsService(file);
        reloaded.load();
        assertEquals(31000, reloaded.port());
    }

    /** 최근 접속 IP는 중복 없이 최신순으로 최대 10개까지 properties 파일에 저장된다. */
    @Test
    void addRecentPeerDeduplicatesAndLimitsToMax() {
        Path file = directory.resolve("settings.properties");
        var settings = new PropertiesAppSettingsService(file);

        for (int i = 1; i <= 12; i++) {
            settings.addRecentPeer("192.168.0." + i, 24700);
        }

        List<RecentPeer> peers = settings.recentPeers();
        assertEquals(P2PSettingsService.MAX_RECENT_PEERS, peers.size());
        assertEquals(new RecentPeer("192.168.0.12", 24700), peers.get(0));
        assertEquals(new RecentPeer("192.168.0.3", 24700), peers.get(9));

        settings.addRecentPeer("192.168.0.5", 24700);
        assertEquals(P2PSettingsService.MAX_RECENT_PEERS, settings.recentPeers().size());
        assertEquals(new RecentPeer("192.168.0.5", 24700), settings.recentPeers().get(0));

        var reloaded = new PropertiesAppSettingsService(file);
        reloaded.load();
        assertEquals(settings.recentPeers(), reloaded.recentPeers());
    }

    /** 최근 접속 IP를 개별 삭제하거나 전체 삭제하면 properties 파일에 반영된다. */
    @Test
    void removeAndClearRecentPeersPersistToFile() {
        Path file = directory.resolve("settings.properties");
        var settings = new PropertiesAppSettingsService(file);
        settings.addRecentPeer("192.168.0.1", 24700);
        settings.addRecentPeer("192.168.0.2", 24701);
        settings.addRecentPeer("192.168.0.3", 24702);

        assertTrue(settings.removeRecentPeer("192.168.0.2", 24701));
        assertFalse(settings.removeRecentPeer("192.168.0.99", 24700));
        assertEquals(
                List.of(new RecentPeer("192.168.0.3", 24702), new RecentPeer("192.168.0.1", 24700)),
                settings.recentPeers());

        var afterRemove = new PropertiesAppSettingsService(file);
        afterRemove.load();
        assertEquals(settings.recentPeers(), afterRemove.recentPeers());

        afterRemove.clearRecentPeers();
        assertTrue(afterRemove.recentPeers().isEmpty());

        var afterClear = new PropertiesAppSettingsService(file);
        afterClear.load();
        assertTrue(afterClear.recentPeers().isEmpty());
    }

    /** 설정 파일이 없거나 값이 손상되면 일괄적으로 기본값을 사용한다. */
    @Test
    void missingOrMalformedFileUsesDefaults() throws Exception {
        Path file = directory.resolve("settings.properties");
        var settings = new PropertiesAppSettingsService(file);
        settings.load();
        assertEquals(AppSettings.DEFAULT, settings.current());

        Files.writeString(file, "screenSize=LARGE\ncolorBlind=maybe\ndifficulty=HARD\n");
        settings.load();
        assertEquals(AppSettings.DEFAULT, settings.current());

        Files.writeString(file, "screenSize=SMALL\ncolorBlind=true\n");
        settings.load();
        assertEquals(AppSettings.DEFAULT, settings.current());

        Files.writeString(file, "screenSize=LARGE\ncolorBlind=true\ndifficulty=HARD\np2pPort=70000\n");
        settings.load();
        assertEquals(AppSettings.DEFAULT, settings.current());

        Files.writeString(file, "screenSize=LARGE\ncolorBlind=true\ndifficulty=HARD\nrecentPeers=invalid-entry\n");
        settings.load();
        assertEquals(AppSettings.DEFAULT, settings.current());
    }

    /** 파일 저장에 실패해도 메모리의 적용된 값은 바꾸지 않는다. */
    @Test
    void failedSaveKeepsCurrentSettings() throws Exception {
        Path file = directory.resolve("settings.properties");
        Files.createDirectory(file);
        var settings = new PropertiesAppSettingsService(file);
        assertThrows(UncheckedIOException.class,
                () -> settings.save(new AppSettings(ScreenSize.SMALL, true, Difficulty.EASY)));
        assertEquals(AppSettings.DEFAULT, settings.current());
    }

    /** AppSettings 생성 시 잘못된 P2P 포트를 거부한다. */
    @Test
    void appSettingsRejectsInvalidP2pPort() {
        assertThrows(IllegalArgumentException.class,
                () -> new AppSettings(ScreenSize.MEDIUM, false, Difficulty.DEFAULT,
                        ScreenSize.MEDIUM, false, Difficulty.DEFAULT, 0, List.of()));
    }
}
