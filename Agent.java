/*********************************************
 *  Agent.java 
*/

import java.util.*;
import java.io.*;
import java.net.*;
import java.nio.*;


public class Agent {

	final static int SIZE_M = 160;
	final static int SIZE_H = 80;

	//have table
   public boolean have_object[] = new boolean[4];    //[Obj_ID]
	//Number of stones
	public int have_stone_num = 0;

	private int get_id = -1;

	//Global information, use absolute coordinate
	/*Agent absolute direction
	|  0  |
	| 3^1 |
	|  2  |
	*/
	public int count_step = 0;
	public int direction = 0; //0 to 3
	//Agent Start at (0,0)
	public int cord_agent[] = new int[2];	
	

	//get_action
   public char get_action( char view[][] )
   {
		char ch=0;

		State  state  = new State();
		Memory memory = new Memory();
		
		if(count_step==0){
			direction =0;
			cord_agent[0] = SIZE_H;
			cord_agent[1] = SIZE_H;
			memory.init_map();//Initialize map in first step.
		}
		memory.draw_map(view,direction,cord_agent);

		//Decide the next move from current information
		if(have_object[3]==false){ 
			System.out.println ("Gold not Get.");//COMMENT
			if(memory.direct_route(direction,cord_agent,have_object)){
				System.out.println ("Accessible Object Found.");//COMMENT
				ch = memory.get_obj_direction();
				
			}
			else{
				if(have_object[0]&&memory.opt_river_find(cord_agent,have_object,have_stone_num))
				{
					System.out.println ("Cross River Required.");//COMMENT
					ch = memory.get_river_direction(direction,cord_agent,have_object);
					System.out.println ("River move:"+ch);//COMMENT
				}
				else{
					System.out.println ("Noting Interesing Found.");//COMMENT
					ch = state.path_explore(view,direction,cord_agent,have_object);
				}
			}			
			//Interact with objects
			ch = object_event(ch, view); 
			//memory.get_object(get_id);
		}
		else{ //Noting important found
			System.out.println ("Gold Get, Return");//COMMENT

			ch = state.return_home(direction,cord_agent);
		}
		
		//Prediction and Record for next state after move
		state.record_step(count_step,cord_agent);
		coordinate_record(ch); //Record coordinate after move
		count_step++;

		//Print Information//COMMENT
		//state.print_cord_object();//COMMENT
		print_info(); //COMMENT
		memory.print_map();//COMMENT
		System.out.println ("Move: " + ch );
		System.out.println ("-------------------------------------------------------------");
      return ch;
   }

	//*********************************************************
	//Auxiliary function

	//return modified action related to object
	private char object_event( char action, char view[][]){
		if(view[1][2]=='o') action = 'f';
		get_id = -1;
		if(action == 'f'){
			switch(view[1][2]){
				case 'o': have_object[0] = true; have_stone_num++; get_id=0; break;
				case 'a': have_object[1] = true; get_id=1; break;
				case 'k': have_object[2] = true; get_id=2; break;
				case 'g': have_object[3] = true; get_id=3; break;

				case 'T': action = 'c';			 break;
				case '-': action = 'u';			 break;
				case '~': have_stone_num--;break;
			}
			if(have_stone_num==0) have_object[0] = false; //have no stone
		}
		return action;
	}


	private void coordinate_record(char ch){
		if(ch =='r'){
			direction++;
			if(direction==4) direction = 0;
		}
		if(ch =='l'){
			direction--;
			if(direction==-1) direction = 3;
		}
		if(ch == 'f'){
			switch(direction){
				case 0: cord_agent[0]--; break;
				case 1: cord_agent[1]++; break;
				case 2: cord_agent[0]++; break;
				case 3: cord_agent[1]--; break;
			}
		}
	}



	private void clear_have_table()
	{
		have_stone_num = 0;
		for(int i=0;i<4;i++)
			have_object[i] = false;
	}


	//*********************************************************
	//Print information
	void print_info()
	{
		int i=0;
		System.out.format("Num: %3d# Next: A(%2d,%2d) Direction: %d \n", count_step,
																			cord_agent[0],cord_agent[1],
																								direction);
		System.out.format(IdtoName(i)+"="+have_object[i]+" Num:"+have_stone_num+" ");
		for( i=1;i<4;i++)
				System.out.format(" |"+IdtoName(i)+"="+have_object[i]+" ");
		System.out.print("\n ");
		
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

   void print_view( char view[][] )
   {
      int i,j;

      System.out.println("\n+-----+");
      for( i=0; i < 5; i++ ) {
         System.out.print("|");
         for( j=0; j < 5; j++ ) {
            if(( i == 2 )&&( j == 2 )) {
               System.out.print('^');
            }
            else {
               System.out.print( view[i][j] );
            }
         }
         System.out.println("|");
      }
      System.out.println("+-----+");

   }

   public static void main( String[] args )
   {
      InputStream in  = null;
      OutputStream out= null;
      Socket socket   = null;
      Agent  agent    = new Agent();
      char   view[][] = new char[5][5];
      char   action   = 'F';
      int port;
      int ch;
      int i,j;

      if( args.length < 2 ) {
         System.out.println("Usage: java Agent -p <port>\n");
         System.exit(-1);
      }

      port = Integer.parseInt( args[1] );

      try { // open socket to Game Engine
         socket = new Socket( "localhost", port );
         in  = socket.getInputStream();
         out = socket.getOutputStream();
      }
      catch( IOException e ) {
         System.out.println("Could not bind to port: "+port);
         System.exit(-1);
      }

      try { // scan 5-by-5 wintow around current location
         while(true) { //COMMENT
            for( i=0; i < 5; i++ ) {
               for( j=0; j < 5; j++ ) {
                  if( !(( i == 2 )&&( j == 2 ))) {
                     ch = in.read();
                     if( ch == -1 ) {
                        System.exit(-1);
                     }
                     view[i][j] = (char) ch;
                  }
               }
            }
            agent.print_view( view ); // COMMENT THIS OUT BEFORE SUBMISSION
            action = agent.get_action( view );
            out.write( action );
         }
      }
      catch( IOException e ) {
         System.out.println("Lost connection to port: "+ port );
         System.exit(-1);
      }
      finally {
         try {
            socket.close();
         }
         catch( IOException e ) {}
      }
   }
}
