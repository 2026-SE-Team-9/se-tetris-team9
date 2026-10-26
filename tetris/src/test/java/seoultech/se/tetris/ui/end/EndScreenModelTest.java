package seoultech.se.tetris.ui.end;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seoultech.se.tetris.engine.Difficulty;
import seoultech.se.tetris.engine.GameMode;
import seoultech.se.tetris.score.CsvScoreRepository;
import seoultech.se.tetris.score.ScoreRecord;
import seoultech.se.tetris.score.ScoreRepository;
import seoultech.se.tetris.ui.ScreenNavigator;
import seoultech.se.tetris.ui.end.EndScreenModel.Phase;

class EndScreenModelTest {

    @TempDir
    Path dir;

    private ScoreRepository repository() {
        return new CsvScoreRepository(dir.resolve("scores.csv"));
    }

    private ScoreRepository filled(int count) {
        ScoreRepository repository = repository();
        for (int i = 1; i <= count; i++) {
            repository.addScore("p" + i, i * 100, GameMode.NORMAL, Difficulty.NORMAL);
        }
        return repository;
    }

    private EndScreenModel model(int score, ScoreRepository repository, List<String> calls) {
        return EndScreenModel.createDefault(score, GameMode.NORMAL, Difficulty.NORMAL, repository, navigator(calls));
    }

    private EndScreenModel model(int score, ScoreRepository repository) {
        return model(score, repository, new ArrayList<>());
    }

    @Test
    void 기록이_없으면_이름_입력으로_시작하고_빈_스코어보드를_보여준다() {
        EndScreenModel model = model(500, repository());

        assertEquals(Phase.NAME_ENTRY, model.getPhase());
        model.typeCharacter('a');
        assertTrue(model.confirmName());

        assertEquals(Phase.SCOREBOARD, model.getPhase());
        assertEquals(1, model.getRecords().size());
        assertEquals(1, model.getHighlightedRank());
    }

    @Test
    void 기록이_0개일_때_이름을_등록하지_않으면_안내용_빈_목록이다() {
        ScoreRepository repository = repository();
        EndScreenModel model = model(500, repository);

        assertEquals(Phase.NAME_ENTRY, model.getPhase());
        assertTrue(model.getRecords().isEmpty());
    }

    @Test
    void 기록이_10개가_차고_점수가_낮으면_이름_입력을_생략한다() {
        EndScreenModel model = model(50, filled(10));

        assertEquals(Phase.SCOREBOARD, model.getPhase());
        assertEquals(10, model.getRecords().size());
        assertEquals(-1, model.getHighlightedRank());
        assertEquals(1000, model.getRecords().get(0).score());
    }

    @Test
    void 기록이_10개여도_점수가_높으면_이름을_입력받고_밀려난_기록은_사라진다() {
        EndScreenModel model = model(550, filled(10));

        assertEquals(Phase.NAME_ENTRY, model.getPhase());
        for (char c : "kim".toCharArray()) {
            model.typeCharacter(c);
        }
        model.confirmName();

        assertEquals(10, model.getRecords().size());
        assertEquals(6, model.getHighlightedRank());
        assertEquals("kim", model.getRecords().get(5).playerName());
        assertFalse(model.getRecords().stream().anyMatch(r -> r.playerName().equals("p1")));
    }

    @Test
    void 이름과_점수가_같은_기존_기록이_있어도_반환된_순위만_강조한다() {
        ScoreRepository repository = repository();
        repository.addScore("kim", 300, GameMode.NORMAL, Difficulty.NORMAL);
        EndScreenModel model = model(300, repository);
        for (char c : "kim".toCharArray()) {
            model.typeCharacter(c);
        }
        model.confirmName();

        assertEquals(2, model.getRecords().size());
        // 먼저 등록된 기록이 1위이므로 새 기록은 2위
        assertEquals(2, model.getHighlightedRank());
    }

    @Test
    void 새로_만든_모델에는_이전_강조가_남지_않는다() {
        ScoreRepository repository = repository();
        EndScreenModel first = model(300, repository);
        first.typeCharacter('a');
        first.confirmName();
        assertEquals(1, first.getHighlightedRank());

        EndScreenModel second = model(100, filled(10));

        assertEquals(-1, second.getHighlightedRank());
    }

    @Test
    void 비어_있는_이름은_등록하지_않고_입력_단계에_머문다() {
        EndScreenModel model = model(500, repository());

        model.typeCharacter(' ');

        assertFalse(model.confirmName());
        assertEquals(Phase.NAME_ENTRY, model.getPhase());
        assertTrue(model.getRecords().isEmpty());
    }

    @Test
    void 이름은_최대_길이까지만_입력되고_제어_문자는_무시한다() {
        EndScreenModel model = model(500, repository());

        for (int i = 0; i < EndScreenModel.MAX_NAME_LENGTH + 5; i++) {
            model.typeCharacter('a');
        }
        model.typeCharacter('\n');

        assertEquals(EndScreenModel.MAX_NAME_LENGTH, model.getName().length());
    }

    @Test
    void 지우기는_마지막_글자를_지우고_빈_이름에서는_아무_일도_없다() {
        EndScreenModel model = model(500, repository());

        model.deleteCharacter();
        model.typeCharacter('a');
        model.typeCharacter('b');
        model.deleteCharacter();

        assertEquals("a", model.getName());
    }

