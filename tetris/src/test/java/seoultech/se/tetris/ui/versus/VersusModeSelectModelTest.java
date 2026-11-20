package seoultech.se.tetris.ui.versus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import seoultech.se.tetris.versus.VersusMode;

class VersusModeSelectModelTest {

    private final List<String> log = new ArrayList<>();
    private VersusModeSelectModel model;

    @BeforeEach
    void setUp() {
        model = new VersusModeSelectModel(mode -> log.add("select:" + mode), () -> log.add("back"));
    }

    private List<String> labels() {
        return model.getOptions().stream().map(VersusModeSelectModel.Option::label).toList();
    }

    @Test
    void 항목은_일반_아이템_시간제한_뒤로_가기_순서다() {
        assertEquals(List.of("일반 모드", "아이템 모드", "시간제한 모드", "뒤로 가기"), labels());
    }

    @Test
    void 모드_항목은_각각의_대전_모드를_가지고_뒤로_가기는_모드가_없다() {
        List<VersusModeSelectModel.Option> options = model.getOptions();

        assertEquals(VersusMode.NORMAL, options.get(0).mode());
        assertEquals(VersusMode.ITEM, options.get(1).mode());
        assertEquals(VersusMode.TIME_LIMIT, options.get(2).mode());
        assertNull(options.get(3).mode());
    }

    @Test
    void 모든_대전_모드를_선택할_수_있다() {
        List<VersusMode> modes = model.getOptions().stream()
                .map(VersusModeSelectModel.Option::mode)
                .filter(mode -> mode != null)
                .toList();

        assertEquals(List.of(VersusMode.values()), modes);
    }

    @Test
    void 시간제한_모드_설명은_대전_모드의_제한_시간을_따른다() {
        String description = model.getOptions().get(2).description();

        assertTrue(description.startsWith("3분"), description);
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

        model.moveUp();
        assertEquals(2, model.getSelectedIndex());
    }

    @Test
    void 일반_모드를_선택하면_일반_모드를_넘긴다() {
        model.select();

        assertEquals(List.of("select:NORMAL"), log);
    }

    @Test
    void 아이템_모드를_선택하면_아이템_모드를_넘긴다() {
        model.moveDown();
        model.select();

        assertEquals(List.of("select:ITEM"), log);
    }

    @Test
    void 시간제한_모드를_선택하면_시간제한_모드를_넘긴다() {
        model.moveDown();
        model.moveDown();
        model.select();

        assertEquals(List.of("select:TIME_LIMIT"), log);
    }

    @Test
    void 뒤로_가기를_선택하면_이전_화면으로_돌아간다() {
        model.moveUp();
        model.select();

        assertEquals(List.of("back"), log);
    }

    @Test
    void Esc는_선택_위치와_관계없이_이전_화면으로_돌아간다() {
        model.moveDown();
        model.back();

        assertEquals(List.of("back"), log);
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
    void 선택하거나_뒤로_가면_안내가_사라진다() {
        model.rejectInput();
        model.select();
        assertFalse(model.isKeyHintVisible());

        model.rejectInput();
        model.back();
        assertFalse(model.isKeyHintVisible());
    }

    @Test
    void 동작이_없으면_만들_수_없다() {
        assertThrows(NullPointerException.class, () -> new VersusModeSelectModel(null, () -> { }));
        assertThrows(NullPointerException.class, () -> new VersusModeSelectModel(mode -> { }, null));
    }
}
