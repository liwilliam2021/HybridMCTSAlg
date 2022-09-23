package com.tictactoe;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;

import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JRadioButton;

/**
 * TicTacToe Application class.
 *
 */
public class GomokuApp implements ActionListener {
	
	private JRadioButton p2pRadioBtn;
	private JRadioButton p2cRadioBtn;		
	private JLabel picturePerson;
	private JLabel pictureComputer;
	private JLabel labelPersonWin;
	private JLabel labelComputerWin;
	private JLabel labelDraw;
	private JButton startBtn;
	private JButton endBtn;
	private JButton undoBtn;
	private ImageIcon emptyIcon;
	private ImageIcon PersonIcon;
	private ImageIcon ComputerIcon;

	public final int n = 9; // JUNYI
	
	//nxn grid buttons, each button is a tile
	//use nxn Two-Dimensional Array
	private JButton[][] gridBtn = new JButton[n][n];
	
	private final int GRID_SIDE_LENTH = 300/n; // JUNYI: length of each grid

	private int previousIndex = -1;	
	private boolean mSelectP2P = true;	

	public enum TILE_STATUS {
		TILE_STATUS_EMPTY, TILE_STATUS_PERSON, TILE_STATUS_COMPUTER
	}
		
	//define the nxn grid tile status
	//use nxn One-Dimensional Array
	public TILE_STATUS[] currentTable = new TILE_STATUS[n*n];

	public enum TURN {
		GAME_RESET, PERSON_TURN, COMPUTER_TURN, GAME_DRAW
	}
	public TURN currentTurn = TURN.PERSON_TURN; 
	
	private GameAlgorithm algorithm = new GameAlgorithm(currentTable);
	private Thread computerThread;
	
	private int move;
	
	/**
	 * TicTacToe Application main.
	 *
	 */	
	public static void main(String[] args) {
		new GomokuApp();
	}

	/**
	 * TicTacToe Application class constructor.
	 *
	 */		
	public GomokuApp() {
		
		JFrame guiFrame = new JFrame(); // make sure the program exits when the frame closes
		
		guiFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		guiFrame.setBackground(Color.white);
		guiFrame.setTitle("TicTacToe");
		guiFrame.setSize(600, 400); // JUNYI: frame size

		// This will center the JFrame in the middle of the screen
		guiFrame.setLocationRelativeTo(null);

		// Set up the content pane.
		addComponentsToPane(guiFrame.getContentPane());

		guiFrame.setVisible(true);
		
		for(int i = 0; i < n*n; ++i){
			currentTable[i] = TILE_STATUS.TILE_STATUS_EMPTY;
		}
	}

	
	/**
	 * Set up the content pane by adding components
	 *
	 * @param pane the app's root view pane
	 */		
	public void addComponentsToPane(Container pane) {
		
		pane.setLayout(null);
		pane.setBackground(Color.white);

		createLeftPane(pane);
		createButtons(pane);
		createGrid(pane);
		
		//set pane layout and GUI component size.
		setComponentSize(pane);
	}
	
	/**
	 * create image icons.
	 *
	 * @param path to image file
	 */	
	protected ImageIcon createImageIcon(String path) {
		URL imgURL;
		imgURL = GomokuApp.class.getResource(path);
		if (imgURL == null) {
			System.err.println("Couldn't find file: " + path);
			return null;
		}
		return new ImageIcon(imgURL);
	}

	/**
	 * create left pane.
	 *
	 * @param pane the app's root view pane
	 */		
	private void createLeftPane(Container pane) {
		
		p2pRadioBtn = new JRadioButton("Person A vs Person B", true);
		p2cRadioBtn = new JRadioButton("Person A vs Computer B", false);
		p2pRadioBtn.setBackground(Color.white);
		p2cRadioBtn.setBackground(Color.white);
		p2pRadioBtn.addActionListener(this);
		p2cRadioBtn.addActionListener(this);

		ButtonGroup groupRadio = new ButtonGroup();
		groupRadio.add(p2pRadioBtn);
		groupRadio.add(p2cRadioBtn);

		picturePerson = new JLabel(
				createImageIcon("images/black.gif"));
		picturePerson.setEnabled(true);
		pictureComputer = new JLabel(
				createImageIcon("images/green.gif"));
		pictureComputer.setEnabled(false);

		labelPersonWin = new JLabel("A Wins!!");
		labelPersonWin.setVisible(false);
		labelComputerWin = new JLabel("B Wins!!");
		labelComputerWin.setVisible(false);
		labelDraw = new JLabel("Draw!!");
		labelDraw.setVisible(false);

		
		pane.add(picturePerson);
		pane.add(pictureComputer);
		pane.add(p2pRadioBtn);
		pane.add(p2cRadioBtn);
		pane.add(labelComputerWin);
		pane.add(labelPersonWin);
		pane.add(labelDraw);
		
	}

