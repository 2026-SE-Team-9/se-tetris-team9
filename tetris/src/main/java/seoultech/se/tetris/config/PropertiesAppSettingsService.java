package seoultech.se.tetris.config;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.Properties;
import seoultech.se.tetris.config.AppSettings.ScreenSize;
import seoultech.se.tetris.engine.Difficulty;

/** 화면 크기·색맹 모드·난이도를 properties 파일에 저장한다. */
public final class PropertiesAppSettingsService {
    private final Path filePath;
    private AppSettings current = AppSettings.DEFAULT;

    public PropertiesAppSettingsService() {
        this(Path.of(System.getProperty("user.home"), ".tetris", "settings.properties"));
    }

    public PropertiesAppSettingsService(Path filePath) {
        this.filePath = Objects.requireNonNull(filePath, "filePath");
    }

    public AppSettings current() {
        return current;
    }

    /** 누락되거나 잘못된 파일은 기본값으로 복구하고, 읽기 오류는 호출자에게 알린다. */
    public void load() {
        if (Files.notExists(filePath)) {
            current = AppSettings.DEFAULT;
            return;
        }
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(filePath)) {
            properties.load(reader);
            String colorBlind = properties.getProperty("colorBlind");
            if (!"true".equals(colorBlind) && !"false".equals(colorBlind)) {
                throw new IllegalArgumentException("색맹 모드 값이 올바르지 않습니다");
            }
            current = new AppSettings(
                    ScreenSize.valueOf(properties.getProperty("screenSize")),
                    Boolean.parseBoolean(colorBlind),
                    Difficulty.valueOf(properties.getProperty("difficulty")));
        } catch (IllegalArgumentException | NullPointerException e) {
            current = AppSettings.DEFAULT;
        } catch (IOException e) {
            throw new UncheckedIOException("설정을 불러올 수 없습니다: " + filePath, e);
        }
    }

    /** 파일 교체가 성공한 후에만 실행 중 설정을 바꾼다. */
    public void save(AppSettings settings) {
        Objects.requireNonNull(settings, "settings");
        Properties properties = new Properties();
        properties.setProperty("screenSize", settings.screenSize().name());
        properties.setProperty("colorBlind", Boolean.toString(settings.colorBlind()));
        properties.setProperty("difficulty", settings.difficulty().name());

        Path temporaryFile = null;
        try {
            Path target = filePath.toAbsolutePath();
            Path parent = target.getParent();
            Files.createDirectories(parent);
            temporaryFile = Files.createTempFile(parent, ".settings-", ".tmp");
            try (Writer writer = Files.newBufferedWriter(temporaryFile)) {
                properties.store(writer, "Tetris settings");
            }
            Files.move(temporaryFile, target,
                    StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            current = settings;
        } catch (IOException e) {
            if (temporaryFile != null) {
                try {
                    Files.deleteIfExists(temporaryFile);
                } catch (IOException cleanupFailure) {
                    e.addSuppressed(cleanupFailure);
                }
            }
            throw new UncheckedIOException("설정을 저장할 수 없습니다: " + filePath, e);
        }
    }
}
