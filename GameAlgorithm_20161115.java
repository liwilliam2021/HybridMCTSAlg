package com.tictactoe;

import com.tictactoe.GomokuApp.TILE_STATUS;
import com.tictactoe.GomokuApp.TURN;

import java.util.ArrayList;
import java.util.Random;

/**
 * TicTacToe Computer Game Algorithm class.
 *
 */
public class GameAlgorithm {
	private TILE_STATUS[] currentTable;
	private final Random r;
	private final int n;
	private int[] minimax4Moves;
	private int minimax4K;
		
	public enum SEARCHMODE2STATUS {
		NOTMET, MET, NODEATHORALLDEATH, SOMEDEATH, WIN
	}

	public SEARCHMODE2STATUS searchMode2Status;
	
	public final int COUNTWIN = 5; // number of straight row, column or diagonal to become the winner. COUNTWIN is used in checkWinnder()

	public final int GAMESTATELENGTH; 
	
	public int NODE_SELECTION_MODE = 1;
	// only meaningful if SEARCH_MODE = 1 or 2
	// 0 means random node selection
	// 1 means best UCT node selection

	public int SEARCH_MODE = 2;
	// 0 means minimax 
	// 1 means MCTS
	// 2 means combined minimax and MCTS; NUMSIMULATIONDEPTH only meaningful in this mode

	public final int NUMROLLOUTSIMULATION = 30000; 
	
	public final int NUMTREELEVEL_MCTS; 
	public final int NUMTREELEVEL_MINIMAX; 
	// GAMESTATELENGTH means keeping on expanding as if level is infinity, 
	// because the max number of level is strictly less than GAMESTATELENGTH  
	// 0 means not expand at all, therefore meaningless; 
	// 1 means only one level down from the root node, which is the minimum value to be taken
	// for now: NUMTREELEVEL_MINIMAX = GAMESTATELENGTH always
	// NUMTREELEVEL_MCTS = GAMESTATELENGTH is the maximum level of expansion.
	// NUMTREELEVEL_MCTS = 2 is the smallest expansion level. Usually MCTS cannot detect immediate traps at this level.

	public static final int NUMSIMULATIONDEPTH = 4; 
	// GAMESTATELENGTH means always running minimax after a roll out simulation, 
	// because the max number of simulation depth is strictly less than GAMESTATELENGTH
	// in general, NUMSIMULATIONDEPTH means that if a roll out simulation hits a terminal state <= NUMSIMULATIONDEPTH steps 
	// of simulation, then run minimax under the leaf node from which the roll out simulation starts
	// 0 means never running minimax after a roll out simulation
	
	public GameAlgorithm(TILE_STATUS[] curTable) {
		currentTable = curTable;
	    r = new Random(1); // random number seed
	    GAMESTATELENGTH = currentTable.length;
	    n = (int) Math.sqrt(GAMESTATELENGTH);
	    minimax4Moves = new int[GAMESTATELENGTH];
	    NUMTREELEVEL_MCTS = GAMESTATELENGTH;
	    NUMTREELEVEL_MINIMAX = GAMESTATELENGTH;
	}

	

	public class MCTSNode
	{
		private ArrayList<MCTSNode> nextMoves;
		private TILE_STATUS[] nodeGameState;
		private TURN nodeTurn;
		private int score;
		private int timesVisited;
		private boolean expanded;
		private int indexFromParentNode;
		private int levelFromTopNode;
		private int simulationDepth;

		public MCTSNode(TILE_STATUS[] state, TURN myTurn, int indexMove, int levelCount)
		{
			nodeGameState = new TILE_STATUS[GAMESTATELENGTH];
			for (int index = 0; index < GAMESTATELENGTH; index++) 
				nodeGameState[index] = state[index];
		   	nodeTurn = myTurn; // nodeTurn is the TURN to make a move given the state = nodeGameState. nodeGameState does not include that move
		   	timesVisited = 0;
		   	score = 0;
		   	nextMoves = null;
		   	expanded = false;
		   	indexFromParentNode = indexMove; // the present node is reached from its parent node by taking a move at indexFromParentNode
		   	levelFromTopNode = levelCount; // the present node is level levelFromTopNode from the root node
		   	// simulationDepth is only used when SEARCH_MODE = 2; 
		   	simulationDepth = 0; // initially the present node has not started roll out simulation yet
		}
		
