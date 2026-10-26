package seoultech.se.tetris.blocks;

/**
 * 테트로미노 7종의 정의.
 *
 * <p>각 블럭의 회전 상태 4개를 미리 정의해 두고, 회전은 인덱스를 순환시키는 방식으로 처리한다.
 * 회전축을 좌표로 계산하지 않으므로 블럭별 예외 처리가 필요 없고,
 * 이후 표준 회전 방식(SRS)으로 교체할 때도 아래 모양 데이터만 갈아끼우면 된다.
 *
 * <p>enum에서 상수 이름은 코드상 이름, {@link #symbol()}이 화면에 표시되는 문자이다.
 * 모양 데이터의 문자는 .과 구분하는 용도이므로 실제 표시 문자와 달라도 동작은 동일하다.
 *
 * <p><b>이 클래스는 색을 모른다.</b> 색 결정은 화면 쪽 팔레트의 역할이다.
 * (계층 규칙 2 — 모델은 색을 모른다)
 *
 * <p>L형 블럭은 줄 삭제 아이템의 요구사항에서 문자 L을 사용하므로 표시 문자를 V로 한다.
 * 실제 동작에 차이는 없다.
 */
public enum BlockType implements Shape {

    I('I', new String[][] {
            {
                "....",
                "IIII",
                "....",
                "...."
            },
            {
                "..I.",
                "..I.",
                "..I.",
                "..I."
            },
            {
                "....",
                "....",
                "IIII",
                "...."
            },
            {
                ".I..",
                ".I..",
                ".I..",
                ".I.."
            }
    }),

    O('O', new String[][] {
            {
                ".OO.",
                ".OO.",
                "....",
                "...."
            },
            {
                ".OO.",
                ".OO.",
                "....",
                "...."
            },
            {
                ".OO.",
                ".OO.",
                "....",
                "...."
            },
            {
                ".OO.",
                ".OO.",
                "....",
                "...."
            }
    }),

    T('T', new String[][] {
            {
                ".T..",
                "TTT.",
                "....",
                "...."
            },
            {
                ".T..",
                ".TT.",
                ".T..",
                "...."
            },
            {
                "....",
                "TTT.",
                ".T..",
                "...."
            },
            {
                ".T..",
                "TT..",
                ".T..",
                "...."
            }
    }),

    S('S', new String[][] {
            {
                ".SS.",
                "SS..",
                "....",
                "...."
            },
            {
                ".S..",
                ".SS.",
                "..S.",
                "...."
            },
            {
                "....",
                ".SS.",
                "SS..",
                "...."
            },
            {
                "S...",
                "SS..",
                ".S..",
                "...."
            }
    }),

    Z('Z', new String[][] {
            {
                "ZZ..",
                ".ZZ.",
                "....",
                "...."
            },
            {
                "..Z.",
                ".ZZ.",
                ".Z..",
                "...."
            },
            {
                "....",
                "ZZ..",
                ".ZZ.",
                "...."
            },
            {
                ".Z..",
                "ZZ..",
                "Z...",
                "...."
            }
    }),

    J('J', new String[][] {
            {
                "J...",
                "JJJ.",
                "....",
                "...."
            },
            {
                ".JJ.",
                ".J..",
                ".J..",
                "...."
            },
            {
                "....",
                "JJJ.",
                "..J.",
                "...."
            },
            {
                ".J..",
                ".J..",
                "JJ..",
                "...."
            }
    }),

    L('V', new String[][] {
            {
                "..L.",
                "LLL.",
                "....",
                "...."
            },
            {
                ".L..",
                ".L..",
                ".LL.",
                "...."
            },
            {
                "....",
                "LLL.",
                "L...",
                "...."
            },
            {
                "LL..",
                ".L..",
                ".L..",
                "...."
            }
    });

    /** 모양 격자의 한 변 길이. 모든 블럭이 동일한 크기를 사용한다. */
    public static final int SIZE = 4;

    /** 회전 상태의 개수. */
    public static final int ROTATIONS = 4;

    /** 빈 칸을 나타내는 문자. */
    private static final char EMPTY = '.';

