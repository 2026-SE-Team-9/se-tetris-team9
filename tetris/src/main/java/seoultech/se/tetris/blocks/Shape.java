package seoultech.se.tetris.blocks;

/**
 * 보드 위에서 움직이는 조각의 모양.
 *
 * <p>테트로미노 7종({@link BlockType})과 자기 모양을 가진 독립형 아이템({@link ItemType})을 구현한다.
 * {@link Block}은 이 인터페이스만 보고 이동, 회전, 충돌을 처리하므로
 * 조각의 종류가 늘어나도 {@link Block}과 보드는 바뀌지 않는다.
 *
 * <p>모든 모양은 {@link BlockType#SIZE} × {@link BlockType#SIZE} 격자 안에 그린다.
 *
 * <p><b>인터페이스는 색을 모른다.</b> 색 결정은 화면 표시의 역할이다. (계층 규칙 2 — 모델은 색을 모른다)
 */
public interface Shape {

    /**
     * 지정한 회전 상태에서 격자의 (row, col) 칸이 채워져 있는지 여부
     *
     * <p>격자 밖은 비어 있는 것으로 답한다.
     */
    boolean isFilled(int rotation, int row, int col);

    /** 보드 칸에 저장할 번호. 번호와 종류의 대응은 {@link CellCode}에서 확인한다. */
    int code();

    /** 화면에 표시할 문자 */
    char symbol();
}