		public void printGameState(TILE_STATUS[] state) {
			for (int i = 0; i < n; i++) {
				for (int j = 0; j < n; j++) {
					int index = i*n+j;
					if (state[index] == TILE_STATUS.TILE_STATUS_EMPTY)
						System.out.format( "-1 ");
					else if (state[index] == TILE_STATUS.TILE_STATUS_PERSON)
						System.out.format( " 0 ");
					else 
						System.out.format( " 1 ");
				}
				System.out.println("");
			}
			System.out.println("");
		}

		public void printAllNodes() {
			
			for (int index = 0; index < GAMESTATELENGTH; index++) {
				if (nodeGameState[index] == TILE_STATUS.TILE_STATUS_EMPTY)
					System.out.format( "-1 ");
				else if (nodeGameState[index] == TILE_STATUS.TILE_STATUS_PERSON)
					System.out.format( " 0 ");
				else 
					System.out.format( " 1 ");
			}
			System.out.println(" turn: " + nodeTurn + " time: " + timesVisited + " score: " + score);
			if (!isLeaf()) {
				for (int i = 0; i < nextMoves.size(); i++) 
					nextMoves.get(i).printAllNodes();
			}
		}

		public void printAnNode(TILE_STATUS[] state, int mode, int num_sim, int index) {
			
			boolean matched = true;
			
			for (int i = 0; i < GAMESTATELENGTH; i++) {
				if (nodeGameState[i] != state[i]) {
					matched = false;
					break;
				}
			} // find the node whose game state exactly matches the given state
			if (matched == true) {
				if (timesVisited>0)
//					System.out.println(" turn: " + nodeTurn + " time: " + timesVisited + " score: " + score + " float score = " +((float) score) / ((float) timesVisited));
					System.out.println("v"+ index+"(" + num_sim + ")=" + timesVisited + "; s"+index+"(" + num_sim + ")=" +((float) score) / ((float) timesVisited) +";");
				if ((mode == 1) && (!isLeaf())) { // print all its child nodes if any, if mode == 1
					for (int i = 0; i < nextMoves.size(); i++) {
						MCTSNode node = nextMoves.get(i);
						System.out.println("v(" +(i+1)+ ")="+ node.timesVisited + "; s(" +(i+1)+")=" + node.score +";");
					}
						
				}
			}
			
			if (!isLeaf()) {
				for (int i = 0; i < nextMoves.size(); i++) 
					nextMoves.get(i).printAnNode(state, mode, num_sim, index);
			}
		}

		public TILE_STATUS[] getGameState() {
			return nodeGameState;
		}
		
		public int getSimulationDepth() {
			return simulationDepth;
		}

		public void resetSimulationDepth() {
			simulationDepth = 0;
		}

		public boolean isLeaf()
		{
			return (nextMoves == null || nextMoves.isEmpty());
		}

		public void incScore() {
			score++;
		}
		   
		public void decScore()
		{
		    score--;
		}   
		   
		public void visit()
		{
			timesVisited++;
		}

		public int getTimesVisited()
		{
			return timesVisited;
		}

		public int getScore()
		{
			return score;
		}
		
		public int getindexFromParentNode() 
		{
			return indexFromParentNode;
		}
		
		public TURN getTurn() {
			return nodeTurn;
		}

		public TURN getNextTurn(TURN turn) {
			if (turn == TURN.COMPUTER_TURN)	
				return TURN.PERSON_TURN;
			else if (turn == TURN.PERSON_TURN)
				return TURN.COMPUTER_TURN;
			else { 
				System.out.println("error in getNextTurn input");
				return TURN.GAME_RESET;
			}
		}

		public void expand()	{
			
			if (!expanded)	{
				expanded = true;
				nextMoves = new ArrayList<MCTSNode>();
								
				for (int indexFromParentNode = 0; indexFromParentNode < GAMESTATELENGTH; indexFromParentNode++) {
					if (nodeGameState[indexFromParentNode] == TILE_STATUS.TILE_STATUS_EMPTY) {
						
						if (nodeTurn == TURN.COMPUTER_TURN) 
							nodeGameState[indexFromParentNode] = TILE_STATUS.TILE_STATUS_COMPUTER;
						else 
							nodeGameState[indexFromParentNode] = TILE_STATUS.TILE_STATUS_PERSON;	
				
						nextMoves.add(new MCTSNode(nodeGameState,getNextTurn(nodeTurn),indexFromParentNode,levelFromTopNode+1));
						nodeGameState[indexFromParentNode] = TILE_STATUS.TILE_STATUS_EMPTY;
					}
				}
			}
		}

