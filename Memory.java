/*********************************************
 *  Memory.java 
*/


import java.util.*;
import java.io.*;
import java.net.*;
import java.nio.*;

//Memory for steps taken, object coordinate

public class Memory{

	final static int INT_MAX = 2147483647;

	//---------------------------Map data-----------------------------
	final static int SIZE_M = 160;
	final static int SIZE_H = 80;

	public static char visited_map[][] = new char[SIZE_M][SIZE_M];
	public static int LEFT,RIGHT,UP,DOWN;

	//---------------------------River-----------------------------
	final static int SIZE_R = 256;
	//Location of rivers that directly reachible
	public int acc_river[][] = new int[SIZE_R][2];
	public int acc_river_num = 0; //number of direct accessible river
	public int river_cost[][] = new int[SIZE_R][SIZE_R];
	public int river_i,river_j; //Optimum choice

	//new land seprated by river, goal to reach
	public int new_land[][] = new int[SIZE_R][2];
	public int new_land_num = 0;
	//Marker for continuous land
	public int marker_land[] = new int[SIZE_R];
	public int marker_land_num = 0;
	public int marker_object[] = new int[4];
	public int marker_stone[] = new int[SIZE_M];

	//---------------------------Table-----------------------------
	//see table
   public static boolean see_object[] = new boolean[4];     //[Obj_ID]

	//accessible table
   public static boolean access_object[] = new boolean[4];  //[Obj_ID]

	//coordinate table
	//next cord to reach each object
	public static int cord_obj_next[] = new int[4];  //[Obj_ID], direction
	//cord of each object
	public static int cord_object[][] = new int[4][2]; //[Obj_ID] [i,j]
	//cord of stone
	public static int cord_stone[][] = new int[SIZE_M][2];
	public static int see_stone_num=0;

	//--------------------------Search-----------------------------
	private boolean travel[][] = new boolean[SIZE_M][SIZE_M];
	private int queue[][] = new int[SIZE_M*SIZE_M][2];
	private int size_q; //Queue size
	private int next_i,next_j;//Queue leave next

	public int prev[] = new int[SIZE_M*SIZE_M]; //record previous path
	public int step[][] = new int[SIZE_M][SIZE_M]; //minimum step for each point.


	//**********************************************************************
	//Map Creation, Modification
	public void init_map()
	{

		clear_see_table();
		clear_access_table();
		clear_cord_next_table();
		clear_cord_obj_table();

		LEFT=80;RIGHT=80;UP=80;DOWN=80;
		for(int i=0;i<SIZE_M;i++)
		{
			for(int j=0;j<SIZE_M;j++)
			{
				visited_map[i][j] = '#';//mist of map
			}
		}
	}

	//Update map according to view, agent direction,coordinate
	public void draw_map(char view[][], int dirn, int cord_agent[])
	{
		int i,j,r=0,c=0;
		int row,col;
		if(cord_agent[0]-2<UP)       UP = cord_agent[0]-2;
		if(cord_agent[0]+2>DOWN)   DOWN = cord_agent[0]+2;
		if(cord_agent[1]-2<LEFT)   LEFT = cord_agent[1]-2;
		if(cord_agent[1]+2>RIGHT) RIGHT = cord_agent[1]+2;

		row = cord_agent[0];
      col = cord_agent[1];

		System.out.println("row:"+row+" col:"+col);

      for( i = -2; i <= 2; i++ ) {
         for( j = -2; j <= 2; j++ ) {
            switch( dirn ) {
             case 0: r = 2+i; c = 2+j; break;
             case 1: r = 2-j; c = 2+i; break;
				 case 2: r = 2-i; c = 2-j; break;
             case 3: r = 2+j; c = 2-i; break;
            }

				if(i==0&&j==0){
					switch( dirn ) {
		          case 0: visited_map[row+i][col+j] = '^'; break;
		          case 1: visited_map[row+i][col+j] = '>'; break;
		          case 2: visited_map[row+i][col+j] = 'v'; break;
		          case 3: visited_map[row+i][col+j] = '<'; break;
		         }
				}
				else{
		         visited_map[row+i][col+j] = view[r][c];
				}
         }
      }
	}

