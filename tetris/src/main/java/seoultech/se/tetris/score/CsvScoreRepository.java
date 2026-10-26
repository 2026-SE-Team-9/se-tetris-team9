package seoultech.se.tetris.score;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import seoultech.se.tetris.engine.Difficulty;
import seoultech.se.tetris.engine.GameMode;

/**
 * 스코어보드 기록을 CSV 파일에 저장한다. 기본 경로는 .tetris/scores.csv
 *
 * <p>전체적인 저장 구조는 PropertiesKeyConfigService와 일관되게 동작하도록 설계한다.
 *
 * <p>모든 모드의 기록을 한 파일에 저장하고 {@code mode} 열로 구분한다.
 * 순위 계산은 각 모드의 {@link ScoreBoard}에 맡긴다.
 *
 * <p>파일 형식 — 첫 줄은 헤더이고, 인코딩은 UTF-8이다.
 * <pre>
 * name,score,mode,difficulty
 * 철수,12000,NORMAL,HARD
 * "홍,길동",8500,ITEM,EASY
 * </pre>
 * CSV 표준 방식에 따라 이름에 쉼표·큰따옴표가 있으면 큰따옴표로 감싸고, 이름 안의 큰따옴표는 두 번 쓴다.
 *
 * <p>한 줄에 기록 하나를 쓰므로 이름에 줄바꿈은 허용하지 않는다.
 * 파일이 일부 손상되어도 그 줄만 건너뛰고 나머지 기록을 불러올 수 있게 하기 위함이다.
 */
public class CsvScoreRepository implements ScoreRepository {

    /** 파일 첫 줄에 쓰는 헤더 형식 */
    static final String HEADER = "name,score,mode,difficulty";

    /** 한 기록의 열 개수. */
    private static final int COLUMNS = 4;

    private final Path filePath;

    /** 모드별 기록 보관 갯수 */
    private final int capacity;

    /** 각 게임 모드에 대한 스코어보드 */
    private final Map<GameMode, ScoreBoard> boards = new EnumMap<>(GameMode.class);

    /** 파일을 읽었는지 여부. 읽기 전에 등록하면 기존 기록을 덮어쓰므로 처음 사용할 때 읽는다. */
    private boolean loaded;

    public CsvScoreRepository() {
        this(Path.of(System.getProperty("user.home"), ".tetris", "scores.csv"));
    }

    /** 저장 경로를 지정하고, 보관 개수는 기본값({@link ScoreBoard#DEFAULT_CAPACITY})으로 만든다. */
    public CsvScoreRepository(Path filePath) {
        this(filePath, ScoreBoard.DEFAULT_CAPACITY);
    }

    /** 저장 경로와 모드별 기록 보관 갯수를 지정해 만든다. 테스트에서는 임시 폴더의 경로를 넘긴다.
     *
     * <p>한 화면에서 보여주는 갯수는 별개로 지정한다. 보관 갯수를 늘리면 화면에서 페이지를 나눈다.
     *
     * <p>보관 갯수를 줄이면 저장 시 기존 기록이 일부 버려질 수 있다.
     *
     * <p>파일은 생성자에서 읽지 않고, 처음 조회·등록할 때 읽는다.
     * PropertiesKeyConfigService와 같이 조작 담당이 {@link #load}를 호출해 미리 읽거나 다시 읽을 수도 있다. */
    public CsvScoreRepository(Path filePath, int capacity) {
        this.filePath = Objects.requireNonNull(filePath, "filePath");
        this.capacity = capacity;
        for (GameMode mode : GameMode.values()) {
            boards.put(mode, newBoard(mode));
        }
    }

    // ------------------------------------------------------------------
    // 조회와 등록
    // ------------------------------------------------------------------

    /* 스코어보드에 대한 계산은 ScoreBoard에서 처리한다 */
    @Override
    public List<ScoreRecord> getTopScores(GameMode mode) {
        return board(mode).getTopScores();
    }

    @Override
    public boolean isQualified(int score, GameMode mode) {
        return board(mode).isQualified(score);
    }

