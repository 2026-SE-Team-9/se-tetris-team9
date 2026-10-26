package seoultech.se.tetris.ui.end;

import javax.swing.JFrame;

import seoultech.se.tetris.engine.Difficulty;
import seoultech.se.tetris.engine.GameMode;
import seoultech.se.tetris.score.ScoreRepository;
import seoultech.se.tetris.ui.ScreenNavigator;

/** 게임 종료 화면을 담는 창. */
public class EndScreenFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    private final EndScreenPanel panel;

    public EndScreenFrame(ScreenNavigator navigator, int finalScore, GameMode mode,
            Difficulty difficulty, ScoreRepository repository) {
        super("SeoulTech SE Tetris");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        panel = new EndScreenPanel(
                EndScreenModel.createDefault(finalScore, mode, difficulty, repository, navigator));
        add(panel);
        pack();
        setLocationRelativeTo(null);
        setResizable(false);
    }

    /** 화면을 표시한 뒤 키보드 포커스를 종료 화면에 준다. */
    public void showScreen() {
        setVisible(true);
        panel.requestFocusInWindow();
    }
}
