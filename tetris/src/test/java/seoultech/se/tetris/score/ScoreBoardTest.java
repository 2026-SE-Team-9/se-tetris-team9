package seoultech.se.tetris.score;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import seoultech.se.tetris.engine.Difficulty;
import seoultech.se.tetris.engine.GameMode;

@DisplayName("ScoreBoard")
class ScoreBoardTest {

    private ScoreBoard board;

    @BeforeEach
    void setUp() {
        board = new ScoreBoard(GameMode.NORMAL);
    }

    /** 보드를 지정한 점수들로 채운다. 이름은 점수를 그대로 쓴다. */
    private void fill(int... scores) {
        for (int score : scores) {
            board.add("P" + score, score, Difficulty.NORMAL);
        }
    }

    /** 일반 모드·보통 난이도의 기록을 만든다. 모드와 난이도가 중요하지 않은 테스트에서 쓴다. */
    private static ScoreRecord normalRecord(String name, int score) {
        return new ScoreRecord(name, score, GameMode.NORMAL, Difficulty.NORMAL);
    }

    /** 현재 기록의 점수만 순서대로 꺼낸다. */
    private List<Integer> scores() {
        return board.getTopScores().stream().map(ScoreRecord::score).toList();
    }

    // ------------------------------------------------------------------
    // 정렬
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("정렬")
    class Sorting {

        @Test
        @DisplayName("점수가 높은 순으로 정렬된다")
        void sortsByScoreDescending() {
            fill(300, 100, 500, 200);

            assertEquals(List.of(500, 300, 200, 100), scores());
        }

        @Test
        @DisplayName("점수가 같으면 먼저 등록한 기록이 앞선다")
        void keepsInsertionOrderOnTie() {
            board.add("먼저", 100, Difficulty.NORMAL);
            board.add("나중", 100, Difficulty.NORMAL);

            assertEquals("먼저", board.getTopScores().get(0).playerName());
            assertEquals("나중", board.getTopScores().get(1).playerName());
        }

        @Test
        @DisplayName("불러온 기록도 정렬한다")
        void sortsLoadedRecords() {
            board.load(List.of(
                    normalRecord("A", 100),
                    normalRecord("B", 500),
                    normalRecord("C", 300)));

            assertEquals(List.of(500, 300, 100), scores());
        }
    }

    // ------------------------------------------------------------------
    // 보관 개수
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("보관 개수")
    class Capacity {

        @Test
        @DisplayName("기본 보관 개수는 10개다")
        void defaultCapacityIsTen() {
            assertEquals(10, board.capacity());
        }

        @Test
        @DisplayName("10개를 넘으면 가장 낮은 점수가 밀려난다")
        void dropsLowestWhenFull() {
            fill(100, 200, 300, 400, 500, 600, 700, 800, 900, 1000);

            board.add("신규", 550, Difficulty.NORMAL);

            assertEquals(10, board.size());
            assertFalse(scores().contains(100), "가장 낮은 점수가 남아 있습니다.");
            assertTrue(scores().contains(550));
        }

        @Test
        @DisplayName("가득 찬 상태에서 최저점보다 낮으면 등록되지 않는다")
        void rejectsScoreBelowLowest() {
            fill(100, 200, 300, 400, 500, 600, 700, 800, 900, 1000);

            int rank = board.add("탈락", 50, Difficulty.NORMAL);

            assertEquals(-1, rank);
            assertFalse(scores().contains(50));
        }

        @Test
        @DisplayName("보관 개수를 늘릴 수 있다")
        void allowsLargerCapacity() {
            ScoreBoard large = new ScoreBoard(GameMode.NORMAL, 25);

            assertEquals(25, large.capacity());
        }

        @Test
        @DisplayName("최소 보관 개수는 요구사항대로 10개다")
        void minimumCapacityIsTen() {
            assertEquals(10, ScoreBoard.MIN_CAPACITY);
        }

        @Test
        @DisplayName("보관 개수가 최소 보관 개수 미만이면 예외를 던진다")
        void throwsOnTooSmallCapacity() {
            // 요구사항이 "최소 상위 10개"이므로 그보다 적게 둘 수 없다
            assertThrows(IllegalArgumentException.class,
                    () -> new ScoreBoard(GameMode.NORMAL, ScoreBoard.MIN_CAPACITY - 1));
        }

