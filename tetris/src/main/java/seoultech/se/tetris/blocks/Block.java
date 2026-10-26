package seoultech.se.tetris.blocks;

import java.util.Objects;

/**
 * 보드 위에 놓인 블럭 하나의 현재 상태
 *
 * <p>모양({@link Shape}), 회전 상태, 보드상의 위치를 함께 가진다.
 * 모양은 테트로미노({@link BlockType})이거나 독립형 아이템({@link ItemType})이다.
 *
 * <p>테트로미노에는 포함형 아이템을 한 칸 붙일 수 있다({@link #withItem}).
 * 붙은 칸은 회전해도 같은 칸을 따라간다.
 *
 * <p><b>이 객체는 불변이다.</b>
 * 이동이나 회전은 기존 객체를 바꾸지 않고 새 객체를 만들어 반환한다.
 * 그러므로 "움직여 보고 안 되면 되돌리기"가 필요 없고,
 * 아래처럼 "결과를 만들어 검증한 뒤 채택하기"로 처리할 수 있다.
 *
 * <pre>{@code
 * Block next = current.rotated();
 * if (board.canPlace(next)) {
 *     current = next;      // 성공
 * }                        // 실패 시 아무것도 하지 않음 — 되돌릴 것이 없다
 * }</pre>
 *
 * <p>회전 시 벽이나 기존 블럭에 걸리는 경우도 같은 방식으로 좌우로 밀어 재시도할 수 있다.
 *
 * <pre>{@code
 * for (int kick : WALL_KICKS) {
 *     Block next = current.rotated().moved(0, kick);
 *     if (board.canPlace(next)) { current = next; break; }
 * }
 * }</pre>
 */
public final class Block {

    /** 포함형 아이템이 없을 때 붙은 칸의 좌표 값 */
    private static final int NO_ITEM = -1;

    private final Shape shape;
    private final int rotation;
    private final int row;
    private final int col;

    /** 붙은 포함형 아이템. 없으면 {@code null} */
    private final ItemType item;

    /** 붙은 칸의 현재 회전 상태 기준 격자 좌표. 없으면 {@link #NO_ITEM} */
    private final int itemRow;
    private final int itemCol;

    /**
     * @param shape    블럭 모양. 테트라미노와 독립형 아이템
     * @param rotation 회전 상태. 범위를 벗어나면 0~3으로 정규화된다
     * @param row      모양 격자의 좌측 상단이 놓인 보드상의 행
     * @param col      모양 격자의 좌측 상단이 놓인 보드상의 열
     *
     * @throws IllegalArgumentException 자기 모양이 없는 포함형 아이템을 {@code shape}로 지정한 경우
     */
    public Block(Shape shape, int rotation, int row, int col) {
        this(shape, rotation, row, col, null, NO_ITEM, NO_ITEM);
    }

    private Block(Shape shape, int rotation, int row, int col, ItemType item, int itemRow, int itemCol) {
        this.shape = Objects.requireNonNull(shape, "블럭 종류는 null일 수 없습니다.");
        if (shape instanceof ItemType itemShape && itemShape.isEmbedded()) {
            throw new IllegalArgumentException(itemShape + "는 블럭에 붙는 아이템이라 단독으로 만들 수 없습니다.");
        }
        this.rotation = BlockType.normalize(rotation);
        this.row = row;
        this.col = col;
        this.item = item;
        this.itemRow = itemRow;
        this.itemCol = itemCol;
    }

    /** 지정한 위치에 회전하지 않은 상태로 생성한다. */
    public static Block of(Shape shape, int row, int col) {
        return new Block(shape, 0, row, col);
    }

    /**
     * 격자의 (localRow, localCol) 칸에 포함형 아이템을 붙인 새 블럭을 반환한다.
     *
     * <p>좌표는 현재 회전 상태 기준이다. 이후 회전하면 붙은 칸도 함께 따라간다.
     *
     * @throws IllegalArgumentException 테트로미노가 아니거나, 포함형 아이템이 아니거나,
     *                                  채워지지 않은 칸이거나, 이미 아이템이 붙어 있는 경우
     */
    public Block withItem(ItemType embedded, int localRow, int localCol) {
        Objects.requireNonNull(embedded, "아이템은 null일 수 없습니다.");
        if (!(shape instanceof BlockType)) {
            throw new IllegalArgumentException("아이템은 테트로미노에만 붙일 수 있습니다: " + shape);
        }
        if (!embedded.isEmbedded()) {
            throw new IllegalArgumentException(embedded + "는 블럭에 붙는 아이템이 아닙니다.");
        }
        if (!isFilled(localRow, localCol)) {
            throw new IllegalArgumentException("채워지지 않은 칸에는 아이템을 붙일 수 없습니다: " + localRow + ", " + localCol);
        }
        if (item != null) {
            throw new IllegalArgumentException("이미 아이템이 붙어 있습니다: " + item);
        }
        return new Block(shape, rotation, row, col, embedded, localRow, localCol);
    }

