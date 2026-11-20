package seoultech.se.tetris.ui.versus;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import seoultech.se.tetris.component.controller.GameController;
import seoultech.se.tetris.component.listener.GameEventListener;
import seoultech.se.tetris.config.BattleKeyConfigService;
import seoultech.se.tetris.ui.game.ClearAnimationTimer;
import seoultech.se.tetris.ui.game.GamePanel;
import seoultech.se.tetris.ui.game.GameProgressTimer;
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
 */
public final class VersusGameFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    private final transient VersusMatch match;
    private final transient VersusScreenModel model;
    private final VersusGamePanel panel;
    private final transient List<GameProgressTimer> progressTimers = new ArrayList<>();
    private final transient List<ClearAnimationTimer> clearTimers = new ArrayList<>();
    private final transient Runnable onReturnToMenu;
    private boolean released;

    private VersusGameFrame(VersusMatch match, double scale, boolean colorBlind,
            BattleKeyConfigService keyConfig, Runnable onReturnToMenu) {
        super("SeoulTech SE Tetris");
        this.match = match;
        this.onReturnToMenu = onReturnToMenu;
        this.model = new VersusScreenModel(match.mode());
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
        match.addListener(new MatchListener() {
            @Override
            public void onPhaseChanged(MatchPhase phase) {
                panel.repaint();
            }

            @Override
            public void onMatchEnded(MatchResult result) {
                // 한쪽이 끝나면 두 보드를 모두 멈추고 승패를 표시한다. (3차 팀 결정)
                stopTimers();
                model.matchEnded(result);
                panel.refresh();
            }
        });

        panel.addKeyListener(new VersusKeyInput(new VersusKeyMap(keyConfig), match::controller, model,
                this::confirmQuit, this::returnToMenu));
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
     * @param onReturnToMenu 창을 닫은 뒤 시작 메뉴로 돌아가는 동작
     */
    public static VersusGameFrame launch(VersusMatch match, double scale, boolean colorBlind,
            BattleKeyConfigService keyConfig, Runnable onReturnToMenu) {
        Objects.requireNonNull(match, "match");
        Objects.requireNonNull(keyConfig, "keyConfig");
        Objects.requireNonNull(onReturnToMenu, "onReturnToMenu");
        VersusGameFrame frame = new VersusGameFrame(match, scale, colorBlind, keyConfig, onReturnToMenu);
        frame.setVisible(true);
        frame.panel.requestFocusInWindow();
        match.begin();
        frame.progressTimers.forEach(GameProgressTimer::start);
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
    }

    /** 대전 중 Esc. 두 보드를 멈추고 나갈지 묻는다. 나가지 않으면 이어서 진행한다. */
    private void confirmQuit() {
        match.pause();
        int choice = JOptionPane.showConfirmDialog(this,
                "현재 대전 진행 상황이 사라집니다. 시작 메뉴로 돌아가시겠습니까?",
                "대전 나가기", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice == JOptionPane.YES_OPTION) {
            returnToMenu();
        } else {
            match.resume();
            panel.requestFocusInWindow();
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

    private void stopTimers() {
        progressTimers.forEach(GameProgressTimer::stop);
        clearTimers.forEach(ClearAnimationTimer::stop);
    }

    /** 타이머를 멈추고 대전을 끝낸다. 여러 번 호출해도 한 번만 처리한다. */
    private void release() {
        if (released) {
            return;
        }
        released = true;
        stopTimers();
        match.quit();
    }

    // 테스트에서 창을 띄우지 않고 화면 구성을 확인한다.
    VersusGamePanel panel() {
        return panel;
    }
}
