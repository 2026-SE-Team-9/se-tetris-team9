package seoultech.se.tetris.component.controller;

import seoultech.se.tetris.component.model.Attack;

/**
 * 대전 모드에서 대전을 진행하는 쪽이 엔진에 내리는 명령.
 *
 * <p>플레이어의 조작({@link GameController})과 분리한다.
 * 키 입력은 조작 명령만, 대전을 진행하는 쪽은 이 명령만 쓰므로 각자에게 필요한 면만 보인다.
 */
public interface VersusController {

    /**
     * 상대의 공격을 대기 줄에 넣는다. (3차 「공격 대기열」)
     *
     * <ul>
     *   <li>대기 줄은 최대 10줄이며, 공간이 모자라면 남은 줄 수만큼 공격의 위쪽 줄부터 기존 대기 줄의 아래에 붙인다.
     *       남은 줄이 없으면 공격은 무시된다</li>
     *   <li>대기 줄은 현재 블럭을 처리한 뒤 다음 블럭을 생성하기 직전에 한 번에 보드 아래쪽에 추가된다</li>
     *   <li>추가하면서 보드 위로 밀려나는 칸이 생기면 게임 오버가 된다
     *       ({@link seoultech.se.tetris.component.listener.GameEventListener#onGameOver(int)})</li>
     *   <li>게임이 끝난 뒤에는 무시한다</li>
     * </ul>
     *
     * @throws IllegalArgumentException 공격 줄의 폭이 보드 폭과 다른 경우.
     *         {@link seoultech.se.tetris.component.model.GarbageLine}은 보드 폭을 모르므로 받는 쪽에서 검사한다
     */
    void receiveAttack(Attack attack);

    /**
     * 시간제한 모드의 제한 시간 종료.
     *
     * <p>진행 중인 줄 삭제 애니메이션이 있으면(아이템 효과 뒤 이어지는 줄 삭제 포함) 애니메이션 없이 즉시 마무리해
     * 점수에 반영한 뒤, 다음 블럭을 만들지 않고 게임을 끝낸다. 대기 중인 공격 줄은 보드에 추가하지 않는다.
     *
     * <p>블럭을 쌓지 못해 끝난 것이 아니므로
     * {@link seoultech.se.tetris.component.listener.GameEventListener#onGameOver(int)}는 호출하지 않는다.
     * 이미 끝난 게임에서는 아무 일도 하지 않는다.
     */
    void finishByTimeUp();
}
