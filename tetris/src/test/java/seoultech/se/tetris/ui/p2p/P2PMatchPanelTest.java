package seoultech.se.tetris.ui.p2p;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;

import seoultech.se.tetris.engine.TetrisEngine;
import seoultech.se.tetris.ui.game.GamePanel;
import seoultech.se.tetris.ui.versus.VersusPresentation;
import seoultech.se.tetris.ui.versus.VersusScreenModel;
import seoultech.se.tetris.versus.EndReason;
import seoultech.se.tetris.versus.MatchResult;
import seoultech.se.tetris.versus.Side;
import seoultech.se.tetris.versus.VersusMode;
import seoultech.se.tetris.versus.p2p.ConnectionState;
import seoultech.se.tetris.versus.p2p.Role;

class P2PMatchPanelTest {

    private final GamePanel p1 = new GamePanel(new TetrisEngine());
    private final GamePanel p2 = new GamePanel(new TetrisEngine());

    private P2PMatchPanel panel(Role role) {
        VersusScreenModel model = new VersusScreenModel(VersusMode.NORMAL, null, VersusPresentation.p2p(role));
        return P2PMatchPanel.create(model, p1, p2);
    }

    private static void layoutAll(Container container) {
        container.doLayout();
        for (Component child : container.getComponents()) {
            if (child instanceof Container nested) {
                layoutAll(nested);
            }
        }
    }

    private static BufferedImage render(P2PMatchPanel panel) {
        Dimension size = panel.getPreferredSize();
        panel.setSize(size);
        layoutAll(panel);
        BufferedImage image = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_RGB);
        panel.printAll(image.getGraphics());
        return image;
    }

    /** 보드가 놓인 열의 왼쪽 x 좌표 */
    private static int columnX(GamePanel board) {
        return board.getParent().getX();
    }

    @Test
    void 방장은_자기_보드인_P1이_왼쪽이다() {
        P2PMatchPanel panel = panel(Role.SERVER);
        render(panel);

        assertTrue(columnX(p1) < columnX(p2));
    }

    @Test
    void 참여자는_자기_보드인_P2가_왼쪽이다() {
        P2PMatchPanel panel = panel(Role.CLIENT);
        render(panel);

        assertTrue(columnX(p2) < columnX(p1));
        assertSame(p2, panel.game().board(Side.P2));
    }

    @Test
    void 연결_상태와_왕복_시간을_색과_함께_보여준다() {
        P2PMatchPanel panel = panel(Role.SERVER);
        assertEquals("연결 상태: 정상", panel.connectionText());

        panel.setConnection(ConnectionState.LAGGING, 340);

        assertEquals("연결 상태: 지연 · 340ms", panel.connectionText());
        assertEquals(P2PPanel.SELECTED, panel.connectionColor());
    }

    @Test
    void 위쪽에_연결_상태_줄이_더해진다() {
        P2PMatchPanel panel = panel(Role.SERVER);

        int gameHeight = panel.game().getPreferredSize().height;
        assertEquals(gameHeight + P2PMatchPanel.STATUS_HEIGHT, panel.getPreferredSize().height);
    }

    @Test
    void 대전_중과_카운트다운과_결과를_그릴_수_있다() {
        P2PMatchPanel panel = panel(Role.CLIENT);
        VersusScreenModel model = new VersusScreenModel(VersusMode.NORMAL, null,
                VersusPresentation.p2p(Role.CLIENT));
        P2PMatchPanel withModel = P2PMatchPanel.create(model, new GamePanel(new TetrisEngine()),
                new GamePanel(new TetrisEngine()));

        assertDoesNotThrow(() -> {
            render(panel);
            model.setCountdownSeconds(2);
            withModel.game().refresh();
            render(withModel);
            model.matchEnded(MatchResult.win(Side.P2, EndReason.FORFEIT, 0, 0));
            withModel.game().refresh();
            for (ConnectionState state : ConnectionState.values()) {
                withModel.setConnection(state, 20);
                render(withModel);
            }
        });
        assertEquals("승리", withModel.game().board(Side.P2).getOverlayText());
    }

    @Test
    void 연결_상태_줄부터_보드까지_같은_레트로_배경이_이어진다() {
        P2PMatchPanel panel = panel(Role.SERVER);
        assertFalse(panel.game().isOpaque());

        BufferedImage image = render(panel);
        // 연결 상태 줄의 왼쪽(글자 없는 곳)과 대전 화면 머리줄 왼쪽 모두 검정이 아닌 남보라 배경이다
        for (int y : new int[] {5, P2PMatchPanel.STATUS_HEIGHT + 5}) {
            Color color = new Color(image.getRGB(5, y));
            assertTrue(color.getBlue() > color.getRed(), "y=" + y + " " + color);
        }
    }

    @Test
    void 필요한_값이_없으면_만들_수_없다() {
        assertThrows(NullPointerException.class, () -> new P2PMatchPanel(null));
        assertThrows(NullPointerException.class, () -> panel(Role.SERVER).setConnection(null, 0));
    }
}
