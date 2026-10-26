package seoultech.se.tetris.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import seoultech.se.tetris.config.AppSettings;
import seoultech.se.tetris.config.AppSettings.ScreenSize;
import seoultech.se.tetris.config.PropertiesAppSettingsService;
import seoultech.se.tetris.engine.Difficulty;

@DisplayName("TetrisApplication")
class TetrisApplicationTest {

    @TempDir
    Path directory;

    @Nested
    @DisplayName("게임 시작 난이도")
    class SavedDifficulty {

        @Test
        @DisplayName("저장된 난이도로 시작한다")
        void usesSavedDifficulty() {
            Path file = directory.resolve("settings.properties");
            new PropertiesAppSettingsService(file).save(new AppSettings(ScreenSize.MEDIUM, false, Difficulty.HARD));

            assertEquals(Difficulty.HARD,
                    TetrisApplication.savedDifficulty(new PropertiesAppSettingsService(file)));
        }

        @Test
        @DisplayName("설정 파일이 없으면 기본 난이도로 시작한다")
        void usesDefaultDifficultyWithoutSettingsFile() {
            Path file = directory.resolve("settings.properties");

            assertEquals(Difficulty.DEFAULT,
                    TetrisApplication.savedDifficulty(new PropertiesAppSettingsService(file)));
        }

        @Test
        @DisplayName("설정 파일을 읽을 수 없으면 예외 없이 기본 난이도로 시작한다")
        void usesDefaultDifficultyWhenSettingsCannotBeRead() throws IOException {
            // 같은 이름의 폴더를 두어 파일 읽기가 실패하도록 한다
            Path file = Files.createDirectory(directory.resolve("settings.properties"));

            assertEquals(Difficulty.DEFAULT,
                    TetrisApplication.savedDifficulty(new PropertiesAppSettingsService(file)));
        }
    }
}
