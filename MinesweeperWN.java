package minesweeper;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

//Web Nguyen
//'22, June 3
//Gridbased program (minesweeper) - resubmitted to add missing comments

//note: right click the top panel to change difficulty

public class MinesweeperWN {
	HashMap<Integer, Color> color = new HashMap<Integer, Color>();
	HashMap<Integer, boolean[]> lights = new HashMap<Integer, boolean[]>();

	JFrame frame;
	DrawingPanel dPanel; 	//Displays the playing board
	MessagePanel mPanel; 	//Displays the bomb counter, time counter, and reset face button 
	Timer timer;

	final static int FLAG = -1, FALSEFLAG = -2, HIDDEN = 0, VISIBLE = 1, HOLDING = 2; 	//values for visible[][]
	final static int BOMB = FLAG, GAMEOVER = FALSEFLAG;				 					//values for board[][]
	final static int SQUARE = 30;

	int board[][];			//Stores info on what each square contains
	int visible[][];		//Tells the dPanel what to paint
	int boardX, boardY;
	int rows, cols;
	int time, bombs = 0, flags = 0;
	int gameMode = 3;		//Starts the game on expert difficulty

	boolean game;
	boolean win;
	boolean bombsLoaded;
	
	public static void main(String[] args) {
		new MinesweeperWN();
	}
	
	MinesweeperWN() {
		construct();

		frame = new JFrame("Minesweeper");
		dPanel = new DrawingPanel();
		mPanel = new MessagePanel();

		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		frame.add(dPanel, BorderLayout.SOUTH);
		frame.add(mPanel);
		
		frame.pack();
		frame.setResizable(false);
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}

	void construct() {
		if(gameMode == 1) beginnerConstruct();
		if(gameMode == 2) intermediateConstruct();
		if(gameMode == 3) expertConstruct();

		//declares the dimensions of the dPanel based on cols/rows * square size
		boardX = SQUARE*cols;
		boardY = SQUARE*rows;
		board = new int[cols][rows];
		visible = new int[cols][rows];

		game = true;
		win = false;
		bombsLoaded = false;
		if(timer != null) timer.cancel();
		time = 0;
		flags = 0;

		//For changing the difficulty, which changes the size of the window
		if(frame != null) {
			dPanel.setPreferredSize(new Dimension(boardX, boardY));
			mPanel.setPreferredSize(new Dimension(boardX, 50));

			frame.add(dPanel, BorderLayout.SOUTH);
			frame.add(mPanel);
	
			frame.pack();
		}

		lights.put(0, new boolean[] {true, true, true, true, false, true, true});
		lights.put(1, new boolean[] {false, false, true, false, false, true, false});
		lights.put(2, new boolean[] {false, true, true, true, true, false, true});
		lights.put(3, new boolean[] {false, true, true, false, true, true, true});
		lights.put(4, new boolean[] {true, false, true, false, true, true, false});
		lights.put(5, new boolean[] {true, true, false, false, true, true, true});
		lights.put(6, new boolean[] {true, true, false, true, true, true, true});
		lights.put(7, new boolean[] {false, true, true, false, false, true, false});
		lights.put(8, new boolean[] {true, true, true, true, true, true, true});
		lights.put(9, new boolean[] {true, true, true, false, true, true, true});
		lights.put(45, new boolean[] {false, false, false, false, true, false, false}); //ascii for negative sign

		color.put(1, Color.BLUE);
		color.put(2, new Color(0, 102, 0)); 	//green
		color.put(3, Color.RED);
		color.put(4, new Color(85,26,139));	//purple
		color.put(5, new Color(139, 0, 0));	//dark red
		color.put(6, new Color(0, 55, 55));	//dark cyan
		color.put(7, Color.BLACK);
		color.put(8, Color.GRAY);
	}

	void beginnerConstruct() {
		rows = 9;
		cols = 9;
		bombs = 10;
	}

	void intermediateConstruct() {
		rows = 16;
		cols = 16;
		bombs = 40;
	}

	void expertConstruct() {
		rows = 16;
		cols = 30;
		bombs = 99;
	}
	
