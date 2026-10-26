package seoultech.se.tetris.score;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import seoultech.se.tetris.engine.Difficulty;
import seoultech.se.tetris.engine.GameMode;

@DisplayName("CsvScoreRepository")
class CsvScoreRepositoryTest {

    @TempDir
    Path tempDir;

    private Path file;
    private CsvScoreRepository repository;

    @BeforeEach
    void setUp() {
        file = tempDir.resolve("scores.csv");
        repository = new CsvScoreRepository(file);
    }

    /** 같은 파일을 새 저장소로 다시 읽는다. 프로그램을 껐다 켠 상황에 해당한다. */
    private CsvScoreRepository reopen() {
        CsvScoreRepository reopened = new CsvScoreRepository(file);
        reopened.load();
        return reopened;
    }

    /** 파일 내용을 직접 쓴 뒤 불러온다. 손상된 파일 등을 재현할 때 쓴다. */
    private void loadFrom(String content) throws IOException {
        Files.writeString(file, content);
        repository.load();
    }

    private static List<String> names(List<ScoreRecord> records) {
        return records.stream().map(ScoreRecord::playerName).toList();
    }

    // ------------------------------------------------------------------
    // 저장과 복원
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("저장과 복원")
    class SaveAndRestore {

        @Test
        @DisplayName("등록한 기록은 따로 저장하지 않아도 다시 열었을 때 남아 있다")
        void addedScoreSurvivesReopen() {
            repository.addScore("철수", 12000, GameMode.NORMAL, Difficulty.HARD);

            List<ScoreRecord> restored = reopen().getTopScores(GameMode.NORMAL);

            assertEquals(List.of(new ScoreRecord("철수", 12000, GameMode.NORMAL, Difficulty.HARD)), restored);
        }

        @Test
        @DisplayName("모드별 기록이 섞이지 않고 복원된다")
        void restoresEachModeSeparately() {
            repository.addScore("일반", 100, GameMode.NORMAL, Difficulty.NORMAL);
            repository.addScore("아이템", 200, GameMode.ITEM, Difficulty.EASY);

            CsvScoreRepository reopened = reopen();

            assertEquals(List.of("일반"), names(reopened.getTopScores(GameMode.NORMAL)));
            assertEquals(List.of("아이템"), names(reopened.getTopScores(GameMode.ITEM)));
        }

        @Test
        @DisplayName("파일 첫 줄은 헤더이고, 일반 모드 기록이 먼저 저장된다")
        void writesHeaderThenRecordsInModeOrder() throws IOException {
            repository.addScore("아이템", 200, GameMode.ITEM, Difficulty.EASY);
            repository.addScore("일반", 100, GameMode.NORMAL, Difficulty.HARD);

            assertEquals(
                    CsvScoreRepository.HEADER + "\n"
                            + "일반,100,NORMAL,HARD\n"
                            + "아이템,200,ITEM,EASY\n",
                    Files.readString(file));
        }

        @Test
        @DisplayName("저장할 폴더가 없으면 만든다")
        void createsMissingDirectory() {
            Path nested = tempDir.resolve("a").resolve("b").resolve("scores.csv");
            CsvScoreRepository nestedRepository = new CsvScoreRepository(nested);

            nestedRepository.addScore("P", 100, GameMode.NORMAL, Difficulty.NORMAL);

            assertTrue(Files.exists(nested));
        }

        @Test
        @DisplayName("저장 후 임시 파일이 남지 않는다")
        void leavesNoTemporaryFile() throws IOException {
            repository.addScore("P", 100, GameMode.NORMAL, Difficulty.NORMAL);

            try (var files = Files.list(tempDir)) {
                assertEquals(List.of(file), files.toList());
            }
        }
    }

    // ------------------------------------------------------------------
    // 이름의 특수 문자
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("이름의 특수 문자")
    class SpecialCharacters {

        @Test
        @DisplayName("쉼표가 든 이름은 큰따옴표로 감싸 저장하고 그대로 복원한다")
        void quotesNameWithComma() throws IOException {
            repository.addScore("홍,길동", 100, GameMode.NORMAL, Difficulty.NORMAL);

            assertTrue(Files.readString(file).contains("\"홍,길동\",100,NORMAL,NORMAL"));
            assertEquals(List.of("홍,길동"), names(reopen().getTopScores(GameMode.NORMAL)));
        }

