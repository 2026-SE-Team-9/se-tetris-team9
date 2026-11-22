package seoultech.se.tetris.ui.versus;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import seoultech.se.tetris.component.controller.GameController;
import seoultech.se.tetris.component.listener.GameEventListener;
import seoultech.se.tetris.config.AppSettings;
import seoultech.se.tetris.config.BattleKeyConfigService;
import seoultech.se.tetris.config.PropertiesAppSettingsService;
import seoultech.se.tetris.config.PropertiesBattleKeyConfigService;
import seoultech.se.tetris.config.PropertiesKeyConfigService;
import seoultech.se.tetris.score.CsvScoreRepository;
import seoultech.se.tetris.ui.game.ClearAnimationTimer;
import seoultech.se.tetris.ui.game.GamePanel;
import seoultech.se.tetris.ui.game.GameProgressTimer;
import seoultech.se.tetris.ui.game.PauseMenuPanel;
import seoultech.se.tetris.ui.settings.SettingsPanel;
import seoultech.se.tetris.versus.MatchListener;
import seoultech.se.tetris.versus.MatchPhase;
import seoultech.se.tetris.versus.MatchResult;
import seoultech.se.tetris.versus.Side;
import seoultech.se.tetris.versus.VersusMatch;

/**
 * 로컬 대전 창. 대전 모델({@link VersusMatch})과 두 보드 화면, 플레이어별 낙하·줄 삭제 애니메이션 타이머,
 * 2인 키 입력을 연결한다.
 *
 * <p>각 플레이어의 진행은 1인 모드와 같은 타이머({@link GameProgressTimer}, {@link ClearAnimationTimer})를
 * 플레이어마다 하나씩 두어 처리한다. 타이머는 대전의 조작기를 거쳐 엔진을 움직이므로,
 * 대전이 진행 중이 아닐 때의 입력은 대전 모델이 걸러낸다.
 *
 * <p>줄 공격 애니메이션({@link AttackEffects})은 플레이어마다 두고, 진행 중인 동안에만 짧은 주기로 보드를 다시 그린다.
 *
 * <p>시간제한 모드에서는 남은 시간({@link TimeLimitWatcher})을 짧은 주기로 확인해 화면에 표시하고,
 * 시간이 다 되면 대전에 시간 종료를 알린다. 일시정지 중에는 남은 시간도 멈춘다.
 *
 * <p>Esc를 누르면 두 보드를 멈추고 1인 모드와 같은 일시정지 메뉴(재개 / 설정 / 시작 메뉴로 돌아가기)를 연다.
 * 설정은 로컬 대전 전용 설정 화면({@link SettingsPanel.Context#LOCAL_BATTLE_IN_GAME})이며,
 * 저장하면 대전용 화면 크기·색맹 모드를 두 보드에, 2인 키를 입력에 바로 적용한다. (3차 팀 결정)
 */
