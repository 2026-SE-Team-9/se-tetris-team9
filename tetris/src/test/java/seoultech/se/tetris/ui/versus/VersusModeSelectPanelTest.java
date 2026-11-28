package seoultech.se.tetris.ui.versus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import seoultech.se.tetris.ui.theme.RetroTheme;
import seoultech.se.tetris.versus.VersusMode;

/** 대전 모드 선택 화면의 레트로 디자인 */
class VersusModeSelectPanelTest {

    private final List<String> calls = new ArrayList<>();
    private final VersusModeSelectModel model = new VersusModeSelectModel(
            mode -> calls.add(mode.name()), () -> calls.add("back"));
    private final VersusModeSelectPanel panel = new VersusModeSelectPanel(model);

    private BufferedImage render() {
        Dimension size = panel.getPreferredSize();
        panel.setSize(size);
        BufferedImage image = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_RGB);
        panel.paint(image.getGraphics());
        return image;
    }

    @Test
    void 시작_메뉴와_같은_크기다() {
        assertEquals(new Dimension(480, 540), panel.getPreferredSize());
    }

    @Test
    void 하늘색_틀_안에_선택한_모드만_노란_상자로_그린다() {
        BufferedImage image = render();

        Rectangle frame = VersusModeSelectPanel.MENU_FRAME;
        Rectangle first = VersusModeSelectPanel.selectionBounds(0);
        Rectangle second = VersusModeSelectPanel.selectionBounds(1);
        assertEquals(RetroTheme.CYAN.getRGB(), image.getRGB(frame.x + frame.width / 2, frame.y));
        assertEquals(RetroTheme.YELLOW.getRGB(), image.getRGB(first.x + first.width / 2, first.y));
        assertNotEquals(RetroTheme.YELLOW.getRGB(), image.getRGB(second.x + second.width / 2, second.y));
        Color corner = new Color(image.getRGB(5, 5));
        assertTrue(corner.getBlue() > corner.getRed());

        panel.handleKey(KeyEvent.VK_DOWN);
        image = render();
        assertEquals(RetroTheme.YELLOW.getRGB(), image.getRGB(second.x + second.width / 2, second.y));
    }

    @Test
    void 네_항목이_모두_틀_안에_들어온다() {
        Rectangle last = VersusModeSelectPanel.selectionBounds(model.getOptions().size() - 1);
        Rectangle frame = VersusModeSelectPanel.MENU_FRAME;

        assertTrue(last.y + last.height < frame.y + frame.height);
    }

    @Test
    void 키_조작은_기존과_같다() {
        panel.handleKey(KeyEvent.VK_DOWN);
        panel.handleKey(KeyEvent.VK_ENTER);
        panel.handleKey(KeyEvent.VK_ESCAPE);
        panel.handleKey(KeyEvent.VK_UP);
        panel.handleKey(KeyEvent.VK_UP);
        panel.handleKey(KeyEvent.VK_ENTER);

        assertEquals(List.of(VersusMode.ITEM.name(), "back", "back"), calls);
    }

    @Test
    void 사용하지_않는_키를_누르면_키_안내를_보여준다() {
        panel.handleKey(KeyEvent.VK_A);
        BufferedImage image = render();

        assertTrue(model.isKeyHintVisible());
        boolean hint = false;
        for (int x = 0; x < image.getWidth() && !hint; x++) {
            for (int y = image.getHeight() - 45; y < image.getHeight() - 20; y++) {
                if (image.getRGB(x, y) == RetroTheme.TEXT.getRGB()) {
                    hint = true;
                    break;
                }
            }
        }
        assertTrue(hint);
    }
}
