package seoultech.se.tetris.ui.settings;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import seoultech.se.tetris.config.P2PSettingsService;

/**
 * 설정 화면의 P2P 대전 탭. 서버 포트 입력칸·초기화 버튼과 최근 접속 IP 목록·삭제 버튼을 둔다.
 *
 * <p>입력 상태와 문구는 {@link P2PSettingsModel}이 가지고, 이 패널은 그리기와 입력 연결만 한다.
 */
final class P2PSettingsPanel extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final Color BACKGROUND = Color.BLACK;
    private static final Color SURFACE = new Color(25, 25, 25);
    private static final Color OUTLINE = new Color(75, 75, 75);
    private static final Color MUTED = new Color(170, 170, 170);
    static final Color ERROR_COLOR = new Color(255, 110, 110);
    private static final int INNER_WIDTH = 432;
    /** 최근 접속 목록의 열 수. 최대 10개를 2열 5행으로 보여준다 */
    private static final int RECENT_COLUMNS = 2;

    private final transient P2PSettingsModel model;
    private final transient BooleanSupplier confirmClear;

    private final JTextField portField = new JTextField(6);
    private final JButton resetPort = new JButton("초기화");
    private final JLabel portMessage = SettingsPanel.label(" ");
    private final JPanel recentList = new JPanel();
    private final JButton clearRecent = new JButton("삭제");
    private boolean updatingField;

    /**
     * @param confirmClear 최근 접속 기록을 지우기 전에 확인을 받는다. {@code true}이면 지운다
     */
    P2PSettingsPanel(P2PSettingsModel model, BooleanSupplier confirmClear) {
        this.model = Objects.requireNonNull(model, "model");
        this.confirmClear = Objects.requireNonNull(confirmClear, "confirmClear");
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(BACKGROUND);
        setAlignmentX(Component.LEFT_ALIGNMENT);

        add(section("서버 포트"));
        add(Box.createVerticalStrut(8));
        add(portRow());
        add(Box.createVerticalStrut(4));
        portMessage.setFont(new Font(Font.DIALOG, Font.PLAIN, 12));
        add(fixedWidth(portMessage, 18));
        add(Box.createVerticalStrut(22));

        add(section("최근 접속 IP"));
        add(Box.createVerticalStrut(4));
        add(fixedWidth(hint("P2P 대전에 참여할 때 접속에 성공한 주소가 최신순으로 저장됩니다."), 18));
        add(Box.createVerticalStrut(10));
        recentList.setLayout(new GridLayout(0, RECENT_COLUMNS, 12, 6));
        recentList.setBackground(SURFACE);
        recentList.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(OUTLINE),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        add(fixedWidth(recentList, 140));
        add(Box.createVerticalStrut(10));

        JPanel clearRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        clearRow.setBackground(BACKGROUND);
        SettingsPanel.styleButton(clearRecent, false);
        clearRecent.addActionListener(event -> clearRecentPeers());
        clearRow.add(clearRecent);
        add(fixedWidth(clearRow, 40));
        add(Box.createVerticalStrut(12));

        refresh();
    }

    private JPanel portRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        row.setBackground(BACKGROUND);
        row.add(SettingsPanel.label("포트 번호"));

        portField.setFont(new Font(Font.MONOSPACED, Font.BOLD, 16));
        portField.setBackground(SURFACE);
        portField.setForeground(Color.WHITE);
        portField.setCaretColor(Color.WHITE);
        setFieldBorder(OUTLINE);
        portField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent event) {
                setFieldBorder(Color.YELLOW);
            }

            @Override
            public void focusLost(FocusEvent event) {
                setFieldBorder(OUTLINE);
            }
        });
        portField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent event) { portTextChanged(); }
            @Override public void removeUpdate(DocumentEvent event) { portTextChanged(); }
            @Override public void changedUpdate(DocumentEvent event) { portTextChanged(); }
        });
        row.add(portField);

        SettingsPanel.styleButton(resetPort, false);
        resetPort.addActionListener(event -> resetPort());
        row.add(resetPort);
        return fixedWidth(row, 40);
    }

    private void setFieldBorder(Color color) {
        portField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)));
    }

    private void portTextChanged() {
        if (updatingField) {
            return;
        }
        model.setPortText(portField.getText());
        refreshPortMessage();
    }

    /** 입력칸을 기본 포트로 되돌린다. 저장하면 반영된다. */
    void resetPort() {
        model.resetPort();
        refresh();
    }

    /** 확인을 받은 뒤 최근 접속 기록을 모두 지운다. 기록이 없으면 묻지 않는다. */
    void clearRecentPeers() {
        if (!model.hasRecentPeers() || !confirmClear.getAsBoolean()) {
            return;
        }
        model.clearRecentPeers();
        refresh();
    }

    /** 모델의 값으로 입력칸, 안내 문구, 최근 접속 목록을 다시 그린다. */
    void refresh() {
        if (!portField.getText().equals(model.portText())) {
            updatingField = true;
            try {
                portField.setText(model.portText());
            } finally {
                updatingField = false;
            }
        }
        refreshPortMessage();
        refreshRecentList();
    }

    private void refreshPortMessage() {
        String error = model.portError();
        portMessage.setText(error == null ? model.defaultPortHint() : error);
        portMessage.setForeground(error == null ? MUTED : ERROR_COLOR);
    }

    private void refreshRecentList() {
        recentList.removeAll();
        List<String> peers = model.recentPeerTexts();
        if (peers.isEmpty()) {
            recentList.setLayout(new GridLayout(1, 1));
            JLabel empty = SettingsPanel.label(P2PSettingsModel.EMPTY_RECENT);
            empty.setForeground(MUTED);
            empty.setHorizontalAlignment(JLabel.CENTER);
            recentList.add(empty);
        } else {
            // 2열로 놓되 위에서 아래로 최신순이 이어지도록 열 우선으로 채운다
            int rows = (P2PSettingsService.MAX_RECENT_PEERS + RECENT_COLUMNS - 1) / RECENT_COLUMNS;
            recentList.setLayout(new GridLayout(rows, RECENT_COLUMNS, 12, 2));
            for (int row = 0; row < rows; row++) {
                for (int column = 0; column < RECENT_COLUMNS; column++) {
                    int index = column * rows + row;
                    recentList.add(index < peers.size() ? peerLabel(index, peers.get(index)) : new JLabel());
                }
            }
        }
        clearRecent.setEnabled(!peers.isEmpty());
        recentList.revalidate();
        recentList.repaint();
    }

    private static JLabel peerLabel(int index, String text) {
        JLabel label = new JLabel((index + 1) + ".  " + text);
        label.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        label.setForeground(Color.WHITE);
        return label;
    }

    /**
     * 키보드 이동 순서. 포트 입력칸, 초기화, 삭제를 위에서 아래로 한 줄씩 둔다.
     * 어느 탭 버튼에서 ↓를 눌러도 포트 입력칸부터 시작하고, 입력칸 안의 ← →와 겹치지 않는다.
     */
    List<List<JComponent>> navigationRows() {
        List<List<JComponent>> rows = new ArrayList<>();
        rows.add(List.of(portField));
        rows.add(List.of(resetPort));
        rows.add(List.of(clearRecent));
        return rows;
    }

    JTextField portField() {
        return portField;
    }

    JButton resetPortButton() {
        return resetPort;
    }

    JButton clearRecentButton() {
        return clearRecent;
    }

    JLabel portMessage() {
        return portMessage;
    }

    JPanel recentList() {
        return recentList;
    }

    private static JLabel section(String text) {
        JLabel section = SettingsPanel.label(text);
        section.setForeground(Color.CYAN);
        section.setFont(new Font(Font.DIALOG, Font.BOLD, 17));
        return fixedWidth(section, 24);
    }

    private static JLabel hint(String text) {
        JLabel hint = SettingsPanel.label(text);
        hint.setFont(new Font(Font.DIALOG, Font.PLAIN, 12));
        hint.setForeground(MUTED);
        return hint;
    }

    private static <T extends JComponent> T fixedWidth(T component, int height) {
        Dimension size = new Dimension(INNER_WIDTH, height);
        component.setAlignmentX(Component.LEFT_ALIGNMENT);
        component.setPreferredSize(size);
        component.setMaximumSize(size);
        return component;
    }
}
