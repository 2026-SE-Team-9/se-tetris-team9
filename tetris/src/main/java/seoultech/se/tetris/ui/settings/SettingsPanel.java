package seoultech.se.tetris.ui.settings;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.ActionEvent;
import java.util.Objects;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractButton;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.KeyStroke;
import javax.swing.BoxLayout;
import javax.swing.Box;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.text.JTextComponent;
import seoultech.se.tetris.config.AppSettings;
import seoultech.se.tetris.config.AppSettings.ScreenSize;
import seoultech.se.tetris.config.BattleKeyBindingEditor;
import seoultech.se.tetris.config.InMemoryP2PSettingsService;
import seoultech.se.tetris.config.KeyBindingEditor;
import seoultech.se.tetris.config.P2PSettingsService;
import seoultech.se.tetris.config.PropertiesAppSettingsService;
import seoultech.se.tetris.config.PropertiesBattleKeyConfigService;
import seoultech.se.tetris.config.PropertiesKeyConfigService;
import seoultech.se.tetris.engine.Difficulty;
import seoultech.se.tetris.score.ScoreRepository;

/** 시작 메뉴와 일시정지 화면에서 재사용할 수 있는 설정 화면. */
public final class SettingsPanel extends JPanel {
    /** 설정 화면 맨 위의 탭 */
    enum Tab {
        SINGLE("일반 모드"),
        LOCAL_BATTLE("로컬 대전"),
        P2P("P2P 대전");

        private final String label;

        Tab(String label) {
            this.label = label;
        }

        String label() {
            return label;
        }
    }

    /**
     * 설정 화면을 연 위치.
     *
     * <p>난이도와 스코어보드 기록 초기화는 시작 메뉴에서만 바꿀 수 있다.
     * 게임 중에는 진행 중인 판의 규칙과 기록이 어긋나지 않도록 두 항목을 보여주지 않는다.
     *
     * <p>게임 중 설정은 진행 중인 게임의 설정만 보여준다. 1인 게임 중에는 일반 모드, 로컬 대전 중에는
     * 로컬 대전 설정만 있다. (3차 팀 결정: 로컬 대전의 일시정지 중 설정 화면은 1인 모드와 따로 둔다)
     * P2P 대전 탭은 대전을 시작하기 전인 시작 메뉴의 설정에만 있다.
     */
    public enum Context {
        /** 시작 메뉴에서 연 설정 */
        START_MENU(true, true, Tab.SINGLE, Tab.LOCAL_BATTLE, Tab.P2P),
        /** 1인 게임 중 일시정지 메뉴에서 연 설정 */
        IN_GAME(false, true, Tab.SINGLE),
        /** 로컬 대전 게임 중 일시정지 메뉴에서 연 설정 */
        LOCAL_BATTLE_IN_GAME(false, true, Tab.LOCAL_BATTLE);

        private final boolean fullAccess;
        private final boolean allowsKeyEditing;
        private final List<Tab> tabs;

        Context(boolean fullAccess, boolean allowsKeyEditing, Tab... tabs) {
            this.fullAccess = fullAccess;
            this.allowsKeyEditing = allowsKeyEditing;
            this.tabs = List.of(tabs);
        }

        /** 이 설정 화면에서 보여주는 탭. 첫 탭을 처음에 연다. */
        List<Tab> tabs() {
            return tabs;
        }

        boolean shows(Tab tab) {
            return tabs.contains(tab);
        }

        boolean allowsP2PSettings() {
            return shows(Tab.P2P);
        }

        boolean allowsDifficulty() {
            return fullAccess;
        }

        boolean allowsScoreReset() {
            return fullAccess;
        }

        boolean allowsKeyEditing() {
            return allowsKeyEditing;
        }
    }

