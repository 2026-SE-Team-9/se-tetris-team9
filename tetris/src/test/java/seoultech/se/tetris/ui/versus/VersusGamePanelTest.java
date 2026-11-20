package seoultech.se.tetris.ui.versus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Dimension;
import java.awt.image.BufferedImage;

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

    @Test
    void 필요한_값이_없으면_만들_수_없다() {
        assertThrows(NullPointerException.class, () -> new VersusGamePanel(null, p1, p2));
        assertThrows(NullPointerException.class, () -> new VersusGamePanel(model, null, p2));
        assertThrows(NullPointerException.class, () -> new VersusGamePanel(model, p1, null));
        assertThrows(NullPointerException.class, () -> panel.board(null));
    }
}
