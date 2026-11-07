package seoultech.se.tetris.versus;

/**
 * 대전 한 판의 진행 단계.
 *
 * <pre>
 * READY ──begin()──▶ PLAYING ◀──pause()/resume()──▶ PAUSED
 *   │                   │                             │
 *   └───────────────────┼─────────────────────────────┘
 *                       ▼
 *                     ENDED
 * </pre>
 *
 * <ul>
 *   <li>PLAYING → ENDED: 한쪽 패배, 제한 시간 종료, 나가기({@code quit()})</li>
 *   <li>READY·PAUSED → ENDED: 나가기({@code quit()}). 카운트다운 중이나 일시정지 메뉴에서 종료한 경우</li>
 * </ul>
 */
public enum MatchPhase {

    /**
     * 시작 전. 시작 전 카운트다운이 표시되는 동안이다.
     *
     * <p>이 동안에는 블럭이 떨어지지 않고 조작도 받지 않는다. (3차 팀 결정: 모든 대전 모드에서 3초 카운트다운)
     */
    READY,

    /** 진행 중 */
    PLAYING,

    /** 일시정지. 로컬 대전 모드에서만 쓰며, 두 플레이어의 보드와 제한 시간이 모두 멈춘다. */
    PAUSED,

    /** 끝남. 결과가 있으면 {@link MatchResult}로 확인한다. */
    ENDED
}