	public void print_map()
	{
		System.out.format("Map: (%3d,%3d)(%3d,%3d)\n",LEFT,RIGHT,UP,DOWN);

		System.out.print("+");
		for(int j=LEFT;j<=RIGHT;j++)
			System.out.print("-");
		System.out.print("+\n");
		for(int i=UP;i<=DOWN;i++)
		{
			System.out.print("|");
			for(int j=LEFT;j<=RIGHT;j++)
			{
				System.out.print(visited_map[i][j]);
			}
			System.out.println("|");
		}
		System.out.print("+");
		for(int j=LEFT;j<=RIGHT;j++)
			System.out.print("-");
		System.out.print("+\n");
	}//end print_map

	//**********************************************************************
	//Map operations
	// Return True when objects found, refresh see table
	private boolean explor_view()
	{
		int i,j;
		int num;
		
		boolean found=false;
		clear_see_table();
		clear_access_table();
		clear_cord_obj_table();
		clear_cord_next_table();
		clear_cord_stone_table();

		for( i=UP; i <= DOWN; i++ )
		{
		   for( j=LEFT;j<=RIGHT; j++ ) 
			{
				num=-1;
				//See Object
		      switch(visited_map[i][j] )
				{
						case 'o': num = 0; break;
						case 'a': num = 1; break;
	   				case 'k': num = 2; break;
						case 'g': num = 3; break;
				}//end switch
				if(num!=-1){
					see_object[num] = true; found=true; 
					cord_object[num][0] = i; cord_object[num][1] = j;
					if(num==0){
						cord_stone[see_stone_num][0]=i;
						cord_stone[see_stone_num][1]=j;
						see_stone_num++;
					}
				}
		   }
		}

		System.out.format("See stone: %d\n",see_stone_num);//COMMEMT
		for(i=0;i<see_stone_num;i++)//COMMEMT
		System.out.format("(%2d,%2d) ",cord_stone[i][0],cord_stone[i][1]);//COMMEMT
		System.out.format("\n");
		return found;
	}//end explor_view



	//Using BFS to determine direct route, True if direct route exists
	public boolean direct_route(int direction,int cord_agent[],boolean have[]){
		int i;
		if(!explor_view()) return false;
		for(i=0;i<4;i++){
			access_object[i] = bfs_route(have,
													cord_agent[0],cord_agent[1],
													cord_object[i][0],cord_object[i][1]);
			if(access_object[i])
				cord_obj_next[i] = find_direction(direction,
															 cord_agent[0],cord_agent[1],
															 cord_object[i][0],cord_object[i][1]);
		}

		print_info();//COMMENT

		for(i=0;i<4;i++){
			if(access_object[i]) return true;
		}
		return false;
	}//end direct_route

	//Use after direction_route
	public char get_obj_direction(){
		char ch = 'f';
		for(int i=0;i<4;i++){
			if(access_object[i]){
				ch = IdtoDirection(cord_obj_next[i]);
			}
		}

		return ch;
	}


