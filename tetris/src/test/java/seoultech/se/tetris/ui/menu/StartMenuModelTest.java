package seoultech.se.tetris.ui.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import seoultech.se.tetris.engine.GameMode;
import seoultech.se.tetris.ui.ScreenNavigator;

class StartMenuModelTest {

    private final List<String> calls = new ArrayList<>();
    private StartMenuModel model;

    @BeforeEach
    void setUp() {
        model = new StartMenuModel(List.of(
                new MenuItem("시작", () -> calls.add("start")),
                new MenuItem("설정", () -> calls.add("settings")),
                new MenuItem("스코어보드", () -> calls.add("scoreboard")),
                new MenuItem("종료", () -> calls.add("exit"))));
    }

    @Test
    void 첫_항목이_기본으로_선택된다() {
        assertEquals(0, model.getSelectedIndex());
    }

    @Test
    void 아래로_이동하고_끝에서는_처음으로_순환한다() {
        model.moveDown();
        assertEquals(1, model.getSelectedIndex());
        model.moveDown();
        model.moveDown();
        model.moveDown();
        assertEquals(0, model.getSelectedIndex());
    }

    @Test
    void 위로_이동하고_처음에서는_끝으로_순환한다() {
        model.moveUp();
        assertEquals(3, model.getSelectedIndex());
    }

    @Test
    void 선택하면_해당_항목의_동작이_수행된다() {
        model.moveDown();
        model.moveDown();
        model.select();
        assertEquals(List.of("scoreboard"), calls);
    }

    @Test
    void 허용되지_않은_키는_안내를_표시하고_이동하면_사라진다() {
        assertFalse(model.isKeyHintVisible());
        model.rejectInput();
        assertTrue(model.isKeyHintVisible());
        model.moveDown();
        assertFalse(model.isKeyHintVisible());
    }

    @Test
    void 빈_메뉴는_만들_수_없다() {
        assertThrows(IllegalArgumentException.class, () -> new StartMenuModel(List.of()));
    }

    // ------------------------------------------------------------------
    // 기본 메뉴 (일반 모드 / 아이템 모드)
    // ------------------------------------------------------------------

    private static ScreenNavigator navigator(List<String> log) {
        return new ScreenNavigator() {
            @Override public void navigateToMenu() { log.add("menu"); }
            @Override public void navigateToGame(GameMode mode) { log.add("game:" + mode); }
            @Override public void navigateToSettings() { log.add("settings"); }
            @Override public void navigateToScoreboard() { log.add("scoreboard"); }
            @Override public void navigateToGameOver(int finalScore) { log.add("gameOver"); }
            @Override public void exitProgram() { log.add("exit"); }
        };
    }

    @Test
    void 기본_메뉴는_시작_아이템_모드_설정_스코어보드_종료_순서다() {
        StartMenuModel menu = StartMenuModel.createDefault(navigator(new ArrayList<>()));

        assertEquals(List.of("시작", "아이템 모드", "설정", "스코어보드", "종료"),
                menu.getItems().stream().map(MenuItem::label).toList());
    }

    @Test
    void 시작을_선택하면_일반_모드로_게임을_시작한다() {
        List<String> log = new ArrayList<>();
        StartMenuModel menu = StartMenuModel.createDefault(navigator(log));

        menu.select();

        assertEquals(List.of("game:NORMAL"), log);
    }

    @Test
    void 아이템_모드를_선택하면_아이템_모드로_게임을_시작한다() {
        List<String> log = new ArrayList<>();
        StartMenuModel menu = StartMenuModel.createDefault(navigator(log));

        menu.moveDown();
        menu.select();

        assertEquals(List.of("game:ITEM"), log);
    }

    @Test
    void 항목이_늘어도_나머지_항목은_기존_동작을_한다() {
        List<String> log = new ArrayList<>();
        StartMenuModel menu = StartMenuModel.createDefault(navigator(log));

        for (int i = 0; i < 2; i++) {
            menu.moveDown();
        }
        menu.select();
        menu.moveDown();
        menu.select();
        menu.moveDown();
        menu.select();

        assertEquals(List.of("settings", "scoreboard", "exit"), log);
    }

    @Test
    void 기본_메뉴에서도_위로_이동하면_마지막_항목으로_순환한다() {
        StartMenuModel menu = StartMenuModel.createDefault(navigator(new ArrayList<>()));

        menu.moveUp();

        assertEquals(menu.getItems().size() - 1, menu.getSelectedIndex());
    }

    @Test
    void 모든_모드가_메뉴에서_시작할_수_있다() {
        List<String> log = new ArrayList<>();
        StartMenuModel menu = StartMenuModel.createDefault(navigator(log));

        for (int i = 0; i < GameMode.values().length; i++) {
            menu.select();
            menu.moveDown();
        }

        for (GameMode mode : GameMode.values()) {
            assertTrue(log.contains("game:" + mode), mode.name());
        }
    }
}