    @Test
    void 이름_입력_중에는_메뉴가_동작하지_않는다() {
        List<String> calls = new ArrayList<>();
        EndScreenModel model = model(500, repository(), calls);

        model.moveDown();
        model.select();
        model.rejectInput();

        assertEquals(0, model.getSelectedIndex());
        assertTrue(calls.isEmpty());
        assertFalse(model.isKeyHintVisible());
    }

    @Test
    void 스코어보드_단계에서는_글자_입력이_무시된다() {
        EndScreenModel model = model(50, filled(10));

        model.typeCharacter('a');
        model.deleteCharacter();

        assertEquals("", model.getName());
        assertFalse(model.confirmName());
    }

    @Test
    void 메인_메뉴가_기본_선택이고_선택하면_시작_메뉴로_돌아간다() {
        List<String> calls = new ArrayList<>();
        EndScreenModel model = model(50, filled(10), calls);

        assertEquals(List.of("메인 메뉴", "종료"),
                model.getItems().stream().map(item -> item.label()).toList());
        assertEquals(0, model.getSelectedIndex());
        model.select();

        assertEquals(List.of("menu"), calls);
    }

    @Test
    void 종료를_선택하면_프로그램을_종료한다() {
        List<String> calls = new ArrayList<>();
        EndScreenModel model = model(50, filled(10), calls);

        model.moveDown();
        model.select();

        assertEquals(List.of("exit"), calls);
    }

    @Test
    void 마지막_항목_아래에서는_첫_항목으로_순환한다() {
        EndScreenModel model = model(50, filled(10));

        model.moveDown();
        model.moveDown();
        assertEquals(0, model.getSelectedIndex());
        model.moveUp();
        assertEquals(1, model.getSelectedIndex());
    }

    @Test
    void 허용되지_않은_키는_안내를_표시하고_이동하면_사라진다() {
        EndScreenModel model = model(50, filled(10));

        assertFalse(model.isKeyHintVisible());
        model.rejectInput();
        assertTrue(model.isKeyHintVisible());
        model.moveUp();
        assertFalse(model.isKeyHintVisible());
    }

    @Test
    void 음수_점수는_종료_화면을_만들_수_없다() {
        assertThrows(IllegalArgumentException.class, () -> model(-1, repository()));
    }

    @Test
    void 저장소를_읽지_못하면_이름_입력을_생략하고_오류를_표시한다() {
        EndScreenModel model = model(500, failingRepository());

        assertEquals(Phase.SCOREBOARD, model.getPhase());
        assertTrue(model.isStorageFailed());
        assertTrue(model.getRecords().isEmpty());
    }

    @Test
    void 등록_중_저장에_실패하면_기록_없이_스코어보드로_넘어간다() {
        ScoreRepository failingOnAdd = new FakeRepository(true, false);
        EndScreenModel model = model(500, failingOnAdd);

        model.typeCharacter('a');
        model.confirmName();

        assertEquals(Phase.SCOREBOARD, model.getPhase());
        assertTrue(model.isStorageFailed());
        assertEquals(-1, model.getHighlightedRank());
    }

    @Test
    void 표시하는_기록은_최대_10개다() {
        ScoreRepository repository = new CsvScoreRepository(dir.resolve("big.csv"), 15);
        for (int i = 1; i <= 15; i++) {
            repository.addScore("p" + i, i * 100, GameMode.NORMAL, Difficulty.NORMAL);
        }

        EndScreenModel model = model(1, repository);

        assertEquals(Phase.SCOREBOARD, model.getPhase());
        assertEquals(EndScreenModel.DISPLAY_COUNT, model.getRecords().size());
        assertEquals(1500, model.getRecords().get(0).score());
    }

    private static ScoreRepository failingRepository() {
        return new FakeRepository(false, true);
    }

    /** 저장소 실패를 흉내 내는 대역. */
    private static final class FakeRepository implements ScoreRepository {
        private final boolean failOnAdd;
        private final boolean failOnRead;

        FakeRepository(boolean failOnAdd, boolean failOnRead) {
            this.failOnAdd = failOnAdd;
            this.failOnRead = failOnRead;
        }

        @Override public List<ScoreRecord> getTopScores(GameMode mode) {
            if (failOnRead) {
                throw new UncheckedIOException(new java.io.IOException("read"));
            }
            return List.of();
        }

        @Override public boolean isQualified(int score, GameMode mode) {
            if (failOnRead) {
                throw new UncheckedIOException(new java.io.IOException("read"));
            }
            return true;
        }

        @Override public int addScore(String name, int score, GameMode mode, Difficulty difficulty) {
            if (failOnAdd) {
                throw new UncheckedIOException(new java.io.IOException("write"));
            }
            return 1;
        }

        @Override public void clearScores() { }

        @Override public void load() { }
    }

    private static ScreenNavigator navigator(List<String> calls) {
        return new ScreenNavigator() {
            @Override public void navigateToMenu() { calls.add("menu"); }
            @Override public void navigateToGame(GameMode mode) { calls.add("game"); }
            @Override public void navigateToSettings() { calls.add("settings"); }
            @Override public void navigateToScoreboard() { calls.add("scoreboard"); }
            @Override public void navigateToGameOver(int finalScore) { calls.add("gameOver"); }
            @Override public void exitProgram() { calls.add("exit"); }
        };
    }
}
