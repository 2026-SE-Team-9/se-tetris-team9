package seoultech.se.tetris.ui.end;

import java.io.UncheckedIOException;
import java.util.List;
import java.util.Objects;

import seoultech.se.tetris.engine.Difficulty;
import seoultech.se.tetris.engine.GameMode;
import seoultech.se.tetris.score.ScoreRecord;
import seoultech.se.tetris.score.ScoreRepository;
import seoultech.se.tetris.ui.ScreenNavigator;
import seoultech.se.tetris.ui.menu.MenuItem;
import seoultech.se.tetris.ui.menu.StartMenuModel;
import seoultech.se.tetris.ui.scoreboard.ScoreboardModel;

/**
 * 게임 종료 화면의 최종 점수, 이름 입력, 스코어보드, 선택 상태.
 *
 * <p>Swing에 의존하지 않는다. 순위 진입 여부 판단과 기록 등록은 {@link ScoreRepository}에 맡기고,
 * 이 클래스는 어느 단계를 보여줄지와 강조할 순위만 관리한다. 모델은 게임이 끝날 때마다 새로 만들므로
 * 강조 상태가 다음 판으로 이어지지 않는다.
 *
 * <p>순위에 들 수 있으면 {@link Phase#NAME_ENTRY}에서 시작하고, 이름을 확정하면 기록을 등록한 뒤
 * {@link Phase#SCOREBOARD}로 넘어간다. 들 수 없으면 바로 스코어보드를 보여준다.
 * 위·아래 이동과 Enter 선택은 시작 메뉴와 같은 리스트 메뉴를 재사용하며 스코어보드 단계에서만 동작한다.
 */
public final class EndScreenModel {

    /** 화면에 보여줄 최대 기록 수. 이를 넘는 기록은 보여주지 않는다. */
    public static final int DISPLAY_COUNT = ScoreboardModel.DISPLAY_COUNT;

    /** 입력할 수 있는 이름의 최대 길이 */
    public static final int MAX_NAME_LENGTH = 12;

    /** 종료 화면의 단계 */
    public enum Phase { NAME_ENTRY, SCOREBOARD }

    private final int finalScore;
    private final GameMode mode;
    private final Difficulty difficulty;
    private final ScoreRepository repository;
    private final StartMenuModel menu;

    private Phase phase;
    private final StringBuilder name = new StringBuilder();
    private List<ScoreRecord> records = List.of();
    private int highlightedRank = -1;
    private boolean storageFailed;

    /**
     * @param finalScore 이번 판의 최종 점수. 0 이상
     * @param items 스코어보드 확인 후 선택할 항목. 비어 있으면 안 된다
     */
    public EndScreenModel(int finalScore, GameMode mode, Difficulty difficulty,
            ScoreRepository repository, List<MenuItem> items) {
        if (finalScore < 0) {
            throw new IllegalArgumentException("점수는 음수일 수 없습니다: " + finalScore);
        }
        this.finalScore = finalScore;
        this.mode = Objects.requireNonNull(mode, "mode");
        this.difficulty = Objects.requireNonNull(difficulty, "difficulty");
        this.repository = Objects.requireNonNull(repository, "repository");
        this.menu = new StartMenuModel(items);

        this.phase = isQualified() ? Phase.NAME_ENTRY : Phase.SCOREBOARD;
        if (phase == Phase.SCOREBOARD) {
            refreshRecords();
        }
    }

    /** 메인 메뉴로 돌아가기와 프로그램 종료만 있는 종료 화면을 만든다. */
    public static EndScreenModel createDefault(int finalScore, GameMode mode, Difficulty difficulty,
            ScoreRepository repository, ScreenNavigator navigator) {
        return new EndScreenModel(finalScore, mode, difficulty, repository, List.of(
                new MenuItem("메인 메뉴", navigator::navigateToMenu),
                new MenuItem("종료", navigator::exitProgram)));
    }

    public int getFinalScore() {
        return finalScore;
    }

