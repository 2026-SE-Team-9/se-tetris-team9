package seoultech.se.tetris.engine;

/**
 * Double or Nothing ({@code D}).
 *
 * <p>고정은 일반 블럭과 같고, {@code D} 칸은 그 줄이 지워질 때까지 보드에 남는다.
 * {@code D}가 있는 줄이 <b>꽉 찬 줄로</b> 지워지면 그 줄 삭제의 최종 점수를 2배로 한다.
 * 줄 삭제 아이템·무게추·Tall Boy·Crusher에 지워지면 아무 효과 없이 사라진다. (2차 팀 결정)
 */
final class DoubleOrNothingEffect implements ItemEffect {

    /** 꽉 찬 줄로 지워질 때의 점수 배수 */
    static final int MULTIPLIER = 2;

    @Override
    public int lineClearMultiplier() {
        return MULTIPLIER;
    }
}