    /** 보드 배열에서 빈 칸을 나타내는 번호. */
    public static final int EMPTY_CODE = 0;

    /** 번호로 블럭 종류를 찾기 위한 표. 인덱스가 곧 번호다. */
    private static final BlockType[] BY_CODE = buildCodeTable();

    private final char symbol;
    private final boolean[][][] shapes;

    /** 시계방향 회전에서 칸이 이동하는 규칙
     * <p>(row, col) → (col + rotationRowOffset, rotationColSum - row)
     *
     * <p>회전 중심을 기준으로 정해지는 값으로 모양 데이터에서 계산한다.
     * <pre>
     * I형        4×4 격자의 중심 (1.5, 1.5)   → (c, 3 − r)
     * O형        2×2 정사각형의 중심 (0.5, 1.5) → (c − 1, 2 − r)
     * 나머지 5종  왼쪽 위 3×3 격자의 중심 (1, 1) → (c, 2 − r)
     * </pre>
     */
    private final int rotationRowOffset;
    private final int rotationColSum;

    BlockType(char symbol, String[][] patterns) {
        this.symbol = symbol;
        this.shapes = parse(patterns);
        int[] rule = detectRotationRule(shapes);
        this.rotationRowOffset = rule[0];
        this.rotationColSum = rule[1];
    }

    /**
     * 이 블럭을 화면에 표시할 때 사용하는 문자.
     *
     * <p>색상과 별개로 블럭을 구분할 수 있게 하기 위한 것이다.
     * 색각이상자를 위한 요구사항을 색상만으로 충족하지 않도록,
     * 기본 상태에서도 문자로 구분이 가능해야 한다.
     */
    public char symbol() {
        return symbol;
    }

    /**
     * 지정한 회전 상태에서 (row, col) 칸이 채워져 있는지 여부.
     *
     * @param rotation 회전 상태. 범위를 벗어나면 4로 나눈 나머지로 순환한다.
     */
    public boolean isFilled(int rotation, int row, int col) {
        if (row < 0 || row >= SIZE || col < 0 || col >= SIZE) {
            return false;
        }
        return shapes[normalize(rotation)][row][col];
    }

    /**
     * 지정한 회전 상태의 모양을 복사해서 반환한다.
     *
     * <p>내부 배열을 직접 넘기지 않으므로 호출한 쪽에서 수정해도 원본은 바뀌지 않는다.
     */
    public boolean[][] shape(int rotation) {
        boolean[][] source = shapes[normalize(rotation)];
        boolean[][] copy = new boolean[SIZE][SIZE];
        for (int r = 0; r < SIZE; r++) {
            System.arraycopy(source[r], 0, copy[r], 0, SIZE);
        }
        return copy;
    }

    /**
     * 시계방향으로 한 번 회전했을 때 (row, col) 칸이 옮겨가는 위치
     *
     * <p>블럭에 붙은 아이템이 회전해도 같은 칸을 따라가도록 할 때 사용한다.
     *
     * @return {행, 열}
     */
    public int[] rotateCellClockwise(int row, int col) {
        return new int[] {col + rotationRowOffset, rotationColSum - row};
    }

    /** 반시계방향으로 한 번 회전했을 때 (row, col) 칸이 옮겨가는 위치. {@link #rotateCellClockwise}의 역방향이다. */
    public int[] rotateCellCounterClockwise(int row, int col) {
        return new int[] {rotationColSum - col, row - rotationRowOffset};
    }

    /** 회전 상태 번호를 0 이상 {@link #ROTATIONS} 미만으로 정규화한다. */
    public static int normalize(int rotation) {
        return Math.floorMod(rotation, ROTATIONS);
    }

    /**
     * 보드 배열에 저장할 번호.
     *
     * <p>{@link #EMPTY_CODE}와 겹치지 않도록 1부터 시작한다.
     *
     * <p><b>번호와 블럭 종류의 대응은 이 클래스에서만 정의한다.</b>
     * 화면 쪽에서 별도의 대응표를 만들면 양쪽이 어긋나도 컴파일 오류가 나지 않아
     * 조용히 잘못 그려지게 된다. 번호를 되돌릴 때는 {@link #fromCode(int)}를 사용한다.
     */
    public int code() {
        return ordinal() + 1;
    }