	//*****************************************************************
	//River Operation
	//Determine optimum river to cross, get object after crossing
	public boolean opt_river_find(int cord_agent[],boolean have[],int stone_num){
		int i,j;
		int min = INT_MAX;
		int step;
		int index = -1;
		clear_river_cost_table();
		if(!find_acc_river(cord_agent,have)) return false;
		if(!find_new_land(cord_agent,have)) return false;
		//Calculate river cost
		cal_river_cost();

		print_acc_river_table(); //COMMENT
		print_new_land_table();  //COMMENT
		print_river_cost_table();//COMMENT
	
		//------------------------------------
		find_object_mark();
 		print_object_mark();
	
		System.out.format("marker_land_num: %d\n",marker_land_num);//COMMENT
		if(marker_land_num==1&&check_signle_river(stone_num)!=-1){
			index = check_signle_river(stone_num);
		}
		else if(marker_land_num>1){

			index = check_opt_multi_river(3);
			System.out.format("opt_multi_index: %d\n",min,index);//COMMENT

			//Cannot find optimum route, find if direct route exists
			if(index ==-1) 
				index = opt_multi_fail_solution( stone_num);

			
			if(index ==-1) 
				return false;
		}
		else 
			return false;

		
		
		System.out.format("Min: %d, index: %d\n",min,index);//COMMENT

		river_i=acc_river[index][0];
		river_j=acc_river[index][1];
		
		System.out.format("Step river: (%2d,%2d)\n",river_i,river_j);//COMMENT

		return true;
	}

	//Must use after opt_river_find
	public char get_river_direction(int direction,int cord_agent[],boolean have[]){
		char ch = 'f';
		int dir=-1;
		if(bfs_route(have, cord_agent[0], cord_agent[1],river_i, river_j))
		dir =find_direction(direction,cord_agent[0], cord_agent[1],river_i,river_j);
		if(dir!=-1) ch = IdtoDirection(dir);
		return ch;
	}

	//********************************************************************************
	// Functions to find new land and river 
	//return direct accessible rivers
	public boolean find_acc_river(int cord_agent[],boolean have[])
	{
		int i,j;
		clear_acc_river_table();
		for( i=UP; i <= DOWN; i++ ){
		   for( j=LEFT;j<=RIGHT; j++ ) {
					//River directly reachible
					if(visited_map[i][j]=='~'&&bfs_route(have,
														cord_agent[0],cord_agent[1],
														i,j))
					{
						acc_river[acc_river_num][0] = i;
						acc_river[acc_river_num][1] = j;
						acc_river_num++;
					}
		        
		    }
		}
		if(acc_river_num==0) return false;
		return true;
	}


	//Find unaccessible land, and check continuous
	private boolean find_new_land(int cord_agent[],boolean have[]){

		int i,j;
		clear_new_land_table();
		for( i=UP; i <= DOWN; i++ ){
		   for( j=LEFT;j<=RIGHT; j++ ) {
					//land but unreachable directly
					if(check_access(i,j,have)&&!bfs_route(have,
																  cord_agent[0],cord_agent[1],
																  i,j))
					{
						new_land[new_land_num][0] = i;
						new_land[new_land_num][1] = j;
						new_land_num++;
					}
		    }
		}
		if(new_land_num==0) return false; 
		for(i=0;i<new_land_num;i++){
			if(marker_land[i]==0){
				marker_land_num++;
				marker_land[i] = marker_land_num;
				find_continuous_land(have,new_land[i][0],new_land[i][1], marker_land_num);
			}
			else continue;
		}//end for
		return true;
	}

	//Calculate river_cost table
	private void cal_river_cost(){
		int i,j,row;
		int min = INT_MAX;
		int step;
		for(row=0;row<marker_land_num;row++)
		{
			for(j=0;j<acc_river_num;j++)
			{
				min = INT_MAX;
				for(i=0;i<new_land_num;i++)
				 if(row+1== marker_land[i]){
					step = find_water_route(
									acc_river[j][0],acc_river[j][1],
										new_land[i][0], new_land[i][1]);
					if(step>0&&step<min) min = step;					
				}
				if(min!=INT_MAX)
					river_cost[row][j]=min;
				else
					river_cost[row][j]=0;
			}
		}
		//Remove all zero rivers
		for(j=0;j<acc_river_num;j++)
		{
			if(check_all_zero(j)){
				acc_river_num--;
				for(i=j;i<acc_river_num;i++){
					acc_river[i]=acc_river[i+1];
					for(row=0;row<marker_land_num;row++)
					river_cost[row][i]=river_cost[row][i+1];
				}
			}
		}
	}