        @Test
        @DisplayName("최소 보관 개수와 같으면 만들 수 있다")
        void allowsMinimumCapacity() {
            assertEquals(ScoreBoard.MIN_CAPACITY,
                    new ScoreBoard(GameMode.NORMAL, ScoreBoard.MIN_CAPACITY).capacity());
        }

        @Test
        @DisplayName("불러온 기록이 보관 개수를 넘으면 잘라낸다")
        void trimsLoadedRecords() {
            ScoreRecord[] many = new ScoreRecord[20];
            for (int i = 0; i < 20; i++) {
                many[i] = normalRecord("P" + i, i * 100);
            }

            board.load(List.of(many));

            assertEquals(10, board.size());
            assertEquals(1900, board.getTopScores().get(0).score());
        }
    }

    // ------------------------------------------------------------------
    // 등록 자격
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("isQualified()")
    class Qualification {

        @Test
        @DisplayName("기록이 덜 찼으면 어떤 점수든 순위에 든다")
        void anyScoreQualifiesWhenNotFull() {
            fill(1000, 900, 800);

            assertTrue(board.isQualified(1));
            assertTrue(board.isQualified(0));
        }

        @Test
        @DisplayName("가득 찼으면 최저점보다 높아야 순위에 든다")
        void needsToBeatLowestWhenFull() {
            fill(100, 200, 300, 400, 500, 600, 700, 800, 900, 1000);

            assertTrue(board.isQualified(101));
            assertFalse(board.isQualified(100));   // 같은 점수로는 밀어내지 못한다
            assertFalse(board.isQualified(99));
        }
    }

    // ------------------------------------------------------------------
    // 조회
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("조회")
    class Query {

        @Test
        @DisplayName("원하는 개수만큼 가져온다")
        void takesRequestedCount() {
            fill(100, 200, 300, 400, 500);

            assertEquals(List.of(500, 400, 300), board.getTopScores(3).stream()
                    .map(ScoreRecord::score).toList());
        }

        @Test
        @DisplayName("가진 기록보다 많이 요청하면 있는 만큼만 준다")
        void returnsWhatItHas() {
            fill(100, 200);

            assertEquals(2, board.getTopScores(10).size());
        }

        @Test
        @DisplayName("반환한 목록은 수정할 수 없다")
        void returnsImmutableList() {
            fill(100);

            List<ScoreRecord> result = board.getTopScores();

            assertThrows(UnsupportedOperationException.class,
                    () -> result.add(normalRecord("침입", 999)));
        }

        @Test
        @DisplayName("순위를 1부터 매긴다")
        void ranksFromOne() {
            fill(100, 300, 200);

            assertEquals(1, board.rankOf(normalRecord("P300", 300)));
            assertEquals(3, board.rankOf(normalRecord("P100", 100)));
        }

        @Test
        @DisplayName("없는 기록의 순위는 -1이다")
        void unknownRecordHasNoRank() {
            fill(100);

            assertEquals(-1, board.rankOf(normalRecord("없음", 999)));
        }

        @Test
        @DisplayName("개수가 음수면 예외를 던진다")
        void throwsOnNegativeCount() {
            assertThrows(IllegalArgumentException.class, () -> board.getTopScores(-1));
        }
    }

    // ------------------------------------------------------------------
    // 등록 순위
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("add()의 반환 순위")
    class AddedRank {

        @Test
        @DisplayName("등록된 기록의 순위를 1부터 돌려준다")
        void returnsRankOfAddedRecord() {
            fill(100, 300);

            assertEquals(2, board.add("신규", 200, Difficulty.NORMAL));
            assertEquals(1, board.add("최고", 400, Difficulty.NORMAL));
        }

        @Test
        @DisplayName("같은 이름·점수의 기록이 있어도 방금 등록한 기록의 순위를 돌려준다")
        void returnsRankOfNewRecordAmongDuplicates() {
            assertEquals(1, board.add("AAA", 500, Difficulty.NORMAL));

            // 점수가 같으면 먼저 등록한 기록이 앞서므로 새 기록은 2위다
            assertEquals(2, board.add("AAA", 500, Difficulty.NORMAL));
        }

