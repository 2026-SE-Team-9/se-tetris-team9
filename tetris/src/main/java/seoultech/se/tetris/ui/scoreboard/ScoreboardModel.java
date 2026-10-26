package seoultech.se.tetris.ui.scoreboard;

import java.io.UncheckedIOException;
import java.util.List;
import java.util.Objects;

import seoultech.se.tetris.engine.GameMode;
import seoultech.se.tetris.score.ScoreRecord;
import seoultech.se.tetris.score.ScoreRepository;

/**
 * 시작 메뉴에서 여는 스코어보드 화면의 모드 선택과 표시할 기록. Swing에 의존하지 않는다.
 *
 * <p>모드별 기록을 따로 보여주며, 모드를 바꿀 때마다 저장소에서 기록을 다시 읽는다.
 * 순위 계산과 파일 입출력은 {@link ScoreRepository}에 맡기고 이 클래스는 무엇을 보여줄지만 정한다.
 */
public final class ScoreboardModel {

    /** 한 화면에 보여줄 최대 기록 수. 이를 넘는 기록은 보여주지 않는다. */
    public static final int DISPLAY_COUNT = 10;

    private static final GameMode[] MODES = GameMode.values();

    private final ScoreRepository repository;
    private final Runnable onBack;

    private int modeIndex;
    private List<ScoreRecord> records = List.of();
    private boolean storageFailed;

    /**
     * @param onBack 스코어보드를 닫고 돌아갈 때 수행할 동작
     */
    public ScoreboardModel(ScoreRepository repository, Runnable onBack) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.onBack = Objects.requireNonNull(onBack, "onBack");
        refresh();
    }

    public GameMode getMode() {
        return MODES[modeIndex];
    }

    /** 현재 모드의 기록. 점수 내림차순이며 최대 {@link #DISPLAY_COUNT}개 */
    public List<ScoreRecord> getRecords() {
        return records;
    }

    /** 저장 파일을 읽지 못했는지 여부. 이때 기록은 비어 있다. */
    public boolean isStorageFailed() {
        return storageFailed;
    }

    /** 다음 모드로 바꾼다. 마지막 모드에서는 첫 모드로 순환한다. */
    public void nextMode() {
        modeIndex = (modeIndex + 1) % MODES.length;
        refresh();
    }

    /** 이전 모드로 바꾼다. 첫 모드에서는 마지막 모드로 순환한다. */
    public void previousMode() {
        modeIndex = (modeIndex - 1 + MODES.length) % MODES.length;
        refresh();
    }

    /** 스코어보드를 닫고 돌아간다. */
    public void back() {
        onBack.run();
    }

    private void refresh() {
        storageFailed = false;
        try {
            List<ScoreRecord> all = repository.getTopScores(getMode());
            records = List.copyOf(all.subList(0, Math.min(DISPLAY_COUNT, all.size())));
        } catch (UncheckedIOException e) {
            storageFailed = true;
            records = List.of();
        }
    }
}
