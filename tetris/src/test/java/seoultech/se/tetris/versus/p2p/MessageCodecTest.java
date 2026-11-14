package seoultech.se.tetris.versus.p2p;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seoultech.se.tetris.component.model.GarbageLineFixtures.line;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import seoultech.se.tetris.blocks.Block;
import seoultech.se.tetris.blocks.BlockType;
import seoultech.se.tetris.blocks.ItemType;
import seoultech.se.tetris.component.model.Attack;
import seoultech.se.tetris.component.model.GarbageLine;
import seoultech.se.tetris.engine.Difficulty;
import seoultech.se.tetris.engine.GameMode;
import seoultech.se.tetris.engine.TetrisEngine;
import seoultech.se.tetris.versus.BoardSnapshot;
import seoultech.se.tetris.versus.EndReason;
import seoultech.se.tetris.versus.MatchResult;
import seoultech.se.tetris.versus.Side;
import seoultech.se.tetris.versus.VersusMode;

@DisplayName("MessageCodec")
class MessageCodecTest {

    // ------------------------------------------------------------------
    // 테스트 보조
    // ------------------------------------------------------------------

    private static Attack attack() {
        return new Attack(List.of(line("XXX.XXXXXX"), line("XXXX.XXXXX")));
    }

    /** 아이템 블럭과 공격 대기 줄까지 담은 화면 정보 */
    private static BoardSnapshot snapshotWithEverything() {
        int[][] board = new int[20][10];
        board[19][0] = BlockType.T.code();
        boolean[][] clearing = new boolean[20][10];
        clearing[19][0] = true;
        Block current = Block.of(BlockType.I, 0, 3).withItem(ItemType.LINE_CLEAR, 1, 2);
        Block next = Block.of(ItemType.WEIGHT, 0, 3);
        return new BoardSnapshot(board, current, next, 18, 1_200, 3, 25, false, true, clearing, attack().lines());
    }

    /** 종류별 메시지 하나씩 */
    private static List<P2PMessage> everyMessage() {
        return List.of(
                new P2PMessage.Hello(P2PMessage.PROTOCOL_VERSION),
                new P2PMessage.Ping(123L),
                new P2PMessage.Pong(123L),
                new P2PMessage.Bye(),
                new P2PMessage.Settings(VersusMode.TIME_LIMIT, Difficulty.HARD),
                new P2PMessage.Ready(true),
                new P2PMessage.Start(VersusMode.ITEM, Difficulty.EASY, 42L),
                new P2PMessage.Snapshot(snapshotWithEverything()),
                new P2PMessage.SendAttack(attack()),
                new P2PMessage.End(true, false, 300),
                new P2PMessage.TimeUp(),
                new P2PMessage.FinalScore(500),
                new P2PMessage.Result(MatchResult.win(Side.P2, EndReason.TOP_OUT, 100, 200)),
                new P2PMessage.Forfeit());
    }

    /** 메시지가 아닌, 허용 목록에 없는 직렬화 클래스 */
    private record Unknown(String text) implements Serializable {
    }

