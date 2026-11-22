package seoultech.se.tetris.ui.game;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.List;

import javax.swing.JPanel;

import seoultech.se.tetris.blocks.Block;
import seoultech.se.tetris.blocks.BlockType;
import seoultech.se.tetris.blocks.CellCode;
import seoultech.se.tetris.component.model.GameState;
import seoultech.se.tetris.component.model.GarbageLine;
import seoultech.se.tetris.engine.Board;

/** 보드(20x10), 다음 블럭, 점수·레벨·줄 수를 그리는 인게임 화면. {@link GameState}를 읽기만 한다. */
public class GamePanel extends JPanel {

    private static final long serialVersionUID = 1L;

    /** 배율 1.0일 때의 칸·여백·옆 패널 크기(px). 실제 크기는 여기에 배율을 곱한다. */
    private static final int BASE_CELL = 28;
    private static final int BASE_MARGIN = 24;
    private static final int BASE_SIDE_W = 150;
    private static final int BASE_MINI_CELL = 22;

    /**
     * 공격 대기열에 표시하는 최대 줄 수. 대전에서 한 번에 대기할 수 있는 공격 줄은 최대 10줄이다. (3차 요구사항)
     */
    static final int ATTACK_QUEUE_ROWS = 10;
    /** 배율 1.0일 때 공격 대기열 칸 크기와 위치(px). 10칸 폭이 옆 패널 폭(150)과 같고, LINES 아래 빈 공간에 들어간다. */
    private static final int BASE_QUEUE_CELL = 15;
    private static final int BASE_QUEUE_TOP = 386;
    /** 대기열이 가득 찼을 때 줄 수 표시 색. 더 받은 공격은 무시된다는 것을 알린다. */
    private static final Color ATTACK_FULL_TEXT = new Color(255, 110, 110);

    /** 공격을 보냈을 때 보드 위에 떠오르는 문구의 색 */
    static final Color ATTACK_POPUP_COLOR = new Color(255, 170, 60);
    /** 공격 대기열에 새로 붙은 줄이 깜빡일 때의 칸 색 */
    static final Color QUEUE_HIGHLIGHT_COLOR = new Color(255, 120, 120);
    /** 문구가 떠오르는 높이(px, 배율 1.0 기준) */
    private static final int BASE_POPUP_RISE = 60;
    private static final Color GRID = new Color(40, 40, 40);
    private static final Color GHOST_FILL = new Color(180, 180, 180, 90);
    private static final Color GHOST_TEXT = new Color(200, 200, 200);

    /** 줄 삭제 애니메이션에서 지워질 칸을 칠하는 색 */
    static final Color FLASH_COLOR = Color.WHITE;

    private final transient GameState state;
    private boolean colorBlind;
    private String overlayText;
    private boolean attackQueueVisible;
    private transient BoardEffects effects = BoardEffects.NONE;
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
     * 보드 위에 덮어 표시할 문구를 정한다. {@code null}이면 게임 상태에 따른 기본 문구("GAME OVER", "일시정지")를 쓴다.
     *
     * <p>대전 모드처럼 게임이 끝난 뒤 승패를 따로 보여줘야 하는 화면에서 사용한다.
     */
    public void setOverlayText(String overlayText) {
        this.overlayText = overlayText;
        repaint();
    }

    public String getOverlayText() {
        return overlayText;
    }

    /**
     * 옆 패널의 점수 아래에 공격 대기열을 표시할지 정한다. 대전 화면에서만 켜며, 1인 모드는 기본값(표시 안 함)이다.
     *
     * <p>대기 줄은 {@link GameState#getPendingGarbage()}에서 읽으므로, 로컬 대전과 P2P 대전의 상대 보드 모두에 쓸 수 있다.
     */
    public void setAttackQueueVisible(boolean attackQueueVisible) {
        this.attackQueueVisible = attackQueueVisible;
        repaint();
    }

    public boolean isAttackQueueVisible() {
        return attackQueueVisible;
    }

    /**
     * 보드 위에 덧그릴 효과(줄 공격 애니메이션 등)를 정한다. {@code null}이면 효과를 그리지 않는다.
     *
     * <p>효과는 시간에 따라 바뀌므로, 효과가 진행 중인 동안 주기적으로 {@link #repaint()}를 호출해야 한다.
     */
    public void setEffects(BoardEffects effects) {
        this.effects = effects == null ? BoardEffects.NONE : effects;
        repaint();
    }

