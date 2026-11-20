package seoultech.se.tetris.ui.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import seoultech.se.tetris.engine.TetrisEngine;

class GamePanelOverlayTest {

    private static BufferedImage render(GamePanel panel) {
        Dimension size = panel.getPreferredSize();
        panel.setSize(size);
        BufferedImage image = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_RGB);
        panel.paint(image.getGraphics());
        return image;
    }

    /** 보드 한가운데 줄(덮어쓰기 문구가 그려지는 줄)의 픽셀 */
    private static int[] centerRow(BufferedImage image) {
        int y = image.getHeight() / 2;
        return image.getRGB(0, y, image.getWidth(), 1, null, 0, image.getWidth());
    }

    @Test
    void 기본으로는_덮어쓰기_문구가_없다() {
        assertNull(new GamePanel(new TetrisEngine()).getOverlayText());
    }

    @Test
    void 덮어쓰기_문구를_정하고_지울_수_있다() {
        GamePanel panel = new GamePanel(new TetrisEngine());

        panel.setOverlayText("승리");
        assertEquals("승리", panel.getOverlayText());

        panel.setOverlayText(null);
        assertNull(panel.getOverlayText());
    }

    @Test
    void 덮어쓰기_문구가_있으면_진행_중인_게임에도_표시된다() {
        TetrisEngine engine = new TetrisEngine();
        GamePanel plain = new GamePanel(engine);
        GamePanel withOverlay = new GamePanel(engine);
        withOverlay.setOverlayText("패배");

        assertNotEquals(Arrays.toString(centerRow(render(plain))),
                Arrays.toString(centerRow(render(withOverlay))));
    }
}
