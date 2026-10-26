package seoultech.se.tetris.ui.scoreboard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seoultech.se.tetris.engine.Difficulty;
import seoultech.se.tetris.engine.GameMode;
import seoultech.se.tetris.score.CsvScoreRepository;
import seoultech.se.tetris.score.ScoreRecord;
import seoultech.se.tetris.score.ScoreRepository;

class ScoreboardModelTest {

    @TempDir
    Path dir;

    private ScoreRepository repository() {
        return new CsvScoreRepository(dir.resolve("scores.csv"), 15);
    }

    private static void fill(ScoreRepository repository, GameMode mode, int count) {
        for (int i = 1; i <= count; i++) {
            repository.addScore(mode.name() + i, i * 100, mode, Difficulty.NORMAL);
        }
    }

    private ScoreboardModel model(ScoreRepository repository) {
        return new ScoreboardModel(repository, () -> { });
    }

    @Test
    void 기록이_없으면_빈_목록으로_일반_모드에서_시작한다() {
        ScoreboardModel model = model(repository());

        assertEquals(GameMode.NORMAL, model.getMode());
        assertTrue(model.getRecords().isEmpty());
        assertFalse(model.isStorageFailed());
    }

    @Test
    void 기록이_하나면_하나만_보여준다() {
        ScoreRepository repository = repository();
        fill(repository, GameMode.NORMAL, 1);

        assertEquals(1, model(repository).getRecords().size());
    }

    @Test
    void 기록은_점수_내림차순으로_보여준다() {
        ScoreRepository repository = repository();
        fill(repository, GameMode.NORMAL, 10);

        List<ScoreRecord> records = model(repository).getRecords();

        assertEquals(10, records.size());
        assertEquals(1000, records.get(0).score());
        assertEquals(100, records.get(9).score());
    }

    @Test
    void 열_개를_넘는_기록은_열_개까지만_보여준다() {
        ScoreRepository repository = repository();
        fill(repository, GameMode.NORMAL, 15);

        List<ScoreRecord> records = model(repository).getRecords();

        assertEquals(ScoreboardModel.DISPLAY_COUNT, records.size());
        assertEquals(1500, records.get(0).score());
    }

    @Test
    void 모드를_바꾸면_그_모드의_기록만_보여준다() {
        ScoreRepository repository = repository();
        fill(repository, GameMode.NORMAL, 2);
        fill(repository, GameMode.ITEM, 3);
        ScoreboardModel model = model(repository);

        assertEquals(2, model.getRecords().size());
        model.nextMode();

        assertEquals(GameMode.ITEM, model.getMode());
        assertEquals(3, model.getRecords().size());
        assertTrue(model.getRecords().stream().allMatch(r -> r.mode() == GameMode.ITEM));
    }

    @Test
    void 모드는_앞뒤로_순환한다() {
        ScoreboardModel model = model(repository());
        int count = GameMode.values().length;

        model.previousMode();
        assertEquals(GameMode.values()[count - 1], model.getMode());
        model.nextMode();
        assertEquals(GameMode.NORMAL, model.getMode());
        for (int i = 0; i < count; i++) {
            model.nextMode();
        }
        assertEquals(GameMode.NORMAL, model.getMode());
    }

    @Test
    void 모드를_바꿔도_저장된_기록이_그대로이고_돌아오면_다시_보인다() {
        ScoreRepository repository = repository();
        fill(repository, GameMode.NORMAL, 2);
        ScoreboardModel model = model(repository);

        model.nextMode();
        assertTrue(model.getRecords().isEmpty());
        model.previousMode();

        assertEquals(2, model.getRecords().size());
    }

    @Test
    void 돌아가기를_선택하면_콜백을_호출한다() {
        AtomicInteger calls = new AtomicInteger();
        ScoreboardModel model = new ScoreboardModel(repository(), calls::incrementAndGet);

        model.back();

        assertEquals(1, calls.get());
    }

    @Test
    void 저장소를_읽지_못하면_빈_목록과_오류_상태가_된다() {
        ScoreboardModel model = model(new FailingRepository());

        assertTrue(model.isStorageFailed());
        assertTrue(model.getRecords().isEmpty());
    }

    @Test
    void 읽기에_실패했다가_다음_모드에서_성공하면_오류_상태가_풀린다() {
        ScoreRepository flaky = new FailingRepository() {
            @Override public List<ScoreRecord> getTopScores(GameMode mode) {
                if (mode == GameMode.NORMAL) {
                    return super.getTopScores(mode);
                }
                return List.of();
            }
        };
        ScoreboardModel model = model(flaky);
        assertTrue(model.isStorageFailed());

        model.nextMode();

        assertFalse(model.isStorageFailed());
    }

    @Test
    void 저장소와_돌아가기_동작은_null일_수_없다() {
        assertThrows(NullPointerException.class, () -> new ScoreboardModel(null, () -> { }));
        assertThrows(NullPointerException.class, () -> new ScoreboardModel(repository(), null));
    }

    /** 읽을 때마다 실패하는 저장소 대역. */
    private static class FailingRepository implements ScoreRepository {
        @Override public List<ScoreRecord> getTopScores(GameMode mode) {
            throw new UncheckedIOException(new IOException("read"));
        }

        @Override public boolean isQualified(int score, GameMode mode) {
            return false;
        }

        @Override public int addScore(String playerName, int score, GameMode mode, Difficulty difficulty) {
            return -1;
        }

        @Override public void clearScores() { }

        @Override public void load() { }
    }
}