    public BoardEffects getEffects() {
        return effects;
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
        drawPopup(g2);
        if (overlayText != null) {
            drawOverlay(g2, overlayText);
        } else if (state.isGameOver()) {
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
        drawInsertedRowsHighlight(g2);
        g2.setColor(Color.WHITE);
        g2.drawRect(margin - 1, margin - 1, boardW + 1, boardH + 1);
    }

    /** 보드에 막 추가된 공격 줄(맨 아래 줄들)을 흰색으로 덮어 빛나게 한다. 시간이 지날수록 옅어진다. */
    private void drawInsertedRowsHighlight(Graphics2D g2) {
        int rows = Math.min(effects.highlightedBottomRows(), Board.VISIBLE_HEIGHT);
        double strength = Math.min(1.0, Math.max(0.0, effects.bottomRowsHighlightStrength()));
        if (rows <= 0 || strength <= 0) {
            return;
        }
        g2.setColor(new Color(255, 255, 255, (int) Math.round(180 * strength)));
        g2.fillRect(margin, margin + boardH - rows * cell, boardW, rows * cell);
    }

    /**
     * 공격을 보냈을 때의 문구를 보드 가운데 아래쪽에서 위로 떠오르게 그린다.
     * 앞의 절반 동안은 선명하게 보이고, 나머지 절반 동안 옅어지며 사라진다.
     */
    private void drawPopup(Graphics2D g2) {
        String text = effects.popupText();
        if (text == null) {
            return;
        }
        double progress = Math.min(1.0, Math.max(0.0, effects.popupProgress()));
        int alpha = (int) Math.round(255 * Math.min(1.0, (1.0 - progress) * 2));
        int y = margin + boardH * 3 / 5 - (int) Math.round(scaled(BASE_POPUP_RISE) * progress);

        g2.setFont(font(Font.DIALOG, Font.BOLD, 28));
        FontMetrics fm = g2.getFontMetrics();
        int x = margin + (boardW - fm.stringWidth(text)) / 2;
        int shadow = Math.max(1, scaled(2));
        g2.setColor(new Color(0, 0, 0, alpha));
        g2.drawString(text, x + shadow, y + shadow);
        g2.setColor(new Color(ATTACK_POPUP_COLOR.getRed(), ATTACK_POPUP_COLOR.getGreen(),
                ATTACK_POPUP_COLOR.getBlue(), alpha));
        g2.drawString(text, x, y);
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

        if (attackQueueVisible) {
            drawAttackQueue(g2);
        }
    }

    /**
     * 공격 대기열. 대기 중인 공격 줄을 넘어온 형태 그대로 위에서부터 순서대로 그린다.
     *
     * <p>대기 줄은 보드에 추가될 때 이 순서 그대로 보드 아래쪽에 붙는다. (3차 팀 결정)
     */
    private void drawAttackQueue(Graphics2D g2) {
        List<GarbageLine> pending = state.getPendingGarbage();
        int top = attackQueueTop();
        int queueCell = attackQueueCell();

        g2.setFont(font(Font.DIALOG, Font.BOLD, 14));
        g2.setColor(Color.GRAY);
        g2.drawString("ATTACK", sideX, top - scaled(8));
        String count = pending.size() + " / " + ATTACK_QUEUE_ROWS;
        g2.setColor(pending.size() >= ATTACK_QUEUE_ROWS ? ATTACK_FULL_TEXT : Color.WHITE);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(count, sideX + Board.WIDTH * queueCell - fm.stringWidth(count), top - scaled(8));

        g2.setColor(GRID);
        for (int r = 0; r <= ATTACK_QUEUE_ROWS; r++) {
            g2.drawLine(sideX, top + r * queueCell, sideX + Board.WIDTH * queueCell, top + r * queueCell);
        }
        for (int c = 0; c <= Board.WIDTH; c++) {
            g2.drawLine(sideX + c * queueCell, top, sideX + c * queueCell, top + ATTACK_QUEUE_ROWS * queueCell);
        }

        for (int r = 0; r < Math.min(pending.size(), ATTACK_QUEUE_ROWS); r++) {
            GarbageLine line = pending.get(r);
            boolean highlighted = effects.isQueueRowHighlighted(r);
            for (int c = 0; c < Math.min(line.width(), Board.WIDTH); c++) {
                if (!line.isFilled(c)) {
                    continue;
                }
                int x = sideX + c * queueCell;
                int y = top + r * queueCell;
                if (highlighted) {
                    // 새로 붙은 줄은 깜빡이는 동안 붉게 칠한다
                    g2.setColor(QUEUE_HIGHLIGHT_COLOR);
                    g2.fillRect(x + 1, y + 1, queueCell - 2, queueCell - 2);
                } else {
                    drawCell(g2, x, y, CellCode.GARBAGE, queueCell);
                }
            }
        }
    }

    /** 공격 대기열 칸 하나의 크기(px) */
    int attackQueueCell() {
        return scaled(BASE_QUEUE_CELL);
    }

    /** 공격 대기열 영역의 위쪽 y 좌표(px) */
    int attackQueueTop() {
        return margin + scaled(BASE_QUEUE_TOP);
    }

    /** 공격 대기열 영역의 왼쪽 x 좌표(px) */
    int attackQueueLeft() {
        return sideX;
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
