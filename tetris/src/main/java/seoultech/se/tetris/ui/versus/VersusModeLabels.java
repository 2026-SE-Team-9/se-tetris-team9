package seoultech.se.tetris.ui.versus;

import java.util.Objects;

import seoultech.se.tetris.versus.VersusMode;

/**
 * 대전 모드의 화면 표시 이름.
 *
 * <p>{@link VersusMode}는 저장 파일에 기록되지 않는 모델 값이므로, 화면에 보여줄 이름은 여기서 정한다.
 */
public final class VersusModeLabels {

    private VersusModeLabels() {
    }

    public static String of(VersusMode mode) {
        Objects.requireNonNull(mode, "mode");
        return switch (mode) {
            case NORMAL -> "일반 모드";
            case ITEM -> "아이템 모드";
            case TIME_LIMIT -> "시간제한 모드";
        };
    }
}
