package seoultech.se.tetris.ui.versus;

import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.swing.JRootPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import seoultech.se.tetris.component.controller.GameController;
import seoultech.se.tetris.component.listener.GameEventListener;
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
 * 로컬 대전 화면의 타이머·일시정지·설정·종료 흐름을 조율한다.
 * {@link JRootPane}을 통해 창 전환 없이(headless 포함) 화면 전환과 일시정지 메뉴를 제어한다.
 */
final class VersusGameSession {

    private static final int CLOCK_TICK_MS = 100;
    private static final int EFFECT_TICK_MS = 33;
    static final int RESULT_DELAY_MS = 1500;

    interface WindowHost {
        void packAndCenter();
        void disposeWindow();
        boolean confirmQuit();
    }

    private final VersusMatch match;
    private final VersusScreenModel model;
    private final VersusGamePanel panel;
    private final JRootPane rootPane;
    private final WindowHost host;
    private final List<GameProgressTimer> progressTimers = new ArrayList<>();
    private final List<ClearAnimationTimer> clearTimers = new ArrayList<>();
    private final List<AttackEffects> attackEffects = new ArrayList<>();
    private final Timer effectTimer;
    private final Runnable onReturnToMenu;
    private final Consumer<MatchResult> onMatchFinished;
    private final Supplier<PropertiesAppSettingsService> settingsFactory;
    private final Timer resultTimer;
    private final TimeLimitWatcher timeLimit;
    private final Timer clockTimer;
    private final VersusKeyInput keyInput;
    private final boolean[] effectsWereActive = new boolean[Side.values().length];
    private MatchResult finishedResult;
    private boolean confirmingQuit;
    private boolean released;