    private static byte[] serialize(Object value) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(value);
        }
        return bytes.toByteArray();
    }

    // ------------------------------------------------------------------
    // 되돌리기
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("되돌리기")
    class RoundTrip {

        @Test
        @DisplayName("모든 종류의 메시지가 같은 값으로 되돌아온다")
        void everyMessageType() throws IOException {
            for (P2PMessage message : everyMessage()) {
                assertEquals(message, MessageCodec.decode(MessageCodec.encode(message)), message.toString());
            }
        }

        @Test
        @DisplayName("테스트가 모든 종류의 메시지를 다룬다")
        void coversEveryMessageType() {
            List<Class<?>> covered = new ArrayList<>();
            for (P2PMessage message : everyMessage()) {
                covered.add(message.getClass());
            }

            assertEquals(List.of(P2PMessage.class.getPermittedSubclasses()).size(), covered.size());
            assertTrue(covered.containsAll(List.of(P2PMessage.class.getPermittedSubclasses())));
        }

        @Test
        @DisplayName("실제 게임에서 만든 화면 정보도 되돌아온다")
        void snapshotFromRealGame() throws IOException {
            TetrisEngine engine = TetrisEngine.seeded(7L, Difficulty.NORMAL, GameMode.ITEM);
            engine.dropToBottom();
            engine.finishClearing();
            engine.receiveAttack(attack());
            P2PMessage message = new P2PMessage.Snapshot(BoardSnapshot.of(engine));

            assertEquals(message, MessageCodec.decode(MessageCodec.encode(message)));
        }

        @Test
        @DisplayName("화면 정보 하나는 한도보다 충분히 작다")
        void snapshotIsSmall() throws IOException {
            byte[] bytes = MessageCodec.encode(new P2PMessage.Snapshot(snapshotWithEverything()));

            assertTrue(bytes.length < MessageCodec.MAX_FRAME_BYTES / 8, bytes.length + "바이트");
        }
    }

    // ------------------------------------------------------------------
    // 받은 데이터 검사
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("받은 데이터 검사")
    class Filtering {

        @Test
        @DisplayName("허용 목록에 없는 클래스는 거절한다")
        void rejectsUnknownClass() throws IOException {
            byte[] bytes = serialize(new Unknown("x"));

            assertThrows(InvalidMessageException.class, () -> MessageCodec.decode(bytes));
        }

        @Test
        @DisplayName("자바 기본 클래스라도 허용 목록에 없으면 거절한다")
        void rejectsJdkClass() throws IOException {
            byte[] bytes = serialize(new java.util.HashMap<String, String>());

            assertThrows(InvalidMessageException.class, () -> MessageCodec.decode(bytes));
        }

        @Test
        @DisplayName("허용된 값이라도 메시지가 아니면 거절한다")
        void rejectsNonMessage() throws IOException {
            byte[] bytes = serialize(MatchResult.draw(EndReason.TIME_UP, 0, 0));

            assertThrows(InvalidMessageException.class, () -> MessageCodec.decode(bytes));
        }

        @Test
        @DisplayName("배열 길이가 한도(64)까지는 받아들이고, 넘으면 거절한다 (공격 줄 64개 / 65개)")
        void arrayLengthLimit() throws IOException {
            byte[] atLimit = MessageCodec.encode(attackOf((int) MessageFilter.MAX_ARRAY_LENGTH));
            byte[] overLimit = MessageCodec.encode(attackOf((int) MessageFilter.MAX_ARRAY_LENGTH + 1));

            assertEquals(attackOf((int) MessageFilter.MAX_ARRAY_LENGTH), MessageCodec.decode(atLimit));
            assertThrows(InvalidMessageException.class, () -> MessageCodec.decode(overLimit));
        }

        private P2PMessage attackOf(int count) {
            List<GarbageLine> lines = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                lines.add(line("XXX.XXXXXX"));
            }
            return new P2PMessage.SendAttack(new Attack(lines));
        }

        @Test
        @DisplayName("값이 올바르지 않으면 생성자 검사에 걸려 거절한다 (음수 점수)")
        void rejectsInvalidValue() throws IOException {
            byte[] bytes = MessageCodec.encode(new P2PMessage.FinalScore(0x01020304));
            // 직렬화된 점수(01 02 03 04)를 음수(FF FF FF FF)로 바꾼다
            int at = lastIndexOf(bytes, new byte[] {1, 2, 3, 4});
            for (int i = 0; i < 4; i++) {
                bytes[at + i] = (byte) 0xFF;
            }

            assertThrows(InvalidMessageException.class, () -> MessageCodec.decode(bytes));
        }

        @Test
        @DisplayName("직렬화 데이터가 아니면 거절한다")
        void rejectsGarbageBytes() {
            byte[] bytes = {1, 2, 3, 4, 5};

            assertThrows(InvalidMessageException.class, () -> MessageCodec.decode(bytes));
        }

        private int lastIndexOf(byte[] data, byte[] pattern) {
            for (int i = data.length - pattern.length; i >= 0; i--) {
                boolean match = true;
                for (int j = 0; j < pattern.length && match; j++) {
                    match = data[i + j] == pattern[j];
                }
                if (match) {
                    return i;
                }
            }
            throw new AssertionError("패턴을 찾지 못했습니다");
        }
    }

    // ------------------------------------------------------------------
    // 통로에 쓰고 읽기
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("통로에 쓰고 읽기")
    class Framing {

        @Test
        @DisplayName("여러 메시지를 쓴 순서대로 하나씩 읽는다")
        void keepsOrder() throws IOException {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            for (P2PMessage message : everyMessage()) {
                MessageCodec.write(out, message);
            }

            DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()));
            for (P2PMessage message : everyMessage()) {
                assertEquals(message, MessageCodec.read(in));
            }
        }

        @Test
        @DisplayName("같은 객체를 내용만 바꿔 다시 보내도 받는 쪽은 바뀐 내용을 받는다")
        void alwaysSendsCurrentContent() throws IOException {
            TetrisEngine engine = TetrisEngine.seeded(1L, Difficulty.NORMAL, GameMode.NORMAL);
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            MessageCodec.write(out, new P2PMessage.Snapshot(BoardSnapshot.of(engine)));
            engine.dropToBottom();
            MessageCodec.write(out, new P2PMessage.Snapshot(BoardSnapshot.of(engine)));

            DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()));
            MessageCodec.read(in);
            P2PMessage.Snapshot second = (P2PMessage.Snapshot) MessageCodec.read(in);

            assertEquals(engine.getScore(), second.snapshot().score());
        }

        @Test
        @DisplayName("한도보다 큰 메시지는 보내지 않고 예외를 던진다")
        void rejectsTooLargeToSend() throws IOException {
            List<GarbageLine> lines = new ArrayList<>();
            for (int i = 0; i < 5_000; i++) {
                lines.add(line("XXX.XXXXXX"));
            }
            P2PMessage huge = new P2PMessage.SendAttack(new Attack(lines));
            assertTrue(MessageCodec.encode(huge).length > MessageCodec.MAX_FRAME_BYTES);
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();

            assertThrows(InvalidMessageException.class, () -> MessageCodec.write(new DataOutputStream(bytes), huge));
            assertEquals(0, bytes.size());   // 아무것도 쓰지 않았다
        }

        @Test
        @DisplayName("더 읽을 것이 없으면 EOFException을 던진다 (상대가 닫음)")
        void endOfStream() {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(new byte[0]));

            assertThrows(EOFException.class, () -> MessageCodec.read(in));
        }

        @Test
        @DisplayName("길이가 0 이하이거나 한도보다 크면 내용을 읽기 전에 거절한다")
        void rejectsBadLength() throws IOException {
            for (int length : new int[] {0, -1, MessageCodec.MAX_FRAME_BYTES + 1}) {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                new DataOutputStream(bytes).writeInt(length);
                DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()));

                InvalidMessageException e = assertThrows(InvalidMessageException.class, () -> MessageCodec.read(in));
                assertTrue(e.getMessage().contains(String.valueOf(length)));
            }
        }

        @Test
        @DisplayName("내용이 길이보다 짧게 끊기면 EOFException을 던진다")
        void truncatedContent() throws IOException {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            out.writeInt(100);
            out.write(new byte[10]);
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()));

            assertThrows(EOFException.class, () -> MessageCodec.read(in));
        }
    }

    // ------------------------------------------------------------------
    // 메시지 값 검사
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("메시지 값 검사")
    class Validation {

        @Test
        @DisplayName("종료 신호는 쌓을 수 없게 됐거나 상대 신호를 받은 경우에만 만들 수 있다")
        void endNeedsReason() {
            assertThrows(IllegalArgumentException.class, () -> new P2PMessage.End(false, false, 0));
        }

        @Test
        @DisplayName("종료 신호와 최종 점수의 점수는 음수일 수 없다")
        void scoresNotNegative() {
            assertThrows(IllegalArgumentException.class, () -> new P2PMessage.End(true, false, -1));
            assertThrows(IllegalArgumentException.class, () -> new P2PMessage.FinalScore(-1));
        }
    }
}
