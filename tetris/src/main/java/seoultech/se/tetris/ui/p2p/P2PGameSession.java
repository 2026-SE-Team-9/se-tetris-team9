package seoultech.se.tetris.ui.p2p;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.LongSupplier;

import javax.swing.JRootPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import seoultech.se.tetris.component.controller.GameController;
import seoultech.se.tetris.component.listener.GameEventListener;
import seoultech.se.tetris.component.model.GameState;
import seoultech.se.tetris.config.KeyConfigService;
import seoultech.se.tetris.config.KeyConfigService.GameAction;
import seoultech.se.tetris.ui.game.ClearAnimationTimer;
import seoultech.se.tetris.ui.game.GamePanel;
import seoultech.se.tetris.ui.game.GameProgressTimer;
import seoultech.se.tetris.ui.versus.AttackEffectWatcher;
import seoultech.se.tetris.ui.versus.AttackEffects;
import seoultech.se.tetris.ui.versus.CountdownClock;
import seoultech.se.tetris.ui.versus.TimeLimitWatcher;
import seoultech.se.tetris.ui.versus.VersusPresentation;
import seoultech.se.tetris.ui.versus.VersusScreenModel;
import seoultech.se.tetris.versus.MatchListener;
import seoultech.se.tetris.versus.MatchPhase;
import seoultech.se.tetris.versus.MatchResult;
import seoultech.se.tetris.versus.Side;
import seoultech.se.tetris.versus.VersusMatch;
import seoultech.se.tetris.versus.p2p.CloseReason;
import seoultech.se.tetris.versus.p2p.ConnectionState;
import seoultech.se.tetris.versus.p2p.P2PSession;

/**
 * P2P 대전 화면의 타이머·키 입력·나가기 메뉴·연결 상태·종료 흐름을 조율한다.
 */
final class P2PGameSession {

    private static final int CLOCK_TICK_MS = 100;
    private static final int COUNTDOWN_TICK_MS = 50;
    private static final int EFFECT_TICK_MS = 33;
    static final int RESULT_DELAY_MS = 1500;

    interface WindowHost {
        void disposeWindow();
    }

    private final P2PSession session;
    private final VersusMatch match;
    private final Side mySide;
    private final VersusScreenModel model;
    private final P2PMatchPanel matchPanel;
    private final P2PExitMenuPanel exitMenu;
    private final JRootPane rootPane;
    private final WindowHost host;
    private final Runnable onReturn;

    private final GameProgressTimer progressTimer;
    private final ClearAnimationTimer clearTimer;
    private final List<AttackEffects> attackEffects = new ArrayList<>();
    private final Timer effectTimer;
    private final Timer resultTimer;
    private final TimeLimitWatcher timeLimit;
    private final Timer clockTimer;
    private final CountdownClock startCountdownClock;
    private final Timer countdownTimer;
    private final boolean[] effectsWereActive = new boolean[Side.values().length];
    private final Map<Integer, GameAction> actionsByKey;

    private final P2PSession.SessionListener sessionListener = new P2PSession.SessionListener() {
        @Override
        public void onMatchStarted(VersusMatch started) {
        }

        @Override
        public void onConnectionChanged(ConnectionState state, int roundTripMillis) {
            if (!released) {
                matchPanel.setConnection(state, roundTripMillis);
            }
        }

        @Override
        public void onClosed(CloseReason reason) {
            if (!released && match.phase() != MatchPhase.ENDED) {
                abortWindow();
            }
        }
    };

    private boolean released;

    P2PGameSession(P2PSession session, VersusMatch match, double scale, boolean colorBlind,
            KeyConfigService keyConfig, JRootPane rootPane, WindowHost host, Runnable onReturn) {
        this(session, match, scale, colorBlind, keyConfig, rootPane, host, onReturn,
                VersusMatch.COUNTDOWN, () -> System.nanoTime() / 1_000_000L);
    }