    /**
     * 보드 배열의 번호를 블럭 종류로 되돌린다.
     *
     * @param code {@link #code()}가 반환한 번호
     * @return 해당 블럭 종류. {@link #EMPTY_CODE}이면 {@code null}
     * @throws IllegalArgumentException 정의되지 않은 번호인 경우
     */
    public static BlockType fromCode(int code) {
        if (code == EMPTY_CODE) {
            return null;
        }
        if (code < 0 || code >= BY_CODE.length || BY_CODE[code] == null) {
            throw new IllegalArgumentException("정의되지 않은 블럭 번호입니다: " + code);
        }
        return BY_CODE[code];
    }

    private static BlockType[] buildCodeTable() {
        BlockType[] table = new BlockType[values().length + 1];
        for (BlockType type : values()) {
            table[type.code()] = type;
        }
        return table;
    }

    /**
     * 모양 데이터에서 회전 중심을 찾아 칸 이동 규칙을 만든다.
     *
     * <p>격자 안의 정수·소수 좌표를 모두 중심 후보로 놓고, "이 점을 중심으로 시계방향으로 돌리면
     * 모든 회전 상태가 다음 회전 상태와 칸이 정확히 일치하는가"를 확인한다.
     * 좌표를 2배 한 정수(centerRow2, centerCol2)로 다뤄 소수 계산을 피한다.
     *
     * @return {rotationRowOffset, rotationColSum}
     */
    private static int[] detectRotationRule(boolean[][][] shapes) {
        for (int centerRow2 = 0; centerRow2 <= 2 * (SIZE - 1); centerRow2++) {
            for (int centerCol2 = 0; centerCol2 <= 2 * (SIZE - 1); centerCol2++) {
                if ((centerRow2 + centerCol2) % 2 != 0) {
                    continue;   // 두 좌표가 하나는 정수, 하나는 반정수이면 칸이 칸으로 옮겨가지 않는다
                }
                int rowOffset = (centerRow2 - centerCol2) / 2;
                int colSum = (centerRow2 + centerCol2) / 2;
                if (rotatesWith(shapes, rowOffset, colSum)) {
                    return new int[] {rowOffset, colSum};
                }
            }
        }
        throw new IllegalArgumentException("회전 상태 사이의 칸 대응을 찾을 수 없는 모양입니다.");
    }

    private static boolean rotatesWith(boolean[][][] shapes, int rowOffset, int colSum) {
        for (int rotation = 0; rotation < ROTATIONS; rotation++) {
            boolean[][] from = shapes[rotation];
            boolean[][] to = shapes[(rotation + 1) % ROTATIONS];
            for (int r = 0; r < SIZE; r++) {
                for (int c = 0; c < SIZE; c++) {
                    if (!from[r][c]) {
                        continue;
                    }
                    int nr = c + rowOffset;
                    int nc = colSum - r;
                    if (nr < 0 || nr >= SIZE || nc < 0 || nc >= SIZE || !to[nr][nc]) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static boolean[][][] parse(String[][] patterns) {
        if (patterns.length != ROTATIONS) {
            throw new IllegalArgumentException("회전 상태는 " + ROTATIONS + "개여야 합니다.");
        }

        boolean[][][] result = new boolean[ROTATIONS][SIZE][SIZE];
        for (int rotation = 0; rotation < ROTATIONS; rotation++) {
            String[] rows = patterns[rotation];
            if (rows.length != SIZE) {
                throw new IllegalArgumentException("모양은 " + SIZE + "줄이어야 합니다.");
            }
            for (int r = 0; r < SIZE; r++) {
                String row = rows[r];
                if (row.length() != SIZE) {
                    throw new IllegalArgumentException("모양의 각 줄은 " + SIZE + "칸이어야 합니다.");
                }
                for (int c = 0; c < SIZE; c++) {
                    result[rotation][r][c] = row.charAt(c) != EMPTY;
                }
            }
        }
        return result;
    }
}
