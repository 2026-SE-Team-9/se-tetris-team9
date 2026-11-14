package seoultech.se.tetris.versus.p2p;

import seoultech.se.tetris.versus.Side;

/**
 * P2P 대전 모드에서 이 PC의 역할.
 *
 * <p>대전 결과({@link seoultech.se.tetris.versus.MatchResult})는 자리({@link Side})로 승자를 나타내므로,
 * 서버를 {@link Side#P1}, 클라이언트를 {@link Side#P2}로 고정한다. 양쪽 PC가 같은 결과를 같은 뜻으로 읽기 위해서다.
 */
public enum Role {

    /** 접속을 기다리는 쪽. 대전 모드와 난이도를 고르고, 제한 시간과 승패를 판정한다. (3차 팀 결정) */
    SERVER,

    /** IP와 포트를 입력해 서버에 접속하는 쪽 */
    CLIENT;

    /** 이 역할의 자리 */
    public Side side() {
        return this == SERVER ? Side.P1 : Side.P2;
    }

    /** 상대의 역할 */
    public Role opponent() {
        return this == SERVER ? CLIENT : SERVER;
    }
}