    /**
     * {@inheritDoc}
     *
     * <p>순위에 들지 못한 기록은 순위표가 바뀌지 않으므로 저장하지 않는다.
     *
     * <p>새 순위표를 따로 만들어 파일에 먼저 저장하고, 저장에 성공한 뒤에 교체한다.
     * 저장에 실패하면 순위표는 등록 전 그대로 남는다.
     *
     * @throws IllegalArgumentException 이름에 줄바꿈이 있는 경우
     */
    @Override
    public int addScore(String playerName, int score, GameMode mode, Difficulty difficulty) {
        requireSingleLine(playerName);
        ScoreBoard updated = newBoard(mode);
        updated.load(board(mode).getTopScores());
        int rank = updated.add(playerName, score, difficulty);
        if (rank != -1) {
            Map<GameMode, ScoreBoard> next = new EnumMap<>(boards);
            next.put(mode, updated);
            save(next);
            boards.put(mode, updated);
        }
        return rank;
    }

    /**
     * {@inheritDoc}
     *
     * <p>빈 순위표를 파일에 먼저 저장하고, 저장에 성공한 뒤에 교체한다.
     * 저장에 실패하면 기록은 초기화 전 그대로 남는다.
     */
    @Override
    public void clearScores() {
        Map<GameMode, ScoreBoard> cleared = new EnumMap<>(GameMode.class);
        for (GameMode mode : GameMode.values()) {
            cleared.put(mode, newBoard(mode));
        }
        save(cleared);
        boards.putAll(cleared);
        // 모두 지웠으므로 파일을 다시 읽을 필요가 없다
        loaded = true;
    }

    /** 한 줄에 기록 하나를 쓰므로 이름에 줄바꿈이 있으면 등록하지 않는다. null 검사는 ScoreBoard에 맡긴다. */
    private static void requireSingleLine(String playerName) {
        if (playerName != null && (playerName.indexOf('\n') >= 0 || playerName.indexOf('\r') >= 0)) {
            throw new IllegalArgumentException("이름에 줄바꿈을 쓸 수 없습니다");
        }
    }

    /** 새로운 스코어보드를 만든다.
     *
     * <p>모든 보드 생성 작업은 이곳을 거친다. */
    private ScoreBoard newBoard(GameMode mode) {
        return new ScoreBoard(mode, capacity);
    }

    /** 모드의 보드를 돌려준다. 아직 파일을 읽지 않았으면 먼저 읽는다. */
    private ScoreBoard board(GameMode mode) {
        Objects.requireNonNull(mode, "mode");
        if (!loaded) {
            load();
        }
        return boards.get(mode);
    }

    // ------------------------------------------------------------------
    // 저장
    // ------------------------------------------------------------------

