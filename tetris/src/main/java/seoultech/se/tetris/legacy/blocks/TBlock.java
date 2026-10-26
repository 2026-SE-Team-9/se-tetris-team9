package seoultech.se.tetris.legacy.blocks;

import java.awt.Color;

public class TBlock extends Block {
	
	public TBlock() {
		shape = new int[][] { 
			{0, 1, 0},
			{1, 1, 1}
		};
		color = Color.MAGENTA;
	}
}
