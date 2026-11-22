package seoultech.se.tetris.ui.game;

/**
 * {@link GamePanel}이 보드 위에 덧그리는 일시적인 효과. 게임 상태({@code GameState})와 별개로 화면 표시에만 쓴다.
 *
 * <p>효과를 계산하는 쪽은 Swing에 의존하지 않도록 이 인터페이스만 구현하고, 그리기는 {@link GamePanel}이 한다.
 */
public interface BoardEffects {

    /** 아무 효과도 없음 */
    BoardEffects NONE = new BoardEffects() {
        @Override public String popupText() { return null; }
        @Override public double popupProgress() { return 1.0; }
        @Override public boolean isQueueRowHighlighted(int row) { return false; }
        @Override public int highlightedBottomRows() { return 0; }
        @Override public double bottomRowsHighlightStrength() { return 0.0; }
        @Override public boolean isActive() { return false; }
    };

    /** 보드 위에 떠오르게 표시할 문구. 없으면 {@code null} */
    String popupText();

    /** 문구 애니메이션의 진행률. 0이면 막 나타났고 1이면 사라졌다 */
    double popupProgress();

    /** 공격 대기열의 위에서 {@code row}번째 줄(0부터)을 강조해 그릴지 */
    boolean isQueueRowHighlighted(int row);

    /** 보드 맨 아래부터 강조할 줄 수. 없으면 0 */
    int highlightedBottomRows();

    /** 맨 아래 줄 강조의 세기. 1이면 가장 밝고 0이면 강조하지 않는다 */
    double bottomRowsHighlightStrength();

    /** 진행 중인 효과가 있는지. 없으면 다시 그릴 필요가 없다 */
    boolean isActive();
}
