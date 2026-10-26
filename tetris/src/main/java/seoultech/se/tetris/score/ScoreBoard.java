package seoultech.se.tetris.score;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import seoultech.se.tetris.engine.Difficulty;
import seoultech.se.tetris.engine.GameMode;

/**
 * 게임 모드별 점수 기록의 순위를 관리한다.
 *
 * <p>점수가 높은 순으로 정렬해 보관하며, 정해진 개수를 넘으면 낮은 점수부터 밀려난다.
 *
 * <p><b>파일 입출력을 하지 않는다.</b> 저장과 불러오기는 {@link ScoreRepository}의 역할이고,
 * 이 클래스는 순위 계산만 담당한다. 파일을 다루지 않으므로 파일 없이 단위 테스트가 가능하다.
 *
 * <p>2차 요구사항에 따라 일반 모드와 아이템 모드의 기록을 따로 관리한다.
 * 각각의 모드별로 {@link ScoreBoard}를 만들어 {@link ScoreRepository}에서 관리한다.
 */
public class ScoreBoard {

    /** 최소 보관 개수. 요구사항에 따라 "최소 상위 10개" */
    public static final int MIN_CAPACITY = 10;

    /** 기본 보관 개수. MIN_CAPACITY 이상의 값으로 지정한다. */
    public static final int DEFAULT_CAPACITY = MIN_CAPACITY;

    private final int capacity;
    private final List<ScoreRecord> records = new ArrayList<>();
    private final GameMode mode;

    /** 모드는 객체 생성 시 무조건 지정 */
    public ScoreBoard(GameMode mode) {
        this(mode, DEFAULT_CAPACITY);
    }

    /**
     * 보관 개수를 지정해 만든다.
     *
     * <p>화면에는 10개씩 보여주더라도 더 많이 보관하면 페이지를 넘기는 방식으로 볼 수 있다.
     */
    public ScoreBoard(GameMode mode, int capacity) {
        this.mode = Objects.requireNonNull(mode, "모드는 null일 수 없습니다.");
        if (capacity < MIN_CAPACITY) {
            throw new IllegalArgumentException(
                    "보관 개수는 " + MIN_CAPACITY + " 이상이어야 합니다: " + capacity);
        }
        this.capacity = capacity;
    }

    /**
     * 기존 기록으로 초기화한다. 파일에서 불러온 내용을 넣을 때 사용한다.
     *
     * <p>순서가 뒤섞여 있어도 점수 순으로 정렬하며, 보관 개수를 넘는 기록은 버린다.
     *
     * <p>{@link #add}와 같은 기준으로 잘못된 기록({@code null} 기록, {@code null} 이름,
     * 음수 점수, 모드/난이도가 없는 기록)은 건너뛴다.
     * 저장 파일이 일부 손상되어도 나머지 기록으로 계속 진행할 수 있도록 하기 위함이다.
     *
     * <p>저장소가 모든 기록을 하나의 목록으로 넘겨도 자신의 모드의 기록만 선택해서 가진다.
     * 이 경우 잘못된 기록으로 간주하지 않는다.
     *
     * @return 잘못되어 건너뛴 기록 수
     */
    public int load(List<ScoreRecord> loaded) {
        Objects.requireNonNull(loaded, "기록 목록은 null일 수 없습니다.");

        records.clear();
        int skipped = 0;
        for (ScoreRecord record : loaded) {
            if (isOtherMode(record)) {
                continue;
            }

            if (isValid(record)) {
                records.add(record);
            } else {
                skipped++;
            }
        }
        sortAndTrim();
        return skipped;
    }

    /**
     * 새 기록을 등록한다.
     *
     * <p>반환된 순위로 방금 등록한 기록을 찾아 강조 표시할 수 있다.
     * 이름과 점수가 같은 기록이 이미 있어도, 방금 등록한 기록의 순위를 돌려준다.
     *
     * <p>기록의 모드는 보드와 동일하게 지정된다.
     *
     * @return 등록된 기록의 순위(1부터). 순위에 들지 못해 밀려났으면 -1
     */
    public int add(String playerName, int score, Difficulty difficulty) {
        Objects.requireNonNull(playerName, "이름은 null일 수 없습니다.");
        if (score < 0) {
            throw new IllegalArgumentException("점수는 음수일 수 없습니다: " + score);
        }
        Objects.requireNonNull(difficulty, "난이도는 null일 수 없습니다.");

        ScoreRecord added = new ScoreRecord(playerName, score, mode, difficulty);
        records.add(added);
        sortAndTrim();

        // record는 모든 값이 같으면 equals가 같다고 판정하므로, 같은 기록을 찾을 때 값이 아니라 방금 만든 객체로 찾는다
        for (int i = 0; i < records.size(); i++) {
            if (records.get(i) == added) {
                return i + 1;
            }
        }
        return -1;
    }

