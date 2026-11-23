package seoultech.se.tetris.ui;

import java.io.UncheckedIOException;
import java.util.Objects;
import java.util.Random;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import seoultech.se.tetris.ui.end.EndScreenFrame;
import seoultech.se.tetris.ui.game.GameFrame;
import seoultech.se.tetris.ui.menu.StartMenuFrame;
import seoultech.se.tetris.ui.scoreboard.ScoreboardModel;
import seoultech.se.tetris.ui.scoreboard.ScoreboardPanel;
import seoultech.se.tetris.ui.settings.SettingsPanel;
import seoultech.se.tetris.ui.versus.VersusModeSelectModel;
import seoultech.se.tetris.ui.versus.VersusEndFrame;
import seoultech.se.tetris.ui.versus.VersusEndModel;
import seoultech.se.tetris.ui.versus.VersusGameFrame;
import seoultech.se.tetris.ui.versus.VersusModeSelectPanel;
import seoultech.se.tetris.versus.LocalMatch;
import seoultech.se.tetris.versus.MatchResult;
import seoultech.se.tetris.versus.VersusMatch;
import seoultech.se.tetris.versus.VersusMode;
import seoultech.se.tetris.config.AppSettings;
import seoultech.se.tetris.config.PropertiesAppSettingsService;
import seoultech.se.tetris.config.PropertiesBattleKeyConfigService;
import seoultech.se.tetris.config.PropertiesKeyConfigService;
import seoultech.se.tetris.engine.Difficulty;
import seoultech.se.tetris.engine.GameMode;
import seoultech.se.tetris.score.CsvScoreRepository;

/** 앱 진입점. 시작 메뉴, 게임 화면, 종료 화면을 한 번에 하나만 연다. */
public final class TetrisApplication implements ScreenNavigator {

    private StartMenuFrame menu;
    private GameFrame game;
    private VersusGameFrame versusGame;
    private VersusEndFrame versusEnd;
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
        closeVersusGame();
        closeVersusEnd();
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

    /** 로컬 대전 모드 진입. 같은 창에서 대전 모드(일반 / 아이템 / 시간제한) 선택 화면으로 전환한다. */
    @Override
    public void navigateToVersus() {
        menu.showVersusModeSelect(new VersusModeSelectPanel(
                new VersusModeSelectModel(this::navigateToVersusGame, () -> menu.showMenu())));
    }

    /**
     * 선택한 대전 모드로 로컬 대전 시작.
     *
     * <p>설정의 난이도·화면 크기·색맹 모드를 두 플레이어에게 똑같이 적용한다. (3차 팀 결정: 로컬 대전 난이도는 설정의 난이도)
     * 두 플레이어는 같은 seed로 블럭 순서가 같다.
     */
    @Override
    public void navigateToVersusGame(VersusMode mode) {
        Objects.requireNonNull(mode, "mode");
        // 창을 닫기 전에 읽어야, 읽기에 실패해도 화면이 모두 닫힌 채로 남지 않는다.
        AppSettings settings = savedSettings(new PropertiesAppSettingsService());
        PropertiesBattleKeyConfigService battleKeys = new PropertiesBattleKeyConfigService();
        loadOrDefault(battleKeys::load);
        VersusMatch match = LocalMatch.create(mode, settings.difficulty(), new Random().nextLong());
        closeMenu();
        closeEndScreen();
        closeGame();
        closeVersusGame();
        closeVersusEnd();
        versusGame = VersusGameFrame.launch(match, settings.battleScreenSize().scale(), settings.battleColorBlind(),
                battleKeys, this::navigateToMenu, result -> navigateToVersusEnd(mode, result));
    }

    /**
     * P2P 대전 모드 진입.
     *
     * <p>P2P 대전 모드 첫 화면(방 생성 / 참여)은 후속 이슈에서 연결한다.
     * 그때까지는 시작 메뉴를 그대로 유지한다.
     */
    @Override
    public void navigateToP2P() {
        // P2P 대전 모드 첫 화면 연결 전까지는 시작 메뉴를 유지한다
    }

    /**
     * 저장된 난이도. 설정 파일을 읽을 수 없으면 기본 난이도로 시작한다.
     *
     * <p>파일 내용이 손상된 경우는 설정 서비스가 이미 기본값으로 복구하므로,
     * 여기서는 파일 자체를 읽지 못한 경우(입출력 오류)만 처리한다.
     */
    static Difficulty savedDifficulty(PropertiesAppSettingsService settings) {
        return savedSettings(settings).difficulty();
    }

    /** 저장된 설정 전체. 설정 파일을 읽을 수 없으면 기본 설정이다. ({@link #savedDifficulty} 참고) */
    static AppSettings savedSettings(PropertiesAppSettingsService settings) {
        // 읽지 못하면 current()는 기본값(AppSettings.DEFAULT) 그대로다
        loadOrDefault(settings::load);
        return settings.current();
    }

    /** 설정 파일을 읽는다. 파일 자체를 읽지 못하면(입출력 오류) 서비스의 기본값을 그대로 쓴다. */
    private static void loadOrDefault(Runnable load) {
        try {
            load.run();
        } catch (UncheckedIOException e) {
            // 기본값 유지
        }
    }

    @Override
    public void navigateToSettings() {
        PropertiesAppSettingsService settings = new PropertiesAppSettingsService();
        settings.load();
        PropertiesKeyConfigService keys = new PropertiesKeyConfigService();
        keys.load();
        PropertiesBattleKeyConfigService battleKeys = new PropertiesBattleKeyConfigService();
        battleKeys.load();
        menu.showSettings(new SettingsPanel(settings, keys, battleKeys, settings, new CsvScoreRepository(),
                SettingsPanel.Context.START_MENU, () -> {}, () -> {}, () -> menu.showMenu()));
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

    /**
     * 로컬 대전 종료 화면을 연다. 대전 모드는 스코어보드로 기록을 관리하지 않는다.
     * 게임 다시하기는 같은 모드로 새 대전을 시작하며, 난이도와 키 설정은 그 시점의 설정을 다시 읽는다.
     */
    private void navigateToVersusEnd(VersusMode mode, MatchResult result) {
        closeVersusGame();
        closeVersusEnd();
        versusEnd = new VersusEndFrame(new VersusEndModel(mode, result,
                () -> navigateToVersusGame(mode), this::navigateToMenu));
        versusEnd.showScreen();
    }

    private void closeVersusEnd() {
        close(versusEnd);
        versusEnd = null;
    }

    private void closeVersusGame() {
        close(versusGame);
        versusGame = null;
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
