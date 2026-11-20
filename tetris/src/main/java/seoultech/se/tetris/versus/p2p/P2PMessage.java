package seoultech.se.tetris.versus.p2p;

import java.io.Serializable;
import java.util.Objects;
import seoultech.se.tetris.component.model.Attack;
import seoultech.se.tetris.engine.Difficulty;
import seoultech.se.tetris.versus.BoardSnapshot;
import seoultech.se.tetris.versus.MatchResult;
import seoultech.se.tetris.versus.VersusMode;

/**
 * P2P 대전 모드에서 두 PC가 주고받는 메시지.
 *
 * <p>두 PC는 각자 자신의 보드를 진행하고, 서로의 화면 정보와 공격 정보만 주고받는다. (3차 팀 결정)
 * 메시지는 직렬화해서 보내며, 받는 쪽은 이 파일에 있는 메시지와 그 안에 담기는 값의 클래스만 받아들인다.
 *
 * <p>보내는 쪽에 따라 다음과 같이 나뉜다. (서→클: 서버가 보냄, 클→서: 클라이언트가 보냄, 양쪽: 둘 다 보냄)
 *
 * <table>
 *   <tr><th>단계</th><th>메시지</th><th>방향</th></tr>
 *   <tr><td>연결</td><td>{@link Hello}, {@link Ping}, {@link Pong}, {@link Bye}</td><td>양쪽</td></tr>
 *   <tr><td>대기</td><td>{@link Settings}, {@link Start}</td><td>서→클</td></tr>
 *   <tr><td>대기</td><td>{@link Ready}</td><td>양쪽</td></tr>
 *   <tr><td>대전</td><td>{@link Snapshot}, {@link SendAttack}, {@link End}, {@link Forfeit}</td><td>양쪽</td></tr>
 *   <tr><td>대전</td><td>{@link TimeUp}, {@link Result}</td><td>서→클</td></tr>
 *   <tr><td>대전</td><td>{@link FinalScore}</td><td>클→서</td></tr>
 * </table>
 */
public sealed interface P2PMessage extends Serializable {

    /** 서로 같은 판의 프로그램인지 확인하는 번호. 메시지 형식을 바꾸면 올린다. */
    int PROTOCOL_VERSION = 1;

    // ------------------------------------------------------------------
    // 연결
    // ------------------------------------------------------------------

    /** 연결 직후 처음 주고받는 메시지. 번호가 다르면 연결을 끊는다. */
    record Hello(int version) implements P2PMessage {
    }

    /**
     * 연결 상태 확인 신호. 0.5초마다 보낸다. (3차 팀 결정)
     *
     * @param sentAtMillis 보낸 쪽의 시각. 받은 쪽은 그대로 {@link Pong}에 담아 돌려준다
     */
    record Ping(long sentAtMillis) implements P2PMessage {
    }

    /** {@link Ping}에 대한 응답. 보낸 쪽은 지금 시각과의 차이로 왕복 시간을 잰다. */
    record Pong(long sentAtMillis) implements P2PMessage {
    }

    /** 스스로 연결을 끊는다. 받은 쪽은 기다리지 않고 바로 연결 끊김으로 처리한다. (3차 팀 결정) */
    record Bye() implements P2PMessage {
    }

    // ------------------------------------------------------------------
    // 대기
    // ------------------------------------------------------------------

    /** 서버가 고른 대전 모드와 난이도. 서버가 바꿀 때마다 보내며, 클라이언트는 대기 화면에 표시한다. */
    record Settings(VersusMode mode, Difficulty difficulty) implements P2PMessage {
        public Settings {
            Objects.requireNonNull(mode, "mode");
            Objects.requireNonNull(difficulty, "difficulty");
        }
    }

    /** '게임 시작' 버튼을 눌렀는지(또는 취소했는지) */
    record Ready(boolean ready) implements P2PMessage {
    }

