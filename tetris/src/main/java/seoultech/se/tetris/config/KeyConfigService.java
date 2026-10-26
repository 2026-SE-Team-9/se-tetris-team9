package seoultech.se.tetris.config;

import java.awt.event.KeyEvent;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;

public interface KeyConfigService {
    /** 조작 액션 정의 */
    enum GameAction {
        MOVE_LEFT, MOVE_RIGHT, SOFT_DROP, HARD_DROP,
        /** 시계 방향 90도 회전 */
        ROTATE,
        /** 반시계 방향 90도 회전 */
        ROTATE_COUNTERCLOCKWISE,
        /** 게임을 일시정지하고 메뉴를 연다. 직접 종료하는 액션이 아니다. */
        PAUSE_MENU
    }

    /** 메인 키는 필수이고 서브키는 선택 사항인 키 설정. */
    record KeyBinding(int mainKeyCode, OptionalInt subKeyCode) {
        /** 메인 키가 있고, 서브키가 지정되었다면 유효하며 메인과 다른지 검사한다. */
        public KeyBinding {
            Objects.requireNonNull(subKeyCode, "subKeyCode");
            if (mainKeyCode <= KeyEvent.VK_UNDEFINED) {
                throw new IllegalArgumentException("메인 키를 지정해야 합니다");
            }
            if (subKeyCode.isPresent()) {
                int code = subKeyCode.getAsInt();
                if (code <= KeyEvent.VK_UNDEFINED || code == mainKeyCode) {
                    throw new IllegalArgumentException("유효하지 않거나 메인 키와 중복된 서브키: " + code);
                }
            }
        }

        public KeyBinding(int mainKeyCode) {
            this(mainKeyCode, OptionalInt.empty());
        }

        public KeyBinding(int mainKeyCode, int subKeyCode) {
            this(mainKeyCode, OptionalInt.of(subKeyCode));
        }

        public List<Integer> keyCodes() {
            return subKeyCode.isPresent()
                    ? List.of(mainKeyCode, subKeyCode.getAsInt())
                    : List.of(mainKeyCode);
        }
    }

    /** UI에서 메인 키와 선택적 서브키를 각각 조회한다.
     * {mainKeyCode: K, subKeyCode: X}	메인·서브를 구분해서 사용
    */
    KeyBinding getKeyBinding(GameAction action);

    /** 입력 판정용으로 메인 키와 선택적 서브키를 순서대로 반환한다.
     * [K, X]	둘 다 같은 회전 키로 등록
    */
    default List<Integer> getKeyCodes(GameAction action) {
        return getKeyBinding(action).keyCodes();
    }

    /** 기본값: 방향키 이동, Space 하드 드롭, ↑/X 시계 회전, Z 반시계 회전, Esc 일시정지 메뉴 */
    void resetBindingsToDefault();

    /** 전체 편집 결과를 검증하여 파일에 저장하고 성공한 경우에만 현재 설정으로 적용한다. */
    void saveBindings(Map<GameAction, KeyBinding> bindings);

    /** 파일에서 키 설정을 로드 */
    void load();
}