		public MCTSNode randomSelection()
		{
			int rand = r.nextInt(nextMoves.size());
		    return nextMoves.get(rand);
		}

		
		public MCTSNode bestUCTSelection()
		{
		    int maxIndex = 0;

		    int turn;
			if (getTurn() == TURN.COMPUTER_TURN) 
				turn = 1;
			else 
				turn = -1;
		      
			//the randomizer is a tiny random number added for tie-breaking
			float bias, randomizer, biasedScore = 0, nodeScore;
		    float max = -Float.MAX_VALUE * turn;
		    float C = 1;
		    for (int i = 0; i < nextMoves.size(); i++) {
		    	MCTSNode node = nextMoves.get(i);
		    	
        		if ((SEARCH_MODE == 2) && (searchMode2Status == SEARCHMODE2STATUS.SOMEDEATH) && (levelFromTopNode == 1)) {
        			// do not bother to select any node that is not in the list of minimax4Moves
        			boolean found = false;
        			int move = node.getindexFromParentNode();
        			for (int j=0; j<minimax4K; j++) {
        				if (move == minimax4Moves[j]) 
        					found = true;
        			}
        			if (found == false)
        				continue; 
        		}    		        			
		    	
		        nodeScore = (float) node.getScore() / ((float) (node.getTimesVisited() + Float.MIN_VALUE));
		        if (this.getTimesVisited() == 0)
		        	bias = 0;
		        else
		        	bias = 2 * C * (float) (Math.sqrt(Math.log((float) this.getTimesVisited()) / ((float) node.getTimesVisited() + Float.MIN_VALUE)));
		        randomizer = Float.MIN_VALUE * r.nextInt(GAMESTATELENGTH * GAMESTATELENGTH);
		        biasedScore = nodeScore + randomizer + (bias * turn);
		       		        
		        if (biasedScore * turn > max * turn) {
		        	max = biasedScore;
		            maxIndex = i;
		        }
		    }
		      		    
		    return nextMoves.get(maxIndex);
		}
    
		public TURN simulateFrom(TILE_STATUS[] state, TURN myTurn, int previousMove) 
		// simulation from state and it is myTurn to move 
		{
			if (checkWinner(state, previousMove)) { // if this state is terminal, then myTurn loses. So getNextTurn(myTurn) wins
				    			
				return(getNextTurn(myTurn)); 
			} 
			else {
				ArrayList<Integer> availableIndex = new ArrayList<Integer>();
				for (int index = 0; index < GAMESTATELENGTH; index++) {
					if (state[index] == TILE_STATUS.TILE_STATUS_EMPTY) 
						availableIndex.add(index);
				}
		      
				if (availableIndex.size() > 0) {
					
					simulationDepth++;
					
					int index = availableIndex.get(r.nextInt(availableIndex.size()));
			  
					if (myTurn == TURN.COMPUTER_TURN) 
						state[index] = TILE_STATUS.TILE_STATUS_COMPUTER;
					else 
						state[index] = TILE_STATUS.TILE_STATUS_PERSON;	
				  
					return(simulateFrom(state, getNextTurn(myTurn), index));
				}
				else {

					return(TURN.GAME_DRAW);
				}
			}
		}
 
		private TURN runTrial()
		{
			TURN rolloutResult = TURN.GAME_DRAW;
				
	        if (checkWinner(nodeGameState, indexFromParentNode)) 
	        	rolloutResult = getNextTurn(nodeTurn); 
	        else {
	        	if (levelFromTopNode < NUMTREELEVEL_MCTS)
	        		expand();

	        	if ((timesVisited == 0) || (isLeaf())) {		    	  
			      	        		
	        		TILE_STATUS[] s;
	        		s = new TILE_STATUS[GAMESTATELENGTH];
	        		for (int index = 0; index < GAMESTATELENGTH; index++) 
	        			s[index] = nodeGameState[index];
  					
	        		simulationDepth = levelFromTopNode; 
	        		// simulationDepth is counted from Top Node, not from the Leaf Node
	        		rolloutResult = simulateFrom(s, nodeTurn, indexFromParentNode);
  					
	        		if ( (simulationDepth <= NUMSIMULATIONDEPTH) && (SEARCH_MODE == 2) && (searchMode2Status == SEARCHMODE2STATUS.NOTMET) ){
	        			searchMode2Status = SEARCHMODE2STATUS.MET;
	        		}
	        	}		      
	        	else {	  			  				
	        		// here one can choose to use random node selection or best UCT node selection
	        		if (NODE_SELECTION_MODE == 0)
	        			rolloutResult = randomSelection().runTrial(); 
	        		else if (NODE_SELECTION_MODE == 1)
	        			rolloutResult = bestUCTSelection().runTrial();
	        	}
	        }
	        
		    visit();

		    if (rolloutResult != TURN.GAME_DRAW) {
		    	if (rolloutResult == TURN.COMPUTER_TURN) 
		    		incScore();
		    	else if (rolloutResult == TURN.PERSON_TURN)
		    		decScore();
		    }		  
	      
		    return rolloutResult;
		}
		