        @Test
        @DisplayName("큰따옴표가 든 이름은 두 번 써서 저장하고 그대로 복원한다")
        void escapesQuoteInName() throws IOException {
            repository.addScore("say \"hi\"", 100, GameMode.NORMAL, Difficulty.NORMAL);

            assertTrue(Files.readString(file).contains("\"say \"\"hi\"\"\",100"));
            assertEquals(List.of("say \"hi\""), names(reopen().getTopScores(GameMode.NORMAL)));
        }

        @Test
        @DisplayName("큰따옴표로 시작하는 이름도 그대로 복원한다")
        void restoresNameStartingWithQuote() {
            repository.addScore("\"시작", 100, GameMode.NORMAL, Difficulty.NORMAL);

            assertEquals(List.of("\"시작"), names(reopen().getTopScores(GameMode.NORMAL)));
        }

        @Test
        @DisplayName("줄바꿈이 든 이름은 등록하지 않는다")
        void rejectsNameWithLineBreak() {
            assertThrows(IllegalArgumentException.class,
                    () -> repository.addScore("첫줄\n둘째줄", 100, GameMode.NORMAL, Difficulty.NORMAL));
            assertThrows(IllegalArgumentException.class,
                    () -> repository.addScore("첫줄\r둘째줄", 100, GameMode.NORMAL, Difficulty.NORMAL));

            assertTrue(repository.getTopScores(GameMode.NORMAL).isEmpty());
            assertFalse(Files.exists(file));
        }

        @Test
        @DisplayName("이름 앞뒤의 공백을 보존한다")
        void keepsSurroundingSpacesInName() {
            repository.addScore(" 공백 ", 100, GameMode.NORMAL, Difficulty.NORMAL);

            assertEquals(List.of(" 공백 "), names(reopen().getTopScores(GameMode.NORMAL)));
        }
    }

    // ------------------------------------------------------------------
    // 불러오기
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("불러오기")
    class Load {

        @Test
        @DisplayName("파일이 없으면 빈 기록으로 시작한다")
        void startsEmptyWithoutFile() {
            repository.load();

            assertTrue(repository.getTopScores(GameMode.NORMAL).isEmpty());
            assertTrue(repository.getTopScores(GameMode.ITEM).isEmpty());
        }

        @Test
        @DisplayName("파일이 사라진 뒤 다시 불러오면 메모리의 기록도 비운다")
        void clearsMemoryWhenFileIsGone() throws IOException {
            repository.addScore("P", 100, GameMode.NORMAL, Difficulty.NORMAL);
            Files.delete(file);

            repository.load();

            assertTrue(repository.getTopScores(GameMode.NORMAL).isEmpty());
        }

        @Test
        @DisplayName("형식이 잘못된 줄은 건너뛰고 나머지를 불러온다")
        void skipsMalformedLines() throws IOException {
            loadFrom(CsvScoreRepository.HEADER + "\n"
                    + "정상,100,NORMAL,EASY\n"
                    + "점수오류,abc,NORMAL,EASY\n"      // 숫자가 아닌 점수
                    + "모드오류,200,VERSUS,EASY\n"      // 정의되지 않은 모드
                    + "난이도오류,300,NORMAL,EXTREME\n" // 정의되지 않은 난이도
                    + "칸부족,400,NORMAL\n"             // 열이 모자람
                    + "\n"                               // 빈 줄
                    + "음수,-5,NORMAL,EASY\n");         // 값이 잘못됨 (ScoreBoard가 거름)

            assertEquals(List.of("정상"), names(repository.getTopScores(GameMode.NORMAL)));
        }

        @Test
        @DisplayName("큰따옴표가 닫히지 않은 줄만 건너뛰고 앞뒤 줄을 불러온다")
        void skipsOnlyLineWithUnclosedQuote() throws IOException {
            loadFrom(CsvScoreRepository.HEADER + "\n"
                    + "앞,300,NORMAL,EASY\n"
                    + "\"닫히지않음,200,NORMAL,EASY\n"
                    + "뒤,100,NORMAL,EASY\n");

            assertEquals(List.of("앞", "뒤"), names(repository.getTopScores(GameMode.NORMAL)));
        }

        @Test
        @DisplayName("칸 중간의 큰따옴표는 글자로 읽는다")
        void readsQuoteInsideUnquotedName() throws IOException {
            loadFrom("A\"x,200,NORMAL,EASY\n"
                    + "B,100,NORMAL,EASY\n");

            assertEquals(List.of("A\"x", "B"), names(repository.getTopScores(GameMode.NORMAL)));
        }

