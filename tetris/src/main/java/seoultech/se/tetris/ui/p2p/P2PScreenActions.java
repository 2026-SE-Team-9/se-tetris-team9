package seoultech.se.tetris.ui.p2p;

/**
 * P2P 대전 모드 연결 화면({@link P2PPanel})에서 키를 눌렀을 때 화면이 부르는 동작.
 *
 * <p>연결과 화면 흐름을 맡는 쪽이 구현한다. 화면은 어떤 키가 눌렸는지만 알리고,
 * 그 키가 지금 화면에서 무엇을 할지(항목 이동, 접속, 다시 시도, 연결 끊기 등)는 구현 쪽이 정한다.
 *
 * <p>화면 아래쪽에는 다음 안내를 표시하므로, 구현은 이 안내와 맞게 동작해야 한다.
 * <ul>
 *   <li>첫 화면: "↑ ↓ 이동   Enter 선택   Esc 뒤로"</li>
 *   <li>방 생성: "Esc 취소". 실패 안내가 있으면 "Enter 다시 시도   Esc 취소"</li>
 *   <li>참여: "↑ ↓ 이동   Enter 접속·선택   Esc 뒤로". 접속 중이면 "Esc 접속 취소"</li>
 *   <li>대기실(방장): "↑ ↓ 이동   ← → 변경   Enter 선택   Esc 나가기"</li>
 *   <li>대기실(참여자): "Enter 게임 시작·취소   Esc 나가기"</li>
 * </ul>
 */
public interface P2PScreenActions {

    /** ↑ */
    void up();

    /** ↓ */
    void down();

    /** ← . 대기실에서 방장이 모드·난이도를 바꿀 때 쓴다 */
    void left();

    /** → . 대기실에서 방장이 모드·난이도를 바꿀 때 쓴다 */
    void right();

    /** Enter */
    void select();

    /** Esc */
    void back();

    /** 글자 입력. 참여 화면의 입력칸에 쓴다 */
    void type(char c);

    /** Backspace */
    void erase();
}