	//Mark object in new land
	private void find_object_mark(){
		int i,j;
		for(i=0;i<new_land_num;i++){
			for(j=0;j<4;j++){
				if(new_land[i][0]==cord_object[j][0]&&new_land[i][1]==cord_object[j][1]){
					marker_object[j] = marker_land[i];
				}
			}
			for(j=0;j<see_stone_num;j++){
				if(new_land[i][0]==cord_stone[j][0]&&new_land[i][1]==cord_stone[j][1]){
					marker_stone[j] = marker_land[i];
				}
			}
		}
	}

	//****************************************************************
	//Auxiliary function for River

	//check river
	private boolean check_river(int loc_i, int loc_j){
		char loc;
		if(loc_i<0||loc_i>SIZE_M||loc_j<0||loc_j>SIZE_M) return false; //out of scope
		else loc = visited_map[loc_i][loc_j];

		if(loc=='~') return true;
		else return false;
	}


	private boolean check_zero(int j){
		for(int i=0;i<marker_land_num;i++){
			if(river_cost[i][j]==0) return true;
		}
		return false;
	}

	private boolean check_all_zero(int j){
		for(int i=0;i<marker_land_num;i++){
			if(river_cost[i][j]!=0) return false;
		}
		return true;
	}
	

	private int check_sum(int j){
		int sum=0;
		for(int i=0;i<marker_land_num;i++){
			sum = sum+river_cost[i][j];
		}
		return sum;
	}

	//Opt sum==3
	private boolean exist_opt_sum(int k){
		for(int i=0;i<marker_land_num;i++){
			for(int j=0;j<marker_land_num;j++){
				if(river_cost[i][k]==1&&river_cost[j][k]==2)
					return true;
			}
		}
		return false;
	}

	//****************************************************************
	//Auxiliary function for Agent
	public char IdtoDirection(int dir){
			char action;
			switch(dir){
			case 0: action = 'f';  break;
			case 1: action = 'r';  break;
			case 2: action = 'r';  break;
			case 3: action = 'l';  break;
			default:action = 'r';  break;
			}
			return action;
	}


	//*********************************************************
	//Print information for Agent
	void print_info(){
		for(int i=0;i<4;i++){
				System.out.format("See:"+IdtoName(i)+ " " +see_object[i]+
						" @:"+cord_object[i][0]+","+cord_object[i][1]+
							" Access:"+access_object[i]+
								" Direction:"+cord_obj_next[i]+"\n");
		
		}

	}//end print_info

	String IdtoName(int id)
	{
		String name;
		switch(id){
			case 0: name = "Stone"; break;
			case 1: name = "Axe";   break;
			case 2: name = "Key";   break;
			case 3: name = "Gold";  break;
			default: name = "NULL"; break;
		}
		return name;
	}

	//**************************************************************
	// Clear tables for Agent
	private void clear_see_table()
	{
		for(int i=0;i<4;i++)
			see_object[i] = false;
	}

	private void clear_access_table()
	{
		for(int i=0;i<4;i++)
			access_object[i] = false;
	}

	private void clear_cord_next_table()
	{
		for(int i=0; i < 4; i++ ) {
					cord_obj_next[i]=-1;
			}
	}

	private void clear_cord_obj_table()
	{
		for(int i=0; i < 4; i++ ) {
		     for(int j=0; j < 2; j++ ) {
					cord_object[i][j]=-1;
				}
			}
	}

	private void clear_cord_stone_table()
	{
		see_stone_num=0;
		for(int i=0; i < SIZE_M; i++ ) {
		     for(int j=0; j < 2; j++ ) {
					cord_stone[i][j]=-1;
				}
			}
	}


	//*****************************************************
	//Clear Tables for River
	//clear acc_river table
	private void clear_acc_river_table(){
		acc_river_num = 0;
		for(int i=0; i < SIZE_R; i++ ) {
		     for(int j=0; j < 2; j++ ) {
					acc_river[i][j]=-1;
				}
			}
	}

