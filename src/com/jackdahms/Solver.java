package com.jackdahms;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
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
import javax.swing.UIManager;

public class Solver extends JPanel{
	
	int rows = 1;
	int cols = 1;
	
	int lineWidth = 1;
	int lineHeight = 1;
	
	int[][] data = new int[1][1];
	
	//TODO
	//decide whether enums are necessary
	//refine checkpiece
	//checkboard
	//brute force solve
	//algorhythmically solve
	
	enum Direction {
		N(-1, 0), //up
		NE(-1, 1), //up and right
		E(0, 1), //right
		SE(1, 1), //down and right
		S(1, 0), //down
		SW(1, -1), //down and left
		W(0, -1), //left
		NW(-1, -1); //up and left
		
		int rowIncrement;
		int colIncrement;
		
		Direction(int rowIncrement, int colIncrement) {
			//I hate that I have to do this
			this.rowIncrement = colIncrement;
			this.colIncrement = rowIncrement;
		}
		
		public int row(int row) {
			return row += rowIncrement;
		}
		
		public int col(int col) {
			return col += colIncrement;
		}
	}
		
	public Solver() {
		setFocusable(true);
		addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_SPACE) {
					solve();
				}
			}
		});
		addMouseListener(new MouseAdapter() {
			public void mouseReleased(MouseEvent e) {
				int x = e.getX();
				int y = e.getY();
				//fuck me, these two assignments should be reversed but I must have fucked up somewhere.
				//good thing its a square
				int c = y / lineHeight;
				int r = x / lineWidth;
				data[r][c]++;
				if (data[r][c] > 2)
					data[r][c] = 0;
				repaint();
				println(checkPiece(r, c));
			}
		});
	}
	
	public void solve() {
		
	}
	
	public void paintComponent(Graphics g) {
		g.setColor(Color.black);
		g.fillRect(0, 0, getWidth(), getHeight());
		
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
					g.fillRect(lineWidth * i + i + 1, lineHeight * k + k + 1, lineWidth - 2, lineHeight - 2);
				} else if (data[i][k] == 2) {
					//yellow
					g.setColor(Color.yellow);
					g.fillRect(lineWidth * i + i + 1, lineHeight * k + k + 1, lineWidth - 2, lineHeight - 2);
				}
			}
		}
	}
	
	public boolean checkBoard() {
		boolean good = true;
		//loop through every colored piece
		for (int i = 0; i < rows; i++) {
			for (int k = 0; k < cols; k++) {
				if (!checkPiece(i, k)) //if checkPiece returns false
					good = false;
			}
		}
		return good;
	}
	
	//checks whether piece is in a line of four
	public boolean checkPiece(int r, int c) {
		//I feel recursion coming on...
		
		//length of chain in the four directions
		//starts at one because all chains have a base
		int vert = 1;
		int horz = 1;
		int diagTopLeft = 1;
		int diagTopRight = 1;
		
		//check surrounding eight pieces, if same color, mark direction
		//check color in that direction, add to total length in that direction
		//I hate this. There HAS to be a better way
		if (compare(Direction.N.row(r), Direction.N.col(c), r, c)) { //color matches north
//			vert += recurseDirection(Direction.N.row(r), Direction.N.col(c), -1, 0, 2);
			vert += recurseDirection(Direction.N.row(r), Direction.N.col(c), Direction.N, 2);
		}
		
		if (compare(Direction.NE.row(r), Direction.NE.col(c), r, c)) { //color matches north east
			diagTopRight += recurseDirection(Direction.NE.row(r), Direction.NE.col(c), Direction.NE, 2);
		}
		
		if (compare(Direction.E.row(r), Direction.E.col(c), r, c)) { //color matches east
			horz += recurseDirection(Direction.E.row(r), Direction.E.col(c), Direction.E, 2);
		}
		
		if (compare(Direction.SE.row(r), Direction.SE.col(c), r, c)) { //color matches south east
			diagTopLeft += recurseDirection(Direction.SE.row(r), Direction.SE.col(c), Direction.SE, 2);
		}
		
		if (compare(Direction.S.row(r), Direction.S.col(c), r, c)) { //color matches south
			vert += recurseDirection(Direction.S.row(r), Direction.S.col(c), Direction.S, 2);
		}
		
		if (compare(Direction.SW.row(r), Direction.SW.col(c), r, c)) { //color matches south west

			diagTopRight += recurseDirection(Direction.SW.row(r), Direction.SW.col(c), Direction.SW, 2);
		}
		
		if (compare(Direction.W.row(r), Direction.W.col(c), r, c)) { //color matches west
			horz += recurseDirection(Direction.W.row(r), Direction.W.col(c), Direction.W, 2);
		}
		
		if (compare(Direction.NW.row(r), Direction.NW.col(c), r, c)) { //color matches north west
			diagTopLeft += recurseDirection(Direction.NW.row(r), Direction.NW.col(c), Direction.NW, 2);
		}
		
		//attempt at better way
//		for (int i = r - 1; i <= r + 1; i++) {
//			for (int k = c - 1; k <= c +1; k++) {
//				
//			}
//		}
		
		//faster than three &&, right?
		return !(vert > 3 || horz > 3 || diagTopLeft > 3 || diagTopRight > 3);
	}
	
	public int recurseDirection(int row, int col, Direction dir, int chainLength) {
		if (chainLength > 4) //chains may become longer than is worth checking
			return 0;
		if (compare(dir.row(row), dir.col(col), row, col))
			return 1 + recurseDirection(dir.row(row), dir.col(col), dir, ++chainLength);
		else
			return 1;
	}
	
	public boolean compare(int r1, int c1, int r2, int c2) {
		try {
			return data[r1][c1] == data[r2][c2];
		} catch (Exception e) { //rather than do all that expensive grid bound checking
			return false;
		}
	}
	
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
				repaint();
				frame.dispose();
			} catch (Exception ex) {
				//I don't need no stinkin' imports!
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
		frame.setSize(700, 700); //I imagine 600x600 as too small and 800x800 as too large
		frame.setLocationRelativeTo(null);
		
		Solver s = new Solver();
		
		frame.getContentPane().add(s);
		
		frame.setVisible(true);
		
		s.getRowsAndCols();
	}
	
	public static void print(Object o) {
		System.out.print(o);
	}
	
	public static void println(Object o) {
		System.out.println(o);
	}
	
}