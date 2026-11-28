package seoultech.se.tetris.ui.theme;

import java.awt.Component;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JDialog;
import javax.swing.SwingUtilities;

/**
 * 레트로 디자인의 확인 창. 자바 기본 확인 창({@code JOptionPane})을 대신해 게임 화면과 같은 디자인으로 묻는다.
 *
 * <p>창은 테두리 없이 부모 창 가운데에 뜨며, 고를 때까지 다음 코드로 넘어가지 않는다(모달).
 * 창을 닫으면 아니오로 본다.
 */
public final class RetroConfirmDialog {

    private RetroConfirmDialog() {
    }

    /**
     * 예·아니오를 묻는다.
     *
     * @param parent  확인 창을 띄울 화면. 그 화면이 속한 창 가운데에 띄운다
     * @param title   확인 창 제목. 예: "타이틀로 돌아가기"
     * @param message 묻는 문구. 문장마다 한 줄로 나눠 보여준다
     * @return 예를 골랐으면 {@code true}, 아니오를 고르거나 창을 닫았으면 {@code false}
     */
    public static boolean confirm(Component parent, String title, String message) {
        Window owner = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
        if (owner == null && parent instanceof Window window) {
            owner = window;
        }
        JDialog dialog = new JDialog(owner, title, JDialog.ModalityType.APPLICATION_MODAL);
        boolean[] answer = {false};
        RetroConfirmPanel panel = new RetroConfirmPanel(title, message, yes -> {
            answer[0] = yes;
            dialog.dispose();
        });
        dialog.setUndecorated(true);
        dialog.setContentPane(panel);
        dialog.pack();
        dialog.setLocationRelativeTo(owner);
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent event) {
                panel.requestFocusInWindow();
            }
        });
        dialog.setVisible(true);
        return answer[0];
    }
}
