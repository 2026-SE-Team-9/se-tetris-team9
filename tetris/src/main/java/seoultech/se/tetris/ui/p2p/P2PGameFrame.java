package seoultech.se.tetris.ui.p2p;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Objects;

import javax.swing.JFrame;

import seoultech.se.tetris.config.KeyConfigService;
import seoultech.se.tetris.versus.VersusMatch;
import seoultech.se.tetris.versus.p2p.P2PSession;

/**
 * P2P 대전 창. {@link P2PSession}, {@link VersusMatch}, {@link P2PGameSession}을 창에 연결한다.
 */
public final class P2PGameFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    private final transient P2PGameSession session;

    private P2PGameFrame(P2PSession p2pSession, VersusMatch match, double scale, boolean colorBlind,
            KeyConfigService keyConfig, Runnable onReturn) {
        super("SeoulTech SE Tetris");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setResizable(false);

        this.session = new P2PGameSession(p2pSession, match, scale, colorBlind, keyConfig,
                getRootPane(), this::dispose, onReturn);

        pack();
        setLocationRelativeTo(null);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                session.release();
                onReturn.run();
            }

            @Override
            public void windowClosed(WindowEvent event) {
                session.release();
            }
        });
    }

    /**
     * P2P 대전 창을 열고 카운트다운 후 대전을 시작한다.
     *
     * @param p2pSession 연결된 P2P 세션
     * @param match      시작된 P2P 대전
     * @param scale      화면 크기 배율
     * @param colorBlind 색맹 모드 여부
     * @param keyConfig  1인용 키 설정
     * @param onReturn   대전 종료 또는 나가기 후 P2P 화면으로 돌아갈 때 실행할 동작
     */
    public static P2PGameFrame launch(P2PSession p2pSession, VersusMatch match, double scale, boolean colorBlind,
            KeyConfigService keyConfig, Runnable onReturn) {
        Objects.requireNonNull(p2pSession, "p2pSession");
        Objects.requireNonNull(match, "match");
        Objects.requireNonNull(keyConfig, "keyConfig");
        Objects.requireNonNull(onReturn, "onReturn");
        P2PGameFrame frame = new P2PGameFrame(p2pSession, match, scale, colorBlind, keyConfig, onReturn);
        frame.setVisible(true);
        frame.session.start();
        return frame;
    }

    P2PMatchPanel panel() {
        return session.panel();
    }
}
