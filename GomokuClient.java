package omokTest;

import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.io.*;
import java.util.*;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JOptionPane;

//DrawBoard class for drawing gomoku board
//and implementing winner check feature
class DrawBoard extends Canvas{
	public static boolean timerStart = false;
	public static boolean timerEnd = false;
	public static final int BLACK = 1;		//constant value for black/white stone
	public static final int WHITE = -1;		//constant value for black/white stone
	private Graphics boardGUI;				//graphics object for drawing board as GUI
	private Graphics boardGUIBuffer;		//graphics object for GUI board buffer
	private Image boardGUIImage;			//image object for GUI board double buffering
	private int[][] board;					//board array for storing stone information
	private int boardSize;					//size of the board, international standard size is 15 by 15
	private int boardCellSize;				//size of each cell of the board, for GUI purpose
	private int myColor = BLACK;			//stone color of user
	private boolean canPlaceStone = false;	//player can place stone if value is true
	private boolean isGameRunning = false;	//check if game is currently playing
	private PrintWriter toServer;			//data stream for sending information to server

	//constructor of the board
	DrawBoard(int size, int cell) {
		this.boardSize = size;
		this.boardCellSize = cell;
		
		board = new int[boardSize + 2][];
		for(int i = 0; i < board.length; i++) {
			board[i] = new int[boardSize + 2];
		}
		
		setBackground(new Color(204, 255, 102));
		setSize(boardSize*(boardCellSize + 1) + boardSize, boardSize*(boardCellSize + 1) + boardSize);
		
		//create mouse event listener by using anonymous class method
		addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent mouseEvent) {
				//if player cannot place stone(not pleyer's turn, terminate mouse event listener
				if(!canPlaceStone) {
					return;
				}
				
				//get x coordinate and y coordinate when mouse is clicked.
				//By using round() method, round off coordinate value to the nearest integer.
				int x = (int)Math.round(mouseEvent.getX() / (double)boardCellSize);
				int y = (int)Math.round(mouseEvent.getY() / (double)boardCellSize);
				
				//terminate mouse event if mouse is clicked on a restricted area
				//When mouse is clicked outside the board or there is a stone already been placed
				if(x == 0 || y == 0 || x == boardSize + 1 || y == boardSize + 1) {
					return;
				}
				
				if(board[x][y] == BLACK || board[x][y] == WHITE) {
					return;
				}
				
				//create point object of the stone's location
				//Point stoneLocation = new Point(x, y);
				
				//check if player attempts to place stone on a prohibited location
				if(prohibitionTest(new Point(x, y), myColor)) {
					return;
				}
				
				//send stone location to server and flag stone status on a board
				toServer.println("[STONE]" + x + " " + y);
				
				System.out.println("[STONE]" + x + " " + y);
				
				board[x][y] = myColor;
				
				//check if player wins by placing 5 stones in a row
				if(winCheck(new Point(x, y), myColor) == true) {
					toServer.println("[WIN]");
				}else {
					timerEnd = true;
				}
				
				//update board status to reflect newly placed stone on the board
				repaint();
				
				//update player status to restrict placing more stones until player's turn
				canPlaceStone = false;
			}
		});
	}
	
	public int timerReset() {
		return 20;
	}
	
	//return game status
	public boolean isRunning() {
		return isGameRunning;
	}
	
	//initial game start: set game status to 'running'. Black first places a stone
	public void startGame(String color) {
		isGameRunning = true;
		if(color.equals("BLACK")) {
			canPlaceStone = true;
			myColor = BLACK;
			timerStart = true;
		}
		else {
			canPlaceStone = false;
			myColor = WHITE;
		}
	}
	
	
	//game ended: reset board, set game status to 'stopped', and player cannot place stone.
	public void stopGame() {
		reset();
		toServer.println("[STOPGAME]");
		isGameRunning = false;
		canPlaceStone = false;
	}
	
	
	
	//place opponent stone on player's screen
	public void placeOpponentStone(int x, int y) {
		board[x][y] = -myColor;
		repaint();
		timerStart = true;
	}
	
	//set player's turn
	public void setPlayerTurn(boolean isPlayerTurn) {
		canPlaceStone = isPlayerTurn;
	}
	
	//set output stream to server
	public void setWriter(PrintWriter toServer) {
		this.toServer = toServer;
	}
	
	//update method is automatically called when repaint() method is called: re-paint board GUI
	public void update(Graphics board) {
		paint(board);
	}
	
	//draw screen as buffered image
	public void paint(Graphics board) {
		if(boardGUIBuffer == null) {
			boardGUIImage = createImage(getWidth(), getHeight());
			boardGUIBuffer = boardGUIImage.getGraphics();
		}
		drawBoardGraphic(board);
	}
	
	//reset board information: clear all stones on the board
	public void reset() {
		for(int i = 0; i < board.length; i++) {
			for(int j = 0; j < board[i].length; j ++) {
				board[i][j] = 0;
			}
		}
		
		repaint();
	}
	
	//draw line on the board
	private void drawLine() {
		boardGUIBuffer.setColor(Color.black);
		
		for(int i = 1; i <= boardSize; i++) {
			boardGUIBuffer.drawLine(boardCellSize, i * boardCellSize, boardCellSize * boardSize, i * boardCellSize);
			boardGUIBuffer.drawLine(i * boardCellSize, boardCellSize, i * boardCellSize, boardCellSize * boardSize);
		}
	}
	
	//draw black stone on the x, y coordinate
	private void drawBlack(int x, int y) {
		Graphics2D boardGUIBuffer = (Graphics2D)this.boardGUIBuffer;
		boardGUIBuffer.setColor(Color.black);
		boardGUIBuffer.fillOval(x * boardCellSize - boardCellSize / 2, y * boardCellSize - boardCellSize / 2, boardCellSize, boardCellSize);
		boardGUIBuffer.setColor(Color.white);
		boardGUIBuffer.drawOval(x * boardCellSize - boardCellSize / 2, y * boardCellSize - boardCellSize / 2, boardCellSize, boardCellSize);
	}
	
	//draw white stone on the x, y coordinate
	private void drawWhite(int x, int y) {
		boardGUIBuffer.setColor(Color.white);
		boardGUIBuffer.fillOval(x * boardCellSize - boardCellSize / 2, y * boardCellSize - boardCellSize / 2, boardCellSize, boardCellSize);
		boardGUIBuffer.setColor(Color.black);
		boardGUIBuffer.drawOval(x * boardCellSize - boardCellSize / 2, y * boardCellSize - boardCellSize / 2, boardCellSize, boardCellSize);
	}
	
	//draw all stones placed on the board
	private void drawStone() {
		for(int i = 1; i <= boardSize; i++) {
			for(int j = 1; j <= boardSize; j++) {
				if(board[i][j] == BLACK) {
					drawBlack(i, j);
				}
				else if(board[i][j] == WHITE) {
					drawWhite(i, j);
				}
			}
		}
	}
	
	//draw board
	synchronized private void drawBoardGraphic(Graphics board) {
		boardGUIBuffer.clearRect(0, 0, getWidth(), getHeight());
		drawLine();
		drawStone();
		//boardGUIBuffer.setColor(Color.red);
		//boardGUIBuffer.drawString(" ", 20, 15);
		board.drawImage(boardGUIImage, 0, 0, this);
	}
	
	//check if player has won
	//white stone can win with more than 6 stones in a row
	private boolean winCheck(Point p, int color) {
		if(color == WHITE) {
			if (count(p, 1, 0, color) + count(p, -1, 0, color) >= 4) {
				return true;
			}
			if (count(p, 0, 1, color) + count(p, 0, -1, color) >= 4) {
				return true;
			}
			if (count(p, -1, -1, color) + count(p, 1, 1, color) >= 4) {
				return true;
			}
			if (count(p, 1, -1, color) + count(p, -1, 1, color) >= 4) {
				return true;
			}
		}
		else {
			if(count(p, 1, 0, color) + count(p, -1, 0, color) == 4) {
				return true;
			}
			if(count(p, 0, 1, color) + count(p, 0, -1, color) == 4) {
				return true;
			}
			if(count(p, -1, -1, color) + count(p, 1, 1, color) == 4) {
				return true;
			}
			if(count(p, 1, -1, color) + count(p, -1, 1, color) == 4) {
				return true;
			}
		}
		return false;
	}
	
	//check for prohibited move
	//black stone cannot place in 3-3 or 4-4 form
	//black stone cannot place 6 or more stones in a row
	private boolean prohibitionTest(Point p, int col) {
		if (col == WHITE)
			return false;
		else if (winCheck(p, col))
			return false;
		else {
			//check for 6 stones or more in a row
			int result = 0;
			result += fourORjang1(p, col, 2);
			result += fourORjang2(p, col, 2);
			result += fourORjang3(p, col, 2);
			result += fourORjang4(p, col, 2);	
			//if one or more 6>in a row is detected
			if(result >= 1)
				return true;
			//check for 4-4 form
			int fourStone = 0;
			fourStone += fourORjang1(p, col, 1);
			fourStone += fourORjang2(p, col, 1);
			fourStone += fourORjang3(p, col, 1);
			fourStone += fourORjang4(p, col, 1);
			if (fourStone >= 2)
				return true;
			//check for 3-3 form
			int open_sam_count = 0;
			open_sam_count += find3_1(p, col);
			open_sam_count += find3_2(p, col);
			open_sam_count += find3_3(p, col);
			open_sam_count += find3_4(p, col);

			if (open_sam_count >= 2)
				return true;
			else
				return false;
		}
	}

	//check for 3-3 position, horizontal
	private int find3_1(Point p, int col) {
		int stone1 = 0;
		int stone2 = 0;
		int allStone = 0;
		// 열린 3인지 체크하기위한것..
		int blink1 = 1;
		int xx = p.x - 1;
		boolean check = false;
		// ←
		left: while (true) {
			// 좌표끝도달
			if (xx == 0)
				break left;

			// check를 false로 바꿈으로 두번연속으로 만나는지 확인할수있게.
			if (board[xx][p.y] == col) {
				check = false;
				stone1++;
			}

			// 상대돌을 만나면 탐색중지
			if (board[xx][p.y] == -1 * col)
				break left;

			if (board[xx][p.y] == 0) {
				// 처음 빈공간을만나 check가 true가 됬는데
				// 연달아 빈공간을만나면 탐색중지
				// 두번연속으로 빈공간만날시 blink카운트를 되돌림.
				if (check == false) {
					check = true;
				} else {
					blink1++;
					break left;
				}

				if (blink1 == 1) {
					blink1--;
				} else {
					break left; // 빈공간을만났으나 빈공간을 두번만나면 끝임
				}
			}
			// 계속탐색
			xx--;
		}
		// →
		xx = p.x + 1; // 달라지는 좌표
		int blink2 = blink1; // blink1남은거만큼 blink2,
		if (blink1 == 1) // 빈공간을 만나지않은경우 없었음을기록
			blink1 = 0;
		check = false;
		right: while (true) {
			// 좌표끝도달
			if (xx == boardSize + 1)
				break right;

			if (board[xx][p.y] == col) {
				check = false;
				stone2++;
			}

			// 상대돌을 만나면 탐색중지
			if (board[xx][p.y] == -1 * col)
				break right;

			if (board[xx][p.y] == 0) {
				// 두번연속으로 빈공간만날시 blink카운트를 되돌림.
				if (check == false) {
					check = true;
				} else {
					blink2++;
					break right;
				}

				if (blink2 == 1) {
					blink2--;
				} else {
					break right; // 빈공간을만났으나 빈공간을 두번만나면 끝임
				}
			}
			xx++;
		}
		allStone = stone1 + stone2;
		// 삼삼이므로 돌갯수가 2 + 1(현재돌)이아니면 0리턴
		// 이부분이 43을 허용하게해줌. 33만 찾게됨
		if (allStone != 2) {
			return 0;
		}
		// 돌갯수가 3이면 열린 3인지 파악.

		int left = (stone1 + blink1);
		int right = (stone2 + blink2);

		// 벽으로 막힌경우 - 열린3이 아님
		if (p.x - left == 1 || p.x + right == boardSize) {
			return 0;
		} else // 상대돌로 막힌경우 - 열린3이 아님
		if (board[p.x - left - 1][p.y] == -1 * col || board[p.x + right + 1][p.y] == -1 * col) {
			return 0;
		} else {
			return 1; // 열린3 일때 1 리턴
		}
	}

	//check for 3-3 form, diagonal
	private int find3_2(Point p, int col) {
		int stone1 = 0;
		int stone2 = 0;
		int allStone = 0;
		int blink1 = 1;

		// ↙
		int xx = p.x - 1;
		int yy = p.y - 1;
		boolean check = false;
		leftDown: while (true) {
			if (xx == 0 || yy == 0)
				break leftDown;

			if (board[xx][yy] == col) {
				check = false;
				stone1++;
			}

			if (board[xx][yy] == -1 * col)
				break leftDown;

			if (board[xx][yy] == 0) {
				if (check == false) {
					check = true;
				} else {
					blink1++;
					break leftDown;
				}

				if (blink1 == 1) {
					blink1--;
				} else {
					break leftDown;
				}
			}
			xx--;
			yy--;
		}

		// ↗
		int blink2 = blink1;
		if (blink1 == 1)
			blink1 = 0;
		xx = p.x + 1;
		yy = p.y + 1;
		check = false;
		rightUp: while (true) {
			if (xx == boardSize + 1 || yy == boardSize + 1)
				break rightUp;

			if (board[xx][yy] == col) {
				check = false;
				stone2++;
			}

			if (board[xx][yy] == -1 * col)
				break rightUp;

			if (board[xx][yy] == 0) {
				if (check == false) {
					check = true;
				} else {
					blink2++;
					break rightUp;
				}

				if (blink2 == 1) {
					blink2--;
				} else {
					break rightUp;
				}
			}

			xx++;
			yy++;
		}

		allStone = stone1 + stone2;
		if (allStone != 2) {
			return 0;
		}

		int leftDown = (stone1 + blink1);
		int rightUp = (stone2 + blink2);

		if (p.y - leftDown == 1 || p.x - leftDown == 1 || p.y + rightUp == boardSize || p.x + rightUp == boardSize) {
			return 0;
		} else if (board[p.x - leftDown - 1][p.y - leftDown - 1] == -1 * col
				|| board[p.x + rightUp + 1][p.y + rightUp + 1] == -1 * col) {
			return 0;
		} else {
			return 1;
		}

	}

	//check for 3-3 form, vertical
	private int find3_3(Point p, int col) {
		int stone1 = 0;
		int stone2 = 0;
		int allStone = 0;
		int blink1 = 1;

		// ↓
		int yy = p.y - 1;
		boolean check = false;
		down: while (true) {
			if (yy == 0)
				break down;

			if (board[p.x][yy] == col) {
				check = false;
				stone1++;
			}

			if (board[p.x][yy] == -1 * col)
				break down;

			if (board[p.x][yy] == 0) {
				if (check == false) {
					check = true;
				} else {
					blink1++;
					break down;
				}

				if (blink1 == 1) {
					blink1--;
				} else {
					break down;
				}
			}
			yy--;
		}

		// ↑
		int blink2 = blink1;
		if (blink1 == 1)
			blink1 = 0;
		yy = p.y + 1;
		check = false;
		up: while (true) {
			if (yy == boardSize + 1)
				break up;

			if (board[p.x][yy] == col) {
				check = false;
				stone2++;
			}

			if (board[p.x][yy] == -1 * col)
				break up;

			if (board[p.x][yy] == 0) {
				if (check == false) {
					check = true;
				} else {
					blink2++;
					break up;
				}

				if (blink2 == 1) {
					blink2--;
				} else {
					break up;
				}
			}

			yy++;
		}

		allStone = stone1 + stone2;
		if (allStone != 2) {
			return 0;
		}

		int down = (stone1 + blink1);
		int up = (stone2 + blink2);

		if (p.y - down == 1 || p.y + up == boardSize) {
			return 0;
		} else if (board[p.x][p.y - down - 1] == -1 * col || board[p.x][p.y + up + 1] == -1 * col) {
			return 0;
		} else {
			return 1;
		}
	}

	//check for 3-3 form, diagonal
	private int find3_4(Point p, int col) {
		int stone1 = 0;
		int stone2 = 0;
		int allStone = 0;
		int blink1 = 1;

		// ↖
		int xx = p.x - 1;
		int yy = p.y + 1;
		boolean check = false;
		leftUp: while (true) {
			if (xx == 0 || yy == boardSize + 1)
				break leftUp;

			if (board[xx][yy] == col) {
				check = false;
				stone1++;
			}

			if (board[xx][yy] == -1 * col)
				break leftUp;

			if (board[xx][yy] == 0) {
				if (check == false) {
					check = true;
				} else {
					blink1++;
					break leftUp;
				}

				if (blink1 == 1) {
					blink1--;
				} else {
					break leftUp;
				}
			}
			xx--;
			yy++;
		}

		// ↘
		int blink2 = blink1;
		if (blink1 == 1)
			blink1 = 0;
		xx = p.x + 1;
		yy = p.y - 1;
		check = false;
		rightDown: while (true) {
			if (xx == boardSize + 1 || yy == 0)
				break rightDown;

			if (board[xx][yy] == col) {
				check = false;
				stone2++;
			}

			if (board[xx][yy] == -1 * col)
				break rightDown;

			if (board[xx][yy] == 0) {
				if (check == false) {
					check = true;
				} else {
					blink2++;
					break rightDown;
				}

				if (blink2 == 1) {
					blink2--;
				} else {
					break rightDown;
				}
			}
			xx++;
			yy--;
		}

		allStone = stone1 + stone2;
		if (allStone != 2) {

			return 0;
		}

		int leftUp = (stone1 + blink1);
		int rightDown = (stone2 + blink2);

		if (p.x - leftUp == 1 || p.y - rightDown == 1 || p.y + leftUp == boardSize || p.x + rightDown == boardSize) {
			return 0;
		} else if (board[p.x - leftUp - 1][p.y + leftUp + 1] == -1 * col
				|| board[p.x + rightDown + 1][p.y - rightDown - 1] == -1 * col) {
			return 0;
		} else {
			return 1;
		}

	}

	//check for 6 or more stones in a row, horizontal
	private int fourORjang1(Point p, int col, int trigger) {
		int stone1 = 0;
		int stone2 = 0;
		int allStone = 0;
		// 열린4인지는 상관은없음. 다만 코드상 빈공간만을 의미.
		int blink1 = 1;

		// ← 탐색
		int yy = p.y;
		int xx = p.x - 1;
		boolean check = false;
		left: while (true) {
			if (xx == 0)
				break left;

			if (board[xx][yy] == col) {
				check = false;
				stone1++;
			}

			if (board[xx][yy] == -1 * col)
				break left;

			if (board[xx][yy] == 0) {
				// 두번연속으로 빈공간만날시 blink카운트를 되돌림.
				if (check == false) {
					check = true;
				} else {
					blink1++;
					break left;
				}

				if (blink1 == 1) {
					blink1--;
				} else {
					break left; // 빈공간을만났으나 빈공간을 두번만나면 끝임
				}

			}

			xx--;
		}

		// → 탐색
		xx = p.x + 1;
		yy = p.y;
		int blink2 = blink1;
		check = false;
		right: while (true) {
			if (xx == boardSize + 1)
				break right;

			if (board[xx][yy] == col) {
				check = false;
				stone2++;
			}

			if (board[xx][yy] == -1 * col)
				break right;

			if (board[xx][yy] == 0) {
				if (check == false) {
					check = true;
				} else {
					blink2++;
					break right;
				}

				if (blink2 == 1) {
					blink2--;
				} else {
					break right;
				}

			}
			xx++;
		}

		allStone = stone1 + stone2;

		// 사사찾는 트리거
		if (trigger == 1) {
			if (allStone != 3)
				return 0; // 놓은돌제외 3개아니면 4가아니니까.
			else
				return 1; // 놓은돌제외 3개면 4임. 닫히고 열린지는 상관없음.
		}

		// 장목찾는 트리거
		if (trigger == 2) {
			// 현재놓은돌 +1 +5 => 6목이상은 장목. 여기서 놓은돌기준 두방향모두 돌이있어야 장목
			if (allStone >= 5 && stone1 != 0 && stone2 != 0)
				return 1;
			else
				return 0;
		}
		// 그럴일을없지만 1 도 2도아니면 0리턴
		return 0;
	}
	
	//check for 6 or more stones in a row, diagonal
	private int fourORjang2(Point p, int col, int trigger) {

		int stone1 = 0;
		int stone2 = 0;
		int allStone = 0;
		int blink1 = 1;

		// ↙ 탐색
		int yy = p.y - 1;
		int xx = p.x - 1;
		boolean check = false;
		leftDown: while (true) {
			if (xx == 0 || yy == 0)
				break leftDown;

			if (board[xx][yy] == col) {
				check = false;
				stone1++;
			}

			if (board[xx][yy] == -1 * col)
				break leftDown;

			if (board[xx][yy] == 0) {
				if (check == false) {
					check = true;
				} else {
					blink1++;
					break leftDown;
				}

				if (blink1 == 1) {
					blink1--;
				} else {
					break leftDown;
				}

			}

			xx--;
			yy--;
		}

		// ↗ 탐색
		yy = p.y + 1;
		xx = p.x + 1;
		check = false;
		int blink2 = blink1;
		rightUp: while (true) {
			if (xx == boardSize + 1 || yy == boardSize + 1)
				break rightUp;

			if (board[xx][yy] == col) {
				check = false;
				stone2++;
			}

			if (board[xx][yy] == -1 * col)
				break rightUp;

			if (board[xx][yy] == 0) {
				if (check == false) {
					check = true;
				} else {
					blink2++;
					break rightUp;
				}

				if (blink2 == 1) {
					blink2--;
				} else {
					break rightUp;
				}

			}
			xx++;
			yy++;
		}

		allStone = stone1 + stone2;

		if (trigger == 1) {
			if (allStone != 3)
				return 0;
			else
				return 1;
		}

		if (trigger == 2) {
			if (allStone >= 5 && stone1 != 0 && stone2 != 0)
				return 1;
			else
				return 0;
		}
		return 0;
	}

	//check for 6 or more stones in a row, vertical
	private int fourORjang3(Point p, int col, int trigger) {
		int stone1 = 0;
		int stone2 = 0;
		int allStone = 0;
		int blink1 = 1;

		// ↓ 탐색
		int yy = p.y - 1;
		int xx = p.x;
		boolean check = false;
		down: while (true) {
			if (yy == 0)
				break down;

			if (board[xx][yy] == col) {
				check = false;
				stone1++;
			}

			if (board[xx][yy] == -1 * col)
				break down;

			if (board[xx][yy] == 0) {
				if (check == false) {
					check = true;
				} else {
					blink1++;
					break down;
				}

				if (blink1 == 1) {
					blink1--;
				} else {
					break down;
				}

			}

			yy--;
		}

		// ↑ 탐색
		yy = p.y + 1;
		xx = p.x;
		check = false;
		int blink2 = blink1;
		up: while (true) {
			if (yy == boardSize + 1)
				break up;

			if (board[xx][yy] == col) {
				check = false;
				stone2++;
			}

			if (board[xx][yy] == -1 * col)
				break up;

			if (board[xx][yy] == 0) {
				if (check == false) {
					check = true;
				} else {
					blink2++;
					break up;
				}

				if (blink2 == 1) {
					blink2--;
				} else {
					break up;
				}
			}
			yy++;
		}

		allStone = stone1 + stone2;

		if (trigger == 1) {
			if (allStone != 3)
				return 0;
			else
				return 1;
		}

		if (trigger == 2) {
			if (allStone >= 5 && stone1 != 0 && stone2 != 0)
				return 1;
			else
				return 0;
		}
		return 0;
	}

	//check for 6 or more stones in a row, diagonal
	private int fourORjang4(Point p, int col, int trigger) {

		int stone1 = 0;
		int stone2 = 0;
		int allStone = 0;
		int blink1 = 1;

		// ↘ 탐색
		int yy = p.y - 1;
		int xx = p.x + 1;
		boolean check = false;
		rightDown: while (true) {
			if (xx == boardSize + 1 || yy == 0)
				break rightDown;

			if (board[xx][yy] == col) {
				check = false;
				stone1++;
			}

			if (board[xx][yy] == -1 * col)
				break rightDown;

			if (board[xx][yy] == 0) {
				if (check == false) {
					check = true;
				} else {
					blink1++;
					break rightDown;
				}

				if (blink1 == 1) {
					blink1--;
				} else {
					break rightDown;
				}

			}

			xx++;
			yy--;
		}

		// ↖ 탐색
		yy = p.y - 1;
		xx = p.x - 1;
		check = false;
		int blink2 = blink1;
		leftUp: while (true) {
			if (xx == 0 || yy == boardSize + 1)
				break leftUp;

			if (board[xx][yy] == col) {
				check = false;
				stone2++;
			}

			if (board[xx][yy] == -1 * col)
				break leftUp;

			if (board[xx][yy] == 0) {
				if (check == false) {
					check = true;
				} else {
					blink2++;
					break leftUp;
				}

				if (blink2 == 1) {
					blink2--;
				} else {
					break leftUp;
				}

			}

			xx--;
			yy++;
		}

		allStone = stone1 + stone2;

		if (trigger == 1) {
			if (allStone != 3)
				return 0;
			else
				return 1;
		}

		if (trigger == 2) {
			if (allStone >= 5 && stone1 != 0 && stone2 != 0)
				return 1;
			else
				return 0;
		}

		return 0;
	}
	
	//count stones
	//dirX represents horizontal direction, dirY represents vertical direction
	//ex) if dirX is -1 and dirY is 0, count same stones from Point p
	private int count(Point p, int dirX, int dirY, int color) {
		int stones = 0;	
		for (; board[p.x + (stones + 1) * dirX][p.y + (stones + 1) * dirY] == color; stones++)
			;
		return stones;
	}

}


