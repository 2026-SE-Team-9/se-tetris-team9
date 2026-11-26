package seoultech.se.tetris.ui.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

class StartMenuPanelTest {

    private final List<String> calls = new ArrayList<>();
    private final StartMenuModel model = new StartMenuModel(List.of(
            new MenuItem("시작", () -> calls.add("시작")),
            new MenuItem("대전 모드", () -> calls.add("대전 모드")),
            new MenuItem("P2P 대전 모드", () -> calls.add("P2P")),
            new MenuItem("아이템 모드", () -> calls.add("아이템")),
            new MenuItem("설정", () -> calls.add("설정")),
            new MenuItem("스코어보드", () -> calls.add("스코어보드")),
            new MenuItem("종료", () -> calls.add("종료"))));
    private final StartMenuPanel panel = new StartMenuPanel(model);

    private BufferedImage render() {
        Dimension size = panel.getPreferredSize();
        panel.setSize(size);
        BufferedImage image = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_RGB);
        panel.paint(image.getGraphics());
        return image;
    }

    private static boolean contains(BufferedImage image, Color color, Rectangle area) {
        for (int x = Math.max(0, area.x); x < Math.min(image.getWidth(), area.x + area.width); x++) {
            for (int y = Math.max(0, area.y); y < Math.min(image.getHeight(), area.y + area.height); y++) {
                if (image.getRGB(x, y) == color.getRGB()) {
                    return true;
                }
            }
        }
        return false;
    }

    @Test
    void 화면_크기는_기존과_같다() {
        assertEquals(new Dimension(480, 540), panel.getPreferredSize());
    }

    @Test
    void 제목은_분홍_글자에_하늘색_그림자다() {
        BufferedImage image = render();
        Rectangle titleArea = new Rectangle(0, 40, 480, StartMenuPanel.TITLE_BASELINE - 30);

        assertTrue(contains(image, RetroTheme.PINK, titleArea));
        assertTrue(contains(image, RetroTheme.CYAN, titleArea));
    }

    @Test
    void 메뉴는_하늘색_이중선_틀_안에_있다() {
        BufferedImage image = render();
        Rectangle frame = StartMenuPanel.MENU_FRAME;

        assertEquals(RetroTheme.CYAN.getRGB(), image.getRGB(frame.x + frame.width / 2, frame.y));
        assertTrue(StartMenuPanel.selectionBounds(6).y + 34 < frame.y + frame.height);
    }

    @Test
    void 선택한_항목만_노란_상자로_강조한다() {
        BufferedImage image = render();
        Rectangle first = StartMenuPanel.selectionBounds(0);
        Rectangle second = StartMenuPanel.selectionBounds(1);

        assertEquals(RetroTheme.YELLOW.getRGB(), image.getRGB(first.x + first.width / 2, first.y));
        assertFalse(contains(image, RetroTheme.YELLOW, second));

        panel.handleKey(KeyEvent.VK_DOWN);
        image = render();
        assertEquals(RetroTheme.YELLOW.getRGB(), image.getRGB(second.x + second.width / 2, second.y));
        assertFalse(image.getRGB(first.x + first.width / 2, first.y) == RetroTheme.YELLOW.getRGB());
    }

    @Test
    void 키_조작은_기존과_같다() {
        panel.handleKey(KeyEvent.VK_DOWN);
        panel.handleKey(KeyEvent.VK_DOWN);
        panel.handleKey(KeyEvent.VK_UP);
        panel.handleKey(KeyEvent.VK_ENTER);
        panel.handleKey(KeyEvent.VK_UP);
        panel.handleKey(KeyEvent.VK_UP);
        panel.handleKey(KeyEvent.VK_ENTER);

        assertEquals(List.of("대전 모드", "종료"), calls);
    }

    @Test
    void 사용하지_않는_키를_누르면_아래에_키_안내를_보여준다() {
        Rectangle hintArea = new Rectangle(0, 490, 480, 50);
        assertFalse(contains(render(), RetroTheme.TEXT, hintArea));

        panel.handleKey(KeyEvent.VK_A);

        assertTrue(model.isKeyHintVisible());
        assertTrue(contains(render(), RetroTheme.TEXT, hintArea));
    }
}