        @Test
        @DisplayName("헤더가 없어도 첫 기록을 잃지 않는다")
        void readsFirstRecordWithoutHeader() throws IOException {
            loadFrom("A,100,NORMAL,EASY\n");

            assertEquals(List.of("A"), names(repository.getTopScores(GameMode.NORMAL)));
        }

        @Test
        @DisplayName("\\r\\n, \\r 줄바꿈과 마지막 줄바꿈 누락을 처리한다")
        void handlesCrLfAndMissingFinalLineBreak() throws IOException {
            loadFrom(CsvScoreRepository.HEADER + "\r\nA,100,NORMAL,EASY\rC,50,NORMAL,EASY\r\nB,200,ITEM,HARD");

            assertEquals(List.of("A", "C"), names(repository.getTopScores(GameMode.NORMAL)));
            assertEquals(List.of("B"), names(repository.getTopScores(GameMode.ITEM)));
        }

        @Test
        @DisplayName("점수·모드·난이도 칸 앞뒤의 공백은 무시한다")
        void trimsValueColumns() throws IOException {
            loadFrom("A, 100 , NORMAL , EASY\n");

            assertEquals(List.of(new ScoreRecord("A", 100, GameMode.NORMAL, Difficulty.EASY)),
                    repository.getTopScores(GameMode.NORMAL));
        }

        @Test
        @DisplayName("파일을 읽을 수 없으면 UncheckedIOException을 던진다")
        void throwsWhenFileCannotBeRead() throws IOException {
            // 같은 이름의 폴더를 두어 파일 읽기가 실패하도록 한다
            Files.createDirectory(file);

            assertThrows(UncheckedIOException.class, () -> repository.load());
        }
    }

    // ------------------------------------------------------------------
    // 불러오기 전 사용
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("불러오기 전 사용")
    class BeforeLoad {

        @Test
        @DisplayName("불러오지 않고 등록해도 기존 기록을 유지한다")
        void addBeforeLoadKeepsExistingRecords() throws IOException {
            Files.writeString(file, CsvScoreRepository.HEADER + "\n" + "기존,500,NORMAL,EASY\n");

            repository.addScore("새기록", 100, GameMode.NORMAL, Difficulty.NORMAL);

            assertEquals(List.of("기존", "새기록"), names(reopen().getTopScores(GameMode.NORMAL)));
        }

        @Test
        @DisplayName("불러오지 않아도 순위 자격을 파일의 기록으로 판단한다")
        void qualificationBeforeLoadUsesFile() throws IOException {
            StringBuilder content = new StringBuilder(CsvScoreRepository.HEADER).append('\n');
            for (int i = 1; i <= 10; i++) {
                content.append("P").append(i).append(',').append(i * 100).append(",NORMAL,NORMAL\n");
            }
            Files.writeString(file, content);

            assertFalse(repository.isQualified(50, GameMode.NORMAL));
        }

        @Test
        @DisplayName("파일을 읽을 수 없으면 등록하지 않고 기존 파일을 유지한다")
        void addFailsBeforeOverwritingUnreadableFile() throws IOException {
            // 같은 이름의 폴더를 두어 파일 읽기가 실패하도록 한다
            Path target = Files.createDirectory(file);
            Path marker = target.resolve("original");
            Files.writeString(marker, "기존 데이터");

            UncheckedIOException thrown = assertThrows(UncheckedIOException.class,
                    () -> repository.addScore("P", 100, GameMode.NORMAL, Difficulty.NORMAL));

            // 저장 단계까지 가지 않고 읽기 단계에서 멈춘다
            assertTrue(thrown.getMessage().startsWith("스코어보드를 불러올 수 없습니다"));
            assertEquals("기존 데이터", Files.readString(marker));
        }

        @Test
        @DisplayName("읽기에 실패한 뒤에는 다음 조회에서 다시 읽는다")
        void retriesLoadAfterFailure() throws IOException {
            Path target = Files.createDirectory(file);
            assertThrows(UncheckedIOException.class, () -> repository.getTopScores(GameMode.NORMAL));

            Files.delete(target);
            Files.writeString(file, "A,100,NORMAL,EASY\n");

            assertEquals(List.of("A"), names(repository.getTopScores(GameMode.NORMAL)));
        }