	//This method is called after the first click on a square
	void bombConstruct(int firstX, int firstY) {
		//Places bombs randomly
		for(int i = 0; i < bombs; i++) {
			int random = (int)(Math.random()*(rows*cols));
			int y = random / cols;
			int x = random % cols;

			//If random picks cords within the area surrounding the first clicked square, then it skips
			if(x >= firstX-1 && x <= firstX+1 
			&& y >= firstY-1 && y <= firstY+1) {
				i--;
				continue;
			}
			
			if(board[x][y] != BOMB) board[x][y] = BOMB;
			else i--;
		}
				
		//Assigns the number of surrounding bombs to squares
		for(int i = 0; i < rows*cols; i++) {
			int y = i / cols;
			int x = i % cols;
			
			if(board[x][y] == 0) board[x][y] = checkSurroundings(x, y, board);
		}
		
		bombsLoaded = true;
		timer = new Timer();
		time = 0;
	}
	
	//Returns the number of bombs around the cords
	int checkSurroundings(int x, int y, int[][] array) {
		int maxX = x+1;
		int maxY = y+1;
		
		//Checks for borders
		if(x+1 == cols) maxX--;
		if(y+1 == rows) maxY--;

		if(x-1 != -1) x--;
		if(y-1 != -1) y--;
		
		int counter = 0;
		//Searches the squares around the point for bombs
		for(; y <= maxY; y++) {
			int temp = x;
			for(; x <= maxX; x++) {
				if(array[x][y] == BOMB /*FLAG has the same value*/) counter++;
			}
			x = temp;
		}
		return counter;
	}
	
	private class MessagePanel extends JPanel implements MouseListener{
		boolean buttonHolding;

		MessagePanel() {
			this.setPreferredSize(new Dimension(boardX, 50));
			this.addMouseListener(this);
		}

		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			Graphics2D g2 = (Graphics2D) g; 
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			bombDisplay(g);
			timeDisplay(g);
			g.setColor(Color.BLACK);
			g.drawRect(boardX/2-20, 5, 40, 40);

			if(!buttonHolding) {
				g.setColor(Color.WHITE);
				g.fillRect(boardX/2-19, 6, 38, 3);
				g.fillRect(boardX/2-19, 6, 3, 38);
				g.setColor(Color.GRAY);
				g.fillRect(boardX/2+17, 6, 3, 38);
				g.fillRect(boardX/2-19, 42, 39, 3);	
			}
			else {
				g.setColor(Color.GRAY);
				g.fillRect(boardX/2-20, 5, 39, 3);
				g.fillRect(boardX/2-20, 5, 3, 39);
			}

			if(!game && !win) dead(g);	//dead
			else smile(g);				//default
			if(win) winFace(g);			//win
		}

		void smile(Graphics g) {
			//head
			g.setColor(Color.YELLOW);
			g.fillOval(boardX/2-15, 10, 30, 30);
			g.setColor(Color.BLACK);
			g.drawOval(boardX/2-15, 10, 30, 30);
			//smile
			g.fillRect(boardX/2-6, 21, 4, 4);
			g.fillRect(boardX/2+3, 21, 4, 4);
			g.fillRect(boardX/2-4, 32, 9, 2);
			g.fillRect(boardX/2-5, 31, 2, 2);
			g.fillRect(boardX/2-6, 30, 2, 2);
			g.fillRect(boardX/2-7, 29, 2, 2);
			g.fillRect(boardX/2+4, 31, 2, 2);
			g.fillRect(boardX/2+5, 30, 2, 2);
			g.fillRect(boardX/2+6, 29, 2, 2);
		}

		void dead(Graphics g) {
			//head
			g.setColor(Color.YELLOW);
			g.fillOval(boardX/2-15, 10, 30, 30);
			g.setColor(Color.BLACK);
			g.drawOval(boardX/2-15, 10, 30, 30);
			//left eye
			g.fillRect(boardX/2-7, 20, 2, 2);
			g.fillRect(boardX/2-7, 24, 2, 2);
			g.fillRect(boardX/2-5, 22, 2, 2);
			g.fillRect(boardX/2-3, 20, 2, 2);
			g.fillRect(boardX/2-3, 24, 2, 2);
			//right eye
			g.fillRect(boardX/2+2, 20, 2, 2);
			g.fillRect(boardX/2+2, 24, 2, 2);
			g.fillRect(boardX/2+4, 22, 2, 2);
			g.fillRect(boardX/2+6, 20, 2, 2);
			g.fillRect(boardX/2+6, 24, 2, 2);
			//mouth
			g.fillRect(boardX/2-4, 28, 9, 2);
			g.fillRect(boardX/2-5, 29, 2, 2);
			g.fillRect(boardX/2-6, 30, 2, 2);
			g.fillRect(boardX/2-7, 31, 2, 2);
			g.fillRect(boardX/2+4, 29, 2, 2);
			g.fillRect(boardX/2+5, 30, 2, 2);
			g.fillRect(boardX/2+6, 31, 2, 2);
		}

