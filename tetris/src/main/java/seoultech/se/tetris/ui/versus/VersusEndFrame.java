package seoultech.se.tetris.ui.versus;

import javax.swing.JFrame;

/** 로컬 대전 종료 화면을 담는 창. */
public class VersusEndFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    private final VersusEndPanel panel;

    public VersusEndFrame(VersusEndModel model) {
        super("SeoulTech SE Tetris");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        panel = new VersusEndPanel(model);
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
