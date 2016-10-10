/*********************************************
 *  State.java 
*/


import java.util.*;
import java.io.*;
import java.net.*;
import java.nio.*;

public class State{
	final static int INT_MAX = 2147483647;

	public static int cord_object[][] = new int[4][2];
	//Record History moves
	public static int cord_history[][] = new int[65536][2]; 

	private static int step_max; //record maximum step
	private int surround_available_path = 0;



	//Explore path when nothing found or accessible
	//Input: have, view, memory
	public char path_explore(char view[][],int direction,int cord_agent[], boolean have[])
	{
		char action = 'f';
		int i;
		boolean direction_avail[] = new boolean[4];

		int dir_step[] = new int[4]; //head back direction step num
		int dir_time[] = new int[4];
		int dir, min,max;//head back direction

			direction_avail = surround_path_check(view, have);

			if(surround_available_path==1) //only one route
			{ 
				for(i=0;i<4;i++)
				{
					if(direction_avail[i])
					{ 
						action = IdtoDirection(i);
						break;
					}
				}
			}
			else if(surround_available_path>=2)
			{
				dir_step = surround_history_check(direction,cord_agent);
				dir_time = surround_visit_times(direction,cord_agent);

				for(i=0;i<4;i++)//COMMENT
					System.out.format("%d=%d %B |",i,dir_time[i],direction_avail[i]); 

				min = INT_MAX;
				dir = 0;
				for(i=0;i<4;i++)
				{
					if(!direction_avail[i]) continue;
					if(dir_time[i] < min){
						min = dir_time[i];
						dir = i;
					}					
				}

				System.out.println(" Dir:"+dir); //COMMENT

				action = IdtoDirection(dir);

			}
		
		return action;
	}


	public char return_home(int direction,int cord_agent[])
	{
			int dir,i,min;
			char action = 'f';
			int dir_step[] = new int[4]; //head back direction step num

			dir_step = surround_history_check(direction,cord_agent);

			for(i=0;i<4;i++)
				System.out.format("%d=%d; ",i,dir_step[i]); //COMMENT

			dir = 0;
			min = dir_step[0];
			for(i=1;i<4;i++){
				if(dir_step[i]<min){
					min = dir_step[i];
					dir = i;
				}
			}
			System.out.println(" Dir:"+dir); //COMMENT

			action = IdtoDirection(dir);

			return action;
	}

	//*************************************************************************
	//History Operation, for return home

	//check surround history
	public int[] surround_history_check(int direction,int cord_agent[]){
		int dir_step[] = new int[4];
			dir_step[0] = history_check(surround_cord(direction,cord_agent,'f'));
			dir_step[1] = history_check(surround_cord(direction,cord_agent,'r'));
			dir_step[2] = history_check(surround_cord(direction,cord_agent,'b'));
			dir_step[3] = history_check(surround_cord(direction,cord_agent,'l'));
		return dir_step;
	}

	//return the most recent history step number
	public int history_check(int coordinate[]){
		int step = INT_MAX;
		for(int i=0;i<=step_max;i++){
			if(coordinate[0]==cord_history[i][0] &&coordinate[1]==cord_history[i][1])
			{
				step = i; 
				break;
			}
		}
		return step;
	}

	//return coordinate of surround, ch = 'f' 'r' 'l' 'b'
	public int[] surround_cord(int direction,int cord_agent[], char ch){
		int cord[] = new int[2];
		int dir;
		switch(ch){
			case 'f': dir = direction;   break;
			case 'r': dir = direction+1; break;
			case 'l': dir = direction-1; break;
			case 'b': dir = direction+2; break;
			default:  dir = direction;   break;
		}
		if(dir<0) dir = dir+4;
		if(dir>3) dir = dir-4;

		switch(dir){
			case 0: cord[0]=cord_agent[0]-1; cord[1]=cord_agent[1];  break;
			case 1: cord[0]=cord_agent[0]; cord[1]=cord_agent[1]+1;  break;
			case 2: cord[0]=cord_agent[0]+1; cord[1]=cord_agent[1];  break;
			case 3: cord[0]=cord_agent[0]; cord[1]=cord_agent[1]-1;  break;
			default: cord[0]=cord_agent[0]; cord[1]=cord_agent[1];   break;
		}
		return cord;
	}


	//*************************************************************************
	//Get travel times from history, for explore

	//check surround history times
	public int[] surround_visit_times(int direction,int cord_agent[]){
		int dir_time[] = new int[4];
			dir_time[0] = visit_times(surround_cord(direction,cord_agent,'f'));
			dir_time[1] = visit_times(surround_cord(direction,cord_agent,'r'));
			dir_time[2] = visit_times(surround_cord(direction,cord_agent,'b'));
			dir_time[3] = visit_times(surround_cord(direction,cord_agent,'l'));
		return dir_time;
	}

	//return how many times visited
	public int visit_times(int coordinate[]){
		int time = 0;
		for(int i=0;i<=step_max;i++){
			if(coordinate[0]==cord_history[i][0] &&coordinate[1]==cord_history[i][1])
			{
				time++;
			}
		}
		return time;
	}

	//*************************************************************************
	//Check surround path availability, for explore

	//check surrounding available path
	public boolean[] surround_path_check(char view[][], boolean have[]){
		boolean dir[] = new boolean[4];
		for(int i=0;i<4;i++) dir[i] = false;
		surround_available_path = 0;

		if(path_check(view[1][2],have)){
			dir[0] = true;
			surround_available_path++;
		} 
		if(path_check(view[2][3],have)){
			dir[1] = true;
			surround_available_path++;
		}
		if(path_check(view[3][2],have)){
			dir[2] = true;
			surround_available_path++;
		}
		if(path_check(view[2][1],have)){
			dir[3] = true;
			surround_available_path++;
		} 
		return dir;
	}

	public boolean path_check(char loc, boolean have[]){
		if(loc=='~'||loc=='T'||loc=='-'||loc=='*'){
			if(have[1] && loc=='T') return true;
    		if(have[2] && loc=='-') return true;
			return false;
		}
		else
			return true;
	}

	//**********************************************************************
	// Record step information
	public void record_step(int step,int cord_agent[])
	{
		step_max = step;
		cord_history[step][0] = cord_agent[0];
		cord_history[step][1] = cord_agent[1];
	}

	//*************************************************************************
	//Auxiliary function

	private char IdtoDirection(int dir){
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

}

















