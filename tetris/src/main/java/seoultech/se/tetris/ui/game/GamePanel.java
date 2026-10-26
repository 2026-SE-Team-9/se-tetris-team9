package seoultech.se.tetris.ui.game;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JPanel;

import seoultech.se.tetris.blocks.Block;
import seoultech.se.tetris.blocks.BlockType;
import seoultech.se.tetris.blocks.CellCode;
import seoultech.se.tetris.component.model.GameState;
import seoultech.se.tetris.engine.Board;

/** 보드(20x10), 다음 블럭, 점수·레벨·줄 수를 그리는 인게임 화면. {@link GameState}를 읽기만 한다. */
public class GamePanel extends JPanel {

    private static final long serialVersionUID = 1L;

    /** 배율 1.0일 때의 칸·여백·옆 패널 크기(px). 실제 크기는 여기에 배율을 곱한다. */
    private static final int BASE_CELL = 28;
    private static final int BASE_MARGIN = 24;
    private static final int BASE_SIDE_W = 150;
    private static final int BASE_MINI_CELL = 22;
    private static final Color GRID = new Color(40, 40, 40);
    private static final Color GHOST_FILL = new Color(180, 180, 180, 90);
    private static final Color GHOST_TEXT = new Color(200, 200, 200);

    /** 줄 삭제 애니메이션에서 지워질 칸을 칠하는 색 */
    static final Color FLASH_COLOR = Color.WHITE;

    private final transient GameState state;
    private boolean colorBlind;
    private double scale;
    private int cell;
    private int margin;
    private int boardW;
    private int boardH;
    private int sideX;
    private int sideW;

    public GamePanel(GameState state) {
        this(state, 1.0);
    }

    /** @param scale 기본 크기에 곱하는 배율. 0보다 커야 한다 */
    public GamePanel(GameState state, double scale) {
        this(state, scale, false);
    }

    /**
     * @param scale      기본 크기에 곱하는 배율. 0보다 커야 한다
     * @param colorBlind 색맹 모드 색표를 쓸지 여부
     */
    public GamePanel(GameState state, double scale, boolean colorBlind) {
        this.state = state;
        this.colorBlind = colorBlind;
        setBackground(Color.BLACK);
        setScale(scale);
    }

    /** 색맹 모드를 켜거나 끈다. 다시 그리기만 하면 되므로 창 크기는 바뀌지 않는다. */
    public void setColorBlind(boolean colorBlind) {
        this.colorBlind = colorBlind;
        repaint();
    }

    public boolean isColorBlind() {
        return colorBlind;
    }

    /**
     * 화면 배율을 바꾼다. 선호 크기가 바뀌므로 창에 담긴 뒤에는 창의 {@code pack()}을 다시 호출해야 한다.
     */
    public final void setScale(double scale) {
        if (!(scale > 0)) {
            throw new IllegalArgumentException("배율은 0보다 커야 합니다: " + scale);
        }
        this.scale = scale;
        cell = scaled(BASE_CELL);
        margin = scaled(BASE_MARGIN);
        boardW = Board.WIDTH * cell;
        boardH = Board.VISIBLE_HEIGHT * cell;
        sideX = margin + boardW + margin;
        sideW = scaled(BASE_SIDE_W);
        setPreferredSize(new Dimension(sideX + sideW + margin, boardH + margin * 2));
        revalidate();
        repaint();
    }

    public double getScale() {
        return scale;
    }

    private int scaled(int base) {
        return (int) Math.round(base * scale);
    }

    private Font font(String family, int style, int baseSize) {
        return new Font(family, style, scaled(baseSize));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        drawBoard(g2);
        drawSidePanel(g2);
        if (state.isGameOver()) {
            drawOverlay(g2, "GAME OVER");
        } else if (state.isPaused()) {
            drawOverlay(g2, "일시정지");
        }
        g2.dispose();
    }

    private void drawBoard(Graphics2D g2) {
        g2.setColor(GRID);
        for (int r = 0; r <= Board.VISIBLE_HEIGHT; r++) {
            g2.drawLine(margin, margin + r * cell, margin + boardW, margin + r * cell);
        }
        for (int c = 0; c <= Board.WIDTH; c++) {
            g2.drawLine(margin + c * cell, margin, margin + c * cell, margin + boardH);
        }

        int[][] grid = state.getBoard();
        boolean[][] clearing = state.getClearingCells();
        int offset = Board.HEIGHT - Board.VISIBLE_HEIGHT;
        for (int r = offset; r < Board.HEIGHT; r++) {
            for (int c = 0; c < Board.WIDTH; c++) {
                if (CellCode.isEmpty(grid[r][c])) {
                    continue;
                }
                int x = margin + c * cell;
                int y = margin + (r - offset) * cell;
                if (isFlashing(clearing, r - offset, c)) {
                    drawFlashCell(g2, x, y);
                } else {
                    drawCell(g2, x, y, grid[r][c], cell);
                }
            }
        }

        // 줄 삭제 애니메이션 중에는 현재 블럭이 이미 보드에 고정되어 있으므로 다시 그리지 않는다.
        Block current = state.getCurrentBlock();
        if (current != null && !state.isGameOver() && !state.isClearing()) {
            Block ghost = current.moved(state.getGhostY() - current.row(), 0);
            if (ghost.row() != current.row()) {
                drawGhost(g2, ghost, offset);
            }
            for (int r = 0; r < BlockType.SIZE; r++) {
                for (int c = 0; c < BlockType.SIZE; c++) {
                    int row = current.boardRow(r) - offset;
                    if (current.isFilled(r, c) && row >= 0) {
                        drawCell(g2, margin + current.boardCol(c) * cell, margin + row * cell,
                                current.codeAt(r, c), cell);
                    }
                }
            }
        }
        g2.setColor(Color.WHITE);
        g2.drawRect(margin - 1, margin - 1, boardW + 1, boardH + 1);
    }