    private static final long serialVersionUID = 1L;
    private static final Color BACKGROUND = Color.BLACK;
    private static final Color SURFACE = new Color(25, 25, 25);
    private static final Color OUTLINE = new Color(75, 75, 75);
    private static final Color MUTED = new Color(170, 170, 170);
    private static final Font BODY_FONT = new Font(Font.DIALOG, Font.PLAIN, 15);
    private static final int CONTENT_WIDTH = 480;
    private static final int INNER_WIDTH = 432; // CONTENT_WIDTH (480) - 좌우 패딩 (24 * 2)

    private static final String CARD_SINGLE = "SINGLE";
    private static final String CARD_BATTLE = "BATTLE";
    private static final String CARD_P2P = "P2P";

    private final PropertiesAppSettingsService settings;
    private final KeyBindingEditor keyEditor;
    private final KeyBindingsPanel keyBindings;
    private final BattleKeyBindingEditor battleKeyEditor;
    private final BattleKeyBindingsPanel battleKeyBindings;
    private final ScoreRepository scores;
    private final transient P2PSettingsModel p2pModel;
    private final P2PSettingsPanel p2pSettings;
    private final Context context;
    private final Runnable onClose;
    private final transient KeyEventDispatcher keyboardDispatcher = this::dispatchKeyboard;
    private final List<List<JComponent>> navigationRows = new ArrayList<>();

    // 일반 모드 및 대전 모드 옵션 컴포넌트
    private final ModeOptionsPanel singleOptions;
    private final ModeOptionsPanel battleOptions;

    // 최상단 탭 버튼 및 CardLayout 컨테이너
    private final JButton singleModeTab = new JButton(Tab.SINGLE.label());
    private final JButton battleModeTab = new JButton(Tab.LOCAL_BATTLE.label());
    private final JButton p2pTab = new JButton(Tab.P2P.label());
    private final CardLayout tabCardLayout = new CardLayout();
    private final JPanel tabBodyContainer = new JPanel(tabCardLayout);
    private final JPanel singleModePanel = new JPanel();
    private final JPanel battleModePanel = new JPanel();

    private final List<JComponent> actionButtonsRow = new ArrayList<>();
    private Tab currentTab = Tab.SINGLE;

    private final FocusListener scrollOnFocus = new FocusAdapter() {
        @Override
        public void focusGained(FocusEvent event) {
            if (event.getComponent() instanceof JComponent control) {
                control.scrollRectToVisible(new Rectangle(control.getSize()));
            }
        }
    };

    public SettingsPanel(PropertiesAppSettingsService settings,
            PropertiesKeyConfigService keys, ScoreRepository scores,
            Context context, Runnable onKeysSaved, Runnable onClose) {
        this(settings, keys, new PropertiesBattleKeyConfigService(), scores,
                context, onKeysSaved, () -> {}, onClose);
    }

    public SettingsPanel(PropertiesAppSettingsService settings,
            PropertiesKeyConfigService keys, PropertiesBattleKeyConfigService battleKeys,
            ScoreRepository scores, Context context,
            Runnable onKeysSaved, Runnable onBattleKeysSaved, Runnable onClose) {
        this(settings, keys, battleKeys, new InMemoryP2PSettingsService(), scores,
                context, onKeysSaved, onBattleKeysSaved, onClose);
    }