		public int minimax_level4(TILE_STATUS[] state) 
		// minimax search from the top node down 4 levels
		// minimaxMove() uses recursion and causes memory issues
		// minimax_level4() uses iteration and does not run into memory issues
		{
			int i1,i2,i3,i4;
			int minimaxValue1, minimaxValue2, minimaxValue3, minimaxValue4;
			int move = -1;
			int tmpValue = 1;			

			minimaxValue1 = -1; 
			// initially, assume LOSS for computer on level 1
			for (i1=0; i1<n*n; i1++) {
				if (state[i1] != TILE_STATUS.TILE_STATUS_EMPTY)
					continue;
				
				state[i1] = TILE_STATUS.TILE_STATUS_COMPUTER;
				if (checkWinner(state, i1)) { 
					// if a winning move is found on level 1 for computer, then no need to further search. 
					// 1 means computer win.
					// This is the move. return move and set the status to be WIN.
					minimaxValue1 = 1;
					move = i1;
					state[i1] = TILE_STATUS.TILE_STATUS_EMPTY;
					searchMode2Status = SEARCHMODE2STATUS.WIN;
					return move;
				}
				
				minimaxValue2 = 1;
				// initially, assume LOSS for person on level 2
				for (i2=0; i2<n*n; i2++) {
					if (state[i2] != TILE_STATUS.TILE_STATUS_EMPTY)
						continue;
					
					state[i2] = TILE_STATUS.TILE_STATUS_PERSON;
					if (checkWinner(state, i2)) {
						// if a winning move is found on level 2 for person, then no need to further search on level 2. 
						// -1 means computer loss.
						// Continue to check other level 1 moves. 
						minimaxValue2 = -1;
						state[i2] = TILE_STATUS.TILE_STATUS_EMPTY;
						break;
					}
					
					minimaxValue3 = -1;
					// initially, assume LOSS for computer on level 3
					for (i3=0; i3<n*n; i3++) {
						if (state[i3] != TILE_STATUS.TILE_STATUS_EMPTY)
							continue;
						
						state[i3] = TILE_STATUS.TILE_STATUS_COMPUTER;
						
						if (checkWinner(state, i3)) {
							// if a winning move is found on level 3 for computer, then no need to further search on level 3. 
							// 1 means computer win
							// Continue to check other level 2 moves. 
							minimaxValue3 = 1;
							state[i3] = TILE_STATUS.TILE_STATUS_EMPTY;
							break;
						}
						
						minimaxValue4 = 0;
						// since level 4 is the last level, the best hope for computer is tie.
						// initially, assume TIE for person on level 4
						for (i4=0; i4<n*n; i4++) {
							if (state[i4] != TILE_STATUS.TILE_STATUS_EMPTY)
								continue;
							
							state[i4] = TILE_STATUS.TILE_STATUS_PERSON;
							
							if (checkWinner(state, i4)) {
								// if a winning move is found on level 4 for person, then no need to further search on level 4. 
								// -1 means computer loss
								// Continue to check other level 3 moves. 
								minimaxValue4 = -1;
								state[i4] = TILE_STATUS.TILE_STATUS_EMPTY;
								break;
							}
							state[i4] = TILE_STATUS.TILE_STATUS_EMPTY;
						}
						
						state[i3] = TILE_STATUS.TILE_STATUS_EMPTY;
						
						if (minimaxValue3 < minimaxValue4)
							minimaxValue3 = minimaxValue4;
						// minimaxValue3 of a level 3 node records the max of minimaxValue4 of all level 4 nodes under that level 3 node
						// because the level 3 node is for computer to move and thus to choose max
					}
					
					state[i2] = TILE_STATUS.TILE_STATUS_EMPTY;

					if (minimaxValue2 > minimaxValue3)
						minimaxValue2 = minimaxValue3;
						// minimaxValue2 of a level 2 node records the min of minimaxValue3 of all level 3 nodes under that level 2 node
						// because the level 2 node is for person to move and thus to choose min
				}
				
				state[i1] = TILE_STATUS.TILE_STATUS_EMPTY;

				if (minimaxValue1 < minimaxValue2) {
					// minimaxValue1 of a level 1 node records the max of minimaxValue2 of all level 2 nodes under that level 1 node
					// because the level 1 node is for computer to move and thus to choose max
					minimaxValue1 = minimaxValue2;
					// if a new max is found for minimaxValue1, then reset minimax4K and minimax4Moves
					move = i1;
					minimax4Moves[0] = move;
					minimax4K = 1;
				}
				else if (minimaxValue1 == minimaxValue2) {
					// if the current minimaxValue2 is as good as the recorded minimaxValue1, then add the current move to minimax4Moves
					minimax4Moves[minimax4K] = i1;
					minimax4K++;					
				}
				
				if (tmpValue > minimaxValue2) // tmpValue is to store the min of minimaxValue2
					tmpValue = minimaxValue2; 

			}							

			// if (tmpValue > -1) then there is no sudden-death. In this case, continue MCTS (return -1).
			// else if (tmpValue ==-1) and (minimaxValue1 == -1), then every move leads to sudden-death. In this case, every move is a bad move. May continue MCTS (return -2).
			// otherwise, there is some sudden-death, but it can be avoided by taking the correct move. In this case, return move obtained in the minimax search.

			if (tmpValue > -1) 
				move = -1; // no sudden-death within 4 steps
			else if (minimaxValue1 == -1)
				move = -2;	// the situation is hopeless, all moves lead to sudden-death
			
			if ((move == -1) || (move ==-2)) 
				searchMode2Status = SEARCHMODE2STATUS.NODEATHORALLDEATH;
			else
				searchMode2Status = SEARCHMODE2STATUS.SOMEDEATH;
				

			return move;
			
		}
		