    VersusGameSession(VersusMatch match, double scale, boolean colorBlind,
            BattleKeyConfigService keyConfig, JRootPane rootPane, WindowHost host,
            Runnable onReturnToMenu, Consumer<MatchResult> onMatchFinished,
            Supplier<PropertiesAppSettingsService> settingsFactory) {
        this.match = Objects.requireNonNull(match, "match");
        this.rootPane = Objects.requireNonNull(rootPane, "rootPane");
        this.host = Objects.requireNonNull(host, "host");
        this.onReturnToMenu = Objects.requireNonNull(onReturnToMenu, "onReturnToMenu");
        this.onMatchFinished = Objects.requireNonNull(onMatchFinished, "onMatchFinished");
        this.settingsFactory = Objects.requireNonNull(settingsFactory, "settingsFactory");
        Objects.requireNonNull(keyConfig, "keyConfig");

        this.resultTimer = new Timer(RESULT_DELAY_MS, event -> showResult());
        this.resultTimer.setRepeats(false);
        this.timeLimit = match.mode().timeLimit()
                .map(limit -> new TimeLimitWatcher(match, CountdownClock.realTime(limit)))
                .orElse(null);
        this.model = new VersusScreenModel(match.mode(), timeLimit == null ? null : timeLimit.clock());
        this.panel = new VersusGamePanel(model,
                new GamePanel(match.state(Side.P1), scale, colorBlind),
                new GamePanel(match.state(Side.P2), scale, colorBlind));

        rootPane.setContentPane(panel);

        for (Side side : Side.values()) {
            connectPlayer(side);
        }
        effectTimer = new Timer(EFFECT_TICK_MS, event -> repaintEffects());
        if (timeLimit != null) {
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
                stopTimers();
                attackEffects.forEach(AttackEffects::clear);
                finishedResult = result;
                model.matchEnded(result);
                panel.refresh();
                resultTimer.start();
            }
        });

        rootPane.setGlassPane(new PauseMenuPanel(this::resumeFromPause, this::showSettings, this::confirmReturnToMenu));
        this.keyInput = new VersusKeyInput(new VersusKeyMap(keyConfig), match::controller, model,
                this::pauseAndShowMenu, this::showResult);
        panel.addKeyListener(keyInput);
    }

    void start() {
        panel.requestFocusInWindow();
        match.begin();
        progressTimers.forEach(GameProgressTimer::start);
        effectTimer.start();
        if (clockTimer != null) {
            clockTimer.start();
        }
    }

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

        AttackEffects effects = AttackEffects.realTime();
        attackEffects.add(effects);
        board.setEffects(effects);
        match.addGameListener(side, new AttackEffectWatcher(match.state(side), effects));
    }

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

    void pauseAndShowMenu() {
        if (released || match.phase() != MatchPhase.PLAYING) {
            return;
        }
        match.pause();
        pauseTimers();
        panel.repaint();
        rootPane.getGlassPane().setVisible(true);
        rootPane.getGlassPane().requestFocusInWindow();
    }

    void resumeFromPause() {
        if (released || match.phase() != MatchPhase.PAUSED) {
            return;
        }
        rootPane.getGlassPane().setVisible(false);
        match.resume();
        resumeTimers();
        panel.requestFocusInWindow();
    }

    void showSettings() {
        if (released) {
            return;
        }
        PropertiesAppSettingsService settings = settingsFactory.get();
        loadOrKeep(settings::load);
        PropertiesKeyConfigService keyConfig = new PropertiesKeyConfigService();
        loadOrKeep(keyConfig::load);
        PropertiesBattleKeyConfigService battleKeys = new PropertiesBattleKeyConfigService();
        loadOrKeep(battleKeys::load);

        SettingsPanel settingsPanel = new SettingsPanel(settings, keyConfig, battleKeys, new CsvScoreRepository(),
                SettingsPanel.Context.LOCAL_BATTLE_IN_GAME, () -> {}, () -> keyInput.applyKeyBindings(battleKeys),
                () -> {
                    double updatedScale = settings.current().battleScreenSize().scale();
                    boolean updatedColorBlind = settings.current().battleColorBlind();
                    for (Side side : Side.values()) {
                        panel.board(side).setScale(updatedScale);
                        panel.board(side).setColorBlind(updatedColorBlind);
                    }
                    rootPane.setContentPane(panel);
                    host.packAndCenter();
                    rootPane.getGlassPane().setVisible(true);
                    rootPane.getGlassPane().requestFocusInWindow();
                });
        rootPane.getGlassPane().setVisible(false);
        rootPane.setContentPane(settingsPanel);
        host.packAndCenter();
    }

    private void confirmReturnToMenu() {
        if (released) {
            return;
        }
        confirmingQuit = true;
        boolean confirmed;
        try {
            confirmed = host.confirmQuit();
        } finally {
            confirmingQuit = false;
        }
        if (confirmed) {
            returnToMenu();
        } else if (finishedResult != null) {
            showResult();
        } else {
            rootPane.getGlassPane().requestFocusInWindow();
        }
    }

    void returnToMenu() {
        if (released) {
            return;
        }
        release();
        host.disposeWindow();
        SwingUtilities.invokeLater(onReturnToMenu);
    }

    void showResult() {
        MatchResult result = finishedResult;
        if (released || result == null || confirmingQuit) {
            return;
        }
        release();
        host.disposeWindow();
        SwingUtilities.invokeLater(() -> onMatchFinished.accept(result));
    }

    private void pauseTimers() {
        progressTimers.forEach(GameProgressTimer::stop);
        clearTimers.forEach(ClearAnimationTimer::pause);
        attackEffects.forEach(AttackEffects::pause);
        effectTimer.stop();
        if (clockTimer != null) {
            clockTimer.stop();
        }
    }

    private void resumeTimers() {
        progressTimers.forEach(GameProgressTimer::start);
        clearTimers.forEach(ClearAnimationTimer::resume);
        attackEffects.forEach(AttackEffects::resume);
        effectTimer.start();
        if (clockTimer != null) {
            clockTimer.start();
        }
    }

    private void stopTimers() {
        progressTimers.forEach(GameProgressTimer::stop);
        clearTimers.forEach(ClearAnimationTimer::stop);
        effectTimer.stop();
        if (clockTimer != null) {
            clockTimer.stop();
        }
    }

    private static void loadOrKeep(Runnable loader) {
        try {
            loader.run();
        } catch (UncheckedIOException e) {
            // 읽지 못하면 기본 설정으로 계속한다
        }
    }

    void release() {
        if (released) {
            return;
        }
        released = true;
        stopTimers();
        resultTimer.stop();
        match.quit();
    }

    VersusGamePanel panel() {
        return panel;
    }

    boolean isEffectTimerRunning() {
        return effectTimer.isRunning();
    }

    boolean isClockTimerRunning() {
        return clockTimer != null && clockTimer.isRunning();
    }
}