    /**
     * @param p2pSettings P2P 대전 탭에서 보여주고 바꿀 포트와 최근 접속 기록
     */
    public SettingsPanel(PropertiesAppSettingsService settings,
            PropertiesKeyConfigService keys, PropertiesBattleKeyConfigService battleKeys,
            P2PSettingsService p2pSettings, ScoreRepository scores, Context context,
            Runnable onKeysSaved, Runnable onBattleKeysSaved, Runnable onClose) {
        super(new BorderLayout());
        this.settings = Objects.requireNonNull(settings, "settings");
        this.keyEditor = new KeyBindingEditor(Objects.requireNonNull(keys, "keys"),
                Objects.requireNonNull(onKeysSaved, "onKeysSaved"));
        this.keyBindings = new KeyBindingsPanel(keyEditor);
        this.battleKeyEditor = new BattleKeyBindingEditor(Objects.requireNonNull(battleKeys, "battleKeys"),
                Objects.requireNonNull(onBattleKeysSaved, "onBattleKeysSaved"));
        this.battleKeyBindings = new BattleKeyBindingsPanel(battleKeyEditor);
        this.scores = Objects.requireNonNull(scores, "scores");
        this.p2pModel = new P2PSettingsModel(Objects.requireNonNull(p2pSettings, "p2pSettings"));
        this.p2pSettings = new P2PSettingsPanel(p2pModel, this::confirmClearRecentPeers);
        this.context = Objects.requireNonNull(context, "context");
        this.onClose = Objects.requireNonNull(onClose, "onClose");
        this.singleOptions = new ModeOptionsPanel(context.allowsDifficulty());
        this.battleOptions = new ModeOptionsPanel(context.allowsDifficulty());

        if (!context.allowsKeyEditing()) {
            battleKeyBindings.setInteractive(false);
        }

        buildContent();
        loadCurrentValues();
        selectTab(availableTabs().get(0));
    }

    @Override
    public void addNotify() {
        super.addNotify();
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(keyboardDispatcher);
        SwingUtilities.invokeLater(() -> {
            if (isShowing() && !navigationRows.isEmpty()) {
                navigationRows.get(0).get(0).requestFocusInWindow();
            }
        });
    }