		void winFace(Graphics g) {
			//face is already there, so this only adds sunglasses
			g.fillRect(boardX/2-10, 20, 21, 2);
			g.fillOval(boardX/2-10, 20, 8, 7);
			g.fillOval(boardX/2+3, 20, 8, 7);
			g.drawLine(boardX/2-15, 28, boardX/2-7, 20);
			g.drawLine(boardX/2+15, 28, boardX/2+8, 20);
		}

		//Draws the counter for num of bombs - flags
		void bombDisplay(Graphics g) {
			g.setColor(Color.BLACK);
			g.fillRect(10, 5, 63, 40);

			int currentBombs = bombs-flags;

			String display = Integer.toString(Math.abs(currentBombs));
			if(currentBombs < 10 && currentBombs > -10) display = "0"+display;
			if(currentBombs < 0) display = "-"+display;
			else if(currentBombs < 100) display = "0"+display;

			numDisplay(display.charAt(0), display.charAt(1)-'0', display.charAt(2)-'0', 15, g);
		}

		//Draws the counter for seconds
		void timeDisplay(Graphics g) {
			g.setColor(Color.BLACK);
			g.fillRect(boardX-73, 5, 63, 40);

			String display = Integer.toString(time);
			if(time < 10) display = "0"+display;
			if(time < 100) display = "0"+display;
			if(time > 999) display = "999";

			numDisplay(display.charAt(0), display.charAt(1)-'0', display.charAt(2)-'0', boardX-68, g);
		}

		//Draws a seven-segment display
		//(It was only after doing the code that I realized there was a convention for the segment ordering/naming, so bare with the unconventional ordering)
		void numDisplay(char one, int two, int three, int xPos, Graphics g) {
			int temp = one;
			if(temp != '-') temp -= '0'; 

			Color darkRed = new Color(139, 0, 0, 150);
			int[] nums = {temp, two, three};
			int distance = 20; //between numbers
			int discFromTop = 15;
	
			for(int i = 0; i < 3; i++) {
				//F segment
				if(lights.get(nums[i])[0]) g.setColor(Color.RED);
				else g.setColor(darkRed);
				g.fillRect(xPos-1+i*distance, discFromTop+1, 3, 7);
				g.fillRect(xPos+i*distance, discFromTop, 1, 9);

				//A segment
				if(lights.get(nums[i])[1]) g.setColor(Color.RED);
				else g.setColor(darkRed);
				g.fillRect(xPos+3+i*distance, discFromTop-3, 7, 3);
				g.fillRect(xPos+2+i*distance, discFromTop-2, 9, 1);

				//B segment
				if(lights.get(nums[i])[2]) g.setColor(Color.RED);
				else g.setColor(darkRed);
				g.fillRect(xPos+11+i*distance, discFromTop+1, 3, 7);
				g.fillRect(xPos+12+i*distance, discFromTop, 1, 9);

				//E segment
				if(lights.get(nums[i])[3]) g.setColor(Color.RED);
				else g.setColor(darkRed);
				g.fillRect(xPos-1+i*distance, discFromTop+13, 3, 7);
				g.drawLine(xPos+i*distance, discFromTop+12, xPos+i*distance, discFromTop+20);

				//G segment
				if(lights.get(nums[i])[4]) g.setColor(Color.RED);
				else g.setColor(darkRed);
				g.fillRect(xPos+3+i*distance, discFromTop+9, 7, 2);
				g.drawLine(xPos+2+i*distance, discFromTop+10, xPos+10+i*distance, discFromTop+10);

				//C segment
				if(lights.get(nums[i])[5]) g.setColor(Color.RED);
				else g.setColor(darkRed);
				g.fillRect(xPos+11+i*distance, discFromTop+13, 3, 7);
				g.drawLine(xPos+12+i*distance, discFromTop+12, xPos+12+i*distance, discFromTop+20);

				//D segment
				if(lights.get(nums[i])[6]) g.setColor(Color.RED);
				else g.setColor(darkRed);
				g.fillRect(xPos+3+i*distance, discFromTop+21, 7, 3);
				g.fillRect(xPos+2+i*distance, discFromTop+22, 9, 1);
			}
		}

