package seoultech.se.tetris.ui.menu;

import java.util.List;

import seoultech.se.tetris.engine.GameMode;
import seoultech.se.tetris.ui.ScreenNavigator;

/**
 * 시작 메뉴의 항목 목록과 선택 상태. Swing/AWT에 의존하지 않는다.
 * 항목은 리스트로 관리하므로 메뉴 확장 시 목록에 항목만 추가하면 된다.
 */
public class StartMenuModel {

    private final List<MenuItem> items;
    private int selectedIndex;
    private boolean keyHintVisible;

    public StartMenuModel(List<MenuItem> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("메뉴 항목이 비어 있습니다.");
        }
        this.items = List.copyOf(items);
    }

    /** 시작 / 아이템 모드 / 설정 / 스코어보드 / 종료 항목으로 구성된 기본 메뉴를 만든다. 시작은 일반 모드다. */
    public static StartMenuModel createDefault(ScreenNavigator navigator) {
        return new StartMenuModel(List.of(
                new MenuItem("시작", () -> navigator.navigateToGame(GameMode.NORMAL)),
                new MenuItem("아이템 모드", () -> navigator.navigateToGame(GameMode.ITEM)),
                new MenuItem("설정", navigator::navigateToSettings),
                new MenuItem("스코어보드", navigator::navigateToScoreboard),
                new MenuItem("종료", navigator::exitProgram)));
    }

    public List<MenuItem> getItems() {
        return items;
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public boolean isKeyHintVisible() {
        return keyHintVisible;
    }

    /** 위 항목으로 이동한다. 첫 항목에서는 마지막 항목으로 순환한다. */
    public void moveUp() {
        selectedIndex = (selectedIndex - 1 + items.size()) % items.size();
        keyHintVisible = false;
    }

    /** 아래 항목으로 이동한다. 마지막 항목에서는 첫 항목으로 순환한다. */
    public void moveDown() {
        selectedIndex = (selectedIndex + 1) % items.size();
        keyHintVisible = false;
    }

    /** 현재 선택된 항목의 동작을 수행한다. */
    public void select() {
        keyHintVisible = false;
        items.get(selectedIndex).action().run();
    }

    /** 메뉴에서 사용하지 않는 키가 입력되었을 때 호출한다. 사용 가능한 키 안내를 표시한다. */
    public void rejectInput() {
        keyHintVisible = true;
    }
}
