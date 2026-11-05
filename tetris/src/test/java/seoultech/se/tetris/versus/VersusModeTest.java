package seoultech.se.tetris.versus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import seoultech.se.tetris.engine.GameMode;

@DisplayName("VersusMode")
class VersusModeTest {

    @Test
    @DisplayName("일반·아이템 모드는 같은 이름의 게임 모드로 진행하고 제한 시간이 없다")
    void normalAndItem() {
        assertEquals(GameMode.NORMAL, VersusMode.NORMAL.gameMode());
        assertEquals(GameMode.ITEM, VersusMode.ITEM.gameMode());

        for (VersusMode mode : new VersusMode[] {VersusMode.NORMAL, VersusMode.ITEM}) {
            assertFalse(mode.hasTimeLimit());
            assertEquals(Optional.empty(), mode.timeLimit());
        }
    }

    @Test
    @DisplayName("시간제한 모드는 일반 모드를 기반으로 하고 제한 시간이 3분이다")
    void timeLimit() {
        assertEquals(GameMode.NORMAL, VersusMode.TIME_LIMIT.gameMode());
        assertTrue(VersusMode.TIME_LIMIT.hasTimeLimit());
        assertEquals(Optional.of(Duration.ofMinutes(3)), VersusMode.TIME_LIMIT.timeLimit());
    }

    @Test
    @DisplayName("대전 모드는 일반, 아이템, 시간제한 세 가지다")
    void threeModes() {
        assertEquals(3, VersusMode.values().length);
    }
}