		public int bestMCTSMove()
		{
			searchMode2Status = SEARCHMODE2STATUS.NOTMET;
			minimax4K = 0;

			for (int i = 0; i < NUMROLLOUTSIMULATION; i++) {

				runTrial();
				if ( (SEARCH_MODE == 2) && (searchMode2Status == SEARCHMODE2STATUS.MET) ) {
					// here once a roll out simulation hits a terminal state <= NUMSIMULATIONDEPTH steps 
					// of simulation, run minimax under the top node

					long startTime = System.currentTimeMillis();
										
					int move = minimax_level4(nodeGameState);
					
					long estimatedTime = System.currentTimeMillis() - startTime;
					
					System.out.println("minimax_level4 estimatedTime = " + estimatedTime);

					// if found a winning move, then simply return
					if (searchMode2Status == SEARCHMODE2STATUS.WIN) 
						return move;
					
					// if no sudden-death within 4 steps or hopeless, then continue MCTS 
					// otherwise, recommended move belongs to the subset obtained by minimax_level4, i.e., the list of minimax4Moves
					if (searchMode2Status == SEARCHMODE2STATUS.SOMEDEATH) {
//						System.out.println("minimax_4 takes effect!");
						// if there is only one move, return the move obtained by minimax_level4 and end the search
						if (minimax4K == 1)
							return move;
						// otherwise, let MCTS continue among the moves in minimax4Moves
					}
				}
			}


			float max = -Float.MAX_VALUE;
		    int maxIndex = r.nextInt(nextMoves.size());
		    float randomizer;
		    for (int i = 0; i < nextMoves.size(); i++) {
		    	MCTSNode node = nextMoves.get(i);
		        float nodeScore = (float) node.getScore() / ((float) (node.getTimesVisited() + Float.MIN_VALUE));
		        randomizer = Float.MIN_VALUE * r.nextInt(GAMESTATELENGTH * GAMESTATELENGTH);
		        nodeScore = nodeScore + randomizer;
		        
//				this is useful output
//		    	System.out.println("i=" + i + " move="+ node.getindexFromParentNode() + " timesVisited: " + node.getTimesVisited() + " nodeScore " + nodeScore);
		        
		        if (nodeScore > max) {
		        	max = nodeScore;
		            maxIndex = i;
		        }
		    }
//	    	System.out.println("bestMCTSMove=" + nextMoves.get(maxIndex).getindexFromParentNode() + " timesVisited: " + nextMoves.get(maxIndex).getTimesVisited() + " maxScore="+ max);

		    return nextMoves.get(maxIndex).getindexFromParentNode();
		}
		   