        @Test
        @DisplayName("최저 기록과 이름·점수가 같아도 밀려나면 -1을 돌려준다")
        void duplicateOfLowestIsRejected() {
            fill(100, 200, 300, 400, 500, 600, 700, 800, 900, 1000);

            // 같은 점수로는 기존 기록을 밀어내지 못한다 (isQualified와 같은 기준)
            assertFalse(board.isQualified(100));
            assertEquals(-1, board.add("P100", 100, Difficulty.NORMAL));
            assertEquals(10, board.size());
        }
    }

    // ------------------------------------------------------------------
    // 페이지
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("페이지")
    class Paging {

        @Test
        @DisplayName("페이지 단위로 나눠 가져온다")
        void splitsIntoPages() {
            ScoreBoard large = new ScoreBoard(GameMode.NORMAL, 25);
            for (int i = 1; i <= 25; i++) {
                large.add("P" + i, i * 100, Difficulty.NORMAL);
            }

            assertEquals(2500, large.getPage(0, 10).get(0).score());
            assertEquals(1500, large.getPage(1, 10).get(0).score());
            assertEquals(5, large.getPage(2, 10).size());
        }

        @Test
        @DisplayName("범위를 벗어난 페이지는 빈 목록이다")
        void emptyBeyondLastPage() {
            fill(100, 200);

            assertTrue(board.getPage(5, 10).isEmpty());
        }

        @Test
        @DisplayName("전체 페이지 수를 센다")
        void countsPages() {
            ScoreBoard large = new ScoreBoard(GameMode.NORMAL, 25);
            for (int i = 1; i <= 25; i++) {
                large.add("P" + i, i * 100, Difficulty.NORMAL);
            }

            assertEquals(3, large.pageCount(10));
        }

        @Test
        @DisplayName("기록이 없어도 페이지는 1개다")
        void emptyBoardHasOnePage() {
            assertEquals(1, board.pageCount(10));
        }

        @Test
        @DisplayName("페이지 크기가 0 이하면 예외를 던진다")
        void throwsOnInvalidPageSize() {
            assertThrows(IllegalArgumentException.class, () -> board.getPage(0, 0));
            assertThrows(IllegalArgumentException.class, () -> board.pageCount(0));
        }
    }

    // ------------------------------------------------------------------
    // 초기화와 검증
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("초기화와 검증")
    class ClearAndValidation {

        @Test
        @DisplayName("모든 기록을 지운다")
        void clearsAll() {
            fill(100, 200, 300);

            board.clear();

            assertTrue(board.isEmpty());
            assertEquals(0, board.size());
        }

        @Test
        @DisplayName("이름이 null이면 예외를 던진다")
        void throwsOnNullName() {
            assertThrows(NullPointerException.class, () -> board.add(null, 100, Difficulty.NORMAL));
        }

        @Test
        @DisplayName("점수가 음수면 예외를 던진다")
        void throwsOnNegativeScore() {
            assertThrows(IllegalArgumentException.class, () -> board.add("P", -1, Difficulty.NORMAL));
        }

        @Test
        @DisplayName("불러온 기록 중 잘못된 기록은 건너뛰고 나머지를 보관한다")
        void skipsInvalidLoadedRecords() {
            // List.of는 null을 담을 수 없으므로 Arrays.asList를 쓴다
            List<ScoreRecord> loaded = Arrays.asList(
                    normalRecord("A", 300),
                    null,                              // 기록 자체가 없음
                    normalRecord(null, 200),        // 이름이 없음
                    normalRecord("C", -5),          // 음수 점수
                    normalRecord("B", 100));

            int skipped = board.load(loaded);

            assertEquals(3, skipped);
            assertEquals(List.of(300, 100), scores());
        }

        @Test
        @DisplayName("잘못된 기록이 없으면 건너뛴 수는 0이다")
        void skipsNothingWhenAllValid() {
            int skipped = board.load(List.of(normalRecord("A", 100)));

            assertEquals(0, skipped);
            assertEquals(1, board.size());
        }

