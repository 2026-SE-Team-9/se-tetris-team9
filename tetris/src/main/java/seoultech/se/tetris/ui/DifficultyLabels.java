package seoultech.se.tetris.ui;

import java.util.Objects;

import seoultech.se.tetris.engine.Difficulty;

/**
 * 난이도를 화면에 보여줄 한글 이름.
 *
 * <p>{@link Difficulty}의 상수 이름은 저장 파일에 그대로 기록되므로 화면 표시는 여기서 따로 정한다.
 * 설정 화면과 스코어보드가 같은 이름을 쓰도록 한 곳에 둔다.
 */
public final class DifficultyLabels {

    private DifficultyLabels() {
    }

    public static String of(Difficulty difficulty) {
        Objects.requireNonNull(difficulty, "difficulty");
        return switch (difficulty) {
            case EASY -> "쉬움";
            case NORMAL -> "중간";
            case HARD -> "어려움";
        };
    }
}