    /**
     * 두 플레이어가 모두 준비되어 대전을 시작한다. 받은 시점부터 양쪽이 카운트다운을 한다.
     *
     * @param seed 블럭 순서를 정하는 시드. 두 PC가 같은 순서로 블럭을 받는다 (3차 팀 결정)
     */
    record Start(VersusMode mode, Difficulty difficulty, long seed) implements P2PMessage {
        public Start {
            Objects.requireNonNull(mode, "mode");
            Objects.requireNonNull(difficulty, "difficulty");
        }
    }

    // ------------------------------------------------------------------
    // 대전
    // ------------------------------------------------------------------

    /** 보낸 쪽 보드의 현재 화면 정보. 받은 쪽은 가장 최신 것만 화면에 반영한다. (3차 팀 결정) */
    record Snapshot(BoardSnapshot snapshot) implements P2PMessage {
        public Snapshot {
            Objects.requireNonNull(snapshot, "snapshot");
        }
    }

    /** 상대에게 보내는 공격. 받은 쪽은 자신의 공격 대기 줄에 넣는다. */
    record SendAttack(Attack attack) implements P2PMessage {
        public SendAttack {
            Objects.requireNonNull(attack, "attack");
        }
    }

    /**
     * 보낸 쪽의 게임이 끝났다는 신호. 한 대전에서 양쪽이 한 번씩 보낸다.
     *
     * <ul>
     *   <li>블럭을 쌓을 수 없어 끝났으면 {@code toppedOut = true}. 그 전에 상대의 종료 신호를 받았는지를 함께 담는다</li>
     *   <li>상대의 종료 신호를 받고 자신의 게임을 멈췄으면 {@code toppedOut = false, opponentEndReceived = true}</li>
     * </ul>
     *
     * <p>서버는 양쪽의 신호를 모아 승패를 정한다. (3차 팀 결정)
     * 두 신호 모두 상대의 신호를 받기 전에 블럭을 쌓을 수 없게 된 것이면 엇갈림으로 무승부,
     * 아니면 먼저 끝난 쪽(상대의 신호를 받기 전에 블럭을 쌓을 수 없게 된 쪽)의 패배다.
     *
     * @param finalScore 보낸 쪽의 최종 점수
     */
    record End(boolean toppedOut, boolean opponentEndReceived, int finalScore) implements P2PMessage {
        public End {
            if (!toppedOut && !opponentEndReceived) {
                throw new IllegalArgumentException("블럭을 쌓을 수 없게 되었거나 상대의 종료 신호를 받은 경우에만 보냅니다.");
            }
            if (finalScore < 0) {
                throw new IllegalArgumentException("점수는 음수일 수 없습니다: " + finalScore);
            }
        }
    }

    /**
     * 서버 측 시간으로 제한 시간이 끝났다. 클라이언트는 자신의 게임을 마무리하고 {@link FinalScore}를 보낸다.
     *
     * <p>시간 종료와 블럭을 쌓을 수 없게 된 것이 겹치면, 서버가 먼저 처리한 쪽으로 판정한다.
     * (3차 팀 결정: 서버 측 시간 기준)
     * 서버가 이 메시지를 보낸 뒤에 도착한 클라이언트의 {@link End}는 승패에 쓰지 않는다.
     */
    record TimeUp() implements P2PMessage {
    }

    /** {@link TimeUp}을 받고 게임을 마무리한 클라이언트의 최종 점수 */
    record FinalScore(int score) implements P2PMessage {
        public FinalScore {
            if (score < 0) {
                throw new IllegalArgumentException("점수는 음수일 수 없습니다: " + score);
            }
        }
    }

    /** 서버가 정한 대전 결과. 클라이언트는 이 결과를 그대로 표시한다. */
    record Result(MatchResult result) implements P2PMessage {
        public Result {
            Objects.requireNonNull(result, "result");
        }
    }

    /**
     * 대전 도중(카운트다운 포함) 게임 나가기를 선택했다.
     * 보낸 쪽은 바로 이어서 {@link Bye}를 보내고 연결을 끊는다.
     *
     * <p>받은 쪽은 결과가 아직 정해지지 않았으면 상대의 기권패를 표시한다. 이미 정해졌으면 그 결과를 유지한다. (3차 팀 결정)
     */
    record Forfeit() implements P2PMessage {
    }
}