		public int minimaxMove(TURN myTurn)
		{
			int turn;
			if (myTurn == TURN.COMPUTER_TURN) 
				turn = 1;
			else 
				turn = -1;
			
			if (checkWinner(nodeGameState, indexFromParentNode)) {
				if (nodeTurn == TURN.COMPUTER_TURN)
					score = -1; // COMPUTER loses
				else 
					score = 1; // COMPUTER wins
				
				return indexFromParentNode;
			}
			else {
				if (levelFromTopNode < NUMTREELEVEL_MINIMAX)
					expand();

				if (isLeaf()) { 
					score = 0;

					return indexFromParentNode;
				}
				else {
					int max = -1 * turn;
					int maxIndex = 0;
					for (int i = 0; i < nextMoves.size(); i++) {
						MCTSNode node = nextMoves.get(i);
						node.minimaxMove(getNextTurn(nodeTurn));
						if (node.getScore() * turn > max * turn) {
				        	max = node.getScore();
				            maxIndex = i;
						}
					}
					score = max;
					
					return nextMoves.get(maxIndex).getindexFromParentNode();
				}
			}
			
		}
	}

	
	
	public int choseTile(int previousMove) {
		
		boolean alreadyFull = true;
		for (int index = 0; index < GAMESTATELENGTH; index++) {
			if (currentTable[index] == GomokuApp.TILE_STATUS.TILE_STATUS_EMPTY)
				alreadyFull = false;
		}
		if (alreadyFull == true)	
			return -1;
		else {
			int i;
/*			
			i=62;
			if (currentTable[i] == GomokuApp.TILE_STATUS.TILE_STATUS_EMPTY)
				return i;
			
			i=50;
			if (currentTable[i] == GomokuApp.TILE_STATUS.TILE_STATUS_EMPTY)
				return i;
			
			i=38;
			if (currentTable[i] == GomokuApp.TILE_STATUS.TILE_STATUS_EMPTY)
				return i;
*/

			MCTSNode curNode = new MCTSNode(currentTable,TURN.COMPUTER_TURN, previousMove, 0);
			
			// here one can choose to use minimax or Monte Carlo simulation algorithm
			long startTime = System.currentTimeMillis();
			
			if ( (SEARCH_MODE == 1) || (SEARCH_MODE == 2) )
				i = curNode.bestMCTSMove();
			else // default (SEARCH_MODE == 0)
				i = curNode.minimaxMove(TURN.COMPUTER_TURN);
			
			long estimatedTime = System.currentTimeMillis() - startTime;
			
			System.out.println("bestMCTSMove estimatedTime = " + estimatedTime);
			return i;
		}
		
	}
	
	
	public boolean checkWinner(TILE_STATUS[] currentTable, int select) {
		
		int count;
		TILE_STATUS color;
		int tx, ty, x, y;

		// See whether the move just made at x,y has won.
		// We need to see if we now have five-in-a-row.
		
		color = currentTable[select];
		
		x = select % n;
		y = select / n;
		
//		System.out.println("select="+select+" n="+n+" x="+x+" y="+y);

		// check horizontal first
		tx = x; ty = y;
		while ((tx>0) && (currentTable[(tx-1)+ty*n]==color))
			tx--;
		count = 1;
		while ((tx < n-1) && (currentTable[(tx+1)+ty*n]==color))
		{
			count++;
			tx++;
		}
		if (count >= COUNTWIN)
			return true;

		// then do the three counts with vertical components
		for (int dx = -1; dx <= 1; dx++)
		{
			tx = x; ty = y;
			while ((ty>0) && ((tx-dx)>=0) && ((tx-dx)<n)
					&& (currentTable[(tx-dx)+(ty-1)*n]==color))
			{
				tx-=dx;
				ty--;
			}
			count = 1;
			while ((ty<n-1) && ((tx+dx)>=0) && ((tx+dx)<n)
					&& (currentTable[(tx+dx)+(ty+1)*n]==color))
			{
				count++;
				tx+=dx;
				ty++;
			}
			if (count >= COUNTWIN)
				return true;
		}
		return false;

	}

}
