package seoultech.se.tetris.ui.versus;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import seoultech.se.tetris.versus.VersusMode;

/**
 * 대전 모드 선택 화면의 항목과 선택 상태. Swing/AWT에 의존하지 않는다.
 *
 * <p>일반 / 아이템 / 시간제한 모드 아래에 "뒤로 가기" 항목을 둔다.
 * 모드를 고르면 선택한 {@link VersusMode}를 넘기고, 뒤로 가기를 고르면 이전 화면으로 돌아간다.
 */
public class VersusModeSelectModel {

    /** 화면에 표시할 항목 하나. 뒤로 가기 항목은 {@code mode}가 {@code null}이다. */
    public record Option(VersusMode mode, String label, String description) {
    }

    static final String BACK_LABEL = "뒤로 가기";

    private final List<Option> options;
    private final Consumer<VersusMode> onSelect;
    private final Runnable onBack;
    private int selectedIndex;
    private boolean keyHintVisible;

    public VersusModeSelectModel(Consumer<VersusMode> onSelect, Runnable onBack) {
        this.onSelect = Objects.requireNonNull(onSelect, "onSelect");
        this.onBack = Objects.requireNonNull(onBack, "onBack");
        this.options = List.of(
                new Option(VersusMode.NORMAL, "일반 모드", "2줄 이상 지우면 상대에게 공격"),
                new Option(VersusMode.ITEM, "아이템 모드", "아이템이 등장하는 일반 대전"),
                new Option(VersusMode.TIME_LIMIT, "시간제한 모드", timeLimitDescription()),
                new Option(null, BACK_LABEL, ""));
    }

    /** 제한 시간은 {@link VersusMode}에 정해진 값을 그대로 보여준다. */
    private static String timeLimitDescription() {
        long minutes = VersusMode.TIME_LIMIT.timeLimit().orElseThrow().toMinutes();
        return minutes + "분 뒤 점수가 높은 플레이어 승리";
    }

    public List<Option> getOptions() {
        return options;
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public boolean isKeyHintVisible() {
        return keyHintVisible;
    }

    /** 위 항목으로 이동한다. 첫 항목에서는 마지막 항목으로 순환한다. */
    public void moveUp() {
        selectedIndex = (selectedIndex - 1 + options.size()) % options.size();
        keyHintVisible = false;
    }

    /** 아래 항목으로 이동한다. 마지막 항목에서는 첫 항목으로 순환한다. */
    public void moveDown() {
        selectedIndex = (selectedIndex + 1) % options.size();
        keyHintVisible = false;
    }

    /** 현재 항목을 선택한다. 모드면 해당 모드를 넘기고, 뒤로 가기면 이전 화면으로 돌아간다. */
    public void select() {
        keyHintVisible = false;
        VersusMode mode = options.get(selectedIndex).mode();
        if (mode == null) {
            onBack.run();
        } else {
            onSelect.accept(mode);
        }
    }

    /** 선택 위치와 관계없이 이전 화면으로 돌아간다. (Esc) */
    public void back() {
        keyHintVisible = false;
        onBack.run();
    }

    /** 사용하지 않는 키가 입력되었을 때 호출한다. 사용 가능한 키 안내를 표시한다. */
    public void rejectInput() {
        keyHintVisible = true;
    }
}