    @Override
    public void removeNotify() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(keyboardDispatcher);
        super.removeNotify();
    }

    private boolean dispatchKeyboard(KeyEvent event) {
        if (event.getID() != KeyEvent.KEY_PRESSED
                || keyBindings.isCapturingKey()
                || battleKeyBindings.isCapturingKey()
                || !isShowing()
                || !SwingUtilities.isDescendingFrom(event.getComponent(), this)) {
            return false;
        }

        // Tab 키를 누르면 다음 탭으로 넘어간다. 마지막 탭 다음은 첫 탭이다.
        if (event.getKeyCode() == KeyEvent.VK_TAB) {
            selectTab(nextTab());
            event.consume();
            return true;
        }

        Component focused = event.getComponent();
        // 펼친 선택 목록에서는 Swing의 방향키 선택과 Esc 닫기를 사용한다.
        if (focused instanceof JComboBox<?> combo && combo.isPopupVisible()) {
            return false;
        }
        // 글자 입력칸에서는 ← →로 커서를 옮긴다.
        if (leavesKeyToTextField(focused, event.getKeyCode())) {
            return false;
        }
        Component target = navigationTarget(focused, event.getKeyCode());
        if (target != null) {
            target.requestFocusInWindow();
            event.consume();
            return true;
        }
        if (event.getKeyCode() == KeyEvent.VK_ENTER) {
            if (focused instanceof JComboBox<?> combo) {
                combo.showPopup();
            } else if (focused instanceof AbstractButton button) {
                button.doClick();
            } else {
                return false;
            }
            event.consume();
            return true;
        }
        if (event.getKeyCode() != KeyEvent.VK_ESCAPE) {
            return false;
        }
        event.consume();
        String[] options = {"예", "아니오", "취소"};
        JOptionPane prompt = new JOptionPane("설정을 저장하시겠습니까?", JOptionPane.QUESTION_MESSAGE,
                JOptionPane.YES_NO_CANCEL_OPTION, null, options, options[0]);
        // 확인창의 버튼도 방향키로 이동하고 Enter로 현재 버튼을 선택한다.
        for (int key : new int[] {KeyEvent.VK_LEFT, KeyEvent.VK_UP, KeyEvent.VK_RIGHT,
                KeyEvent.VK_DOWN, KeyEvent.VK_ENTER}) {
            String action = "dialog-navigation-" + key;
            prompt.getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(key, 0), action);
            prompt.getActionMap().put(action, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
                    if (key == KeyEvent.VK_ENTER) {
                        if (manager.getFocusOwner() instanceof AbstractButton button) {
                            button.doClick();
                        }
                    } else if (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_UP) {
                        manager.focusPreviousComponent();
                    } else {
                        manager.focusNextComponent();
                    }
                }
            });
        }
        JDialog dialog = prompt.createDialog(this, "설정 나가기");
        try {
            dialog.setVisible(true);
        } finally {
            dialog.dispose();
        }
        for (int choice = 0; choice < options.length; choice++) {
            if (options[choice].equals(prompt.getValue())) {
                handleEscapeChoice(choice);
                break;
            }
        }
        return true;
    }

    /** 글자 입력칸에 있을 때 ← →는 화면 이동에 쓰지 않고 입력칸의 커서 이동에 맡긴다. */
    static boolean leavesKeyToTextField(Component focused, int keyCode) {
        return focused instanceof JTextComponent
                && (keyCode == KeyEvent.VK_LEFT || keyCode == KeyEvent.VK_RIGHT);
    }

    /** 화면의 행과 열을 기준으로 방향키 이동 대상을 찾는다. */
    Component navigationTarget(Component current, int keyCode) {
        for (int row = 0; row < navigationRows.size(); row++) {
            int column = navigationRows.get(row).indexOf(current);
            if (column < 0) {
                continue;
            }
            int nextRow = row;
            int nextColumn = column;
            switch (keyCode) {
                case KeyEvent.VK_UP -> nextRow = Math.max(0, row - 1);
                case KeyEvent.VK_DOWN -> nextRow = Math.min(navigationRows.size() - 1, row + 1);
                case KeyEvent.VK_LEFT -> nextColumn = Math.max(0, column - 1);
                case KeyEvent.VK_RIGHT -> nextColumn = Math.min(navigationRows.get(row).size() - 1, column + 1);
                default -> { return null; }
            }
            List<JComponent> destination = navigationRows.get(nextRow);
            return destination.get(Math.min(nextColumn, destination.size() - 1));
        }
        return null;
    }

    void handleEscapeChoice(int choice) {
        if (choice == JOptionPane.YES_OPTION) {
            saveSettings();
        } else if (choice == JOptionPane.NO_OPTION) {
            keyEditor.resetChanges();
            battleKeyEditor.resetChanges();
            close();
        }
    }

    private void buildContent() {
        setBackground(BACKGROUND);
        JPanel mainContent = new JPanel();
        mainContent.setLayout(new BoxLayout(mainContent, BoxLayout.Y_AXIS));
        mainContent.setBackground(BACKGROUND);
        mainContent.setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));
        mainContent.setPreferredSize(new Dimension(CONTENT_WIDTH, 630));

        // 최상단 타이틀
        JLabel title = new JLabel("설정");
        title.setFont(new Font(Font.DIALOG, Font.BOLD, 26));
        title.setForeground(Color.CYAN);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainContent.add(title);
        mainContent.add(Box.createVerticalStrut(14));

        // 최상단 탭 바 (일반 모드 / 로컬 대전 / P2P 대전). 탭이 하나뿐인 게임 중 설정에는 두지 않는다.
        if (hasTabBar()) {
            JPanel topTabBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
            topTabBar.setBackground(BACKGROUND);
            topTabBar.setAlignmentX(Component.LEFT_ALIGNMENT);
            for (Tab tab : availableTabs()) {
                JButton button = tabButton(tab);
                styleButton(button, tab == availableTabs().get(0));
                button.addActionListener(event -> selectTab(tab));
                topTabBar.add(button);
            }
            mainContent.add(topTabBar);
            mainContent.add(Box.createVerticalStrut(6));

            JLabel tabHint = label(tabHintText());
            tabHint.setFont(new Font(Font.DIALOG, Font.PLAIN, 11));
            tabHint.setForeground(MUTED);
            tabHint.setAlignmentX(Component.LEFT_ALIGNMENT);
            mainContent.add(tabHint);
            mainContent.add(Box.createVerticalStrut(14));
        }

        // 일반 모드 패널 및 대전 모드 패널 조립
        buildSingleModePanel();
        buildBattleModePanel();

        tabBodyContainer.setBackground(BACKGROUND);
        tabBodyContainer.setAlignmentX(Component.LEFT_ALIGNMENT);
        tabBodyContainer.add(singleModePanel, CARD_SINGLE);
        tabBodyContainer.add(battleModePanel, CARD_BATTLE);
        if (context.allowsP2PSettings()) {
            tabBodyContainer.add(p2pSettings, CARD_P2P);
        }
        mainContent.add(tabBodyContainer);

        // 하단 안내 라벨
        JLabel navigationHint = label("↑ ↓ 항목 이동 · ← → 키 칸/버튼 이동 · Enter 선택");
        navigationHint.setFont(new Font(Font.DIALOG, Font.PLAIN, 12));
        navigationHint.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainContent.add(navigationHint);

        // 하단 액션 버튼
        JPanel actions = new JPanel();
        actions.setBackground(BACKGROUND);
        actions.setBorder(BorderFactory.createEmptyBorder(12, 12, 16, 12));
        JButton reset = new JButton("모든 설정 기본값");
        styleButton(reset, false);
        reset.addActionListener(event -> restoreDefaults());
        actions.add(reset);
        if (context.allowsScoreReset()) {
            JButton clearScores = new JButton("스코어보드 기록 초기화");
            styleButton(clearScores, false);
            clearScores.addActionListener(event -> clearScores());
            actions.add(clearScores);
        }
        JButton save = new JButton("저장");
        styleButton(save, true);
        save.addActionListener(event -> saveSettings());
        actions.add(save);
        JButton cancel = new JButton("취소");
        styleButton(cancel, false);
        cancel.addActionListener(event -> close());
        actions.add(cancel);

        actionButtonsRow.clear();
        for (Component component : actions.getComponents()) {
            actionButtonsRow.add((JComponent) component);
        }

        JScrollPane scroll = new JScrollPane(mainContent);
        scroll.setBorder(null);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getViewport().setBackground(BACKGROUND);
        add(scroll, BorderLayout.CENTER);
        add(actions, BorderLayout.SOUTH);
    }

    /** 1인(일반) 모드 설정 화면 구성 */
    private void buildSingleModePanel() {
        singleModePanel.setLayout(new BoxLayout(singleModePanel, BoxLayout.Y_AXIS));
        singleModePanel.setBackground(BACKGROUND);
        singleModePanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        singleModePanel.add(singleOptions);
        singleModePanel.add(Box.createVerticalStrut(18));

        JLabel section = label("1인 조작 키");
        section.setForeground(Color.CYAN);
        section.setFont(new Font(Font.DIALOG, Font.BOLD, 17));
        section.setAlignmentX(Component.LEFT_ALIGNMENT);
        singleModePanel.add(section);
        singleModePanel.add(Box.createVerticalStrut(4));

        JLabel keyHint = label("키 칸 선택 후 입력  ·  Esc로 해제");
        keyHint.setFont(new Font(Font.DIALOG, Font.PLAIN, 12));
        keyHint.setForeground(MUTED);
        keyHint.setAlignmentX(Component.LEFT_ALIGNMENT);
        keyHint.setPreferredSize(new Dimension(INNER_WIDTH, 18));
        keyHint.setMaximumSize(new Dimension(INNER_WIDTH, 18));
        singleModePanel.add(keyHint);
        singleModePanel.add(Box.createVerticalStrut(10));

        Dimension keyPanelSize = new Dimension(INNER_WIDTH, 230);
        keyBindings.setAlignmentX(Component.LEFT_ALIGNMENT);
        keyBindings.setPreferredSize(keyPanelSize);
        keyBindings.setMaximumSize(keyPanelSize);
        singleModePanel.add(keyBindings);
        singleModePanel.add(Box.createVerticalStrut(12));
    }

    /** 로컬 대전(2인) 설정 화면 구성 */
    private void buildBattleModePanel() {
        battleModePanel.setLayout(new BoxLayout(battleModePanel, BoxLayout.Y_AXIS));
        battleModePanel.setBackground(BACKGROUND);
        battleModePanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        battleModePanel.add(battleOptions);
        battleModePanel.add(Box.createVerticalStrut(18));

        JLabel section = label("로컬 대전 조작 키 (2인)");
        section.setForeground(Color.CYAN);
        section.setFont(new Font(Font.DIALOG, Font.BOLD, 17));
        section.setAlignmentX(Component.LEFT_ALIGNMENT);
        battleModePanel.add(section);
        battleModePanel.add(Box.createVerticalStrut(4));

        String hintText = context.allowsKeyEditing()
                ? "키 칸 선택 후 입력  ·  Esc로 해제"
                : "대전 중에는 조작 키를 변경할 수 없습니다.";
        JLabel keyHint = label(hintText);
        keyHint.setFont(new Font(Font.DIALOG, Font.PLAIN, 12));
        keyHint.setForeground(MUTED);
        keyHint.setAlignmentX(Component.LEFT_ALIGNMENT);
        keyHint.setPreferredSize(new Dimension(INNER_WIDTH, 18));
        keyHint.setMaximumSize(new Dimension(INNER_WIDTH, 18));
        battleModePanel.add(keyHint);
        battleModePanel.add(Box.createVerticalStrut(10));

        Dimension keyPanelSize = new Dimension(INNER_WIDTH, 230);
        battleKeyBindings.setAlignmentX(Component.LEFT_ALIGNMENT);
        battleKeyBindings.setPreferredSize(keyPanelSize);
        battleKeyBindings.setMaximumSize(keyPanelSize);
        battleModePanel.add(battleKeyBindings);
        battleModePanel.add(Box.createVerticalStrut(12));
    }

    /** 일반 모드 탭 또는 로컬 대전 탭을 고른다. */
    void selectTab(boolean battle) {
        selectTab(battle ? Tab.LOCAL_BATTLE : Tab.SINGLE);
    }

    /** 탭을 고른다. 이 설정 화면에서 보여주지 않는 탭이면 무시한다. */
    void selectTab(Tab tab) {
        if (!availableTabs().contains(tab)) {
            return;
        }
        keyBindings.cancelKeyCapture();
        battleKeyBindings.cancelKeyCapture();
        currentTab = tab;

        for (Tab each : availableTabs()) {
            updateTabButtonStyle(tabButton(each), each == tab);
        }

        tabCardLayout.show(tabBodyContainer, switch (tab) {
            case SINGLE -> CARD_SINGLE;
            case LOCAL_BATTLE -> CARD_BATTLE;
            case P2P -> CARD_P2P;
        });
        tabBodyContainer.revalidate();
        tabBodyContainer.repaint();

        rebuildNavigationRows();
    }

    Tab currentTab() {
        return currentTab;
    }

    /** 이 설정 화면에서 보여주는 탭 */
    List<Tab> availableTabs() {
        return context.tabs();
    }

    /** 탭이 둘 이상일 때만 탭 바를 둔다. */
    boolean hasTabBar() {
        return availableTabs().size() > 1;
    }

    /** 탭 바 아래 안내. 예: "Tab 키 또는 방향키로 일반 모드 / 로컬 대전 / P2P 대전 설정을 전환할 수 있습니다." */
    String tabHintText() {
        return "Tab 키 또는 방향키로 "
                + String.join(" / ", availableTabs().stream().map(Tab::label).toList())
                + " 설정을 전환할 수 있습니다.";
    }

    private Tab nextTab() {
        List<Tab> tabs = availableTabs();
        return tabs.get((tabs.indexOf(currentTab) + 1) % tabs.size());
    }

    private JButton tabButton(Tab tab) {
        return switch (tab) {
            case SINGLE -> singleModeTab;
            case LOCAL_BATTLE -> battleModeTab;
            case P2P -> p2pTab;
        };
    }

    P2PSettingsPanel p2pSettingsPanel() {
        return p2pSettings;
    }

    private static void updateTabButtonStyle(JButton button, boolean active) {
        button.setBackground(active ? Color.CYAN : SURFACE);
        button.setForeground(active ? BACKGROUND : Color.WHITE);
        setButtonBorder(button, active ? Color.CYAN : OUTLINE);
    }

    void rebuildNavigationRows() {
        navigationRows.clear();
        if (hasTabBar()) {
            navigationRows.add(availableTabs().stream().map(tab -> (JComponent) tabButton(tab)).toList());
        }

        switch (currentTab) {
            case SINGLE -> {
                navigationRows.addAll(singleOptions.navigationRows());
                navigationRows.addAll(keyBindings.navigationRows());
            }
            case LOCAL_BATTLE -> {
                navigationRows.addAll(battleOptions.navigationRows());
                navigationRows.addAll(battleKeyBindings.navigationRows());
            }
            case P2P -> navigationRows.addAll(p2pSettings.navigationRows());
        }

        navigationRows.add(List.copyOf(actionButtonsRow));

        for (List<JComponent> row : navigationRows) {
            for (JComponent control : row) {
                control.removeFocusListener(scrollOnFocus);
                control.addFocusListener(scrollOnFocus);
            }
        }
    }

    static JLabel label(String text) {
        JLabel label = new JLabel(text);
        label.setFont(BODY_FONT);
        label.setForeground(Color.WHITE);
        return label;
    }



    static void styleButton(JButton button, boolean accent) {
        button.setUI(new BasicButtonUI());
        button.setFont(BODY_FONT);
        button.setBackground(accent ? Color.CYAN : SURFACE);
        button.setForeground(accent ? BACKGROUND : Color.WHITE);
        button.setFocusPainted(false);
        setButtonBorder(button, accent ? Color.CYAN : OUTLINE);
        button.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent event) {
                setButtonBorder(button, Color.YELLOW);
            }

            @Override
            public void focusLost(FocusEvent event) {
                setButtonBorder(button, accent ? Color.CYAN : OUTLINE);
            }
        });
    }

    private static void setButtonBorder(JButton button, Color color) {
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color),
                BorderFactory.createEmptyBorder(7, 12, 7, 12)));
    }

    private void close() {
        keyBindings.cancelKeyCapture();
        battleKeyBindings.cancelKeyCapture();
        onClose.run();
    }

    private void loadCurrentValues() {
        AppSettings current = settings.current();
        singleOptions.setScreenSize(current.screenSize());
        singleOptions.setDifficulty(current.difficulty());
        singleOptions.setColorBlind(current.colorBlind());

        battleOptions.setScreenSize(current.battleScreenSize());
        battleOptions.setDifficulty(current.battleDifficulty());
        battleOptions.setColorBlind(current.battleColorBlind());

        keyBindings.refreshKeys();
        battleKeyBindings.refreshKeys();
    }

    /** 이 화면에 보이는 탭의 설정만 기본값으로 되돌린다. 보이지 않는 탭의 설정은 그대로 둔다. */
    private void restoreDefaults() {
        keyBindings.cancelKeyCapture();
        battleKeyBindings.cancelKeyCapture();
        if (context.shows(Tab.SINGLE)) {
            singleOptions.setScreenSize(AppSettings.DEFAULT.screenSize());
            if (context.allowsDifficulty()) {
                singleOptions.setDifficulty(AppSettings.DEFAULT.difficulty());
            }
            singleOptions.setColorBlind(AppSettings.DEFAULT.colorBlind());
            keyEditor.restoreDefaults();
            keyBindings.refreshKeys();
        }
        if (context.shows(Tab.LOCAL_BATTLE)) {
            battleOptions.setScreenSize(AppSettings.DEFAULT.battleScreenSize());
            if (context.allowsDifficulty()) {
                battleOptions.setDifficulty(AppSettings.DEFAULT.battleDifficulty());
            }
            battleOptions.setColorBlind(AppSettings.DEFAULT.battleColorBlind());
            battleKeyEditor.restoreDefaults();
            battleKeyBindings.refreshKeys();
        }
        if (context.shows(Tab.P2P)) {
            // 포트는 기본값으로 되돌리되, 최근 접속 기록은 삭제 버튼으로만 지운다
            p2pModel.resetPort();
            p2pSettings.refresh();
        }
    }

    private void saveSettings() {
        keyBindings.cancelKeyCapture();
        battleKeyBindings.cancelKeyCapture();
        if (context.shows(Tab.SINGLE) && !keyEditor.canSave()) {
            selectTab(Tab.SINGLE);
            JOptionPane.showMessageDialog(this, "모든 동작에 메인 키가 필요합니다.", "설정", JOptionPane.WARNING_MESSAGE);
            keyBindings.focusMissingMainKey();
            return;
        }
        if (context.shows(Tab.LOCAL_BATTLE) && !battleKeyEditor.canSave()) {
            selectTab(Tab.LOCAL_BATTLE);
            JOptionPane.showMessageDialog(this, "로컬 대전의 모든 조작 키(1P, 2P 총 10개)가 필요합니다.", "설정", JOptionPane.WARNING_MESSAGE);
            battleKeyBindings.focusMissingKey();
            return;
        }
        if (context.allowsP2PSettings() && !p2pModel.isPortValid()) {
            selectTab(Tab.P2P);
            JOptionPane.showMessageDialog(this, p2pModel.portError(), "설정", JOptionPane.WARNING_MESSAGE);
            p2pSettings.portField().requestFocusInWindow();
            return;
        }
        try {
            boolean single = context.shows(Tab.SINGLE);
            boolean battle = context.shows(Tab.LOCAL_BATTLE);
            if (single) {
                keyEditor.save();
            }
            if (battle && context.allowsKeyEditing()) {
                battleKeyEditor.save();
            }
            // 보이지 않는 탭의 설정과, 게임 중에는 보여주지 않는 난이도는 저장된 값을 그대로 유지한다.
            AppSettings stored = settings.current();
            Difficulty savedDifficulty = single && context.allowsDifficulty()
                    ? singleOptions.getDifficulty()
                    : stored.difficulty();
            Difficulty savedBattleDifficulty = battle && context.allowsDifficulty()
                    ? battleOptions.getDifficulty()
                    : stored.battleDifficulty();
            settings.save(new AppSettings(
                    single ? singleOptions.getScreenSize() : stored.screenSize(),
                    single ? singleOptions.isColorBlind() : stored.colorBlind(),
                    savedDifficulty,
                    battle ? battleOptions.getScreenSize() : stored.battleScreenSize(),
                    battle ? battleOptions.isColorBlind() : stored.battleColorBlind(),
                    savedBattleDifficulty));
            if (context.allowsP2PSettings()) {
                p2pModel.applyPort();
            }
            close();
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(this,
                    e.getMessage() + "\n키 설정과 다른 설정은 별도 파일이므로 일부가 저장되었을 수 있습니다.",
                    "설정 저장 실패", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** 최근 접속 기록을 지우기 전에 묻는다. */
    private boolean confirmClearRecentPeers() {
        int choice = JOptionPane.showConfirmDialog(this, "최근 접속 기록을 모두 삭제하시겠습니까?",
                "최근 접속 IP 삭제", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        return choice == JOptionPane.YES_OPTION;
    }

    private void clearScores() {
        keyBindings.cancelKeyCapture();
        battleKeyBindings.cancelKeyCapture();
        if (!context.allowsScoreReset()) {
            return;
        }
        int choice = JOptionPane.showConfirmDialog(this, "모든 모드의 점수 기록을 삭제하시겠습니까?",
                "스코어보드 초기화", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice == JOptionPane.YES_OPTION) {
            try {
                scores.clearScores();
                JOptionPane.showMessageDialog(this, "점수 기록을 초기화했습니다.");
            } catch (RuntimeException e) {
                JOptionPane.showMessageDialog(this, e.getMessage(), "초기화 실패", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
