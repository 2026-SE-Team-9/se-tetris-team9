package seoultech.se.tetris.ui.scoreboard;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.util.List;

import seoultech.se.tetris.score.ScoreRecord;
import seoultech.se.tetris.ui.DifficultyLabels;
import seoultech.se.tetris.ui.theme.RetroTheme;

/**
 * 순위·이름·난이도·점수 표를 그린다. 종료 화면과 시작 메뉴의 스코어보드가 같은 표를 쓴다.
 *
 * <p>레트로 디자인({@link RetroTheme})의 고정폭 글꼴로 그린다. 머리글은 보조 글자색, 기록은 기본 글자색이며,
 * 방금 등록한 순위는 노란 글자와 옅은 노란 줄로 강조한다.
 */
public final class ScoreboardTablePainter {

    /**
     * 열의 위치(px). 이름은 최대 12자, 점수는 여덟 자리까지 겹치지 않는 폭으로 나눈다.
     * 순위·이름·난이도는 왼쪽 끝, 점수는 오른쪽 끝을 기준으로 맞춘다.
     */
    static final int RANK_X = 40;
    static final int NAME_X = 90;
    static final int DIFFICULTY_X = 300;
    static final int SCORE_X = 440;

    /** 표 한 행의 높이 */
    public static final int ROW_HEIGHT = 24;

    /** 머리글 색 */
    static final Color HEADER_COLOR = RetroTheme.MUTED;
    /** 기록 색 */
    static final Color ROW_COLOR = RetroTheme.TEXT;
    /** 방금 등록한 순위의 강조 색 */
    static final Color HIGHLIGHT_COLOR = RetroTheme.YELLOW;

    private ScoreboardTablePainter() {
    }

    /**
     * 머리글과 기록을 그린다.
     *
     * @param headerY 머리글의 기준선 y좌표. 첫 기록은 한 행 아래에 그린다
     * @param highlightedRank 강조할 순위(1부터). 강조하지 않으면 0 이하
     * @param emptyMessage 기록이 없을 때 가운데에 보여줄 문구
     * @param emptyColor 안내 문구의 색
     */
    public static void paint(Graphics2D g2, int width, int headerY, List<ScoreRecord> records,
            int highlightedRank, String emptyMessage, Color emptyColor) {
        g2.setFont(RetroTheme.mono(Font.BOLD, 15));
        g2.setColor(HEADER_COLOR);
        g2.drawString("순위", RANK_X, headerY);
        g2.drawString("이름", NAME_X, headerY);
        g2.drawString("난이도", DIFFICULTY_X, headerY);
        drawRight(g2, "점수", SCORE_X, headerY);

        if (records.isEmpty()) {
            g2.setColor(emptyColor);
            FontMetrics metrics = g2.getFontMetrics();
            g2.drawString(emptyMessage, (width - metrics.stringWidth(emptyMessage)) / 2,
                    headerY + 2 * ROW_HEIGHT);
            return;
        }
        int y = headerY;
        for (int i = 0; i < records.size(); i++) {
            ScoreRecord record = records.get(i);
            y += ROW_HEIGHT;
            // 강조는 등록 때 반환된 순위로만 정한다. 이름과 점수가 같은 다른 기록은 강조하지 않는다.
            boolean highlighted = i + 1 == highlightedRank;
            if (highlighted) {
                g2.setColor(RetroTheme.withAlpha(HIGHLIGHT_COLOR, 28));
                g2.fillRect(RANK_X - 12, y - ROW_HEIGHT + 6, SCORE_X - RANK_X + 24, ROW_HEIGHT);
            }
            g2.setColor(highlighted ? HIGHLIGHT_COLOR : ROW_COLOR);
            g2.drawString(String.valueOf(i + 1), RANK_X, y);
            g2.drawString(record.playerName(), NAME_X, y);
            g2.drawString(DifficultyLabels.of(record.difficulty()), DIFFICULTY_X, y);
            drawRight(g2, String.valueOf(record.score()), SCORE_X, y);
        }
    }

    private static void drawRight(Graphics2D g2, String text, int rightX, int y) {
        g2.drawString(text, rightX - g2.getFontMetrics().stringWidth(text), y);
    }
}
