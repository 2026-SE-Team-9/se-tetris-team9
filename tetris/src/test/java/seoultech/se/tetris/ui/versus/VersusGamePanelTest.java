package seoultech.se.tetris.ui.versus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.time.Duration;

import org.junit.jupiter.api.Test;

import seoultech.se.tetris.engine.Difficulty;
import seoultech.se.tetris.ui.game.GamePanel;
import seoultech.se.tetris.versus.EndReason;
import seoultech.se.tetris.versus.LocalMatch;
import seoultech.se.tetris.versus.MatchResult;
import seoultech.se.tetris.versus.Side;
import seoultech.se.tetris.versus.VersusMode;

class VersusGamePanelTest {

    private final LocalMatch match = LocalMatch.create(VersusMode.ITEM, Difficulty.NORMAL, 7L);
    private final VersusScreenModel model = new VersusScreenModel(VersusMode.ITEM);
    private final GamePanel p1 = new GamePanel(match.state(Side.P1));
    private final GamePanel p2 = new GamePanel(match.state(Side.P2));
    private final VersusGamePanel panel = new VersusGamePanel(model, p1, p2);

    @Test
    void 두_보드를_자리별로_가진다() {
        assertSame(p1, panel.board(Side.P1));
        assertSame(p2, panel.board(Side.P2));
    }

    @Test
    void 대전_화면의_두_보드는_공격_대기열을_표시한다() {
        assertTrue(p1.isAttackQueueVisible());
        assertTrue(p2.isAttackQueueVisible());
    }

    @Test
    void 두_보드를_나란히_담을_만큼_넓다() {
        Dimension size = panel.getPreferredSize();
        Dimension board = p1.getPreferredSize();

        assertTrue(size.width >= board.width * 2, size.toString());
        assertTrue(size.height > board.height, size.toString());
    }

    @Test
    void 대전이_끝나면_각_보드에_승패를_덮어_표시한다() {
        model.matchEnded(MatchResult.win(Side.P2, EndReason.TOP_OUT, 0, 100));

        panel.refresh();

        assertEquals("패배", p1.getOverlayText());
        assertEquals("승리", p2.getOverlayText());
    }

    @Test
    void 대전_중에는_보드의_기본_표시를_쓴다() {
        panel.refresh();

        assertNull(p1.getOverlayText());
        assertNull(p2.getOverlayText());
    }

    @Test
    void 대전_중과_종료_후_모두_그릴_수_있다() {
        Dimension size = panel.getPreferredSize();
        panel.setSize(size);
        panel.doLayout();
        BufferedImage image = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB);

        panel.printAll(image.getGraphics());
        model.matchEnded(MatchResult.draw(EndReason.TIME_UP, 0, 0));
        panel.refresh();
        panel.printAll(image.getGraphics());
    }

    /** 위쪽 줄 오른쪽(남은 시간 자리)에 주어진 색의 픽셀이 있는지 */
    private static boolean timerAreaHas(VersusGamePanel target, java.awt.Color color) {
        Dimension size = target.getPreferredSize();
        target.setSize(size);
        target.doLayout();
        BufferedImage image = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_RGB);
        target.printAll(image.getGraphics());
        for (int x = size.width - 160; x < size.width; x++) {
            for (int y = 0; y < 76; y++) {
                if (image.getRGB(x, y) == color.getRGB()) {
                    return true;
                }
            }
        }
        return false;
    }

    private VersusGamePanel timeLimitPanel(long elapsedMillis) {
        long[] now = {0};
        CountdownClock clock = new CountdownClock(Duration.ofMinutes(3), () -> now[0]);
        clock.start();
        now[0] = elapsedMillis;
        VersusScreenModel timeModel = new VersusScreenModel(VersusMode.TIME_LIMIT, clock);
        return new VersusGamePanel(timeModel, new GamePanel(match.state(Side.P1)), new GamePanel(match.state(Side.P2)));
    }

    @Test
    void 시간제한_모드에서는_남은_시간을_그린다() {
        assertTrue(timerAreaHas(timeLimitPanel(0), VersusGamePanel.TIMER_COLOR));
        assertFalse(timerAreaHas(timeLimitPanel(0), VersusGamePanel.TIMER_URGENT_COLOR));
    }

    @Test
    void 남은_시간이_얼마_없으면_붉게_그린다() {
        assertTrue(timerAreaHas(timeLimitPanel(175_000), VersusGamePanel.TIMER_URGENT_COLOR));
    }

    @Test
    void 시간제한이_아니면_남은_시간을_그리지_않는다() {
        assertFalse(timerAreaHas(panel, VersusGamePanel.TIMER_COLOR));
        panel.repaintTimer();
    }

    @Test
    void 필요한_값이_없으면_만들_수_없다() {
        assertThrows(NullPointerException.class, () -> new VersusGamePanel(null, p1, p2));
        assertThrows(NullPointerException.class, () -> new VersusGamePanel(model, null, p2));
        assertThrows(NullPointerException.class, () -> new VersusGamePanel(model, p1, null));
        assertThrows(NullPointerException.class, () -> panel.board(null));
    }
}