    /** 실제 블럭 아래에 착지 예상 위치를 흐린 # 문자로 그린다. */
    private void drawGhost(Graphics2D g2, Block ghost, int offset) {
        g2.setFont(new Font(Font.MONOSPACED, Font.BOLD, cell * 5 / 8));
        FontMetrics fm = g2.getFontMetrics();
        for (int r = 0; r < BlockType.SIZE; r++) {
            for (int c = 0; c < BlockType.SIZE; c++) {
                if (!ghost.isFilled(r, c)) {
                    continue;
                }
                int row = ghost.boardRow(r) - offset;
                if (row < 0 || row >= Board.VISIBLE_HEIGHT) {
                    continue;
                }
                int x = margin + ghost.boardCol(c) * cell;
                int y = margin + row * cell;
                g2.setColor(GHOST_FILL);
                g2.fillRect(x + 1, y + 1, cell - 2, cell - 2);
                g2.setColor(GHOST_TEXT);
                g2.drawString("#", x + (cell - fm.stringWidth("#")) / 2,
                        y + (cell + fm.getAscent() - fm.getDescent()) / 2);
            }
        }
    }

    private void drawSidePanel(Graphics2D g2) {
        g2.setColor(Color.WHITE);
        g2.setFont(font(Font.DIALOG, Font.BOLD, 16));
        g2.drawString("NEXT", sideX, margin + scaled(16));
        Block next = state.getNextBlock();
        if (next != null) {
            int mini = scaled(BASE_MINI_CELL);
            for (int r = 0; r < BlockType.SIZE; r++) {
                for (int c = 0; c < BlockType.SIZE; c++) {
                    if (next.isFilled(r, c)) {
                        drawCell(g2, sideX + c * mini, margin + scaled(30) + r * mini, next.codeAt(r, c), mini);
                    }
                }
            }
        }

        int y = margin + scaled(170);
        y = drawStat(g2, "SCORE", String.valueOf(state.getScore()), y);
        y = drawStat(g2, "LEVEL", String.valueOf(state.getLevel()), y);
        drawStat(g2, "LINES", String.valueOf(state.getLinesCleared()), y);
    }

    private int drawStat(Graphics2D g2, String label, String value, int y) {
        g2.setFont(font(Font.DIALOG, Font.BOLD, 14));
        g2.setColor(Color.GRAY);
        g2.drawString(label, sideX, y);
        g2.setFont(font(Font.MONOSPACED, Font.BOLD, 22));
        g2.setColor(Color.WHITE);
        g2.drawString(value, sideX, y + scaled(26));
        return y + scaled(66);
    }

    /**
     * 보이는 보드의 (행, 열)이 줄 삭제 애니메이션으로 지워질 칸인지 판단한다.
     *
     * @param clearing {@link GameState#getClearingCells()}의 값
     * @param visibleRow 보이는 보드 기준 행 (맨 위가 0)
     */
    static boolean isFlashing(boolean[][] clearing, int visibleRow, int col) {
        return visibleRow >= 0 && visibleRow < clearing.length
                && col >= 0 && col < clearing[visibleRow].length
                && clearing[visibleRow][col];
    }

    private void drawFlashCell(Graphics2D g2, int x, int y) {
        g2.setColor(FLASH_COLOR);
        g2.fillRect(x + 1, y + 1, cell - 2, cell - 2);
    }

    /**
     * 칸 번호의 색과 문자로 칸 하나를 그린다. 테트로미노 칸과 아이템 칸 모두 같은 방식이다.
     *
     * <p>색맹 모드의 아이템 칸은 안쪽에 2픽셀 테두리를 그린다. 테두리는 바깥 1픽셀을 띄워
     * 칸의 가장자리 색은 그대로 남긴다.
     */
    private void drawCell(Graphics2D g2, int x, int y, int code, int size) {
        Color fill = BlockPalette.colorOf(code, colorBlind);
        g2.setColor(fill);
        g2.fillRect(x + 1, y + 1, size - 2, size - 2);
        Color border = BlockPalette.borderColorOf(code, colorBlind);
        if (border != null) {
            g2.setColor(border);
            g2.fillRect(x + 3, y + 3, size - 6, size - 6);
            g2.setColor(fill);
            g2.fillRect(x + 5, y + 5, size - 10, size - 10);
        }
        g2.setColor(BlockPalette.textColorOf(code, colorBlind));
        g2.setFont(new Font(Font.MONOSPACED, Font.BOLD, size * 5 / 8));
        FontMetrics fm = g2.getFontMetrics();
        String s = String.valueOf(CellCode.symbolOf(code));
        g2.drawString(s, x + (size - fm.stringWidth(s)) / 2, y + (size + fm.getAscent() - fm.getDescent()) / 2);
    }

    private void drawOverlay(Graphics2D g2, String text) {
        g2.setColor(new Color(0, 0, 0, 170));
        g2.fillRect(margin, margin, boardW, boardH);
        g2.setColor(Color.WHITE);
        g2.setFont(font(Font.DIALOG, Font.BOLD, 26));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(text, margin + (boardW - fm.stringWidth(text)) / 2, margin + boardH / 2);
    }
}