		@Override
		public void mouseClicked(MouseEvent e) {
			//Displays a popupmenu for changing the difficulty
			if(e.getButton() == MouseEvent.BUTTON3) {
				JPopupMenu difficulty = new JPopupMenu("Difficulty");

				JMenuItem beginner = new JMenuItem("Beginner");
				JMenuItem intermediate = new JMenuItem("Intermediate");
				JMenuItem expert = new JMenuItem("Expert");

				difficulty.add(beginner);
				difficulty.add(intermediate);
				difficulty.add(expert);

				difficulty.show(this, e.getX(), e.getY());  

				beginner.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						gameMode = 1;
						construct();
					}
				});
		 
				intermediate.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						gameMode = 2;
						construct();
					}
				});
		 
				expert.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						gameMode = 3;
						construct();
					}
				});
			}
		}

		@Override
		public void mousePressed(MouseEvent e) {
			int xCord = e.getX();
			int yCord = e.getY();

			//Displays a holding sprite
			if(xCord > boardX/2-20 && xCord < boardX/2+20
			&& yCord > 5 && yCord < 45 && e.getButton() != MouseEvent.BUTTON3) {
				buttonHolding = true;
				this.repaint();
			}
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			int xCord = e.getX();
			int yCord = e.getY();

			//Restarts the game if clicked
			if(xCord > boardX/2-20 && xCord < boardX/2+20
			&& yCord > 5 && yCord < 45 && e.getButton() != MouseEvent.BUTTON3) {
				construct();
				dPanel.repaint();
				if(timer != null) {
					timer.cancel();
					time = 0;
				}
			}

			buttonHolding = false;
			this.repaint();
		}

		@Override
		public void mouseEntered(MouseEvent e) {
			
		}

		@Override
		public void mouseExited(MouseEvent e) {

		}
	}

	private class DrawingPanel extends JPanel implements MouseListener {
		Point mouse;

		DrawingPanel() {
			this.setPreferredSize(new Dimension(boardX, boardY));
			this.addMouseListener(this);
		}
		
		@Override
		public void paintComponent(Graphics g) {
			super.paintComponent(g);

			//Draws grid
			for(int i = 0; i < cols; i++) {
				g.drawLine(SQUARE*i, 0, SQUARE*i, boardY);
			}
			for(int i = 0; i < rows; i++) {
				g.drawLine(0, SQUARE*i, boardX, SQUARE*i);
			}

			Font font1 = new Font("Arial", Font.BOLD, 20);
			g.setFont(font1);

			//Draws graphics for each tile
			for(int i = 0; i < cols; i++) {
				for(int j = 0; j < rows; j++) {
					if(visible[i][j] == VISIBLE) g.setColor(color.get(board[i][j]));
					else g.setColor(Color.BLACK);
					if(visible[i][j] == VISIBLE && board[i][j] > 0) g.drawString(Integer.toString(board[i][j]), i*SQUARE + SQUARE/2 -5, j*SQUARE + SQUARE/2 +7);
					if(visible[i][j] == FLAG) drawFlag(i, j, g);
					if(visible[i][j] != VISIBLE && visible[i][j] != HOLDING) hiddenTile(i, j, g);

					g.setColor(Color.RED);
					if(board[i][j] == GAMEOVER) g.fillRect(i*SQUARE, j*SQUARE, 30, 30);
					g.setColor(Color.BLACK);
					if(board[i][j] <= BOMB && visible[i][j] == VISIBLE) bombTile(i, j, g);
					if(visible[i][j] == FALSEFLAG) falseFlag(i, j, g);
				}
			}

			//Checks for a win, freezes and paints win if true
			if(checkWin() && game)  {
				win = true;
				gameOver();				
				for(int i = 0; i < cols; i++) {
					for(int j = 0; j < rows; j++) {
						if(board[i][j] == BOMB) visible[i][j] = FLAG;
					}
				}
				this.repaint();
			}
		}

		void drawFlag(int x, int y, Graphics g) {
			g.setColor(Color.BLACK);
			g.fillRect(x*SQUARE+6, y*SQUARE+23, 18, 3);
			g.fillRect(x*SQUARE+14, y*SQUARE+6, 3, 17);
			g.setColor(Color.RED);
			g.fillPolygon(new int[]{x*SQUARE+17, x*SQUARE+5, x*SQUARE+17}, new int[]{y*SQUARE+5, y*SQUARE+12, y*SQUARE+19}, 3);
		}

		//default tile sprite
		void hiddenTile(int x, int y, Graphics g) {
			g.setColor(Color.WHITE);
			g.fillRect(x*SQUARE+1, y*SQUARE+1, 29, 3);
			g.fillRect(x*SQUARE+1, y*SQUARE+1, 3, 29);
			g.setColor(Color.GRAY);
			g.fillRect(x*SQUARE+1, y*SQUARE+27, 29, 3);
			g.fillRect(x*SQUARE+27, y*SQUARE+1, 3, 29);
		}

		void falseFlag(int x, int y, Graphics g) {
			bombTile(x, y, g);
			g.setColor(Color.RED);
			g.drawLine(x*SQUARE+1, y*SQUARE+1, x*SQUARE+28, y*SQUARE+28);
			g.drawLine(x*SQUARE+1, y*SQUARE+28, x*SQUARE+28, y*SQUARE+1);
		}

		void bombTile(int x, int y, Graphics g) {
			g.fillOval(x*SQUARE+6, y*SQUARE+6, 18, 18);
			g.drawLine(x*SQUARE+3, y*SQUARE+15,x*SQUARE+27, y*SQUARE+15);
			g.drawLine(x*SQUARE+15,y*SQUARE+3, x*SQUARE+15, y*SQUARE+27);
			g.drawLine(x*SQUARE+6, y*SQUARE+6, x*SQUARE+24, y*SQUARE+24);
			g.drawLine(x*SQUARE+6, y*SQUARE+24,x*SQUARE+24, y*SQUARE+6);
		}
		
		@Override
		public void mouseClicked(MouseEvent e) {
			
		}

		@Override
		public void mousePressed(MouseEvent e) {
			int xCord = e.getX()/SQUARE;
			int yCord = e.getY()/SQUARE;
			mouse = new Point(xCord, yCord);
			
			int maxX = xCord;
			int maxY = yCord;
			
			if(e.getButton() == MouseEvent.BUTTON2) {
				maxX = xCord+1;
				maxY = yCord+1;

				//Checks for borders
				if(xCord+1 == cols) maxX--;
				if(yCord+1 == rows) maxY--;

				if(xCord-1 != -1) xCord--;
				if(yCord-1 != -1) yCord--;
			}

			//Iterates through the squares around the point
			for(; yCord <= maxY; yCord++) {
				int temp = xCord;
				for(; xCord <= maxX; xCord++) {
					if(visible[xCord][yCord] == HIDDEN) visible[xCord][yCord] = HOLDING; //(a holding sprite)
				}
				xCord = temp;
			}
		
			this.repaint();
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			int xCord = (int) mouse.getX();
			int yCord = (int) mouse.getY();

			int maxX = xCord+1;
			int maxY = yCord+1;
			int x = xCord;
			int y = yCord;
	
			//Checks for borders
			if(xCord+1 == cols) maxX--;
			if(yCord+1 == rows) maxY--;
	
			if(xCord-1 != -1) x--;
			if(yCord-1 != -1) y--;

			//Returns the chord holding sprite to normal
			for(; y <= maxY; y++) {
				int temp = x;
				for(; x <= maxX; x++) {
					if(visible[x][y] == HOLDING) visible[x][y] = HIDDEN;
				}
				x = temp;
			}

			this.repaint();

			xCord = e.getX()/SQUARE;
			yCord = e.getY()/SQUARE;
			//Checks if the mouse lets go in the same tile as it clicked. If not, return
			if(xCord != (int) mouse.getX()) return;
			if(yCord != (int) mouse.getY()) return;
			
			//If first click, the game starts
			if(!bombsLoaded && e.getButton() == MouseEvent.BUTTON1) {
				bombConstruct(xCord, yCord);
				beginTimer();
			}

			if(game) {
				if(e.getButton() == MouseEvent.BUTTON1) reveal(xCord, yCord); 	//Left mouse button
				if(e.getButton() == MouseEvent.BUTTON2) chord(xCord, yCord);	//Mouse wheel
				if(e.getButton() == MouseEvent.BUTTON3) flag(xCord, yCord);		//Right mouse button
			}
			
			this.repaint();
			mPanel.repaint();
		}

		@Override
		public void mouseEntered(MouseEvent e) {
			
		}

		@Override
		public void mouseExited(MouseEvent e) {
			
		}
		
		//Reveals tiles
		private void reveal(int x, int y) {	
			if(board[x][y] == 0) {
				revealEmpty(x, y);
				//chord(x, y);
				//Calling the chord method works perfectly fine (and is more intuitive), but I wanted to implement bfs
				return;
			}

			if(visible[x][y] == HIDDEN) visible[x][y] = VISIBLE;
			else return;

			if(board[x][y] == BOMB) {
				gameOver();
				board[x][y] = GAMEOVER;
			}	
		}
		
		//Chording reveals all the spaces around a number, provided that an adequate number of flags to the number are present
		private void chord(int x, int y) {
			//Returns if surrounding flags != surrounding bombs, or if the square hasn't been discovered / has been flagged
			if(checkSurroundings(x, y, board) != checkSurroundings(x, y, visible)
			|| visible[x][y] == HIDDEN || visible[x][y] == FLAG) return;
			
			int maxX = x+1;
			int maxY = y+1;
			
			//Checks for borders
			if(x+1 == cols) maxX--;
			if(y+1 == rows) maxY--;

			if(x-1 != -1) x--;
			if(y-1 != -1) y--;

			//Calls the reveal method on all surrounding squares
			for(; y <= maxY; y++) {
				int temp = x;
				for(; x <= maxX; x++) {
					if(visible[x][y] == HIDDEN) reveal(x, y);
				}
				x = temp;
			}
		}

		//Flags the tile
		private void flag(int x, int y) {
			if(visible[x][y] == HIDDEN) {
				visible[x][y] = FLAG;
				flags++;
				return;
			}

			if(visible[x][y] == FLAG) {
				visible[x][y] = HIDDEN;
				flags--;
			}
		}

		//Reveals a field of empty tiles
		void revealEmpty(int x, int y) {
			boolean[][] checked = new boolean[cols][rows];
			ArrayList<Point> queue = new ArrayList<Point>();
			
			queue.add(new Point(x,y));
			
			while(true) {
				x = (int) queue.get(0).getX();
				y = (int) queue.get(0).getY();
				int maxX = x+1;
				int maxY = y+1;
				
				//Checks for borders
				if(x+1 == cols) maxX--;
				if(y+1 == rows) maxY--;
	
				if(x-1 != -1) x--;
				if(y-1 != -1) y--;
	
				//Reveals the squares around the point, and adds no number squares to queue
				for(; y <= maxY; y++) {
					int temp = x;
					for(; x <= maxX; x++) {
						if(checked[x][y]) continue;
						checked[x][y] = true;

						if(board[x][y] == 0) queue.add(new Point(x,y));
						if(visible[x][y] != FLAG) visible[x][y] = VISIBLE;
					}
					x = temp;
				}
				
				queue.remove(0);
				if(queue.size() == 0) break;
			}
		}	

		//Begins a timer thread that increases the time counter every second
		private void beginTimer() {
			timer.scheduleAtFixedRate(new TimerTask() {
				@Override
				public void run() {
					time++;
					mPanel.repaint();
				}
			}, 1000, 1000);
		}
	}

	boolean checkWin() {
		for(int i = 0; i < cols; i++) {
			for(int j = 0; j < rows; j++) {
				//if there is an empty field that has not been discovered, then no win
				if(board[i][j] != BOMB && (visible[i][j] == HIDDEN || visible[i][j] == HOLDING || visible[i][j] == FLAG)) return false;
			}
		}
		
		//there is no need to flag all the bombs for a win, only discover all safe tiles
		return true;
	}
	
	void gameOver() {		
		game = false;
		timer.cancel();
		mPanel.repaint();

		//checks if the game over is a win or not
		//if(win) return;

		//Reveals all bombs and false flags
		for(int i = 0; i < cols; i++) {
			for(int j = 0; j < rows; j++) {
				if(board[i][j] == BOMB && visible[i][j] == HIDDEN) visible[i][j] = VISIBLE;
				if(board[i][j] != BOMB && visible[i][j] == FLAG) visible[i][j] = FALSEFLAG;
			}
		}
	}
}