        @Test
        @DisplayName("초기화한 뒤에는 파일을 다시 읽지 않는다")
        void clearDoesNotReloadAfterwards() throws IOException {
            Files.writeString(file, "기존,500,NORMAL,EASY\n");

            repository.clearScores();
            repository.addScore("새기록", 100, GameMode.NORMAL, Difficulty.NORMAL);

            assertEquals(List.of("새기록"), names(reopen().getTopScores(GameMode.NORMAL)));
        }
    }

    // ------------------------------------------------------------------
    // 등록과 조회
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("등록과 조회")
    class AddAndQuery {

        @Test
        @DisplayName("등록한 기록의 순위를 돌려준다")
        void returnsRank() {
            repository.addScore("A", 300, GameMode.NORMAL, Difficulty.NORMAL);

            assertEquals(2, repository.addScore("B", 200, GameMode.NORMAL, Difficulty.NORMAL));
            assertEquals(1, repository.addScore("C", 400, GameMode.NORMAL, Difficulty.NORMAL));
        }

        @Test
        @DisplayName("순위 자격은 모드마다 따로 판단한다")
        void qualificationIsPerMode() {
            for (int i = 1; i <= 10; i++) {
                repository.addScore("P" + i, i * 100, GameMode.NORMAL, Difficulty.NORMAL);
            }

            assertFalse(repository.isQualified(50, GameMode.NORMAL));
            assertTrue(repository.isQualified(50, GameMode.ITEM));
        }

        @Test
        @DisplayName("순위에 들지 못한 기록은 파일을 바꾸지 않는다")
        void rejectedScoreDoesNotRewriteFile() throws IOException {
            for (int i = 1; i <= 10; i++) {
                repository.addScore("P" + i, i * 100, GameMode.NORMAL, Difficulty.NORMAL);
            }
            String before = Files.readString(file);

            assertEquals(-1, repository.addScore("탈락", 50, GameMode.NORMAL, Difficulty.NORMAL));
            assertEquals(before, Files.readString(file));
        }

        @Test
        @DisplayName("모드가 null이면 예외를 던진다")
        void throwsOnNullMode() {
            assertThrows(NullPointerException.class, () -> repository.getTopScores(null));
            assertThrows(NullPointerException.class, () -> repository.isQualified(100, null));
            assertThrows(NullPointerException.class,
                    () -> repository.addScore("P", 100, null, Difficulty.NORMAL));
        }
    }

    // ------------------------------------------------------------------
    // 보관 개수
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("보관 개수")
    class Capacity {

        /** 지정한 개수만큼 일반 모드 기록을 파일에 써 둔다. 점수는 100, 200, ... 순이다. */
        private void writeRecords(int count) throws IOException {
            StringBuilder content = new StringBuilder(CsvScoreRepository.HEADER).append('\n');
            for (int i = 1; i <= count; i++) {
                content.append("P").append(i).append(',').append(i * 100).append(",NORMAL,NORMAL\n");
            }
            Files.writeString(file, content);
        }

        @Test
        @DisplayName("기본 보관 개수는 모드마다 10개다")
        void keepsDefaultCapacityPerMode() throws IOException {
            writeRecords(15);

            repository.load();

            assertEquals(ScoreBoard.DEFAULT_CAPACITY, repository.getTopScores(GameMode.NORMAL).size());
        }

        @Test
        @DisplayName("보관 개수를 늘린 저장소는 등록 후에도 그 개수를 유지한다")
        void largerCapacitySurvivesAdd() throws IOException {
            writeRecords(20);
            CsvScoreRepository large = new CsvScoreRepository(file, 25);

            large.addScore("새기록", 5000, GameMode.NORMAL, Difficulty.NORMAL);

            assertEquals(21, large.getTopScores(GameMode.NORMAL).size());
            assertEquals(21, new CsvScoreRepository(file, 25).getTopScores(GameMode.NORMAL).size());
        }

        @Test
        @DisplayName("보관 개수를 늘린 저장소는 초기화 후에도 그 개수까지 등록한다")
        void largerCapacitySurvivesClear() {
            CsvScoreRepository large = new CsvScoreRepository(file, 25);
            large.clearScores();

            for (int i = 1; i <= 25; i++) {
                large.addScore("P" + i, i * 100, GameMode.NORMAL, Difficulty.NORMAL);
            }

            assertEquals(25, large.getTopScores(GameMode.NORMAL).size());
        }

        @Test
        @DisplayName("보관 개수가 최소 보관 개수 미만이면 예외를 던진다")
        void throwsOnTooSmallCapacity() {
            assertThrows(IllegalArgumentException.class,
                    () -> new CsvScoreRepository(file, ScoreBoard.MIN_CAPACITY - 1));
        }
    }