	//clear new_land, marker_land table
	private void clear_new_land_table(){
		new_land_num = 0;
		marker_land_num = 0;
		for(int i=0; i < SIZE_R; i++ ) {
			marker_land[i] = 0;
		     for(int j=0; j < 2; j++ ) {
					new_land[i][j]=-1;
				}
			}
	}

	//clear river cost table
	private void clear_river_cost_table(){
		for(int i=0; i < SIZE_R; i++ ) {
		     for(int j=0; j < SIZE_R; j++ ) {
					river_cost[i][j]=0;
				}
			}
	}


	//******************************************************************
	//Print for River

	//print acc river table
	public void print_acc_river_table(){
		System.out.println("Location to cross river:");
		for(int i=0; i < acc_river_num; i++ ) {
					System.out.format("(%2d,%2d) ",acc_river[i][0],acc_river[i][1]);
			}
		System.out.print("\n");
	}

	//print new land table
	public void print_new_land_table(){
		System.out.format("New land: %2d\n",marker_land_num);
		for(int i=0; i < new_land_num; i++ ) {
					System.out.format("(%2d,%2d) ",new_land[i][0],new_land[i][1]);
			}
		System.out.print("\n");
		for(int i=0; i < new_land_num; i++ ) {
					System.out.format(" %2d     ",marker_land[i]);
			}
		System.out.print("\n");
	}


	//print river cost table
	public void print_river_cost_table(){
		System.out.print("River Cost:\n");

		for(int i=0; i < marker_land_num; i++ ) {
		     for(int j=0; j < acc_river_num; j++ ) {
					System.out.format("%2d ",river_cost[i][j]);
			}
			System.out.print("\n");
		}
	}

	public void print_object_mark(){
		System.out.print("Object Mark:");
		for(int i=0; i < 4; i++ )
			System.out.print(marker_object[i]+" ");
		System.out.print("\nStone Mark:");
		for(int i=0; i < see_stone_num; i++ )
			System.out.print(marker_stone[i]+" ");
		System.out.print("\n");
	}
	//***********************************************************************
	//************************** Search Algorithms **************************
	//***********************************************************************

	//*****************************************************************
	//Search Algorithms: BFS for route on land

	//Return True if route exists, update loc next for next move 
	public boolean bfs_route(boolean have[],int str_i,int str_j, int loc_i, int loc_j)
	{
		travel_clear();
		step_clear();
		queue_clear();
		prev_clear();

		queue_join(str_i,str_j);
		travel[str_i][str_j] = true;

		step[str_i][str_j] = 0;

		while(size_q!=0){
			queue_leave();
			//System.out.println("i:"+next_i+" j:"+next_j);
			if(check_surround(next_i,next_j,loc_i, loc_j)) return true;

			add_surround_queue(next_i, next_j,have);
		}

		return false;
	}//end find_route

	//return first step direction using prev[]
	//Must use directly after bfs_route
	//Pos: cord object ; str: cord_agent
	public int find_direction(int dirn,int str_i,int str_j,int pos_i,int pos_j){
		int index = pos_i*SIZE_M+pos_j;
		int dest = str_i*SIZE_M+str_j;
		while(prev[index]!=dest){
			index = prev[index];
		}
			System.out.format("find dirn: %d Agent: %d ",index,dest);//COMMENT
			System.out.format("Agen_dirn: %d",dirn);//COMMENT
			if(index ==(dest-SIZE_M)) 		 index = 0-dirn; // up
			else if(index ==(dest+1))      index = 1-dirn; // right
			else if(index ==(dest+SIZE_M)) index = 2-dirn; // down
			else if(index ==(dest-1)) 	    index = 3-dirn; // left
		
		if(index<0) index=index+4;
		if(index>3) index=index-4;
			System.out.format(" Direction: %d \n",index);//COMMENT
		return index;
	}//end find_direction


	//************************************************************************
	//Aux functions for BFS