public final class VersusGameFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    /** 시간제한 모드에서 남은 시간을 확인하고 다시 그리는 주기(ms). 1초보다 짧게 두어 표시가 늦지 않게 한다. */
    private static final int CLOCK_TICK_MS = 100;
    /** 줄 공격 애니메이션을 다시 그리는 주기(ms). 약 30프레임 */
    private static final int EFFECT_TICK_MS = 33;
    /** 대전이 끝난 뒤 두 보드의 승패를 보여 주는 시간(ms). 이후 대전 종료 화면으로 넘어간다 */
    static final int RESULT_DELAY_MS = 1500;

    private final transient VersusMatch match;
    private final transient VersusScreenModel model;
    private final VersusGamePanel panel;
    private final transient List<GameProgressTimer> progressTimers = new ArrayList<>();
    private final transient List<ClearAnimationTimer> clearTimers = new ArrayList<>();
    private final transient List<AttackEffects> attackEffects = new ArrayList<>();
    /** 줄 공격 애니메이션이 진행 중인 동안 보드를 다시 그린다 */
    private final Timer effectTimer;
    private final transient Runnable onReturnToMenu;
    private final transient Consumer<MatchResult> onMatchFinished;
    /** 대전이 끝나 승패를 잠시 보여 준 뒤 종료 화면으로 넘어가는 타이머 */
    private final Timer resultTimer;
    /** 결과가 정해져 끝났을 때의 결과. 끝나지 않았으면 {@code null} */
    private transient MatchResult finishedResult;
    /** 일시정지 메뉴, 설정 화면, 나가기 확인 창 중 하나가 열려 있는지 */
    private boolean pausedUi;
    private final transient VersusKeyInput keyInput;
    private final PauseMenuPanel pauseMenu;
    /** 시간제한 모드가 아니면 {@code null} */
    private final transient TimeLimitWatcher timeLimit;
    /** 시간제한 모드가 아니면 {@code null} */
    private final Timer clockTimer;
    private final boolean[] effectsWereActive = new boolean[Side.values().length];
    private boolean released;

    private VersusGameFrame(VersusMatch match, double scale, boolean colorBlind,
            BattleKeyConfigService keyConfig, Runnable onReturnToMenu, Consumer<MatchResult> onMatchFinished) {
        super("SeoulTech SE Tetris");
        this.match = match;
        this.onReturnToMenu = onReturnToMenu;
        this.onMatchFinished = onMatchFinished;
        this.resultTimer = new Timer(RESULT_DELAY_MS, event -> showResult());
        resultTimer.setRepeats(false);
        this.timeLimit = match.mode().timeLimit()
                .map(limit -> new TimeLimitWatcher(match, CountdownClock.realTime(limit)))
                .orElse(null);
        this.model = new VersusScreenModel(match.mode(), timeLimit == null ? null : timeLimit.clock());
        this.panel = new VersusGamePanel(model,
                new GamePanel(match.state(Side.P1), scale, colorBlind),
                new GamePanel(match.state(Side.P2), scale, colorBlind));

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setContentPane(panel);
        pack();
        setLocationRelativeTo(null);
        setResizable(false);

        for (Side side : Side.values()) {
            connectPlayer(side);
        }
        effectTimer = new Timer(EFFECT_TICK_MS, event -> repaintEffects());
        if (timeLimit != null) {
            // 대전 시작(begin) 전에 등록해야 시작 시점부터 잰다
            match.addListener(timeLimit);
            clockTimer = new Timer(CLOCK_TICK_MS, event -> {
                timeLimit.tick();
                panel.repaintTimer();
            });
        } else {
            clockTimer = null;
        }
        match.addListener(new MatchListener() {
            @Override
            public void onPhaseChanged(MatchPhase phase) {
                panel.repaint();
            }

            @Override
            public void onMatchEnded(MatchResult result) {
                // 한쪽이 끝나면 두 보드를 모두 멈추고 승패를 표시한다. (3차 팀 결정)
                stopTimers();
                attackEffects.forEach(AttackEffects::clear);
                finishedResult = result;
                model.matchEnded(result);
                panel.refresh();
                // 두 보드의 승패를 잠시 보여 준 뒤 대전 종료 화면으로 넘어간다
                resultTimer.start();
            }
        });

        keyInput = new VersusKeyInput(new VersusKeyMap(keyConfig), match::controller, model,
                this::openPauseMenu, this::showResult);
        panel.addKeyListener(keyInput);
        pauseMenu = new PauseMenuPanel(this::resumeFromPause, this::showSettings,
                "시작 메뉴로 돌아가기", this::confirmLeave);
        setGlassPane(pauseMenu);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent event) { release(); }
            @Override public void windowClosed(WindowEvent event) { release(); }
        });
    }

    /**
     * 대전 창을 열고 대전을 시작한다.
     *
     * @param scale          설정의 화면 크기 배율. 두 보드에 똑같이 적용한다
     * @param colorBlind     설정의 색맹 모드. 두 보드에 똑같이 적용한다
     * @param keyConfig      로컬 대전용 2인 키 설정
     * @param onReturnToMenu  대전 도중 나가기로 창을 닫은 뒤 시작 메뉴로 돌아가는 동작
     * @param onMatchFinished 결과가 정해져 끝난 대전의 창을 닫은 뒤 대전 종료 화면을 여는 동작
     */
    public static VersusGameFrame launch(VersusMatch match, double scale, boolean colorBlind,
            BattleKeyConfigService keyConfig, Runnable onReturnToMenu, Consumer<MatchResult> onMatchFinished) {
        Objects.requireNonNull(match, "match");
        Objects.requireNonNull(keyConfig, "keyConfig");
        Objects.requireNonNull(onReturnToMenu, "onReturnToMenu");
        Objects.requireNonNull(onMatchFinished, "onMatchFinished");
        VersusGameFrame frame = new VersusGameFrame(match, scale, colorBlind, keyConfig, onReturnToMenu,
                onMatchFinished);
        frame.setVisible(true);
        frame.panel.requestFocusInWindow();
        match.begin();
        frame.progressTimers.forEach(GameProgressTimer::start);
        frame.effectTimer.start();
        if (frame.clockTimer != null) {
            frame.clockTimer.start();
        }
        return frame;
    }

    /** 한 플레이어의 보드 다시 그리기, 자동 낙하, 줄 삭제 애니메이션을 연결한다. */
    private void connectPlayer(Side side) {
        GameController controller = match.controller(side)
                .orElseThrow(() -> new IllegalStateException("로컬 대전은 두 플레이어를 모두 조작해야 합니다: " + side));
        GamePanel board = panel.board(side);

        GameProgressTimer progress = new GameProgressTimer(match.state(side), controller);
        ClearAnimationTimer clearing = new ClearAnimationTimer(controller);
        progressTimers.add(progress);
        clearTimers.add(clearing);
        match.addGameListener(side, progress);
        match.addGameListener(side, clearing);
        match.addGameListener(side, new GameEventListener() {
            @Override public void onBoardUpdated() { board.repaint(); }
            @Override public void onLinesCleared(int clearedCount, int gainedScore) { }
            @Override public void onSpeedIncreased(int currentInterval) { }
            @Override public void onGameOver(int finalScore) { board.repaint(); }
        });

        // 줄 공격 애니메이션: 공격을 보내고 받는 것과 대기 줄이 보드에 추가되는 것을 보드에 표시한다
        AttackEffects effects = AttackEffects.realTime();
        attackEffects.add(effects);
        board.setEffects(effects);
        match.addGameListener(side, new AttackEffectWatcher(match.state(side), effects));
    }

    /**
     * 애니메이션이 진행 중인 보드를 다시 그린다. 애니메이션이 끝난 직후에도 한 번 더 그려야 마지막 모습이
     * 남지 않으므로, 진행 중이었던 보드도 다시 그린다.
     */
    private void repaintEffects() {
        for (Side side : Side.values()) {
            GamePanel board = panel.board(side);
            boolean active = board.getEffects().isActive();
            if (active || effectsWereActive[side.ordinal()]) {
                board.repaint();
            }
            effectsWereActive[side.ordinal()] = active;
        }
    }

    /** 대전 중 Esc. 두 보드를 멈추고 일시정지 메뉴를 연다. */
    private void openPauseMenu() {
        if (released || finishedResult != null) {
            return;
        }
        match.pause();
        pausedUi = true;
        showPauseMenu();
    }

    private void showPauseMenu() {
        getGlassPane().setVisible(true);
        pauseMenu.requestFocusInWindow();
        panel.repaint();
    }

    /**
     * 일시정지 메뉴의 재개(또는 Esc). 두 보드를 이어서 진행한다.
     * 일시정지 중에 대전이 끝났으면(멈추기 전의 줄 삭제 처리로 끝난 경우) 결과로 넘어간다.
     */
    private void resumeFromPause() {
        getGlassPane().setVisible(false);
        pausedUi = false;
        if (finishedResult != null) {
            showResult();
            return;
        }
        match.resume();
        panel.requestFocusInWindow();
    }

    /** 일시정지 메뉴의 시작 메뉴로 돌아가기. 확인을 받으면 대전을 끝내고 시작 메뉴로 돌아간다. */
    private void confirmLeave() {
        int choice = JOptionPane.showConfirmDialog(this,
                "현재 대전 진행 상황이 사라집니다. 시작 메뉴로 돌아가시겠습니까?",
                "대전 나가기", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice == JOptionPane.YES_OPTION) {
            returnToMenu();
        } else {
            pauseMenu.requestFocusInWindow();
        }
    }

    /** 일시정지 메뉴의 설정. 로컬 대전 전용 설정 화면으로 바꾼다. */
    private void showSettings() {
        PropertiesAppSettingsService settings = new PropertiesAppSettingsService();
        loadOrKeepDefaults(settings::load);
        PropertiesKeyConfigService keys = new PropertiesKeyConfigService();
        loadOrKeepDefaults(keys::load);
        PropertiesBattleKeyConfigService battleKeys = new PropertiesBattleKeyConfigService();
        loadOrKeepDefaults(battleKeys::load);
        SettingsPanel settingsPanel = new SettingsPanel(settings, keys, battleKeys, new CsvScoreRepository(),
                SettingsPanel.Context.LOCAL_BATTLE_IN_GAME, () -> { },
                () -> keyInput.setKeyMap(new VersusKeyMap(battleKeys)),
                () -> closeSettings(settings));
        getGlassPane().setVisible(false);
        setContentPane(settingsPanel);
        pack();
        setLocationRelativeTo(null);
    }

    /** 설정 화면을 닫고 일시정지 메뉴로 돌아간다. 저장했다면 바뀐 대전용 화면 크기와 색맹 모드를 두 보드에 적용한다. */
    private void closeSettings(PropertiesAppSettingsService settings) {
        AppSettings current = settings.current();
        for (Side side : Side.values()) {
            GamePanel board = panel.board(side);
            board.setScale(current.battleScreenSize().scale());
            board.setColorBlind(current.battleColorBlind());
        }
        setContentPane(panel);
        pack();
        setLocationRelativeTo(null);
        showPauseMenu();
    }

    /** 설정 파일을 읽지 못하면 기본값으로 둔다. */
    private static void loadOrKeepDefaults(Runnable load) {
        try {
            load.run();
        } catch (UncheckedIOException e) {
            // 기본값으로 계속 진행한다
        }
    }

    /** 대전을 정리하고 창을 닫은 뒤 시작 메뉴로 돌아간다. */
    private void returnToMenu() {
        if (released) {
            return;
        }
        release();
        dispose();
        SwingUtilities.invokeLater(onReturnToMenu);
    }

    /** 대전을 정리하고 창을 닫은 뒤 대전 종료 화면으로 넘어간다. 결과가 정해지지 않았으면 아무것도 하지 않는다. */
    private void showResult() {
        MatchResult result = finishedResult;
        if (released || result == null || pausedUi) {
            // 일시정지 화면이 열려 있으면 재개할 때 resumeFromPause에서 처리한다
            return;
        }
        release();
        dispose();
        SwingUtilities.invokeLater(() -> onMatchFinished.accept(result));
    }

    private void stopTimers() {
        progressTimers.forEach(GameProgressTimer::stop);
        clearTimers.forEach(ClearAnimationTimer::stop);
        effectTimer.stop();
        if (clockTimer != null) {
            clockTimer.stop();
        }
    }

    /** 타이머를 멈추고 대전을 끝낸다. 여러 번 호출해도 한 번만 처리한다. */
    private void release() {
        if (released) {
            return;
        }
        released = true;
        stopTimers();
        resultTimer.stop();
        match.quit();
    }

    // 테스트에서 창을 띄우지 않고 화면 구성을 확인한다.
    VersusGamePanel panel() {
        return panel;
    }
}
