package seoultech.se.tetris.ui.versus;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Objects;
import java.util.function.Consumer;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import seoultech.se.tetris.config.BattleKeyConfigService;
import seoultech.se.tetris.config.PropertiesAppSettingsService;
import seoultech.se.tetris.versus.MatchResult;
import seoultech.se.tetris.versus.VersusMatch;

/**
 * 로컬 대전 창. 대전 모델({@link VersusMatch})과 {@link VersusGameSession}을 창에 연결한다.
 */
public final class VersusGameFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    static final int RESULT_DELAY_MS = VersusGameSession.RESULT_DELAY_MS;

    private final transient VersusGameSession session;

    private VersusGameFrame(VersusMatch match, double scale, boolean colorBlind,
            BattleKeyConfigService keyConfig, Runnable onReturnToMenu, Consumer<MatchResult> onMatchFinished) {
        super("SeoulTech SE Tetris");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        this.session = new VersusGameSession(match, scale, colorBlind, keyConfig, getRootPane(),
                new VersusGameSession.WindowHost() {
                    @Override
                    public void packAndCenter() {
                        pack();
                        setLocationRelativeTo(null);
                    }

                    @Override
                    public void disposeWindow() {
                        dispose();
                    }

                    @Override
                    public boolean confirmQuit() {
                        int choice = JOptionPane.showConfirmDialog(VersusGameFrame.this,
                                "현재 대전 진행 상황이 사라집니다. 시작 메뉴로 돌아가시겠습니까?",
                                "대전 나가기", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                        return choice == JOptionPane.YES_OPTION;
                    }
                },
                onReturnToMenu, onMatchFinished, PropertiesAppSettingsService::new);

        pack();
        setLocationRelativeTo(null);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent event) { session.release(); }
            @Override public void windowClosed(WindowEvent event) { session.release(); }
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
        frame.session.start();
        return frame;
    }

    VersusGamePanel panel() {
        return session.panel();
    }
}