	//check access of single pixel
	private boolean check_access(int loc_i,int loc_j,boolean have_object[])
	{
		char loc;
		if(loc_i<0||loc_i>SIZE_M||loc_j<0||loc_j>SIZE_M) return false; //out of scope
		else loc = visited_map[loc_i][loc_j];

		if(loc=='~'||loc=='T'||loc=='-'||loc=='*'||loc=='#'||loc=='.'){
			if(have_object[1] && loc=='T') return true;
    		if(have_object[2] && loc=='-') return true;
			return false;
		}
		else{
			return true;
		}
	}//end check_access


	private void add_surround_queue(int str_i,int str_j, 
												boolean have[])
	{
		if(check_access(str_i-1,str_j,have)&& !travel[str_i-1][str_j]){
			travel[str_i-1][str_j] = true;
			step[str_i-1][str_j] = step[str_i][str_j]+1;
			queue_join(str_i-1,str_j);
			prev_join(str_i-1,str_j,str_i,str_j);
		}
		if(check_access(str_i+1,str_j,have)&& !travel[str_i+1][str_j]){
			travel[str_i+1][str_j] = true;
			step[str_i+1][str_j] = step[str_i][str_j]+1;
			queue_join(str_i+1,str_j);
			prev_join(str_i+1,str_j,str_i,str_j);
		}
		if(check_access(str_i,str_j-1,have)&& !travel[str_i][str_j-1]){
			travel[str_i][str_j-1] = true;
			step[str_i][str_j-1] = step[str_i][str_j]+1;
			queue_join(str_i,str_j-1);
			prev_join(str_i,str_j-1,str_i,str_j);
		}
		if(check_access(str_i,str_j+1,have)&& !travel[str_i][str_j+1]){
			travel[str_i][str_j+1] = true;
			step[str_i][str_j+1] = step[str_i][str_j]+1;
			queue_join(str_i,str_j+1);
			prev_join(str_i,str_j+1,str_i,str_j);
		}

	}

	private boolean check_surround(int str_i,int str_j,int loc_i, int loc_j){
		if(str_i-1==loc_i&& str_j==loc_j ){
			step[str_i-1][str_j] = step[str_i][str_j]+1;
			prev_join(str_i-1,str_j,str_i,str_j);
			return true;
		}
		if(str_i+1==loc_i&& str_j==loc_j ){
			step[str_i+1][str_j] = step[str_i][str_j]+1;
			prev_join(str_i+1,str_j,str_i,str_j);
			return true;
		}
		if(str_i==loc_i&& str_j-1==loc_j ){
			step[str_i][str_j-1] = step[str_i][str_j]+1;
			prev_join(str_i,str_j-1,str_i,str_j);
			return true;
		}
		if(str_i==loc_i&& str_j+1==loc_j ){
			step[str_i][str_j+1] = step[str_i][str_j]+1;
			prev_join(str_i,str_j+1,str_i,str_j);
			return true;
		}

		return false;
	}
	//************************************************************************
	//Queue Operation
	void queue_clear(){
		size_q = 0;
		for(int i=0; i < SIZE_M*SIZE_M; i++ ) {
		    for(int j=0; j < 2; j++ ){
					queue[i][j]=-1;
			}
		}
	}//end clear_step_table


	void queue_join(int pos_i,int pos_j){
		queue[size_q][0] = pos_i;
		queue[size_q][1] = pos_j;
		size_q++;
	}
	
	void queue_leave(){
		size_q--;
		if(queue[0][0]!=-1 && queue[0][1]!=-1){
			next_i = queue[0][0];
			next_j = queue[0][1];

			for(int i=0;i<size_q;i++){
				queue[i][0] = queue[i+1][0];
				queue[i][1] = queue[i+1][1];
			}
			queue[size_q][0] = 0;
			queue[size_q][1] = 0;
		}
	}

	//************************************************************************
	//Prev Operation
	void prev_clear(){
		for(int i=0; i <SIZE_M*SIZE_M ; i++ ) {
					prev[i]=-1;
		}
	}//end clear_prev_table