    /**
     * 이 점수가 순위에 들 수 있는지 판단한다.
     *
     * <p>게임이 끝났을 때 이름 입력 화면을 띄울지 결정하는 데 사용한다.
     */
    public boolean isQualified(int score) {
        if (records.size() < capacity) {
            return true;
        }
        return score > records.get(records.size() - 1).score();
    }

    /** 점수가 높은 순으로 정렬된 전체 기록. 수정해도 원본은 바뀌지 않는다. */
    public List<ScoreRecord> getTopScores() {
        return List.copyOf(records);
    }

    /**
     * 점수가 높은 순으로 최대 {@code count}개.
     *
     * <p>한 화면에 보여줄 개수만큼 잘라 올 때 사용한다.
     */
    public List<ScoreRecord> getTopScores(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("개수는 음수일 수 없습니다: " + count);
        }
        return List.copyOf(records.subList(0, Math.min(count, records.size())));
    }

    /**
     * 지정한 페이지의 기록.
     *
     * <p>페이지 번호는 0부터 시작한다. 범위를 벗어나면 빈 목록을 반환한다.
     */
    public List<ScoreRecord> getPage(int pageIndex, int pageSize) {
        if (pageIndex < 0) {
            throw new IllegalArgumentException("페이지 번호는 음수일 수 없습니다: " + pageIndex);
        }
        if (pageSize <= 0) {
            throw new IllegalArgumentException("페이지 크기는 1 이상이어야 합니다: " + pageSize);
        }

        int from = pageIndex * pageSize;
        if (from >= records.size()) {
            return List.of();
        }
        int to = Math.min(from + pageSize, records.size());
        return List.copyOf(records.subList(from, to));
    }

    /** 해당 페이지 크기로 나눴을 때의 전체 페이지 수. 기록이 없으면 1. */
    public int pageCount(int pageSize) {
        if (pageSize <= 0) {
            throw new IllegalArgumentException("페이지 크기는 1 이상이어야 합니다: " + pageSize);
        }
        if (records.isEmpty()) {
            return 1;
        }
        return (records.size() + pageSize - 1) / pageSize;
    }

    /**
     * 특정 기록의 순위. 1부터 시작하며, 없으면 -1.
     *
     * <p>이름과 점수가 같은 기록이 여러 개 있는 경우 그 중 가장 높은 순위를 출력하는 구조이다.
     * 다른 방식으로 동작해야 하는 경우 수정 요망 */
    public int rankOf(ScoreRecord record) {
        int index = records.indexOf(record);
        return index < 0 ? -1 : index + 1;
    }

    /** 모든 기록을 지운다. 설정 화면의 "스코어보드 초기화"에 해당한다. */
    public void clear() {
        records.clear();
    }

    public int size() {
        return records.size();
    }

    public boolean isEmpty() {
        return records.isEmpty();
    }

    public int capacity() {
        return capacity;
    }

    public GameMode mode() {
        return mode;
    }

    /** record가 이 보드의 모드와 다른지 판단한다. 양쪽의 null값 여부도 동시에 검사한다. */
    private boolean isOtherMode(ScoreRecord record) {
        return record != null && record.mode() != null && record.mode() != mode;
    }

    /** {@link #add}와 같은 기준으로 기록이 유효한지 판단한다. */
    private static boolean isValid(ScoreRecord record) {
        return record != null
            && record.playerName() != null
            && record.score() >= 0
            && record.mode() != null
            && record.difficulty() != null;
    }

    /**
     * 점수 내림차순으로 정렬하고 보관 개수를 넘는 기록을 버린다.
     *
     * <p>안정 정렬 방식으로 정렬하므로 점수가 같으면 먼저 등록된 기록이 우선된다.
     */
    private void sortAndTrim() {
        records.sort((a, b) -> Integer.compare(b.score(), a.score()));

        while (records.size() > capacity) {
            records.remove(records.size() - 1);
        }
    }
}
