package seoultech.se.tetris.ui.menu;

/** 시작 메뉴에 표시되는 항목 하나. 라벨과 선택 시 수행할 동작을 가진다. */
public record MenuItem(String label, Runnable action) {
}
