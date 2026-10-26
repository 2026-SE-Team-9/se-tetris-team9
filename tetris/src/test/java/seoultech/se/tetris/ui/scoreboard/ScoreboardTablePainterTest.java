package seoultech.se.tetris.ui.scoreboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import seoultech.se.tetris.engine.Difficulty;
import seoultech.se.tetris.engine.GameMode;
import seoultech.se.tetris.score.ScoreRecord;
import seoultech.se.tetris.ui.DifficultyLabels;

class ScoreboardTablePainterTest {

    private static final int WIDTH = 480;
    private static final int HEADER_Y = 40;
    private static final Font FONT = new Font(Font.DIALOG, Font.BOLD, 16);

    private static ScoreRecord record(String name, int score, Difficulty difficulty) {
        return new ScoreRecord(name, score, GameMode.NORMAL, difficulty);
    }

    private static BufferedImage draw(List<ScoreRecord> records, int highlightedRank) {
        BufferedImage image = new BufferedImage(WIDTH, 400, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = image.createGraphics();
        ScoreboardTablePainter.paint(g2, WIDTH, HEADER_Y, records, highlightedRank, "기록 없음", Color.GRAY);
        g2.dispose();
        return image;
    }

    /** 가로 구간 [fromX, toX)와 행 y 범위 안에 지정한 색이 하나라도 있는지 */
    private static boolean hasColor(BufferedImage image, int fromX, int toX, int row, Color color) {
        int top = HEADER_Y + row * ScoreboardTablePainter.ROW_HEIGHT - ScoreboardTablePainter.ROW_HEIGHT + 4;
        int bottom = top + ScoreboardTablePainter.ROW_HEIGHT - 4;
        for (int y = top; y < bottom; y++) {
            for (int x = fromX; x < toX; x++) {
                if ((image.getRGB(x, y) & 0xFFFFFF) == (color.getRGB() & 0xFFFFFF)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static FontMetrics metrics() {
        Graphics2D g2 = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB).createGraphics();
        g2.setFont(FONT);
        return g2.getFontMetrics();
    }

    @Test
    void 가장_긴_이름과_가장_큰_점수에서도_열이_겹치지_않는다() {
        FontMetrics fm = metrics();
        // 이름은 최대 12자, 점수는 여덟 자리, 난이도는 가장 긴 "어려움"으로 본다
        int nameEnd = ScoreboardTablePainter.NAME_X + fm.stringWidth("가".repeat(12));
        int difficultyEnd = ScoreboardTablePainter.DIFFICULTY_X
                + fm.stringWidth(DifficultyLabels.of(Difficulty.HARD));
        int scoreStart = ScoreboardTablePainter.SCORE_X - fm.stringWidth("99999999");

        assertTrue(nameEnd < ScoreboardTablePainter.DIFFICULTY_X, "이름이 난이도 열을 침범합니다");
        assertTrue(difficultyEnd < scoreStart, "난이도가 점수 열을 침범합니다");
        assertTrue(ScoreboardTablePainter.SCORE_X < WIDTH, "점수가 화면 밖으로 나갑니다");
    }

    @Test
    void 열은_순위_이름_난이도_점수_순서로_배치된다() {
        assertTrue(ScoreboardTablePainter.RANK_X < ScoreboardTablePainter.NAME_X);
        assertTrue(ScoreboardTablePainter.NAME_X < ScoreboardTablePainter.DIFFICULTY_X);
        assertTrue(ScoreboardTablePainter.DIFFICULTY_X < ScoreboardTablePainter.SCORE_X);
    }

    @Test
    void 기록마다_난이도_열에_글자를_그린다() {
        List<ScoreRecord> records = List.of(
                record("a", 300, Difficulty.EASY),
                record("b", 200, Difficulty.NORMAL),
                record("c", 100, Difficulty.HARD));

        BufferedImage image = draw(records, 0);

        for (int row = 1; row <= records.size(); row++) {
            assertTrue(hasColor(image, ScoreboardTablePainter.DIFFICULTY_X,
                    ScoreboardTablePainter.DIFFICULTY_X + 60, row, Color.WHITE), "행 " + row);
        }
    }

    @Test
    void 난이도_열_머리글을_그린다() {
        BufferedImage image = draw(List.of(record("a", 1, Difficulty.NORMAL)), 0);

        int top = HEADER_Y - 18;
        boolean found = false;
        for (int y = top; y < HEADER_Y + 4 && !found; y++) {
            for (int x = ScoreboardTablePainter.DIFFICULTY_X; x < ScoreboardTablePainter.DIFFICULTY_X + 60; x++) {
                if ((image.getRGB(x, y) & 0xFFFFFF) == (Color.GRAY.getRGB() & 0xFFFFFF)) {
                    found = true;
                    break;
                }
            }
        }
        assertTrue(found);
    }

    @Test
    void 강조한_행은_난이도도_강조_색으로_그린다() {
        List<ScoreRecord> records = List.of(
                record("a", 300, Difficulty.HARD),
                record("b", 200, Difficulty.HARD));

        BufferedImage image = draw(records, 2);

        int from = ScoreboardTablePainter.DIFFICULTY_X;
        int to = from + 60;
        assertTrue(hasColor(image, from, to, 2, Color.YELLOW));
        assertFalse(hasColor(image, from, to, 1, Color.YELLOW));
        assertTrue(hasColor(image, from, to, 1, Color.WHITE));
    }

    @Test
    void 기록이_없으면_난이도_열_없이_안내_문구만_그린다() {
        BufferedImage image = draw(new ArrayList<>(), 0);

        assertFalse(hasColor(image, ScoreboardTablePainter.DIFFICULTY_X,
                ScoreboardTablePainter.DIFFICULTY_X + 60, 1, Color.WHITE));
        assertEquals(WIDTH, image.getWidth());
    }

    @Test
    void 기록이_열_개여도_모두_그린다() {
        List<ScoreRecord> records = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            records.add(record("p" + i, 1000 - i, Difficulty.values()[i % Difficulty.values().length]));
        }

        BufferedImage image = draw(records, 0);

        for (int row = 1; row <= 10; row++) {
            assertTrue(hasColor(image, ScoreboardTablePainter.DIFFICULTY_X,
                    ScoreboardTablePainter.DIFFICULTY_X + 60, row, Color.WHITE), "행 " + row);
        }
    }
}
