package seoultech.se.tetris.ui.p2p;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

import org.junit.jupiter.api.Test;

import seoultech.se.tetris.ui.menu.MenuItem;

class P2PExitMenuPanelTest {

    private final List<String> calls = new ArrayList<>();
    private final P2PExitMenuPanel menu = new P2PExitMenuPanel(() -> calls.add("continue"), () -> calls.add("leave"));

    private BufferedImage render() {
        menu.setSize(1000, 700);
        BufferedImage image = new BufferedImage(1000, 700, BufferedImage.TYPE_INT_ARGB);
        menu.paint(image.getGraphics());
        return image;
    }

    @Test
    void 항목은_계속하기와_게임_나가기이고_계속하기가_먼저_선택된다() {
        assertEquals(List.of("계속하기", "게임 나가기 (기권패)"), menu.getItems().stream().map(MenuItem::label).toList());
        assertEquals(0, menu.getSelectedIndex());
    }

    @Test
    void 방향키와_Enter로_고르고_Esc는_계속하기다() {
        menu.handleKey(KeyEvent.VK_ENTER);
        menu.handleKey(KeyEvent.VK_DOWN);
        menu.handleKey(KeyEvent.VK_ENTER);
        menu.handleKey(KeyEvent.VK_ESCAPE);
        menu.handleKey(KeyEvent.VK_UP);
        menu.handleKey(KeyEvent.VK_A);

        assertEquals(List.of("continue", "leave", "continue"), calls);
    }

    @Test
    void 다시_열면_계속하기가_선택된다() {
        menu.handleKey(KeyEvent.VK_DOWN);

        menu.reset();

        assertEquals(0, menu.getSelectedIndex());
    }

    @Test
    void 실제_키_이벤트도_처리한다() {
        for (var listener : menu.getKeyListeners()) {
            listener.keyPressed(new KeyEvent(new JPanel(), KeyEvent.KEY_PRESSED, 0, 0, KeyEvent.VK_ESCAPE,
                    KeyEvent.CHAR_UNDEFINED));
        }

        assertEquals(List.of("continue"), calls);
    }

    @Test
    void 보드가_비쳐_보이도록_옅게_덮는다() {
        BufferedImage image = render();

        // 메뉴 상자 밖의 구석은 반투명 덮개만 있다
        int alpha = (image.getRGB(5, 5) >>> 24) & 0xFF;
        assertEquals(P2PExitMenuPanel.DIM.getAlpha(), alpha);
        assertTrue(alpha < 200);
        menu.handleKey(KeyEvent.VK_F1);
        assertDoesNotThrow(this::render);
    }

    @Test
    void 동작이_있어야_만들_수_있다() {
        assertThrows(NullPointerException.class, () -> new P2PExitMenuPanel(null, () -> { }));
        assertThrows(NullPointerException.class, () -> new P2PExitMenuPanel(() -> { }, null));
    }
}
