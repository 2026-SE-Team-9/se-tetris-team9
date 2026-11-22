package seoultech.se.tetris.ui.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.KeyEvent;
import java.nio.file.Path;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seoultech.se.tetris.config.InMemoryP2PSettingsService;
import seoultech.se.tetris.config.P2PSettingsService.RecentPeer;
import seoultech.se.tetris.config.PropertiesAppSettingsService;
import seoultech.se.tetris.config.PropertiesBattleKeyConfigService;
import seoultech.se.tetris.config.PropertiesKeyConfigService;
import seoultech.se.tetris.score.CsvScoreRepository;
import seoultech.se.tetris.ui.settings.SettingsPanel.Tab;

/** 설정 화면의 일반 모드 / 로컬 대전 / P2P 대전 탭 구성 */
class SettingsPanelTabsTest {

    @TempDir
    Path tempDir;

    private final InMemoryP2PSettingsService p2p = new InMemoryP2PSettingsService(
            List.of(new RecentPeer("192.168.0.12", 24700)));

    private SettingsPanel panel(SettingsPanel.Context context, Runnable onClose) {
        return new SettingsPanel(new PropertiesAppSettingsService(tempDir.resolve("settings.properties")),
                new PropertiesKeyConfigService(tempDir.resolve("keys.properties")),
                new PropertiesBattleKeyConfigService(tempDir.resolve("battle.properties")),
                p2p, new CsvScoreRepository(tempDir.resolve("scores.csv")), context,
                () -> { }, () -> { }, onClose);
    }

    private static JButton findButton(Container root, String text) {
        for (Component component : root.getComponents()) {
            if (component instanceof JButton button && text.equals(button.getText())) {
                return button;
            }
            if (component instanceof Container child) {
                JButton found = findButton(child, text);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    @Test
    void 시작_메뉴_설정에는_세_탭이_있다() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            SettingsPanel panel = panel(SettingsPanel.Context.START_MENU, () -> { });

            assertEquals(List.of(Tab.SINGLE, Tab.LOCAL_BATTLE, Tab.P2P), panel.availableTabs());
            for (Tab tab : Tab.values()) {
                assertTrue(findButton(panel, tab.label()) != null, tab.label());
            }
            assertEquals("일반 모드", Tab.SINGLE.label());
            assertEquals("로컬 대전", Tab.LOCAL_BATTLE.label());
            assertEquals("P2P 대전", Tab.P2P.label());
        });
    }

    @Test
    void 게임_중_설정에는_P2P_대전_탭이_없다() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            for (SettingsPanel.Context context : new SettingsPanel.Context[] {
                    SettingsPanel.Context.IN_GAME, SettingsPanel.Context.LOCAL_BATTLE_IN_GAME}) {
                SettingsPanel panel = panel(context, () -> { });

                assertEquals(List.of(Tab.SINGLE, Tab.LOCAL_BATTLE), panel.availableTabs());
                assertNull(findButton(panel, "P2P 대전"), context.name());
                panel.selectTab(Tab.P2P);
                assertSame(Tab.SINGLE, panel.currentTab());
            }
        });
    }

    @Test
    void 탭_버튼은_한_줄에서_좌우로_이동한다() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            SettingsPanel panel = panel(SettingsPanel.Context.START_MENU, () -> { });
            JButton single = findButton(panel, "일반 모드");
            JButton local = findButton(panel, "로컬 대전");
            JButton p2pTab = findButton(panel, "P2P 대전");

            assertSame(local, panel.navigationTarget(single, KeyEvent.VK_RIGHT));
            assertSame(p2pTab, panel.navigationTarget(local, KeyEvent.VK_RIGHT));
            assertSame(p2pTab, panel.navigationTarget(p2pTab, KeyEvent.VK_RIGHT));
        });
    }

    @Test
    void P2P_대전_탭을_고르면_포트와_최근_접속_항목으로_이동한다() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            SettingsPanel panel = panel(SettingsPanel.Context.START_MENU, () -> { });
            JButton p2pTab = findButton(panel, "P2P 대전");
            P2PSettingsPanel p2pPanel = panel.p2pSettingsPanel();

            p2pTab.doClick();

            assertSame(Tab.P2P, panel.currentTab());
            assertSame(p2pPanel.portField(), panel.navigationTarget(p2pTab, KeyEvent.VK_DOWN));
            assertSame(p2pPanel.resetPortButton(), panel.navigationTarget(p2pPanel.portField(), KeyEvent.VK_DOWN));
            assertSame(p2pPanel.clearRecentButton(),
                    panel.navigationTarget(p2pPanel.resetPortButton(), KeyEvent.VK_DOWN));
            // 삭제 아래는 하단 버튼 줄이다
            Component below = panel.navigationTarget(p2pPanel.clearRecentButton(), KeyEvent.VK_DOWN);
            assertEquals("모든 설정 기본값", ((AbstractButton) below).getText());
        });
    }

    @Test
    void 저장하면_입력한_포트를_반영한다() throws Exception {
        int[] closes = {0};
        SwingUtilities.invokeAndWait(() -> {
            SettingsPanel panel = panel(SettingsPanel.Context.START_MENU, () -> closes[0]++);
            panel.selectTab(Tab.P2P);
            panel.p2pSettingsPanel().portField().setText("31000");

            findButton(panel, "저장").doClick();
        });

        assertEquals(31000, p2p.port());
        assertEquals(1, closes[0]);
    }

    @Test
    void 취소하면_입력한_포트를_반영하지_않는다() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            SettingsPanel panel = panel(SettingsPanel.Context.START_MENU, () -> { });
            panel.p2pSettingsPanel().portField().setText("31000");

            findButton(panel, "취소").doClick();
        });

        assertEquals(24700, p2p.port());
    }

    @Test
    void 모든_설정_기본값은_포트만_되돌리고_최근_접속_기록은_남긴다() throws Exception {
        p2p.setPort(31000);
        SwingUtilities.invokeAndWait(() -> {
            SettingsPanel panel = panel(SettingsPanel.Context.START_MENU, () -> { });
            assertEquals("31000", panel.p2pSettingsPanel().portField().getText());

            findButton(panel, "모든 설정 기본값").doClick();
            assertEquals("24700", panel.p2pSettingsPanel().portField().getText());
            findButton(panel, "저장").doClick();
        });

        assertEquals(24700, p2p.port());
        assertEquals(1, p2p.recentPeers().size());
    }

    @Test
    void 포트_입력칸에서_좌우_키는_커서_이동에_맡긴다() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            SettingsPanel panel = panel(SettingsPanel.Context.START_MENU, () -> { });
            Component field = panel.p2pSettingsPanel().portField();
            Component reset = panel.p2pSettingsPanel().resetPortButton();

            assertTrue(SettingsPanel.leavesKeyToTextField(field, KeyEvent.VK_LEFT));
            assertTrue(SettingsPanel.leavesKeyToTextField(field, KeyEvent.VK_RIGHT));
            assertFalse(SettingsPanel.leavesKeyToTextField(field, KeyEvent.VK_DOWN));
            assertFalse(SettingsPanel.leavesKeyToTextField(reset, KeyEvent.VK_LEFT));
        });
    }

    @Test
    void 로컬_대전_탭은_기존_2인_키_설정을_보여준다() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            SettingsPanel panel = panel(SettingsPanel.Context.START_MENU, () -> { });

            panel.selectTab(true);

            assertSame(Tab.LOCAL_BATTLE, panel.currentTab());
            panel.selectTab(false);
            assertSame(Tab.SINGLE, panel.currentTab());
        });
    }
}