    P2PGameSession(P2PSession session, VersusMatch match, double scale, boolean colorBlind,
            KeyConfigService keyConfig, JRootPane rootPane, WindowHost host, Runnable onReturn,
            Duration countdown, LongSupplier nowMillis) {
        this.session = Objects.requireNonNull(session, "session");
        this.match = Objects.requireNonNull(match, "match");
        this.mySide = session.role().side();
        this.rootPane = Objects.requireNonNull(rootPane, "rootPane");
        this.host = Objects.requireNonNull(host, "host");
        this.onReturn = Objects.requireNonNull(onReturn, "onReturn");
        Objects.requireNonNull(keyConfig, "keyConfig");
        Objects.requireNonNull(countdown, "countdown");
        Objects.requireNonNull(nowMillis, "nowMillis");

        this.actionsByKey = buildKeyMap(keyConfig);
        this.resultTimer = new Timer(RESULT_DELAY_MS, event -> finishToReturn());
        this.resultTimer.setRepeats(false);

        this.timeLimit = match.mode().timeLimit()
                .map(limit -> new TimeLimitWatcher(match, CountdownClock.realTime(limit)))
                .orElse(null);
        this.model = new VersusScreenModel(match.mode(),
                timeLimit == null ? null : timeLimit.clock(),
                VersusPresentation.p2p(session.role()));
        this.matchPanel = P2PMatchPanel.create(model,
                new GamePanel(match.state(Side.P1), scale, colorBlind),
                new GamePanel(match.state(Side.P2), scale, colorBlind));
        this.matchPanel.setConnection(session.connectionState(), session.roundTripMillis());
        this.matchPanel.setFocusable(true);

        if (!countdown.isZero()) {
            this.startCountdownClock = new CountdownClock(countdown, nowMillis);
            this.countdownTimer = new Timer(COUNTDOWN_TICK_MS, event -> tickCountdown());
        } else {
            this.startCountdownClock = null;
            this.countdownTimer = null;
        }

        rootPane.setContentPane(matchPanel);

        GameController myController = match.controller(mySide)
                .orElseThrow(() -> new IllegalStateException("내 보드의 조작기가 없습니다: " + mySide));
        this.progressTimer = new GameProgressTimer(match.state(mySide), myController);
        this.clearTimer = new ClearAnimationTimer(myController);
        match.addGameListener(mySide, progressTimer);
        match.addGameListener(mySide, clearTimer);

        for (Side side : Side.values()) {
            GamePanel board = matchPanel.game().board(side);
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

        this.effectTimer = new Timer(EFFECT_TICK_MS, event -> repaintEffects());
        if (timeLimit != null) {
            match.addListener(timeLimit);
            this.clockTimer = new Timer(CLOCK_TICK_MS, event -> {
                timeLimit.tick();
                matchPanel.game().repaintTimer();
            });
        } else {
            this.clockTimer = null;
        }

        match.addListener(new MatchListener() {
            @Override
            public void onPhaseChanged(MatchPhase phase) {
                matchPanel.game().repaint();
            }

            @Override
            public void onMatchEnded(MatchResult result) {
                stopTimers();
                attackEffects.forEach(AttackEffects::clear);
                exitMenu.setVisible(false);
                model.matchEnded(result);
                matchPanel.game().refresh();
                matchPanel.requestFocusInWindow();
                resultTimer.start();
            }
        });

        session.addListener(sessionListener);

        this.exitMenu = new P2PExitMenuPanel(this::hideExitMenu, this::leaveMatch);
        rootPane.setGlassPane(exitMenu);

        this.matchPanel.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent event) {
                handleKey(event.getKeyCode());
            }
        });
    }

    void start() {
        matchPanel.requestFocusInWindow();
        if (startCountdownClock == null) {
            beginMatch();
            return;
        }
        startCountdownClock.start();
        updateCountdownDisplay();
        countdownTimer.start();
    }

    private void tickCountdown() {
        if (released || startCountdownClock == null || match.phase() != MatchPhase.READY) {
            return;
        }
        if (startCountdownClock.isExpired()) {
            startCountdownClock.stop();
            countdownTimer.stop();
            model.setCountdownSeconds(0);
            matchPanel.game().refresh();
            beginMatch();
            return;
        }
        updateCountdownDisplay();
    }

    private void updateCountdownDisplay() {
        int seconds = (int) ((startCountdownClock.remainingMillis() + 999L) / 1000L);
        if (seconds != model.countdownSeconds()) {
            model.setCountdownSeconds(seconds);
            matchPanel.game().refresh();
        }
    }

    private void beginMatch() {
        match.begin();
        progressTimer.start();
        effectTimer.start();
        if (clockTimer != null) {
            clockTimer.start();
        }
    }

    private void repaintEffects() {
        for (Side side : Side.values()) {
            GamePanel board = matchPanel.game().board(side);
            boolean active = board.getEffects().isActive();
            if (active || effectsWereActive[side.ordinal()]) {
                board.repaint();
            }
            effectsWereActive[side.ordinal()] = active;
        }
    }

    void handleKey(int keyCode) {
        if (released) {
            return;
        }
        if (model.isEnded()) {
            if (keyCode == KeyEvent.VK_ENTER) {
                finishToReturn();
            }
            return;
        }
        if (keyCode == KeyEvent.VK_ESCAPE) {
            showExitMenu();
            return;
        }
        GameAction action = actionsByKey.get(keyCode);
        if (action == null) {
            return;
        }
        if (action == GameAction.PAUSE_MENU) {
            showExitMenu();
            return;
        }
        if (match.phase() != MatchPhase.PLAYING) {
            return;
        }
        GameState myState = match.state(mySide);
        if (myState.isGameOver() || myState.isClearing()) {
            return;
        }
        GameController controller = match.controller(mySide).orElse(null);
        if (controller == null) {
            return;
        }
        switch (action) {
            case MOVE_LEFT -> controller.moveLeft();
            case MOVE_RIGHT -> controller.moveRight();
            case SOFT_DROP -> controller.moveDown();
            case HARD_DROP -> controller.dropToBottom();
            case ROTATE -> controller.rotate();
            case ROTATE_COUNTERCLOCKWISE -> controller.rotateCounterclockwise();
            case PAUSE_MENU -> showExitMenu();
        }
    }

    private void showExitMenu() {
        if (released || model.isEnded()) {
            return;
        }
        exitMenu.reset();
        exitMenu.setVisible(true);
        exitMenu.requestFocusInWindow();
    }

    private void hideExitMenu() {
        if (released) {
            return;
        }
        exitMenu.setVisible(false);
        matchPanel.requestFocusInWindow();
    }

    private void leaveMatch() {
        if (released) {
            return;
        }
        release();
        session.leave();
        host.disposeWindow();
        SwingUtilities.invokeLater(onReturn);
    }

    private void finishToReturn() {
        if (released) {
            return;
        }
        release();
        host.disposeWindow();
        SwingUtilities.invokeLater(onReturn);
    }

    private void abortWindow() {
        if (released) {
            return;
        }
        release();
        host.disposeWindow();
    }

    void release() {
        if (released) {
            return;
        }
        released = true;
        session.removeListener(sessionListener);
        stopTimers();
        resultTimer.stop();
        if (match.phase() != MatchPhase.ENDED) {
            match.quit();
        }
    }

    private void stopTimers() {
        if (countdownTimer != null) {
            countdownTimer.stop();
        }
        if (startCountdownClock != null) {
            startCountdownClock.stop();
        }
        progressTimer.stop();
        clearTimer.stop();
        effectTimer.stop();
        if (clockTimer != null) {
            clockTimer.stop();
        }
    }

    P2PMatchPanel panel() {
        return matchPanel;
    }

    private static Map<Integer, GameAction> buildKeyMap(KeyConfigService keyConfig) {
        Map<Integer, GameAction> map = new HashMap<>();
        for (GameAction action : GameAction.values()) {
            if (action == GameAction.PAUSE_MENU) {
                continue;
            }
            for (int keyCode : keyConfig.getKeyCodes(action)) {
                if (keyCode != KeyEvent.VK_ESCAPE) {
                    map.put(keyCode, action);
                }
            }
        }
        map.put(KeyEvent.VK_ESCAPE, GameAction.PAUSE_MENU);
        return Map.copyOf(map);
    }
}
