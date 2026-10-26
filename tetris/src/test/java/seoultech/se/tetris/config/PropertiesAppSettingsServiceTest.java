package seoultech.se.tetris.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import seoultech.se.tetris.config.AppSettings.ScreenSize;
import seoultech.se.tetris.engine.Difficulty;

class PropertiesAppSettingsServiceTest {
    @TempDir Path directory;

    /** 세 설정값을 저장하고 새 서비스에서 다시 불러온다. */
    @Test
    void savesAndLoadsAllOptions() {
        Path file = directory.resolve("settings.properties");
        var settings = new PropertiesAppSettingsService(file);
        var selected = new AppSettings(ScreenSize.LARGE, true, Difficulty.HARD);
        settings.save(selected);

        var restored = new PropertiesAppSettingsService(file);
        restored.load();
        assertEquals(selected, restored.current());
    }

    /** 설정 파일이 없거나 값이 손상되면 일괄적으로 기본값을 사용한다. */
    @Test
    void missingOrMalformedFileUsesDefaults() throws Exception {
        Path file = directory.resolve("settings.properties");
        var settings = new PropertiesAppSettingsService(file);
        settings.load();
        assertEquals(AppSettings.DEFAULT, settings.current());

        Files.writeString(file, "screenSize=LARGE\ncolorBlind=maybe\ndifficulty=HARD\n");
        settings.load();
        assertEquals(AppSettings.DEFAULT, settings.current());

        Files.writeString(file, "screenSize=SMALL\ncolorBlind=true\n");
        settings.load();
        assertEquals(AppSettings.DEFAULT, settings.current());
    }

    /** 파일 저장에 실패해도 메모리의 적용된 값은 바꾸지 않는다. */
    @Test
    void failedSaveKeepsCurrentSettings() throws Exception {
        Path file = directory.resolve("settings.properties");
        Files.createDirectory(file);
        var settings = new PropertiesAppSettingsService(file);
        assertThrows(UncheckedIOException.class,
                () -> settings.save(new AppSettings(ScreenSize.SMALL, true, Difficulty.EASY)));
        assertEquals(AppSettings.DEFAULT, settings.current());
    }
}
