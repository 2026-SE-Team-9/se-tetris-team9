package seoultech.se.tetris.versus;

/**
 * 대전에서 플레이어의 자리.
 *
 * <p>로컬 대전에서는 화면 왼쪽이 {@link #P1}, 오른쪽이 {@link #P2}다.
 */
public enum Side {

    P1,
    P2;

    /** 상대의 자리 */
    public Side opponent() {
        return this == P1 ? P2 : P1;
    }
}