//GomokuClient class for creating client GUI window and
//implementing server-client connection features
public class GomokuClient extends Frame implements Runnable, ActionListener {
	private TextArea messageWindow = new TextArea("", 1, 1, 1);
	private TextArea ruleWindow = new TextArea(
			"Rules:\nThis game uses Free Renju rule.\n1. Black is permitted from using 3-3 or 4-4.\n2. Black is permitted from putting 6 or more stones.\n3. Players have 30 seconds each turn.\n");
	private Button startButton = new Button("Start Game");
	private Button resignButton = new Button("Resign Game");
	private DrawBoard board = new DrawBoard(15, 30);
	private BufferedReader fromServer;
	private PrintWriter toServer;
	private Socket clientSocket;

	public Panel timePanel = new Panel();
	
	// client constructor
	public GomokuClient(String title) {
		super(title);
		setLayout(null);

		ruleWindow.setEditable(false);
		messageWindow.setEditable(false);
		board.setLocation(10, 70);
		add(board);
		
		timePanel.setBackground(new Color(255, 229, 204));
		timePanel.setLayout(new BorderLayout());
		timePanel.setBounds(500, 40, 350, 70);
		Label timeLabel = new Label();
		timePanel.add(timeLabel);
		timePanel.setVisible(true);
		Timer timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {
			int i = 20;

			public void run() {
				if (DrawBoard.timerStart == true) {

					timeLabel.setText("Time left: " + i);
					i--;
					
					if(DrawBoard.timerEnd == true) {
						timer.cancel();
					}
					
					if (i < 0) {
						timer.cancel();
						timeLabel.setText("Time Over");
						new Timer();
					}
				}
				
			}
		}, 0, 1000);

