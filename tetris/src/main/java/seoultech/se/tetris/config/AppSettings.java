package seoultech.se.tetris.config;

import java.util.List;
import java.util.Objects;
import seoultech.se.tetris.config.P2PSettingsService.RecentPeer;
import seoultech.se.tetris.engine.Difficulty;

/** 게임 화면과 진행 규칙, P2P 접속에 적용할 사용자 설정. */
public record AppSettings(
        ScreenSize screenSize, boolean colorBlind, Difficulty difficulty,
        ScreenSize battleScreenSize, boolean battleColorBlind, Difficulty battleDifficulty,
        int p2pPort, List<RecentPeer> recentPeers) {

    public static final int DEFAULT_P2P_PORT = P2PSettingsService.DEFAULT_PORT;

    public static final AppSettings DEFAULT = new AppSettings(
            ScreenSize.MEDIUM, false, Difficulty.DEFAULT,
            ScreenSize.MEDIUM, false, Difficulty.DEFAULT,
            DEFAULT_P2P_PORT, List.of());

    public AppSettings {
        Objects.requireNonNull(screenSize, "screenSize");
        Objects.requireNonNull(difficulty, "difficulty");
        Objects.requireNonNull(battleScreenSize, "battleScreenSize");
        Objects.requireNonNull(battleDifficulty, "battleDifficulty");
        if (!P2PSettingsService.isValidPort(p2pPort)) {
            throw new IllegalArgumentException("포트 번호가 범위를 벗어났습니다: " + p2pPort);
        }
        recentPeers = P2PSettingsService.normalizeRecentPeers(recentPeers);
    }

    /** 1인 모드 설정만 지정하는 호환용 생성자. 대전 모드와 P2P 설정은 기본값으로 지정한다. */
    public AppSettings(ScreenSize screenSize, boolean colorBlind, Difficulty difficulty) {
        this(screenSize, colorBlind, difficulty,
                DEFAULT.battleScreenSize(), DEFAULT.battleColorBlind(), DEFAULT.battleDifficulty(),
                DEFAULT_P2P_PORT, List.of());
    }

    /** 1인 모드와 대전 모드 설정만 지정하는 호환용 생성자. P2P 설정은 기본값으로 지정한다. */
    public AppSettings(ScreenSize screenSize, boolean colorBlind, Difficulty difficulty,
            ScreenSize battleScreenSize, boolean battleColorBlind, Difficulty battleDifficulty) {
        this(screenSize, colorBlind, difficulty,
                battleScreenSize, battleColorBlind, battleDifficulty,
                DEFAULT_P2P_PORT, List.of());
    }

    /** P2P 서버 포트를 바꾼 새 설정을 돌려준다. */
    public AppSettings withP2pPort(int newP2pPort) {
        return new AppSettings(screenSize, colorBlind, difficulty,
                battleScreenSize, battleColorBlind, battleDifficulty,
                newP2pPort, recentPeers);
    }

    /** 최근 접속 목록을 바꾼 새 설정을 돌려준다. */
    public AppSettings withRecentPeers(List<RecentPeer> newRecentPeers) {
        return new AppSettings(screenSize, colorBlind, difficulty,
                battleScreenSize, battleColorBlind, battleDifficulty,
                p2pPort, newRecentPeers);
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
