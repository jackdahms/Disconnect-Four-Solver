package com.jackdahms;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Solver extends JPanel{
	
	int rows = 1;
	int cols = 1;
	
	int lineWidth = 1;
	int lineHeight = 1;
	
	int[][] data = new int[1][1];
	int[][] check = new int[1][1];	
	
	enum Direction {
		N(-1, 0, 0), //up
		NE(-1, 1, 1), //up and right
		E(0, 1, 2), //right
		SE(1, 1, 3), //down and right
		S(1, 0, 4), //down
		SW(1, -1, 5), //down and left
		W(0, -1, 6), //left
		NW(-1, -1, 7); //up and left
		
		int rowIncrement;
		int colIncrement;
		int index;
		
		Direction opposite;
		
		Direction(int rowIncrement, int colIncrement, int index) {
			this.rowIncrement = rowIncrement;
			this.colIncrement = colIncrement;
			this.index = index;
		}
		
		public int row(int row) {
			return row += rowIncrement;
		}
		
		public int col(int col) {
			return col += colIncrement;
		}
		
		public int getRowIncrement() {
			return rowIncrement;
		}
		
		public int getColIncrement() {
			return colIncrement;
		}
	}
	
	Direction[] directions = {Direction.N,
							  Direction.S,
							  Direction.NE,
							  Direction.SW,
							  Direction.E,
							  Direction.W,
							  Direction.SE,
							  Direction.NW};
	
	public Solver() {
		setFocusable(true);
		addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					updateCheckedTiles();
					solve();
				}
			}
		});
		addMouseListener(new MouseAdapter() {
			public void mouseReleased(MouseEvent e) {
				int x = e.getX();
				int y = e.getY();
				int r = y / lineHeight;
				int c = x / lineWidth;
				data[r][c]++;
				if (data[r][c] > 2)
					data[r][c] = 0;
				repaint();
			}
		});
	}
		
	
	public void solve() {	
		
		new Thread(() -> { //separate thread so solving doesn't interfere with frame or repainting
			boolean updated = false;
			do {
				updated = checkCapsAndPlugs();
				updateCheckedTiles();
			} while (updated);
			
			if (!checkBoard()) {
				System.out.println("br");
				bruteForce();
			}
		}).start();
	}
	
	/**
	 * Updates which tiles to check based on data.
	 */
	public void updateCheckedTiles() {
		//if tile is filled, mark it as 2
		for (int i = 0; i < rows; i++) {
			for (int k = 0; k < cols; k++) {
				if (data[i][k] > 0) check[i][k] = 2;
			}
		}
	}
	
	/**
	 * Checks for logical places tiles need to be placed. This includes caps on the ends of threes-in-a-row and plugs in twos-and-ones
	 * @return number of caps found
	 */
	public boolean checkCapsAndPlugs() {
		
		boolean updated = false;
		
		for (int r = 0; r < rows; r++) {
			for (int c = 0; c < cols; c++) {
				if (check[r][c] == 0) {
					for (Direction dir : Direction.values()) {
						int dr = r + dir.rowIncrement;
						int dc = c + dir.colIncrement;
						
						//check for caps on the end of threes
						try {
							if (check[dr][dc] == 2 && recurseDirection(dr, dc, dir, 0) > 2) {								
								if (data[dr][dc] == 1) 
									data[r][c] = 2;
								else 
									data[r][c] = 1;
								
								updated = true;
								repaint();
							}
						} catch (Exception e) {}
						
						int chainLength = 0;
						
						//check for plugs in between twos and ones
						try {
							if (check[dr][dc] == 2) {
								chainLength += recurseDirection(dr, dc, dir, 0);
							}
							
							int nr = r - dir.rowIncrement;
							int nc = c - dir.colIncrement;
							if (check[nr][nc] == 2) {
								chainLength += recurseDirection(nr, nc, dir.opposite, 0);
							}
							
							if (chainLength > 2 && compare(dr, dc, nr, nc)) {
								if (data[nr][nc] == 1) 
									data[r][c] = 2;
								else 
									data[r][c] = 1;
								
								updated = true;
								repaint();
							}
						} catch (Exception e) {}
					}
				}
			}
		}
		return updated;
	}
	
	public boolean checkBoard() {
		boolean good = true;
		//checks from the last piece backwards because if there is an error at the end, we don't need to check all the pieces in the beginning
		rowLoop: for (int i = rows - 1; i >= 0; i--) {
			for (int k = cols - 1; k >= 0; k--) {
				if (checkPiece(i, k)) {
					good = false;
					break rowLoop;// no sense in continuing, right?
				}
			}
		}
		return good;
	}
		
	/**
	 * Checks if piece is in a line of four
	 * @param r		row of piece
	 * @param c		column of piece
	 * @return		if piece is not in a line of four
	 */
	public boolean checkPiece(int r, int c) {		
		//length of chain in the four directions
		//starts at one because all chains have a base
		int vert = 1;
		int horz = 1;
		int diagTopLeft = 1;
		int diagTopRight = 1;
		
		//check surrounding eight pieces, if same color, mark direction
		//check color in that direction, add to total length in that direction
		
		if (compare(Direction.N.row(r), Direction.N.col(c), r, c)) { //color matches north
			vert += recurseDirection(Direction.N.row(r), Direction.N.col(c), Direction.N, 2);
			if (compare(Direction.S.row(r), Direction.S.col(c), r, c)) { //color matches south
				vert += recurseDirection(Direction.S.row(r), Direction.S.col(c), Direction.S, 2);
			}
		}
		
		if (compare(Direction.NE.row(r), Direction.NE.col(c), r, c)) { //color matches north east
			diagTopRight += recurseDirection(Direction.NE.row(r), Direction.NE.col(c), Direction.NE, 2);
			if (compare(Direction.SW.row(r), Direction.SW.col(c), r, c)) { //color matches south west
				diagTopRight += recurseDirection(Direction.SW.row(r), Direction.SW.col(c), Direction.SW, 2);
			}
		}
		
		if (compare(Direction.E.row(r), Direction.E.col(c), r, c)) { //color matches east
			horz += recurseDirection(Direction.E.row(r), Direction.E.col(c), Direction.E, 2);
			if (compare(Direction.W.row(r), Direction.W.col(c), r, c)) { //color matches west
				horz += recurseDirection(Direction.W.row(r), Direction.W.col(c), Direction.W, 2);
			}
		}
		
		if (compare(Direction.SE.row(r), Direction.SE.col(c), r, c)) { //color matches south east
			diagTopLeft += recurseDirection(Direction.SE.row(r), Direction.SE.col(c), Direction.SE, 2);
			if (compare(Direction.NW.row(r), Direction.NW.col(c), r, c)) { //color matches north west
				diagTopLeft += recurseDirection(Direction.NW.row(r), Direction.NW.col(c), Direction.NW, 2);
			}
		}
				
		return vert > 3 || horz > 3 || diagTopLeft > 3 || diagTopRight > 3;
	}
	
	//like counting on your fingers in binary
	public void bruteForce() {
		int[][] originalData = new int[data.length][data[0].length];
		for (int i = 0; i < data.length; i++) {
			for (int k = 0; k < data[i].length; k++) {
				//keep the original data
				originalData[i][k] = data[i][k];
				//populate with reds
				if (originalData[i][k] == 0) {
					data[i][k] = 1;
				}
			}
		}
		repaint();
		
		rowLoop: for (int i = 0; i < data.length; i++) {
			for (int k = 0; k < data[0].length; k++) {
				if (originalData[i][k] == 0) { //if valid position
					if (checkBoard()) {
						break rowLoop;
					}
					if (data[i][k] == 2) {
						//do nothing, just needs to move on
					} else if (data[i][k] == 1) {
						data[i][k] = 2;
						//wipe previous
						for (int n = 0; n < i; n++) {
							for (int m = 0; m < data[i].length; m++) {
								if (originalData[n][m] == 0) //recheck validity
									data[n][m] = 1;
							}
						}
						//wipe only part of the last row
						for (int m = 0; m < k; m++) {
							if (originalData[i][m] == 0) //recheck validity
								data[i][m] = 1;
						}
						/**
						if (!checkBoard()) { //in case of lots of yellows
							//find red after yellows
							//set to yellow
							//wipe previous
						}
						*/
						//start at beginning
						i = 0;
						k = -1; //-1 because it increments after completing the loop
					}
					repaint(); //comment out to save time
				}
			}
		}
		repaint();
	}
		
	/**
	 * Returns length of chain containing the piece located at row, col
	 * @param row			row of the piece to check
	 * @param col			column of the piece to check
	 * @param dir			direction to check chain in
	 * @param chainLength	length of the current chain
	 * @return
	 */
	public int recurseDirection(int row, int col, Direction dir, int chainLength) {
		if (chainLength > 3) //chains may become longer than is worth checking
			return 0;
		if (compare(dir.row(row), dir.col(col), row, col))
			return 1 + recurseDirection(dir.row(row), dir.col(col), dir, ++chainLength);
		else
			return 1;
	}
	
	public boolean compare(int r1, int c1, int r2, int c2) {
		try {
			return data[r1][c1] == data[r2][c2];
		} catch (Exception e) { //happens when tiles cannot be compared, i.e. if one is out of bounds
			return false;
		}
	}
	
	public void paintComponent(Graphics g) {
		g.setColor(Color.black);
		g.fillRect(0, 0, getWidth(), getHeight());
		System.out.println("hey");
		g.setColor(Color.lightGray);
		lineWidth = getWidth() / (cols);
		lineHeight = getHeight() / (rows);
		//horizontal lines
		for (int i = 0; i < rows - 1; i++) {
			g.drawLine(0, lineHeight * (i + 1) + i, getWidth(), lineHeight * (i + 1) + i);
		}
		//vertical lines
		for (int i = 0; i < cols - 1; i++) {
			g.drawLine(lineWidth * (i + 1) + i, 0, lineWidth * (i + 1) + i, getHeight());
		}
		//data
		for (int i = 0; i < rows; i++) {
			for (int k = 0; k < cols; k++) {
				if (data[i][k] == 1) {
					//red
					g.setColor(Color.red);
					g.fillRect(lineWidth * k + k + 1, lineHeight * i + i + 1, lineWidth - 2, lineHeight - 2);
				} else if (data[i][k] == 2) {
					//yellow
					g.setColor(Color.yellow);
					g.fillRect(lineWidth * k + k + 1, lineHeight * i + i + 1, lineWidth - 2, lineHeight - 2);
				}
			}
		}
	}
	
	/**
	 * Asks user how large to make the grid
	 */
	public void getRowsAndCols() {
		JFrame frame = new JFrame();
		frame.setTitle("Enter number of rows and columns");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(100, 100); //eh
		frame.setLocationRelativeTo(null);
		
		JPanel pane = new JPanel();
		SpringLayout layout = new SpringLayout();
		
		JLabel colsLabel = new JLabel("Size:");
		JTextField colsField = new JTextField();
		colsField.setPreferredSize(new Dimension(colsLabel.getPreferredSize().width, colsField.getPreferredSize().height));
		JButton ok = new JButton("OK");
		ok.setPreferredSize(new Dimension(112, ok.getPreferredSize().height));
		ok.addActionListener((ActionEvent e) ->
		{
			
			try {
				rows = Integer.parseInt(colsField.getText());
				cols = Integer.parseInt(colsField.getText());
				data = new int[rows][cols];
				check = new int[data.length][data.length];
				repaint();
				frame.dispose();
			} catch (Exception ex) {
				javax.swing.JOptionPane.showMessageDialog(null, "Something fucked up (hint: it was you). Make sure there are no letters in the fields.",
						"Good job.", javax.swing.JOptionPane.ERROR_MESSAGE);
			}
		});
						
		layout.putConstraint(SpringLayout.WEST, colsLabel, 0, SpringLayout.WEST, ok);
		layout.putConstraint(SpringLayout.NORTH, colsLabel, 2, SpringLayout.NORTH, colsField);
		pane.add(colsLabel);
		
		layout.putConstraint(SpringLayout.EAST, colsField, 0, SpringLayout.EAST, ok);
		layout.putConstraint(SpringLayout.SOUTH, colsField, -5, SpringLayout.NORTH, ok);
		pane.add(colsField);
		
		layout.putConstraint(SpringLayout.WEST, ok, 5, SpringLayout.WEST, pane);
		layout.putConstraint(SpringLayout.SOUTH, ok, -5, SpringLayout.SOUTH, pane);
		pane.add(ok);
		
		pane.setLayout(layout);
		
		frame.getContentPane().add(pane);
		frame.setVisible(true);
		
	}
	
	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			System.err.println("Could not set look and feel!");
		}
		
		JFrame frame = new JFrame();
		frame.setTitle("Disconnect Four Solver (hit enter to start)");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(700, 700); 
		frame.setLocationRelativeTo(null);
		
		//cannot mention an object before definition, so this cannot be done in direction's constructor
		Direction.N.opposite = Direction.S;
		Direction.NE.opposite = Direction.SW;
		Direction.E.opposite = Direction.W;
		Direction.SE.opposite = Direction.NW;
		Direction.S.opposite = Direction.N;
		Direction.SW.opposite = Direction.NE;
		Direction.W.opposite = Direction.E;
		Direction.NW.opposite = Direction.SE;
		
		Solver s = new Solver();
		
		frame.getContentPane().add(s);
		
		frame.setVisible(true);
		
		s.getRowsAndCols();
	}
	
}