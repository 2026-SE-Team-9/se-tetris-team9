package seoultech.se.tetris.engine;

/**
 * 아이템의 동작. 아이템마다 하나씩 구현하고, 엔진은 정해진 시점에 호출한다. (2차 팀 결정)
 *
 * <ul>
 *   <li>{@link #onLock}: 블럭이 더 내려갈 수 없어 고정될 때</li>
 *   <li>{@link #canSlideWhenLanded}: 바닥이나 블럭에 닿은 상태에서 좌우 이동을 허용할지 판단할 때</li>
 *   <li>{@link #lineClearMultiplier}: 이 아이템 칸이 있는 줄이 줄 삭제로 지워질 때</li>
 * </ul>
 *
 * <p>효과는 보드를 직접 고치지 않고 {@link LockContext}(게임 문맥 객체)에 "무엇을 지울지"를 기록한다.
 * 엔진은 기록된 칸을 줄 삭제 애니메이션으로 보여준 뒤에 실제로 지운다.
 *
 * <p>모든 메서드에 기본 동작이 있어, 일반 블럭은 이 인터페이스의 기본 동작을 그대로 쓴다.
 * 새 아이템을 추가할 때는 이 인터페이스를 구현하고 {@link ItemEffects#of}에 등록한다.
 */
interface ItemEffect {

    /**
     * 블럭을 고정할 때의 동작
     *
     * <p>기본 동작: 블럭을 보드에 놓고, 가득 찬 줄이 있으면 줄 삭제로 지운다.
     */
    default void onLock(LockContext context) {
        context.placeBlock();
        context.clearLines(context.fullRows());
    }

    /** 바닥이나 블럭에 닿은 뒤에도 좌우로 움직일 수 있는지 여부. 기본은 움직일 수 있다. */
    default boolean canSlideWhenLanded() {
        return true;
    }

    /**
     * 이 아이템 칸이 있는 줄이 <b>가득 찬 줄로</b> 지워질 때 줄 삭제 점수에 곱할 배수. 기본은 1.
     *
     * <p>한 번의 줄 삭제에 여러 아이템 칸이 있으면 가장 큰 배수 하나만 적용한다.
     */
    default int lineClearMultiplier() {
        return 1;
    }
}
