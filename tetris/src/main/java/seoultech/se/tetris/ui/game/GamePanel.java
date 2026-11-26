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
import seoultech.se.tetris.ui.theme.RetroTheme;

/**
 * 보드(20x10), 다음 블럭, 점수·레벨·줄 수를 그리는 인게임 화면. {@link GameState}를 읽기만 한다.
 *
 * <p>레트로 디자인({@link RetroTheme})으로 그린다. 배경 격자 위에 보드와 옆 패널의 각 항목을 하늘색 이중선 틀에 담고,
 * 블럭 칸에는 밝은 윗면과 어두운 아랫면 음영을 더한다. 블럭 색과 문자, 고스트 블럭의 흐린 {@code #}, 공격 칸의 회색
 * {@code X}는 팀 결정대로 그대로 둔다. 보드와 공격 대기열의 바탕은 블럭이 가장 잘 보이도록 검정으로 둔다.
 */
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
    private static final Color ATTACK_FULL_TEXT = RetroTheme.PINK;

    /** 공격을 보냈을 때 보드 위에 떠오르는 문구의 색 */
    static final Color ATTACK_POPUP_COLOR = RetroTheme.YELLOW;
    /** 공격 대기열에 새로 붙은 줄이 깜빡일 때의 칸 색 */
    static final Color QUEUE_HIGHLIGHT_COLOR = RetroTheme.PINK;
    /** 문구가 떠오르는 높이(px, 배율 1.0 기준) */
    private static final int BASE_POPUP_RISE = 60;
    /** 보드와 공격 대기열의 격자선. 검정 바탕에서 옅게 보이는 남보라 */
    static final Color GRID = new Color(0x18, 0x14, 0x34);
    /** 보드와 공격 대기열의 바탕. 블럭 색이 가장 잘 보이도록 검정으로 둔다 */
    static final Color BOARD_BACKGROUND = Color.BLACK;

    /** 배율 1.0일 때 보드·옆 패널 틀이 내용 바깥으로 나가는 거리(px) */
    private static final int BASE_FRAME_PAD = 6;
    /** 배율 1.0일 때 옆 패널 틀의 위치와 크기(px). 위쪽은 보드 틀과 맞춘다 */
    private static final int BASE_NEXT_HEIGHT = 130;
    private static final int BASE_STAT_TOP = 140;
    private static final int BASE_STAT_HEIGHT = 58;
    private static final int BASE_STAT_GAP = 10;
    private static final int BASE_QUEUE_LABEL = 30;
    /** 블럭 칸의 밝은 윗면과 어두운 아랫면 */
    private static final Color CELL_LIGHT = new Color(255, 255, 255, 90);
    private static final Color CELL_SHADE = new Color(0, 0, 0, 70);
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
        setBackground(RetroTheme.BACKGROUND_TOP);
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

    private int framePad() {
        return Math.max(2, scaled(BASE_FRAME_PAD));
    }

    /** 옆 패널 틀의 왼쪽 x 좌표. 틀은 옆 패널 내용보다 조금 바깥에서 시작한다 */
    private int sideFrameX() {
        return sideX - framePad();
    }

    private int sideFrameWidth() {
        return sideW + 2 * framePad();
    }

    /** 옆 패널 틀의 위쪽 y 좌표. 보드 틀의 위쪽과 맞춘다 */
    private int sideFrameTop() {
        return margin - framePad();
    }

    /** 다음 블럭의 첫 칸 왼쪽 x 좌표(px). 4칸 폭을 NEXT 틀 가운데에 놓는다 */
    int nextLeft() {
        return sideX + (sideW - BlockType.SIZE * scaled(BASE_MINI_CELL)) / 2;
    }

    /** 다음 블럭의 첫 칸 위쪽 y 좌표(px) */
    int nextTop() {
        return sideFrameTop() + scaled(34);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        // 대전 화면처럼 바깥 화면이 배경을 이어서 그리는 경우에는 불투명하지 않게 두어 배경을 그리지 않는다
        if (isOpaque()) {
            RetroTheme.paintBackground(g2, getWidth(), getHeight());
        }

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
        int pad = framePad();
        RetroTheme.drawFrame(g2, margin - pad, margin - pad, boardW + 2 * pad, boardH + 2 * pad, RetroTheme.CYAN);
        g2.setColor(BOARD_BACKGROUND);
        g2.fillRect(margin, margin, boardW, boardH);
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

        g2.setFont(font(Font.MONOSPACED, Font.BOLD, 28));
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

    /** 옆 패널. NEXT는 분홍 틀, 점수·레벨·줄 수와 공격 대기열은 하늘색 틀에 담는다. */
    private void drawSidePanel(Graphics2D g2) {
        int top = sideFrameTop();
        RetroTheme.drawFrame(g2, sideFrameX(), top, sideFrameWidth(), scaled(BASE_NEXT_HEIGHT), RetroTheme.PINK);
        g2.setColor(RetroTheme.PINK);
        g2.setFont(font(Font.MONOSPACED, Font.BOLD, 14));
        g2.drawString("NEXT", sideX + scaled(6), top + scaled(22));
        Block next = state.getNextBlock();
        if (next != null) {
            int mini = scaled(BASE_MINI_CELL);
            for (int r = 0; r < BlockType.SIZE; r++) {
                for (int c = 0; c < BlockType.SIZE; c++) {
                    if (next.isFilled(r, c)) {
                        drawCell(g2, nextLeft() + c * mini, nextTop() + r * mini, next.codeAt(r, c), mini);
                    }
                }
            }
        }

        int y = top + scaled(BASE_STAT_TOP);
        y = drawStat(g2, "SCORE", String.valueOf(state.getScore()), y, RetroTheme.YELLOW);
        y = drawStat(g2, "LEVEL", String.valueOf(state.getLevel()), y, RetroTheme.TEXT);
        drawStat(g2, "LINES", String.valueOf(state.getLinesCleared()), y, RetroTheme.TEXT);

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
        int pad = framePad();
        int queueHeight = ATTACK_QUEUE_ROWS * queueCell;
        int frameTop = top - scaled(BASE_QUEUE_LABEL);
        RetroTheme.drawFrame(g2, sideFrameX(), frameTop, sideFrameWidth(), top + queueHeight + pad - frameTop,
                RetroTheme.CYAN);
        g2.setColor(BOARD_BACKGROUND);
        g2.fillRect(sideX, top, Board.WIDTH * queueCell, queueHeight);

        g2.setFont(font(Font.MONOSPACED, Font.BOLD, 13));
        g2.setColor(RetroTheme.MUTED);
        g2.drawString("ATTACK", sideX + scaled(6), top - scaled(8));
        String count = pending.size() + " / " + ATTACK_QUEUE_ROWS;
        g2.setColor(pending.size() >= ATTACK_QUEUE_ROWS ? ATTACK_FULL_TEXT : RetroTheme.TEXT);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(count, sideX + Board.WIDTH * queueCell - scaled(6) - fm.stringWidth(count), top - scaled(8));

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

    /** 이름과 값을 하늘색 틀 하나에 그리고, 다음 틀의 위쪽 y 좌표를 돌려준다. */
    private int drawStat(Graphics2D g2, String label, String value, int top, Color valueColor) {
        int height = scaled(BASE_STAT_HEIGHT);
        RetroTheme.drawFrame(g2, sideFrameX(), top, sideFrameWidth(), height, RetroTheme.CYAN);
        g2.setFont(font(Font.MONOSPACED, Font.BOLD, 13));
        g2.setColor(RetroTheme.MUTED);
        g2.drawString(label, sideX + scaled(6), top + scaled(20));
        g2.setFont(font(Font.MONOSPACED, Font.BOLD, 22));
        g2.setColor(valueColor);
        g2.drawString(value, sideX + scaled(6), top + scaled(46));
        return top + height + scaled(BASE_STAT_GAP);
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
     *
     * <p>레트로 느낌을 위해 칸의 맨 윗줄을 밝게, 아래쪽 두 줄을 어둡게 덧칠한다. 칸 안쪽(왼쪽 위 모서리 안쪽 포함)의
     * 색은 블럭 색 그대로 남긴다.
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
        int light = Math.max(1, size / 20);
        int shade = Math.max(1, size / 14);
        g2.setColor(CELL_LIGHT);
        g2.fillRect(x + 1, y + 1, size - 2, light);
        g2.setColor(CELL_SHADE);
        g2.fillRect(x + 1, y + size - 1 - shade, size - 2, shade);
        g2.setColor(BlockPalette.textColorOf(code, colorBlind));
        g2.setFont(new Font(Font.MONOSPACED, Font.BOLD, size * 5 / 8));
        FontMetrics fm = g2.getFontMetrics();
        String s = String.valueOf(CellCode.symbolOf(code));
        g2.drawString(s, x + (size - fm.stringWidth(s)) / 2, y + (size + fm.getAscent() - fm.getDescent()) / 2);
    }

    /** 보드를 어둡게 덮고 가운데에 분홍 글자와 하늘색 그림자로 문구를 그린다. (일시정지, GAME OVER, 승패, 카운트다운) */
    private void drawOverlay(Graphics2D g2, String text) {
        g2.setColor(RetroTheme.withAlpha(RetroTheme.BACKGROUND_TOP, 190));
        g2.fillRect(margin, margin, boardW, boardH);
        g2.setFont(font(Font.MONOSPACED, Font.BOLD, 30));
        RetroTheme.drawShadowText(g2, text, margin, boardW, margin + boardH / 2, Math.max(1, scaled(3)));
    }
}
