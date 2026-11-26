package seoultech.se.tetris.ui.theme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

import org.junit.jupiter.api.Test;

class RetroConfirmPanelTest {

    private final List<Boolean> answers = new ArrayList<>();
    private final RetroConfirmPanel panel = new RetroConfirmPanel("타이틀로 돌아가기",
            "현재 게임 진행 상황이 사라집니다. 타이틀로 돌아가시겠습니까?", answers::add);

    private BufferedImage render() {
        Dimension size = panel.getPreferredSize();
        panel.setSize(size);
        BufferedImage image = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_RGB);
        panel.paint(image.getGraphics());
        return image;
    }

    @Test
    void 문구는_문장마다_한_줄로_나눈다() {
        assertEquals(List.of("현재 게임 진행 상황이 사라집니다.", "타이틀로 돌아가시겠습니까?"), panel.messageLines());
        assertEquals(List.of("한 문장"), RetroConfirmPanel.splitSentences("  한 문장  "));
    }

    @Test
    void 처음에는_예가_선택되어_있고_Enter로_고른다() {
        assertEquals(RetroConfirmPanel.YES_INDEX, panel.selectedIndex());

        panel.handleKey(KeyEvent.VK_ENTER);

        assertEquals(List.of(true), answers);
    }

    @Test
    void 방향키나_Tab으로_아니오로_바꿔_고른다() {
        panel.handleKey(KeyEvent.VK_RIGHT);
        assertEquals(RetroConfirmPanel.NO_INDEX, panel.selectedIndex());
        panel.handleKey(KeyEvent.VK_TAB);
        panel.handleKey(KeyEvent.VK_DOWN);
        panel.handleKey(KeyEvent.VK_A);

        panel.handleKey(KeyEvent.VK_ENTER);

        assertEquals(List.of(false), answers);
    }

    @Test
    void Esc는_아니오다() {
        for (var listener : panel.getKeyListeners()) {
            listener.keyPressed(new KeyEvent(new JPanel(), KeyEvent.KEY_PRESSED, 0, 0, KeyEvent.VK_ESCAPE,
                    KeyEvent.CHAR_UNDEFINED));
        }

        assertEquals(List.of(false), answers);
    }

    @Test
    void 분홍_틀에_선택한_버튼만_노란_상자로_그린다() {
        BufferedImage image = render();
        Rectangle yes = RetroConfirmPanel.buttonBounds(RetroConfirmPanel.YES_INDEX);
        Rectangle no = RetroConfirmPanel.buttonBounds(RetroConfirmPanel.NO_INDEX);

        assertEquals(RetroTheme.PINK.getRGB(), image.getRGB(RetroConfirmPanel.WIDTH / 2, 1));
        assertEquals(RetroTheme.YELLOW.getRGB(), image.getRGB(yes.x + yes.width / 2, yes.y));
        assertNotEquals(RetroTheme.YELLOW.getRGB(), image.getRGB(no.x + no.width / 2, no.y));
        assertTrue(yes.x + yes.width < no.x);

        panel.handleKey(KeyEvent.VK_LEFT);
        image = render();
        assertEquals(RetroTheme.YELLOW.getRGB(), image.getRGB(no.x + no.width / 2, no.y));
    }

    @Test
    void 필요한_값이_없으면_만들_수_없다() {
        assertThrows(NullPointerException.class, () -> new RetroConfirmPanel(null, "문구", yes -> { }));
        assertThrows(NullPointerException.class, () -> new RetroConfirmPanel("제목", null, yes -> { }));
        assertThrows(NullPointerException.class, () -> new RetroConfirmPanel("제목", "문구", null));
    }
}
