package seoultech.se.tetris.score;

import java.util.List;
import seoultech.se.tetris.engine.Difficulty;
import seoultech.se.tetris.engine.GameMode;

/**
 * 모드별 스코어보드 기록을 저장하고 불러온다.
 *
 * <p>조회·등록·초기화 중에 저장 파일을 읽거나 쓸 수 있으며, 실패하면 {@link java.io.UncheckedIOException}을 던진다.
 */
public interface ScoreRepository {
    /** 입력한 모드의 상위 점수 기록 리스트 (점수 내림차순)
     *
     * <p>저장소의 보관 갯수만큼 반환한다. 화면에 표시할 갯수는 화면 쪽에서 정해서 목록을 잘라 나눈다. */
    List<ScoreRecord> getTopScores(GameMode mode);

    /** 입력한 모드의 점수가 순위에 등록되는지 확인 */
    boolean isQualified(int score, GameMode mode);

    /**
     * 새 점수 기록 등록 (이름, 점수, 모드, 난이도)
     *
     * <p>반환값은 등록된 순위. 등록한 기록을 강조할 때 사용한다. 등록되지 않았으면 -1 */
    int addScore(String playerName, int score, GameMode mode, Difficulty difficulty);

    /** 모든 모드의 스코어보드 기록 전체 초기화 (설정 화면의 스코어보드 초기화) */
    void clearScores();

    /** 저장된 기록을 다시 읽는다. 호출하지 않아도 처음 조회·등록할 때 읽는다. */
    void load();
}