	void prev_join(int pos_i,int pos_j, int prev_i,int prev_j){
		prev[pos_i*SIZE_M+pos_j] = prev_i*SIZE_M+prev_j;
	}

	void prev_print(){
		for(int i=0; i < SIZE_M*SIZE_M; i++ )
					System.out.format("%2d ",i);
					System.out.print("\n");
		for(int i=0; i < SIZE_M*SIZE_M; i++ )
					System.out.format("%2d ",prev[i]);
					System.out.print("\n");
	}//end clear_prev_table

	//************************************************************************
	//Travel Operation
	void travel_clear(){
		for(int i=0; i < SIZE_M; i++ ) {
		     for(int j=0; j < SIZE_M; j++ ) {
					travel[i][j]=false;
				}
			}
	}//end clear_travel_table



	//************************************************************************
	//Travel Operation
	void step_clear(){
		for(int i=0; i < SIZE_M; i++ ) {
		    for(int j=0; j < SIZE_M; j++ ){
					step[i][j] = 0;
			}
		}
	}//end clear_step_table

	void step_print(){
		for(int i=0; i < SIZE_M; i++ ) {
		    for(int j=0; j < SIZE_M; j++ ){
					System.out.format("%2d ",step[i][j]);
			}
			System.out.print("\n");
		}
	}//end clear_step_table


	//END BFS for route on land
	//*****************************************************************

	//*****************************************************************
	// Search Algorithm: BFS for route in river
	//*****************************************************************
	//Search Algorithms: BFS for route in water

	//Return true if water route exists
	public int find_water_route(int str_i,int str_j, int loc_i, int loc_j)
	{
		boolean found = false;
		travel_clear();
		step_clear();
		queue_clear();
		prev_clear();

		queue_join(str_i,str_j);
		travel[str_i][str_j] = true;

		step[str_i][str_j] = 0;

		while(size_q!=0){
			queue_leave();
			//System.out.println("i:"+next_i+" j:"+next_j);
			if(check_surround(next_i,next_j,loc_i, loc_j))
			{
				found = true;
				break;
			}
			//Add water, not in acc_water_table
			add_water_queue(next_i, next_j);
		}
		//Find way, Calculate steps
		if(found)
			return step[loc_i][loc_j];
		else
			return 0;
	}//end find_water_route


	public void find_continuous_land(boolean have[],int str_i,int str_j,int index)
	{
		travel_clear();
		queue_clear();

		queue_join(str_i,str_j);
		travel[str_i][str_j] = true;

		while(size_q!=0){
			queue_leave();
			add_continuous_queue(have,next_i, next_j,index);
		}
	}//end find_continuous_land





	//*****************************************************************
	// Auxiliary functions for BFS River Search

	private void add_water_queue(int str_i,int str_j)
	{
		if(check_river(str_i-1,str_j)&& !travel[str_i-1][str_j]
				&&!check_acc_water(str_i-1,str_j))
		{
			travel[str_i-1][str_j] = true;
			step[str_i-1][str_j] = step[str_i][str_j]+1;
			queue_join(str_i-1,str_j);
			prev_join(str_i-1,str_j,str_i,str_j);
		}
		if(check_river(str_i+1,str_j)&& !travel[str_i+1][str_j]
				&&!check_acc_water(str_i+1,str_j))
		{
			travel[str_i+1][str_j] = true;
			step[str_i+1][str_j] = step[str_i][str_j]+1;
			queue_join(str_i+1,str_j);
			prev_join(str_i+1,str_j,str_i,str_j);
		}
		if(check_river(str_i,str_j-1)&& !travel[str_i][str_j-1]
				&&!check_acc_water(str_i,str_j-1))
		{
			travel[str_i][str_j-1] = true;
			step[str_i][str_j-1] = step[str_i][str_j]+1;
			queue_join(str_i,str_j-1);
			prev_join(str_i,str_j-1,str_i,str_j);
		}
		if(check_river(str_i,str_j+1)&& !travel[str_i][str_j+1]
				&&!check_acc_water(str_i,str_j+1))
		{
			travel[str_i][str_j+1] = true;
			step[str_i][str_j+1] = step[str_i][str_j]+1;
			queue_join(str_i,str_j+1);
			prev_join(str_i,str_j+1,str_i,str_j);
		}

	}


