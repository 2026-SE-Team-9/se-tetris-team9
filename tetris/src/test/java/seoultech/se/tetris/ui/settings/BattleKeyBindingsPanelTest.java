package seoultech.se.tetris.ui.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.KeyEvent;
import java.nio.file.Path;
import java.util.OptionalInt;

import javax.swing.JButton;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seoultech.se.tetris.config.BattleKeyBindingEditor;
import seoultech.se.tetris.config.BattleKeyConfigService.BattleAction;
import seoultech.se.tetris.config.BattleKeyConfigService.BattleKeySlot;
import seoultech.se.tetris.config.BattleKeyConfigService.BattlePlayer;
import seoultech.se.tetris.config.PropertiesBattleKeyConfigService;

class BattleKeyBindingsPanelTest {

    @TempDir
    Path tempDir;

    private PropertiesBattleKeyConfigService config;
    private BattleKeyBindingEditor editor;
    private BattleKeyBindingsPanel panel;

    @BeforeEach
    void setUp() {
        config = new PropertiesBattleKeyConfigService(tempDir.resolve("keys.properties"));
        editor = new BattleKeyBindingEditor(config, () -> {});
        panel = new BattleKeyBindingsPanel(editor);
    }

    @Test
    void panel_initializesRowsAndButtons() {
        var rows = panel.navigationRows();
        assertEquals(5, rows.size());
        for (var row : rows) {
            assertEquals(2, row.size()); // 1P, 2P 버튼 2개
            assertTrue(row.get(0) instanceof JButton);
            assertTrue(row.get(1) instanceof JButton);
        }
    }

    @Test
    void setInteractive_disablesButtonsAndNavigation() {
        assertTrue(panel.isInteractive());
        panel.setInteractive(false);
        assertFalse(panel.isInteractive());

        var rows = panel.navigationRows();
        assertTrue(rows.isEmpty());
    }

    @Test
    void focusMissingKey_returnsTrueWhenKeyIsMissing() {
        assertFalse(panel.focusMissingKey());

        // 1P ROTATE 키를 비움
        editor.captureKey(new BattleKeySlot(BattlePlayer.P1, BattleAction.ROTATE), KeyEvent.VK_ESCAPE);
        assertTrue(editor.getDraftBinding(BattlePlayer.P1, BattleAction.ROTATE).isEmpty());

        assertTrue(panel.focusMissingKey());
    }

    @Test
    void cancelKeyCapture_resetsCapturingState() {
        assertFalse(panel.isCapturingKey());
        panel.cancelKeyCapture();
        assertFalse(panel.isCapturingKey());
    }
}
