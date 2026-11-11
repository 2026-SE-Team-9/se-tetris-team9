package seoultech.se.tetris.component.model;

/**
 * 공격 줄 테스트용 도우미. 줄을 문자열로 적어서 만든다.
 *
 * <p>{@code "X"}는 채워진 칸, {@code "."}은 빈 칸이다. 예: {@code line("XXX.XXXXXX")}
 */
public final class GarbageLineFixtures {

    private GarbageLineFixtures() {
    }

    public static GarbageLine line(String text) {
        return new GarbageLine(cells(text));
    }

    public static boolean[] cells(String text) {
        boolean[] cells = new boolean[text.length()];
        for (int i = 0; i < cells.length; i++) {
            cells[i] = text.charAt(i) == 'X';
        }
        return cells;
    }
}
