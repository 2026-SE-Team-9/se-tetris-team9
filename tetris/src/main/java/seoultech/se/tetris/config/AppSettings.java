package seoultech.se.tetris.config;

import java.util.Objects;
import seoultech.se.tetris.engine.Difficulty;

/** 게임 화면과 진행 규칙에 적용할 사용자 설정. 1인 모드와 대전 모드 설정을 각각 분리하여 관리한다. */
public record AppSettings(
        ScreenSize screenSize, boolean colorBlind, Difficulty difficulty,
        ScreenSize battleScreenSize, boolean battleColorBlind, Difficulty battleDifficulty) {

    public static final AppSettings DEFAULT = new AppSettings(
            ScreenSize.MEDIUM, false, Difficulty.DEFAULT,
            ScreenSize.MEDIUM, false, Difficulty.DEFAULT);

    public AppSettings {
        Objects.requireNonNull(screenSize, "screenSize");
        Objects.requireNonNull(difficulty, "difficulty");
        Objects.requireNonNull(battleScreenSize, "battleScreenSize");
        Objects.requireNonNull(battleDifficulty, "battleDifficulty");
    }

    /** 1인 모드 설정만 지정하는 호환용 생성자. 대전 모드 설정은 기본값으로 지정한다. */
    public AppSettings(ScreenSize screenSize, boolean colorBlind, Difficulty difficulty) {
        this(screenSize, colorBlind, difficulty,
                DEFAULT.battleScreenSize(), DEFAULT.battleColorBlind(), DEFAULT.battleDifficulty());
    }

    /** 설정 화면에서 선택할 수 있는 세 가지 화면 크기. */
    public enum ScreenSize {
        SMALL("작게", 0.8), MEDIUM("보통", 1.0), LARGE("크게", 1.3);

        private final String label;
        private final double scale;

        ScreenSize(String label, double scale) {
            this.label = label;
            this.scale = scale;
        }

        /** 게임 화면의 기본 크기에 곱하는 배율. 보통은 기본 크기 그대로다. */
        public double scale() {
            return scale;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