		Panel rulePanel = new Panel();
		rulePanel.setBackground(new Color(255,255,100));
		rulePanel.setLayout(new BorderLayout());
		rulePanel.add(ruleWindow,"Center");

		Panel rulePanelButton = new Panel();
		rulePanelButton.add(startButton);
		rulePanelButton.add(resignButton);
		rulePanel.add(rulePanelButton,"South");
		startButton.setEnabled(false);
		resignButton.setEnabled(false);
		rulePanel.setBounds(500,110,350,180);

		Panel messagePanel = new Panel();
		messagePanel.setLayout(new BorderLayout());
		messagePanel.add(messageWindow,"Center");
		messagePanel.setBounds(500,300,350,250);
		
		add(timePanel);
		add(rulePanel);
		add(messagePanel);
		
		startButton.addActionListener(this);
		resignButton.addActionListener(this);
		
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent close) {
				System.exit(0);
			}
		});
	}


	// event listener for buttons
	public void actionPerformed(ActionEvent buttonClick) {
		if (buttonClick.getSource() == startButton) {
			try {
				toServer.println("[START]");
				startButton.setEnabled(false);
			}
			catch (Exception e) {
				
			}
		} else if (buttonClick.getSource() == resignButton) {
			try {
				toServer.println("[DROPGAME]");
				JOptionPane.showMessageDialog(null, "You resigned the game\nYou Lose");
				endGame("You resigned.");
			}catch (Exception e) {
			}
		}
		
		
	}

	// end game
	private void endGame(String s) {
		messageWindow.append(s + "\n");
		startButton.setEnabled(true);
		resignButton.setEnabled(false);
		if (board.isRunning() == true) {
			board.stopGame();
		}
	}

	// connect to server and initiate new thread
	private void connect() {
		try {
			messageWindow.append("Connecting to server\n");
			clientSocket = new Socket("127.0.0.1", 6789);
			messageWindow.append("Connection complete\n");
			messageWindow.append("Click Start Game to start\n");
			//toServer.writeBytes("[ROOM]" + 1);
			
			fromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			toServer = new PrintWriter(clientSocket.getOutputStream(), true);
			
			startButton.setEnabled(true);
			
			new Thread(this).start();
			board.setWriter(toServer);
		} catch (Exception e) {
			messageWindow.append("Unable to connect to server\n");
		}
	}

	// run GomokuClient
	public void run() {
		String messageFromServer;
		
		try {
			while((messageFromServer = fromServer.readLine()) != null) {
				if(messageFromServer.startsWith("[STONE]")) {
					String temp = messageFromServer.substring(7);
					int x = Integer.parseInt(temp.substring(0, temp.indexOf(" ")));
					int y = Integer.parseInt(temp.substring(temp.indexOf(" ") + 1));
					board.placeOpponentStone(x, y);
					board.setPlayerTurn(true);
				}
				else if(messageFromServer.startsWith("[DISCONNECT]")) {
					JOptionPane.showMessageDialog(null, "Opponent player left the game");
					endGame("Opponent player left the game");
				}
				else if(messageFromServer.startsWith("[COLOR]")) {
					String color = messageFromServer.substring(7);
					board.startGame(color);
					if(color.equals("BLACK")) {
						messageWindow.append("You got black stone\n");
					}
					else {
						messageWindow.append("You got white stone\n");
					}
					resignButton.setEnabled(true);
				}
				else if(messageFromServer.startsWith("[DROPGAME]")) {
					JOptionPane.showMessageDialog(null, "Opponent resigned the game\nYou win");
					endGame("Opponent resigned the game\nYou win");
				}
				else if(messageFromServer.startsWith("[WIN]")) {
					JOptionPane.showMessageDialog(null, "You Win");
					endGame("You win");
				}
				else if(messageFromServer.startsWith("[LOSE]")) {
					JOptionPane.showMessageDialog(null, "You Lose");
					endGame("You lose");
				}
				else {
					messageWindow.append(messageFromServer + "\n");
				}
			}
		}
		catch(IOException e) {
			messageWindow.append(e + "\n");
		}
		messageWindow.append("lost connection\n");
	}



	// main method
	public static void main(String[] args) {
		GomokuClient client = new GomokuClient("네트워크 오목 게임");
		client.setSize(860, 560);
		client.setVisible(true);
		client.connect();
	}

}