    /** 시계방향으로 90도 회전한 새 블럭을 반환
     *
     * <p>붙은 아이템도 같은 칸을 따라간다.
     */
    public Block rotated() {
        if (item == null) {
            return new Block(shape, rotation + 1, row, col, null, NO_ITEM, NO_ITEM);
        }
        int[] cell = ((BlockType) shape).rotateCellClockwise(itemRow, itemCol);
        return new Block(shape, rotation + 1, row, col, item, cell[0], cell[1]);
    }

    /** 반시계방향으로 90도 회전한 새 블럭을 반환 */
    public Block rotatedCounterClockwise() {
        if (item == null) {
            return new Block(shape, rotation - 1, row, col, null, NO_ITEM, NO_ITEM);
        }
        int[] cell = ((BlockType) shape).rotateCellCounterClockwise(itemRow, itemCol);
        return new Block(shape, rotation - 1, row, col, item, cell[0], cell[1]);
    }

    /**
     * 지정한 만큼 이동한 새 블럭 반환
     *
     * @param rowDelta 행 변화량. 아래로 한 칸은 {@code +1}
     * @param colDelta 열 변화량. 오른쪽으로 한 칸은 {@code +1}
     */
    public Block moved(int rowDelta, int colDelta) {
        if (item == null) {
            return new Block(shape, rotation, row + rowDelta, col + colDelta);
        }
        return new Block(shape, rotation, row + rowDelta, col + colDelta, item, itemRow, itemCol);
    }

    /**
     * 모양 격자의 (localRow, localCol) 칸이 채워져 있는지 여부를 반환
     *
     * <p>좌표는 블럭 자신의 격자 기준이며, 보드 좌표가 아니다.
     * 보드 좌표는 {@link #boardRow(int)}, {@link #boardCol(int)}로 얻는다.
     */
    public boolean isFilled(int localRow, int localCol) {
        return shape.isFilled(rotation, localRow, localCol);
    }

    /**
     * 격자의 (localRow, localCol) 칸에 해당하는 보드 칸 번호
     *
     * <p>보드에 고정할 때와 화면에 그릴 때 이 번호를 쓴다. 아이템이 붙은 칸은 아이템 번호,
     * 나머지 채워진 칸은 모양의 번호, 빈 칸은 {@link CellCode#EMPTY}이다.
     * 표시 문자는 {@link CellCode#symbolOf(int)}로 얻는다.
     */
    public int codeAt(int localRow, int localCol) {
        if (!isFilled(localRow, localCol)) {
            return CellCode.EMPTY;
        }
        if (item != null && localRow == itemRow && localCol == itemCol) {
            return item.code();
        }
        return shape.code();
    }

    /** 모양 격자의 행 번호를 보드상의 행 번호로 변환한다. */
    public int boardRow(int localRow) {
        return row + localRow;
    }

    /** 모양 격자의 열 번호를 보드상의 열 번호로 변환한다. */
    public int boardCol(int localCol) {
        return col + localCol;
    }

    /** 블럭 모양. 테트로미노이거나 독립형 아이템이다. */
    public Shape shape() {
        return shape;
    }

    /**
     * 테트로미노 종류
     *
     * @throws IllegalStateException 독립형 아이템인 경우. 모양이 필요하면 {@link #shape()},
     *                               칸마다의 번호가 필요하면 {@link #codeAt(int, int)}를 쓴다
     */
    public BlockType type() {
        if (shape instanceof BlockType type) {
            return type;
        }
        throw new IllegalStateException("테트로미노가 아닌 블럭입니다: " + shape);
    }

    /**
     * 이 블럭이 가진 아이템. 붙은 포함형 아이템이거나, 블럭 자체가 독립형 아이템이다.
     *
     * @return 아이템이 없는 일반 블럭이면 {@code null}
     */
    public ItemType item() {
        if (item != null) {
            return item;
        }
        return shape instanceof ItemType standalone ? standalone : null;
    }

    /** 붙은 포함형 아이템의 현재 격자 행. 붙은 아이템이 없으면 -1 */
    public int itemRow() {
        return itemRow;
    }

    /** 붙은 포함형 아이템의 현재 격자 열. 붙은 아이템이 없으면 -1 */
    public int itemCol() {
        return itemCol;
    }

    public int rotation() {
        return rotation;
    }

    /** 모양 격자의 좌측 상단이 놓인 보드상의 행. */
    public int row() {
        return row;
    }

    /** 모양 격자의 좌측 상단이 놓인 보드상의 열. */
    public int col() {
        return col;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Block other)) {
            return false;
        }
        return rotation == other.rotation
                && row == other.row
                && col == other.col
                && shape == other.shape
                && item == other.item
                && itemRow == other.itemRow
                && itemCol == other.itemCol;
    }

    @Override
    public int hashCode() {
        return Objects.hash(shape, rotation, row, col, item, itemRow, itemCol);
    }

    @Override
    public String toString() {
        String itemText = item == null ? "" : " item=" + item + "@" + itemRow + "," + itemCol;
        return "Block[" + shape + " rot=" + rotation + " row=" + row + " col=" + col + itemText + "]";
    }
}
