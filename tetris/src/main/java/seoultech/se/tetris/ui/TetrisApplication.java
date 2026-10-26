package seoultech.se.tetris.ui;

import java.io.UncheckedIOException;
import java.util.Objects;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import seoultech.se.tetris.ui.end.EndScreenFrame;
import seoultech.se.tetris.ui.game.GameFrame;
import seoultech.se.tetris.ui.menu.StartMenuFrame;
import seoultech.se.tetris.ui.scoreboard.ScoreboardModel;
import seoultech.se.tetris.ui.scoreboard.ScoreboardPanel;
import seoultech.se.tetris.ui.settings.SettingsPanel;
import seoultech.se.tetris.config.PropertiesAppSettingsService;
import seoultech.se.tetris.config.PropertiesKeyConfigService;
import seoultech.se.tetris.engine.Difficulty;
import seoultech.se.tetris.engine.GameMode;
import seoultech.se.tetris.score.CsvScoreRepository;

/** 앱 진입점. 시작 메뉴, 게임 화면, 종료 화면을 한 번에 하나만 연다. */
public final class TetrisApplication implements ScreenNavigator {

    private StartMenuFrame menu;
    private GameFrame game;
    private EndScreenFrame endScreen;
    private Difficulty currentDifficulty = Difficulty.DEFAULT;
    private GameMode currentMode = GameMode.NORMAL;

    private TetrisApplication() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TetrisApplication().navigateToMenu());
    }

    @Override
    public void navigateToMenu() {
        closeGame();
        closeEndScreen();
        closeMenu();
        menu = new StartMenuFrame(this);
        menu.showMenu();
    }

    @Override
    public void navigateToGame(GameMode mode) {
        // 창을 닫기 전에 읽어야, 읽기에 실패해도 화면이 모두 닫힌 채로 남지 않는다.
        currentDifficulty = savedDifficulty(new PropertiesAppSettingsService());
        currentMode = Objects.requireNonNull(mode, "mode");
        closeMenu();
        closeEndScreen();
        closeGame();
        game = GameFrame.launch(currentDifficulty, currentMode, this::navigateToGameOver, this::navigateToMenu);
    }

    /**
     * 저장된 난이도. 설정 파일을 읽을 수 없으면 기본 난이도로 시작한다.
     *
     * <p>파일 내용이 손상된 경우는 설정 서비스가 이미 기본값으로 복구하므로,
     * 여기서는 파일 자체를 읽지 못한 경우(입출력 오류)만 처리한다.
     */
    static Difficulty savedDifficulty(PropertiesAppSettingsService settings) {
        try {
            settings.load();
        } catch (UncheckedIOException e) {
            // 읽지 못하면 current()는 기본값(AppSettings.DEFAULT) 그대로다
        }
        return settings.current().difficulty();
    }

    @Override
    public void navigateToSettings() {
        PropertiesAppSettingsService settings = new PropertiesAppSettingsService();
        settings.load();
        PropertiesKeyConfigService keys = new PropertiesKeyConfigService();
        keys.load();
        menu.showSettings(new SettingsPanel(settings, keys, new CsvScoreRepository(),
                SettingsPanel.Context.START_MENU, () -> {}, () -> menu.showMenu()));
    }

    @Override
    public void navigateToScoreboard() {
        menu.showScoreboard(new ScoreboardPanel(
                new ScoreboardModel(new CsvScoreRepository(), () -> menu.showMenu())));
    }

    @Override
    public void navigateToGameOver(int finalScore) {
        closeGame();
        closeEndScreen();
        // 시작할 때 고른 모드와 난이도로 기록한다. 저장소는 판마다 새로 만들어 파일에서 다시 읽는다.
        endScreen = new EndScreenFrame(this, finalScore, currentMode,
                currentDifficulty, new CsvScoreRepository());
        endScreen.showScreen();
    }

    /** 열려 있는 창을 닫는다. 이미 닫혀 있으면 아무 것도 하지 않는다. */
    private static void close(JFrame frame) {
        if (frame != null) {
            frame.dispose();
        }
    }

    private void closeMenu() {
        close(menu);
        menu = null;
    }

    private void closeGame() {
        close(game);
        game = null;
    }

    private void closeEndScreen() {
        close(endScreen);
        endScreen = null;
    }

    @Override
    public void exitProgram() {
        System.exit(0);
    }
}
