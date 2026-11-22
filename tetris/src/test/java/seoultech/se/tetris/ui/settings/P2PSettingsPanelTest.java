package seoultech.se.tetris.ui.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Test;

import seoultech.se.tetris.config.InMemoryP2PSettingsService;
import seoultech.se.tetris.config.P2PSettingsService.RecentPeer;

class P2PSettingsPanelTest {

    private static List<RecentPeer> peers(int count) {
        List<RecentPeer> peers = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            peers.add(new RecentPeer("192.168.0." + (i + 1), 24700 + i));
        }
        return peers;
    }

    /** 최근 접속 목록에 보이는 글자들. 빈 칸은 뺀다 */
    private static List<String> recentTexts(P2PSettingsPanel panel) {
        List<String> texts = new ArrayList<>();
        for (Component component : panel.recentList().getComponents()) {
            String text = ((JLabel) component).getText();
            if (text != null && !text.isEmpty()) {
                texts.add(text);
            }
        }
        return texts;
    }

    @Test
    void 저장된_포트와_기본값_안내를_보여준다() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            InMemoryP2PSettingsService service = new InMemoryP2PSettingsService();
            service.setPort(31000);
            P2PSettingsPanel panel = new P2PSettingsPanel(new P2PSettingsModel(service), () -> true);

            assertEquals("31000", panel.portField().getText());
            assertTrue(panel.portMessage().getText().contains("24700"));
        });
    }

    @Test
    void 잘못된_포트를_입력하면_안내를_붉게_보여준다() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            P2PSettingsModel model = new P2PSettingsModel(new InMemoryP2PSettingsService());
            P2PSettingsPanel panel = new P2PSettingsPanel(model, () -> true);

            panel.portField().setText("99999");

            assertEquals("99999", model.portText());
            assertEquals(P2PSettingsModel.INVALID_PORT, panel.portMessage().getText());
            assertEquals(P2PSettingsPanel.ERROR_COLOR, panel.portMessage().getForeground());

            panel.portField().setText("8080");
            assertFalse(panel.portMessage().getForeground().equals(P2PSettingsPanel.ERROR_COLOR));
        });
    }

    @Test
    void 초기화를_누르면_입력칸이_기본_포트가_된다() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            P2PSettingsPanel panel = new P2PSettingsPanel(
                    new P2PSettingsModel(new InMemoryP2PSettingsService()), () -> true);
            panel.portField().setText("abc");

            panel.resetPortButton().doClick();

            assertEquals("24700", panel.portField().getText());
        });
    }

    @Test
    void 최근_접속_기록을_번호와_함께_최신순으로_보여준다() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            P2PSettingsPanel panel = new P2PSettingsPanel(
                    new P2PSettingsModel(new InMemoryP2PSettingsService(peers(3))), () -> true);

            assertEquals(List.of("1.  192.168.0.1:24700", "2.  192.168.0.2:24701", "3.  192.168.0.3:24702"),
                    recentTexts(panel));
            assertTrue(panel.clearRecentButton().isEnabled());
        });
    }

    @Test
    void 기록이_10개면_2열로_위에서_아래로_이어진다() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            P2PSettingsPanel panel = new P2PSettingsPanel(
                    new P2PSettingsModel(new InMemoryP2PSettingsService(peers(10))), () -> true);

            Component[] cells = panel.recentList().getComponents();
            assertEquals(10, cells.length);
            // 첫 줄은 1번과 6번이다
            assertTrue(((JLabel) cells[0]).getText().startsWith("1."));
            assertTrue(((JLabel) cells[1]).getText().startsWith("6."));
        });
    }

    @Test
    void 기록이_없으면_없다는_문구를_보여주고_삭제_버튼을_끈다() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            P2PSettingsPanel panel = new P2PSettingsPanel(
                    new P2PSettingsModel(new InMemoryP2PSettingsService()), () -> true);

            assertEquals(List.of(P2PSettingsModel.EMPTY_RECENT), recentTexts(panel));
            assertFalse(panel.clearRecentButton().isEnabled());
        });
    }

    @Test
    void 삭제는_확인을_받은_뒤에만_지운다() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            InMemoryP2PSettingsService service = new InMemoryP2PSettingsService(peers(2));
            AtomicInteger asked = new AtomicInteger();
            boolean[] answer = {false};
            P2PSettingsPanel panel = new P2PSettingsPanel(new P2PSettingsModel(service), () -> {
                asked.incrementAndGet();
                return answer[0];
            });

            panel.clearRecentButton().doClick();
            assertEquals(2, service.recentPeers().size());

            answer[0] = true;
            panel.clearRecentButton().doClick();
            assertTrue(service.recentPeers().isEmpty());
            assertEquals(List.of(P2PSettingsModel.EMPTY_RECENT), recentTexts(panel));
            assertEquals(2, asked.get());

            // 기록이 없으면 다시 묻지 않는다
            panel.clearRecentPeers();
            assertEquals(2, asked.get());
        });
    }

    @Test
    void 키보드_이동은_포트_입력칸_초기화_삭제_순서다() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            P2PSettingsPanel panel = new P2PSettingsPanel(
                    new P2PSettingsModel(new InMemoryP2PSettingsService()), () -> true);

            List<List<JComponent>> rows = panel.navigationRows();
            assertEquals(List.of(List.of(panel.portField()), List.of(panel.resetPortButton()),
                    List.of(panel.clearRecentButton())), rows);
        });
    }

    @Test
    void 그릴_수_있고_필요한_값이_없으면_만들_수_없다() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            P2PSettingsPanel panel = new P2PSettingsPanel(
                    new P2PSettingsModel(new InMemoryP2PSettingsService(peers(4))), () -> true);
            panel.setSize(432, 360);
            panel.doLayout();
            panel.paint(new BufferedImage(432, 360, BufferedImage.TYPE_INT_RGB).getGraphics());

            P2PSettingsModel model = new P2PSettingsModel(new InMemoryP2PSettingsService());
            assertThrows(NullPointerException.class, () -> new P2PSettingsPanel(null, () -> true));
            assertThrows(NullPointerException.class, () -> new P2PSettingsPanel(model, null));
        });
    }
}