    public Phase getPhase() {
        return phase;
    }

    public String getName() {
        return name.toString();
    }

    /** 화면에 보여줄 기록. 점수 내림차순이며 최대 {@link #DISPLAY_COUNT}개 */
    public List<ScoreRecord> getRecords() {
        return records;
    }

    /** 방금 등록한 기록의 순위(1부터). 등록하지 않았으면 -1 */
    public int getHighlightedRank() {
        return highlightedRank;
    }

    /** 저장 파일을 읽거나 쓰지 못했는지 여부. 이때 스코어보드는 비어 있을 수 있다. */
    public boolean isStorageFailed() {
        return storageFailed;
    }

    public List<MenuItem> getItems() {
        return menu.getItems();
    }

    public int getSelectedIndex() {
        return menu.getSelectedIndex();
    }

    public boolean isKeyHintVisible() {
        return menu.isKeyHintVisible();
    }

    // ------------------------------------------------------------------
    // 이름 입력
    // ------------------------------------------------------------------

    /** 이름 끝에 글자를 붙인다. 제어 문자이거나 최대 길이를 넘으면 무시한다. */
    public void typeCharacter(char c) {
        if (phase != Phase.NAME_ENTRY || Character.isISOControl(c) || name.length() >= MAX_NAME_LENGTH) {
            return;
        }
        name.append(c);
    }

    /** 이름의 마지막 글자를 지운다. */
    public void deleteCharacter() {
        if (phase == Phase.NAME_ENTRY && name.length() > 0) {
            name.setLength(name.length() - 1);
        }
    }

    /**
     * 입력한 이름으로 기록을 등록하고 스코어보드를 보여준다.
     *
     * <p>이름이 비어 있으면 등록하지 않고 입력 단계에 머문다. 저장에 실패하면 기록 없이 스코어보드로 넘어간다.
     *
     * @return 입력 단계가 끝났으면 true
     */
    public boolean confirmName() {
        String trimmed = name.toString().trim();
        if (phase != Phase.NAME_ENTRY || trimmed.isEmpty()) {
            return false;
        }
        try {
            highlightedRank = repository.addScore(trimmed, finalScore, mode, difficulty);
        } catch (UncheckedIOException e) {
            storageFailed = true;
        }
        phase = Phase.SCOREBOARD;
        refreshRecords();
        return true;
    }

    // ------------------------------------------------------------------
    // 스코어보드 메뉴
    // ------------------------------------------------------------------

    /** 위 항목으로 이동한다. 첫 항목에서는 마지막 항목으로 순환한다. */
    public void moveUp() {
        if (phase == Phase.SCOREBOARD) {
            menu.moveUp();
        }
    }

    /** 아래 항목으로 이동한다. 마지막 항목에서는 첫 항목으로 순환한다. */
    public void moveDown() {
        if (phase == Phase.SCOREBOARD) {
            menu.moveDown();
        }
    }

    /** 현재 선택된 항목의 동작을 수행한다. */
    public void select() {
        if (phase == Phase.SCOREBOARD) {
            menu.select();
        }
    }

    /** 이 화면에서 쓰지 않는 키가 들어왔을 때 사용 가능한 키 안내를 켠다. */
    public void rejectInput() {
        if (phase == Phase.SCOREBOARD) {
            menu.rejectInput();
        }
    }

    private boolean isQualified() {
        try {
            return repository.isQualified(finalScore, mode);
        } catch (UncheckedIOException e) {
            // 순위표를 읽지 못하면 등록도 할 수 없으므로 이름 입력을 생략한다
            storageFailed = true;
            return false;
        }
    }

    private void refreshRecords() {
        try {
            List<ScoreRecord> all = repository.getTopScores(mode);
            records = List.copyOf(all.subList(0, Math.min(DISPLAY_COUNT, all.size())));
        } catch (UncheckedIOException e) {
            storageFailed = true;
            records = List.of();
        }
    }
}
