package seoultech.se.tetris.ui.settings;

import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;

import seoultech.se.tetris.config.P2PSettingsService;
import seoultech.se.tetris.config.P2PSettingsService.RecentPeer;

/**
 * 설정 화면 P2P 대전 탭의 입력 상태와 표시 문구. Swing/AWT에 의존하지 않는다.
 *
 * <p>포트 입력칸의 글자를 들고 있다가, 설정을 저장할 때 {@link #applyPort()}로 반영한다.
 * 최근 접속 기록 삭제는 저장과 관계없이 바로 반영한다.
 */
public final class P2PSettingsModel {

    static final String EMPTY_RECENT = "저장된 접속 기록이 없습니다";
    static final String INVALID_PORT = "포트는 " + P2PSettingsService.MIN_PORT + "~"
            + P2PSettingsService.MAX_PORT + " 사이의 숫자로 입력하세요";

    private final P2PSettingsService service;
    private String portText;

    public P2PSettingsModel(P2PSettingsService service) {
        this.service = Objects.requireNonNull(service, "service");
        this.portText = String.valueOf(service.port());
    }

    /** 포트 입력칸의 글자 */
    public String portText() {
        return portText;
    }

    /** 포트 입력칸의 글자가 바뀌었다. */
    public void setPortText(String text) {
        portText = text == null ? "" : text;
    }

    /** 입력한 글자를 포트 번호로 읽는다. 숫자가 아니거나 범위를 벗어나면 비어 있다. */
    public OptionalInt parsedPort() {
        String text = portText.strip();
        if (text.isEmpty() || text.length() > 5 || !text.chars().allMatch(Character::isDigit)) {
            return OptionalInt.empty();
        }
        int port = Integer.parseInt(text);
        return P2PSettingsService.isValidPort(port) ? OptionalInt.of(port) : OptionalInt.empty();
    }

    public boolean isPortValid() {
        return parsedPort().isPresent();
    }

    /** 포트 입력이 잘못되었을 때 보여줄 안내. 올바르면 {@code null} */
    public String portError() {
        return isPortValid() ? null : INVALID_PORT;
    }

    /** 포트 입력칸을 기본값으로 되돌린다. 설정을 저장할 때 반영된다. */
    public void resetPort() {
        portText = String.valueOf(P2PSettingsService.DEFAULT_PORT);
    }

    /** 기본 포트 안내 문구 */
    public String defaultPortHint() {
        return "기본값 " + P2PSettingsService.DEFAULT_PORT + "  ·  방을 만들면 이 포트로 상대를 기다립니다";
    }

    /**
     * 입력한 포트를 설정에 반영한다.
     *
     * @return 반영했으면 {@code true}, 입력이 잘못되어 반영하지 않았으면 {@code false}
     */
    public boolean applyPort() {
        OptionalInt port = parsedPort();
        if (port.isEmpty()) {
            return false;
        }
        service.setPort(port.getAsInt());
        return true;
    }

    /** 최근 접속 기록을 "IP:포트" 형식으로, 최신순으로 */
    public List<String> recentPeerTexts() {
        return service.recentPeers().stream().map(RecentPeer::text).toList();
    }

    public boolean hasRecentPeers() {
        return !service.recentPeers().isEmpty();
    }

    /** 최근 접속 기록을 모두 지운다. 바로 반영된다. */
    public void clearRecentPeers() {
        service.clearRecentPeers();
    }
}