    // ------------------------------------------------------------------
    // 초기화와 저장 실패
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("초기화와 저장 실패")
    class ClearAndFailure {

        @Test
        @DisplayName("초기화하면 모든 모드의 기록이 지워지고 파일에도 반영된다")
        void clearRemovesAllModesAndPersists() throws IOException {
            repository.addScore("일반", 100, GameMode.NORMAL, Difficulty.NORMAL);
            repository.addScore("아이템", 200, GameMode.ITEM, Difficulty.NORMAL);

            repository.clearScores();

            assertTrue(repository.getTopScores(GameMode.NORMAL).isEmpty());
            assertTrue(repository.getTopScores(GameMode.ITEM).isEmpty());
            assertEquals(CsvScoreRepository.HEADER + "\n", Files.readString(file));
            assertTrue(reopen().getTopScores(GameMode.ITEM).isEmpty());
        }

        @Test
        @DisplayName("저장에 실패하면 예외를 던지고, 기존 데이터를 유지하며 임시 파일을 정리한다")
        void failedSaveKeepsTargetAndRemovesTemporaryFile() throws IOException {
            // 먼저 읽어 두어야 등록이 읽기 단계가 아닌 저장 단계에서 실패한다
            repository.load();
            // 내용이 있는 폴더를 저장 대상으로 두어 파일 교체 단계에서 실패하도록 한다
            Path target = Files.createDirectory(file);
            Path marker = target.resolve("original");
            Files.writeString(marker, "기존 데이터");

            assertThrows(UncheckedIOException.class,
                    () -> repository.addScore("P", 100, GameMode.NORMAL, Difficulty.NORMAL));
            assertEquals("기존 데이터", Files.readString(marker));
            try (var files = Files.list(tempDir)) {
                assertEquals(List.of(target), files.toList());
            }
        }

        /** 이미 읽은 뒤 저장 파일 자리에 내용이 있는 폴더를 두어, 이후 저장이 파일 교체 단계에서 실패하게 한다. */
        private void blockSaving() throws IOException {
            repository.load();
            Files.deleteIfExists(file);
            Files.createDirectory(file);
            Files.writeString(file.resolve("original"), "기존 데이터");
        }

        /** {@link #blockSaving}으로 둔 폴더를 지워 다시 저장할 수 있게 한다. */
        private void unblockSaving() throws IOException {
            Files.delete(file.resolve("original"));
            Files.delete(file);
        }

        @Test
        @DisplayName("등록 중 저장에 실패하면 순위표는 등록 전 그대로다")
        void failedAddKeepsBoardUnchanged() throws IOException {
            for (int i = 1; i <= 10; i++) {
                repository.addScore("P" + i, i * 100, GameMode.NORMAL, Difficulty.NORMAL);
            }
            List<ScoreRecord> before = repository.getTopScores(GameMode.NORMAL);
            blockSaving();

            assertThrows(UncheckedIOException.class,
                    () -> repository.addScore("새기록", 5000, GameMode.NORMAL, Difficulty.NORMAL));

            // 새 기록이 없고, 밀려날 뻔한 10위 기록도 남아 있다
            assertEquals(before, repository.getTopScores(GameMode.NORMAL));
        }

        @Test
        @DisplayName("초기화 중 저장에 실패하면 기록은 초기화 전 그대로다")
        void failedClearKeepsRecords() throws IOException {
            repository.addScore("일반", 100, GameMode.NORMAL, Difficulty.NORMAL);
            repository.addScore("아이템", 200, GameMode.ITEM, Difficulty.NORMAL);
            blockSaving();

            assertThrows(UncheckedIOException.class, () -> repository.clearScores());

            assertEquals(List.of("일반"), names(repository.getTopScores(GameMode.NORMAL)));
            assertEquals(List.of("아이템"), names(repository.getTopScores(GameMode.ITEM)));
        }

        @Test
        @DisplayName("저장에 실패한 기록은 이후의 저장에 섞이지 않는다")
        void failedRecordIsNotSavedLater() throws IOException {
            blockSaving();
            assertThrows(UncheckedIOException.class,
                    () -> repository.addScore("실패", 300, GameMode.NORMAL, Difficulty.NORMAL));
            unblockSaving();

            repository.addScore("성공", 100, GameMode.NORMAL, Difficulty.NORMAL);

            assertEquals(List.of("성공"), names(reopen().getTopScores(GameMode.NORMAL)));
        }
    }
}