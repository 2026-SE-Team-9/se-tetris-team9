package seoultech.se.tetris.ui.versus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import seoultech.se.tetris.versus.EndReason;
import seoultech.se.tetris.versus.MatchResult;
import seoultech.se.tetris.versus.Side;
import seoultech.se.tetris.versus.VersusMode;

class VersusEndPanelTest {

    private final List<String> log = new ArrayList<>();

    private VersusEndPanel panel(MatchResult result) {
        return new VersusEndPanel(new VersusEndModel(VersusMode.NORMAL, result,
                () -> log.add("restart"), () -> log.add("menu")));
    }

    private static BufferedImage render(VersusEndPanel panel) {
        Dimension size = panel.getPreferredSize();
        panel.setSize(size);
        BufferedImage image = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_RGB);
        panel.paint(image.getGraphics());
        return image;
    }

    /** 플레이어 카드의 위쪽 테두리 가운데 픽셀 */
    private static Color cardTopBorder(VersusEndPanel panel, BufferedImage image, Side side) {
        return new Color(image.getRGB(panel.cardLeft(side) + 90, panel.cardTop()));
    }

    @Test
    void 기존_종료_화면과_같은_크기다() {
        assertEquals(new Dimension(480, 560), panel(MatchResult.draw(EndReason.TIME_UP, 0, 0)).getPreferredSize());
    }

    @Test
    void 승리한_플레이어의_카드만_테두리를_강조한다() {
        VersusEndPanel panel = panel(MatchResult.win(Side.P2, EndReason.TOP_OUT, 100, 900));
        BufferedImage image = render(panel);

        assertEquals(VersusEndPanel.WINNER_BORDER, cardTopBorder(panel, image, Side.P2));
        assertFalse(VersusEndPanel.WINNER_BORDER.equals(cardTopBorder(panel, image, Side.P1)));
    }

    @Test
    void 무승부이면_어느_카드도_강조하지_않는다() {
        VersusEndPanel panel = panel(MatchResult.draw(EndReason.TIME_UP, 500, 500));
        BufferedImage image = render(panel);

        for (Side side : Side.values()) {
            assertFalse(VersusEndPanel.WINNER_BORDER.equals(cardTopBorder(panel, image, side)), side.name());
        }
    }

    @Test
    void 두_카드는_화면_가운데에_좌우로_놓인다() {
        VersusEndPanel panel = panel(MatchResult.draw(EndReason.TIME_UP, 0, 0));
        panel.setSize(panel.getPreferredSize());

        int left = panel.cardLeft(Side.P1);
        int right = panel.cardLeft(Side.P2);
        assertTrue(left < right);
        assertEquals(panel.getWidth() - (right + 180), left);
    }

    @Test
    void 방향키와_Enter로_메뉴를_선택한다() {
        VersusEndPanel panel = panel(MatchResult.win(Side.P1, EndReason.TOP_OUT, 0, 0));

        panel.handleKey(KeyEvent.VK_DOWN);
        panel.handleKey(KeyEvent.VK_UP);
        panel.handleKey(KeyEvent.VK_ENTER);
        panel.handleKey(KeyEvent.VK_DOWN);
        panel.handleKey(KeyEvent.VK_ENTER);

        assertEquals(List.of("restart", "menu"), log);
    }

    @Test
    void 사용하지_않는_키를_누른_뒤에도_그릴_수_있다() {
        VersusEndPanel panel = panel(MatchResult.win(Side.P1, EndReason.FORFEIT, 0, 0));

        panel.handleKey(KeyEvent.VK_A);
        render(panel);

        assertTrue(log.isEmpty());
    }

    @Test
    void 모델이_없으면_만들_수_없다() {
        assertThrows(NullPointerException.class, () -> new VersusEndPanel(null));
    }
}
