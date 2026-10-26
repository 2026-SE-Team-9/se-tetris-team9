package seoultech.se.tetris.score;

import seoultech.se.tetris.engine.Difficulty;
import seoultech.se.tetris.engine.GameMode;

/**
 * 스코어보드에 표시하고 저장할 플레이어의 점수 기록
 *
 * <p>2차 요구사항에 따라 모드와 난이도의 기록을 추가하여 저장한다.
 *
 * <p><b>값을 검증하지 않는다.</b> 저장 파일에서 읽은 잘못된 기록도 일단 만들 수 있어야
 * {@link ScoreBoard#load}가 그 기록을 건너뛸지 판단할 수 있기 때문이다.
 * 새 기록은 {@link ScoreBoard#add}로 등록하며, 그때 값을 검증한다.
 */
public record ScoreRecord(String playerName, int score, GameMode mode, Difficulty difficulty) {
}