	private void add_continuous_queue(boolean have[],int str_i,int str_j,int mark)
	{
		int index;
		if(check_access(str_i-1,str_j,have)&& !travel[str_i-1][str_j]){
			travel[str_i-1][str_j] = true;
			queue_join(str_i-1,str_j);
			index = search_new_land_table(str_i-1,str_j);
			if(index>=0)
			marker_land[index] = mark;
		}
		if(check_access(str_i+1,str_j,have)&& !travel[str_i+1][str_j]){
			travel[str_i+1][str_j] = true;
			queue_join(str_i+1,str_j);
			index = search_new_land_table(str_i+1,str_j);
			if(index>=0)
			marker_land[index] = mark;

		}
		if(check_access(str_i,str_j-1,have)&& !travel[str_i][str_j-1]){
			travel[str_i][str_j-1] = true;
			queue_join(str_i,str_j-1);
			index = search_new_land_table(str_i,str_j-1);
			if(index>=0)
			marker_land[index] = mark;

		}
		if(check_access(str_i,str_j+1,have)&& !travel[str_i][str_j+1]){
			travel[str_i][str_j+1] = true;
			queue_join(str_i,str_j+1);
			index = search_new_land_table(str_i,str_j+1);
			if(index>=0)
			marker_land[index] = mark;
		}
	}

	//return index in new land table
	private int search_new_land_table(int loc_i, int loc_j){
		int index = -1;
		for(int i=0; i < new_land_num; i++ ) {
			if(new_land[i][0]==loc_i&&new_land[i][1]==loc_j){
				index = i;
				break;
			}
		}
		return index;
	}

	//Prevent duplicate route in water
	private boolean check_acc_water(int loc_i,int loc_j){
		for(int i=0;i<acc_river_num;i++)
		{
			if(acc_river[i][0]==loc_i &&acc_river[i][1]==loc_j )
			{
				return true;
			}
		}
		return false;
	}

	//*****************************************************************
	//Cases for crossing river

	private int check_signle_river(int stone_num){
		int j;
			for(j=0;j<acc_river_num;j++)
			{
				if(river_cost[0][j]<=stone_num&&river_cost[0][j]!=0&&marker_object[3]==1)
				{
					return j;
				}
			}
		return -1;
	}


	private int check_opt_multi_river(int threshold){
		int min,index,j;
			min = INT_MAX;
			index = -1;
			for(j=0;j<acc_river_num;j++)
			{
				if(check_zero(j)) continue;
				else{
					if(exist_opt_sum(j)){
						return j;
					}
				}
			}
		return index;
	}

	private int opt_multi_fail_solution(int stone_num){
		int index=-1;
		int i,j;
		for(i=0;i<marker_land_num;i++){
			for(j=0;j<acc_river_num;j++){
				if(river_cost[i][j]<=stone_num&&river_cost[i][j]!=0){
					System.out.format("Cost: %d, %d,%d\n",river_cost[i][j],i,j);//COMMENT
					for(int k=0;k<4;k++){
						if(marker_object[k]==i+1){ 
							index = j;
				
							System.out.format("Obj:%d, %d, index: %d\n",k,i+1,index);//COMMENT
							return index;
						}
					}
					for(int k=0;k<see_stone_num;k++){
						if(marker_stone[k]==i+1){ 
							index = j;
				
							System.out.format("Stone:%d, %d, index: %d\n",k,i+1,index);//COMMENT
							return index;
						}
					}
				}
			}
		}
		return index;
	}

}