        @Test
        @DisplayName("페이지 번호가 음수면 예외를 던진다")
        void throwsOnNegativePageIndex() {
            assertThrows(IllegalArgumentException.class, () -> board.getPage(-1, 10));
        }
    }

    // ------------------------------------------------------------------
    // 모드와 난이도 (2차 요구사항)
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("모드와 난이도")
    class ModeAndDifficulty {

        @Test
        @DisplayName("등록한 기록에 보드의 모드와 지정한 난이도가 남는다")
        void addedRecordHasBoardModeAndGivenDifficulty() {
            ScoreBoard itemBoard = new ScoreBoard(GameMode.ITEM);

            itemBoard.add("P", 100, Difficulty.HARD);

            ScoreRecord record = itemBoard.getTopScores().get(0);
            assertEquals(GameMode.ITEM, record.mode());
            assertEquals(Difficulty.HARD, record.difficulty());
        }

        @Test
        @DisplayName("난이도가 달라도 한 순위표에서 함께 정렬된다")
        void differentDifficultiesShareOneRanking() {
            board.add("쉬움", 300, Difficulty.EASY);
            board.add("어려움", 500, Difficulty.HARD);
            board.add("보통", 400, Difficulty.NORMAL);

            assertEquals(List.of(500, 400, 300), scores());
        }

        @Test
        @DisplayName("불러올 때 다른 모드의 기록은 무시하고 건너뛴 수에 세지 않는다")
        void ignoresOtherModeOnLoad() {
            int skipped = board.load(List.of(
                    normalRecord("일반", 100),
                    new ScoreRecord("아이템", 900, GameMode.ITEM, Difficulty.NORMAL)));

            assertEquals(0, skipped);
            assertEquals(List.of(100), scores());
        }

        @Test
        @DisplayName("같은 목록을 넘겨도 각 모드의 보드가 자기 기록만 갖는다")
        void splitsSharedListByMode() {
            List<ScoreRecord> all = List.of(
                    normalRecord("일반", 100),
                    new ScoreRecord("아이템", 900, GameMode.ITEM, Difficulty.EASY));
            ScoreBoard itemBoard = new ScoreBoard(GameMode.ITEM);

            board.load(all);
            itemBoard.load(all);

            assertEquals("일반", board.getTopScores().get(0).playerName());
            assertEquals("아이템", itemBoard.getTopScores().get(0).playerName());
        }

        @Test
        @DisplayName("모드나 난이도가 없는 기록은 잘못된 기록으로 건너뛴다")
        void skipsRecordsWithoutModeOrDifficulty() {
            int skipped = board.load(List.of(
                    normalRecord("정상", 100),
                    new ScoreRecord("모드 없음", 200, null, Difficulty.NORMAL),
                    new ScoreRecord("난이도 없음", 300, GameMode.NORMAL, null)));

            assertEquals(2, skipped);
            assertEquals(List.of(100), scores());
        }

        @Test
        @DisplayName("이름·점수가 같아도 난이도가 다르면 다른 기록이다")
        void sameNameAndScoreWithDifferentDifficultyIsDifferentRecord() {
            board.add("P", 100, Difficulty.EASY);

            assertEquals(1, board.rankOf(new ScoreRecord("P", 100, GameMode.NORMAL, Difficulty.EASY)));
            assertEquals(-1, board.rankOf(new ScoreRecord("P", 100, GameMode.NORMAL, Difficulty.HARD)));
        }

        @Test
        @DisplayName("보드의 모드를 알려준다")
        void exposesMode() {
            assertEquals(GameMode.NORMAL, board.mode());
            assertEquals(GameMode.ITEM, new ScoreBoard(GameMode.ITEM).mode());
        }

        @Test
        @DisplayName("모드가 null이면 예외를 던진다")
        void throwsOnNullMode() {
            assertThrows(NullPointerException.class, () -> new ScoreBoard(null));
        }

        @Test
        @DisplayName("난이도가 null이면 예외를 던진다")
        void throwsOnNullDifficulty() {
            assertThrows(NullPointerException.class, () -> board.add("P", 100, null));
        }
    }
}