    /**
     * 주어진 순위표들의 기록을 임시 파일에 쓴 뒤 통째로 교체한다. 실패하면 임시 파일을 정리한다.
     *
     * <p>실행 중인 순위표가 아니라 저장할 순위표를 받으므로, 실패해도 메모리의 기록은 바뀌지 않는다.
     */
    private void save(Map<GameMode, ScoreBoard> source) {
        StringBuilder content = new StringBuilder(HEADER).append('\n');
        for (ScoreBoard board : source.values()) {
            for (ScoreRecord record : board.getTopScores()) {
                content.append(escape(record.playerName())).append(',')
                        .append(record.score()).append(',')
                        .append(record.mode().name()).append(',')
                        .append(record.difficulty().name()).append('\n');
            }
        }

        Path temporaryFile = null;
        try {
            Path target = filePath.toAbsolutePath();
            Path parent = target.getParent();
            Files.createDirectories(parent);
            temporaryFile = Files.createTempFile(parent, ".scores-", ".tmp");
            try (Writer writer = Files.newBufferedWriter(temporaryFile, StandardCharsets.UTF_8)) {
                writer.write(content.toString());
            }
            // 교체가 불가능하면 저장 실패로 처리하여 기존 파일을 유지한다.
            Files.move(temporaryFile, target,
                    StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            if (temporaryFile != null) {
                try {
                    Files.deleteIfExists(temporaryFile);
                } catch (IOException cleanupFailure) {
                    e.addSuppressed(cleanupFailure);
                }
            }
            throw new UncheckedIOException("스코어보드를 저장할 수 없습니다: " + filePath, e);
        }
    }

    /** 쉼표·큰따옴표가 있는 이름은 큰따옴표로 감싸고, 안의 큰따옴표는 두 번 쓴다. */
    private static String escape(String name) {
        boolean needsQuote = name.indexOf(',') >= 0 || name.indexOf('"') >= 0;
        if (!needsQuote) {
            return name;
        }
        return '"' + name.replace("\"", "\"\"") + '"';
    }

    // ------------------------------------------------------------------
    // 불러오기
    // ------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>파일이 없으면 빈 기록으로 시작한다. 형식이 잘못된 줄은 건너뛰고 나머지 기록을 불러온다.
     *
     * <p>호출하지 않아도 처음 조회·등록할 때 읽는다. 이미 읽은 뒤에 호출하면 파일 내용으로 다시 읽는다.
     *
     * @throws UncheckedIOException 파일은 있으나 읽을 수 없는 경우. 이때는 읽지 않은 상태로 남아 다음 조회·등록에서 다시 시도한다
     */
    @Override
    public void load() {
        List<ScoreRecord> records = new ArrayList<>();
        if (Files.exists(filePath)) {
            String content;
            try {
                content = Files.readString(filePath, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new UncheckedIOException("스코어보드를 불러올 수 없습니다: " + filePath, e);
            }
            for (List<String> row : parseRows(content)) {
                ScoreRecord record = toRecord(row);
                if (record != null) {
                    records.add(record);
                }
            }
        }
        // 각 보드가 자기 모드의 기록만 골라 간다
        for (ScoreBoard board : boards.values()) {
            board.load(records);
        }
        loaded = true;
    }

    /**
     * 한 줄의 칸들을 기록으로 바꾼다.
     *
     * <p>헤더로 작성한 줄은 건너뛴다.
     *
     * @return 헤더이거나 형식이 잘못된 줄이면 {@code null}
     */
    private static ScoreRecord toRecord(List<String> row) {
        if (row.size() != COLUMNS || String.join(",", row).equals(HEADER)) {
            return null;
        }
        try {
            return new ScoreRecord(
                    row.get(0),
                    Integer.parseInt(row.get(1).trim()),
                    GameMode.valueOf(row.get(2).trim()),
                    Difficulty.valueOf(row.get(3).trim()));
        } catch (IllegalArgumentException e) {
            // 숫자가 아닌 점수(NumberFormatException 포함)나 정의되지 않은 모드·난이도
            return null;
        }
    }

    /**
     * CSV 내용을 줄로 나눈 뒤 각 줄을 칸으로 나눈다.
     *
     * <p>줄바꿈은 {@code \r\n}(Windows), {@code \n}(Linux·macOS), {@code \r} 모두 인식한다.
     * 기록이 한 줄씩이므로 손상된 줄이 있어도 다른 줄에는 영향을 주지 않는다.
     */
    private static List<List<String>> parseRows(String content) {
        List<List<String>> rows = new ArrayList<>();
        for (String line : content.split("\r\n|\r|\n")) {
            rows.add(parseLine(line));
        }
        return rows;
    }

    /**
     * 한 줄을 칸으로 나눈다.
     *
     * <p>칸의 시작에 있는 큰따옴표만 감싼 칸으로 보고, 그 안에서는 쉼표를 글자로 취급하며 큰따옴표 두 개는 하나로 읽는다.
     * 칸 중간의 큰따옴표는 글자로 읽는다.
     *
     * @return 나눈 칸들. 큰따옴표가 닫히지 않은 줄이면 빈 목록
     */
    private static List<String> parseLine(String line) {
        List<String> row = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c != '"') {
                    field.append(c);
                } else if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    field.append('"');      // 큰따옴표 두 개 → 글자 하나
                    i++;
                } else {
                    inQuotes = false;       // 감싼 칸이 끝남
                }
            } else if (c == '"' && field.length() == 0) {
                inQuotes = true;            // 칸의 시작에서만 감싼 칸으로 본다
            } else if (c == ',') {
                row.add(field.toString());
                field.setLength(0);
            } else {
                field.append(c);
            }
        }
        if (inQuotes) {
            return List.of();               // 닫히지 않은 큰따옴표: 잘못된 줄
        }
        row.add(field.toString());
        return row;
    }
}