	/**
	 * create buttons.
	 *
	 * @param pane the app's root view pane
	 */			
	private void createButtons(Container pane) {
		
		startBtn = new JButton("Start");
		startBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				reset();
			}
		});
		
		endBtn = new JButton("End");
		endBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				System.exit(0);
			}
		});
		
		undoBtn = new JButton("Undo");
		if (previousIndex == -1) {
			undoBtn.setEnabled(false);
		}
		undoBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				if (previousIndex != -1) {
					currentTable[previousIndex] = TILE_STATUS.TILE_STATUS_EMPTY;
					getBtn(previousIndex).setIcon(emptyIcon);
					previousIndex = -1;
					undoBtn.setEnabled(false);
				}
				if (currentTurn == TURN.PERSON_TURN) {
					currentTurn = TURN.COMPUTER_TURN;
				} else {
					currentTurn = TURN.PERSON_TURN;
				}
				flipTurnPicture(currentTurn);
			}
		});
		
		pane.add(startBtn);
		pane.add(endBtn);
		pane.add(undoBtn);		
	}

	/**
	 * create nxn grid buttons.
	 *
	 * @param pane the app's root view pane
	 */			
	private void createGrid(Container pane) {
		
		emptyIcon = createImageIcon("images/empty.gif");
		PersonIcon = createImageIcon("images/black.gif");
		ComputerIcon = createImageIcon("images/green.gif");

		for(int i=0; i<n; i++)
		{
			for (int j=0; j<n; j++)
			{
				gridBtn[i][j] = new JButton(emptyIcon);
				gridBtn[i][j].addActionListener(this);
				gridBtn[i][j].setActionCommand(String.valueOf(i*n+j));
				pane.add(gridBtn[i][j]);
			}
		}
	}

	/**
	 * set pane layout and GUI component size.
	 *
	 * @param pane the app's root view pane
	 */				
	private void setComponentSize(Container pane) {

		Insets insets = pane.getInsets();
		
		Dimension size;

		size= p2pRadioBtn.getPreferredSize();
		p2pRadioBtn.setBounds(30 + insets.left, 20 + insets.top, size.width,
				size.height);
		
		size = p2cRadioBtn.getPreferredSize();
		p2cRadioBtn.setBounds(30 + insets.left, 50 + insets.top, size.width,
				size.height);

		size = startBtn.getPreferredSize();
		startBtn.setBounds(20 + insets.left, 230 + insets.top, size.width,
				size.height);

		size = endBtn.getPreferredSize();
		endBtn.setBounds(90 + insets.left, 230 + insets.top, size.width,
				size.height);

		size = undoBtn.getPreferredSize();
		undoBtn.setBounds(155 + insets.left, 230 + insets.top, size.width,
				size.height);

		picturePerson.setBounds(50 + insets.left, 120 + insets.top, 50, 50);
		pictureComputer.setBounds(100 + insets.left, 120 + insets.top, 50, 50);

		labelPersonWin.setBounds(70 + insets.left, 120 + insets.top, 100, 50);
		labelComputerWin.setBounds(70 + insets.left, 120 + insets.top, 100, 50);
		labelDraw.setBounds(70 + insets.left, 120 + insets.top, 100, 50);
		
		for(int i=0; i<n; i++)
		{
			for (int j=0; j<n; j++)
			{
				gridBtn[i][j].setBounds(250 + GRID_SIDE_LENTH*j + insets.left, 20 + GRID_SIDE_LENTH*i + insets.top,
						GRID_SIDE_LENTH, GRID_SIDE_LENTH);
			}
		}

	}


	@Override
	/**
	 * handle click event
	 *
	 * @param event the click event
	 */			
	public void actionPerformed(ActionEvent event) {
		
		//get event command name
		String action = event.getActionCommand();
		
		//game selection mode is clicked
		if(action.contains("vs P")) {
			mSelectP2P = true;
		} else if (action.contains("vs C")) {
			mSelectP2P = false;
		} else {
			
			//handle tile click
			procTileClick(action);
		}
	}

	/**
	 * process tile action after a tile is clicked, update tile image and check who win
	 *
	 * @param action: the tile button event command name
	 */			
	private void procTileClick(String action) {
		
		//find the tile index from tile button command name
		int select = Integer.parseInt(action);
		
		move = select;
		
		//process tile command, update tile image
		if (select >= 0 && select < n*n) {
			updateTable(select);
		}

		//check if there is a winner
		if(algorithm.checkWinner(currentTable, select) == true)
			showWinner(currentTurn);
		
		//check if draw (full without winner)
		else if(noEmptyTile())
			showWinner(TURN.GAME_DRAW);
	}
	
	/**
	 * check if all tiles are used 
	 *
	 * @param none
	 */				
	boolean noEmptyTile() {
		//check if computer could win		
		for (int index = 0; index < currentTable.length; index++) {
			
			if (currentTable[index] == GomokuApp.TILE_STATUS.TILE_STATUS_EMPTY) {
				return false;
			}		
		}
		
		return true;
	}

	/**
	 * process tile click and update tile image 
	 *
	 * @param the tile number to be updated
	 */				
	private void updateTable(int number) {

		//person vs person
		if (currentTurn == TURN.PERSON_TURN
				&& currentTable[number] == TILE_STATUS.TILE_STATUS_EMPTY) {
			currentTurn = TURN.COMPUTER_TURN;
			previousIndex = number;
			currentTable[number] = TILE_STATUS.TILE_STATUS_PERSON;
			getBtn(number).setIcon(PersonIcon);
			if (mSelectP2P == true) {
				undoBtn.setEnabled(true);
			}
		} else if (currentTurn == TURN.COMPUTER_TURN
				&& currentTable[number] == TILE_STATUS.TILE_STATUS_EMPTY) {
			currentTurn = TURN.PERSON_TURN;
			previousIndex = number;
			currentTable[number] = TILE_STATUS.TILE_STATUS_COMPUTER;
			getBtn(number).setIcon(ComputerIcon);
			if (mSelectP2P == true) {
				undoBtn.setEnabled(true);
			}
		}
		
		
		flipTurnPicture(currentTurn);

		//person vs computer
		if (mSelectP2P == false && currentTurn == TURN.COMPUTER_TURN) {
			if (computerThread != null && computerThread.isAlive()) {
				computerThread.interrupt();
				try {
					Thread.sleep(1);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			if (algorithm.checkWinner(currentTable, number) == false && noEmptyTile() == false) {
				computerThread = new Thread(new ComputerRunnable());
				computerThread.start();
			}
		}
	}

	/**
	 * find the nxn grid tile button from tile index
	 *
	 * @param index of the tile button
	 */						
	private JButton getBtn(int index) {
		
		if(index < 0 || index >= n*n)
			return null;
			
		return gridBtn[index/n][index%n];
	}
	
	/**
	 * switch the picture for the next gamer
	 *
	 * @param turn: who will place the next move
	 */					
	private void flipTurnPicture(TURN currentTurn2) {
		if (currentTurn2 == TURN.PERSON_TURN) {
			picturePerson.setEnabled(true);
			pictureComputer.setEnabled(false);
		} else {
			picturePerson.setEnabled(false);
			pictureComputer.setEnabled(true);
		}
	}

	
	/**
	 * show who is the winner
	 *
	 * @param currentTurn2  the winner
	 */							
	private void showWinner(TURN currentTurn2) {
		if (currentTurn2 == TURN.PERSON_TURN) {
			labelComputerWin.setVisible(true);
			labelPersonWin.setVisible(false);
			labelDraw.setVisible(false);
			pictureComputer.setVisible(false);
			picturePerson.setVisible(false);
			disableAllGrid();
		} else if (currentTurn2 == TURN.COMPUTER_TURN) {
			labelPersonWin.setVisible(true);
			labelDraw.setVisible(false);
			labelComputerWin.setVisible(false);
			pictureComputer.setVisible(false);
			picturePerson.setVisible(false);
			disableAllGrid();
		} else if (currentTurn2 == TURN.GAME_DRAW) {
			labelPersonWin.setVisible(false);
			labelDraw.setVisible(true);
			labelComputerWin.setVisible(false);
			pictureComputer.setVisible(false);
			picturePerson.setVisible(false);
			disableAllGrid();
		} else {
			labelComputerWin.setVisible(false);
			labelDraw.setVisible(false);
			labelPersonWin.setVisible(false);
			pictureComputer.setVisible(true);
			picturePerson.setVisible(true);
		}

	}



	/**
	 * disable all grid buttons after a winner is declared
	 *
	 * @param none
	 */											
	private void disableAllGrid() {
		for (int index = 0; index < currentTable.length; index++) {
			getBtn(index).setEnabled(false);
		}
	}


	/**
	 * reset game
	 *
	 * @param none
	 */											
	private void reset() {
		for (int index = 0; index < currentTable.length; index++) {
			currentTable[index] = TILE_STATUS.TILE_STATUS_EMPTY;
			getBtn(index).setIcon(emptyIcon);
			getBtn(index).setEnabled(true);
		}
		showWinner(TURN.GAME_RESET);
		currentTurn = TURN.PERSON_TURN;
		previousIndex = -1;
		flipTurnPicture(currentTurn);
	}


	/**
	 * person vs computer, computer makes tile selection after 500 milliseconds
	 *
	 * 
	 */												
	private class ComputerRunnable implements Runnable {
		@Override
		public void run() {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			JButton btn = computerSelect();
			if (btn != null) {
				btn.doClick();
			}
		}

	}

	
	/**
	 * computer selection, select 
	 * next tile
	 * 
	 * @param none
	 */												
	private JButton computerSelect() {
		
		int btnIndex = algorithm.choseTile(move);
		if (btnIndex != -1) {
			return getBtn(btnIndex);
		}
		return null;
	}
	
}
