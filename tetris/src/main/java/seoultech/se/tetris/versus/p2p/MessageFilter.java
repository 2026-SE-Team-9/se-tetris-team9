package seoultech.se.tetris.versus.p2p;

import java.io.ObjectInputFilter;
import java.util.HashSet;
import java.util.Set;
import seoultech.se.tetris.blocks.Block;
import seoultech.se.tetris.blocks.BlockType;
import seoultech.se.tetris.blocks.ItemType;
import seoultech.se.tetris.component.model.Attack;
import seoultech.se.tetris.component.model.GarbageLine;
import seoultech.se.tetris.engine.Difficulty;
import seoultech.se.tetris.versus.BoardSnapshot;
import seoultech.se.tetris.versus.EndReason;
import seoultech.se.tetris.versus.MatchResult;
import seoultech.se.tetris.versus.Side;
import seoultech.se.tetris.versus.VersusMode;

/**
 * 받은 데이터를 객체로 되돌릴 때 허용 목록에 있는 클래스만 받아들인다. (3차 팀 결정)
 *
 * <p>직렬화된 데이터는 어떤 클래스든 만들어 낼 수 있으므로, 상대가 보낸 데이터를 그대로 되돌리면
 * 의도하지 않은 클래스의 코드가 실행될 수 있다. 메시지와 그 안에 담기는 값의 클래스만 허용하고,
 * 크기·깊이·개수에도 한도를 둔다. 한도는 메시지 하나를 기준으로 한다.
 */
final class MessageFilter implements ObjectInputFilter {

    /** 메시지 하나의 최대 크기(바이트). 화면 정보 하나가 수 KB이므로 여유를 둔 값이다. */
    static final long MAX_BYTES = 64 * 1024;

    /** 객체가 다른 객체를 품는 최대 깊이 */
    static final long MAX_DEPTH = 10;

    /** 메시지 하나에 담기는 최대 객체 수 */
    static final long MAX_REFERENCES = 1_000;

    /** 배열의 최대 길이. 보드 높이(20)와 공격 대기 줄(10)보다 넉넉한 값이다. */
    static final long MAX_ARRAY_LENGTH = 64;

    private static final Set<Class<?>> ALLOWED = allowedClasses();

    private static Set<Class<?>> allowedClasses() {
        Set<Class<?>> allowed = new HashSet<>();
        // 메시지: 새 메시지를 추가하면 자동으로 포함된다
        allowed.addAll(Set.of(P2PMessage.class.getPermittedSubclasses()));
        // 메시지에 담기는 값
        allowed.addAll(Set.of(
                BoardSnapshot.class, Block.class, BlockType.class, ItemType.class,
                GarbageLine.class, Attack.class,
                MatchResult.class, Side.class, EndReason.class, VersusMode.class, Difficulty.class));
        // 값 안의 배열
        allowed.addAll(Set.of(int[].class, int[][].class, boolean[].class, boolean[][].class));
        // 열거형의 상위 클래스
        allowed.add(Enum.class);
        return Set.copyOf(allowed);
    }

    /**
     * {@code List.copyOf}로 만든 목록. 대리 객체({@code CollSer})로 직렬화되고, 되돌릴 때 대리 객체가 고칠 수 없는 목록을 다시 만든다.
     * 모두 공개되지 않은 JDK 내부 클래스라 이름으로 허용한다.
     */
    private static final Set<String> IMMUTABLE_LIST_CLASSES = Set.of(
            "java.util.CollSer",
            "java.util.ImmutableCollections$List12",
            "java.util.ImmutableCollections$ListN");

    @Override
    public Status checkInput(FilterInfo info) {
        if (info.streamBytes() > MAX_BYTES
                || info.depth() > MAX_DEPTH
                || info.references() > MAX_REFERENCES
                || info.arrayLength() > MAX_ARRAY_LENGTH) {
            return Status.REJECTED;
        }
        Class<?> type = info.serialClass();
        if (type == null) {
            return Status.UNDECIDED;   // 클래스 없이 한도만 확인하는 호출
        }
        // Object[]는 목록 대리 객체가 원소를 담을 때 쓴다. 원소 하나하나는 다시 이 검사를 거친다
        if (ALLOWED.contains(type) || IMMUTABLE_LIST_CLASSES.contains(type.getName()) || type == Object[].class) {
            return Status.ALLOWED;
        }
        return Status.REJECTED;
    }
}
