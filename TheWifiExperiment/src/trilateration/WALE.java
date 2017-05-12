package trilateration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class WALE extends Circle{
	
	 public WALE(int x, int y, int r) {
		super(x, y, r);
										//dont want you right now !! sorry :(
	}
	 									 //factor by which the radius is varied
	static	float [] beta= new float [3];
									// C is an array of circles 
									//obtained from available router RSSIs
									//array of circles in decreasing order of radius
	static Circle C[]=new Circle[3];
	/*************************************************************************
					cyclic  circle increasing function
		*************************************************************************/
		
	public static void CircleIncrease(){
		//Q is a queue to temporarily save circles for cyclic order
		//say 3 circles
		//obviously can do without queue,just to follow the algorithm
			
		QueueIntf Q= new QueueArray(3);		
		Q.enqueue(0);//circle 0
		Q.enqueue(1);//circle 1
		Q.enqueue(2);//circle 2
				
		int i =0; //counter in while	
			
		while(!Q.isEmpty()){//infinite loop
				
			i=Q.first();	
			if(!C[i].overlaps(C[(i+1)%3])|| !C[i].overlaps(C[(i+2)%3])){
			//if i doesn't intersect with at least 1 of the other 2
			C[i].R1=C[i].R1+beta[i]*C[i].R;
			System.out.println("increase c["+i+"]  radius to "+C[i].R1 );
			Q.enqueue(Q.dequeue());//rotating the circles in a cycle							
			}
	
			else{
				//infinite loop termination point
				break;
			}
		}
	}

	/*************************************************************************
								THE MAIN FUNCTION
		*************************************************************************/
	
	public static void main(String [] args){
		
		//get the 3 best circles in decreasing order of radius
		//pass array of their router numbers and distances
		int [] routerNumber={1,2,3};
		float [] radius = {10,11,21};
		//retrieving info from config file
		try {
		    Properties props = new Properties();
		    InputStream configFile=WALE.class.getClassLoader().getResourceAsStream("config.properties");		 
		    props.load(configFile);

		    String host = props.getProperty("host");
		    System.out.print("Host name is: " + host+"\n");
		    
		    //for each router
		    for(int i=0;i<3;i++){
		    String input="router"+routerNumber[i];
		    String routerLocation = props.getProperty(input);
		    String [] routerCenter=routerLocation.split(",");
		    System.out.print("router"+routerNumber[i]+" location in x:" + routerCenter[0]+" in y:" + routerCenter[1]+"\n");
			C[i]=new Circle(Integer.valueOf(routerCenter[0]),Integer.valueOf(routerCenter[1]),radius[i]);
			//one circle created
		    }
			
			
			
			
		    configFile.close();
		} catch (FileNotFoundException ex) {
		    // file does not exist
		} catch (IOException ex) {
		    // I/O error
		}
		
		
		
			
		C[1]=new Circle(200,200,40);
		C[2]=new Circle(150,150,30);
		
		//beta estimation
			//as we know c[0].R > c[1].R
			//C[0].R:C[1].R:C[2].R :: C[0].R/C[2].R:C[1].R/C[2].R:1
		
		beta[0]=C[0].R/C[2].R;
		beta[1]=C[1].R/C[2].R;
		beta[2]=1;
				
	/*************************************************************************
							Distance re-estimation
	*************************************************************************/

		
//overEstimation case1			
		if(C[0].isInside(C[1])||C[1].isInside(C[2])||C[0].isInside(C[2])){
		C[0].R1=(float) Math.hypot(C[0].X - C[1].X , C[0].Y-C[1].Y) -C[1].R1;
		CircleIncrease();
		}
//under estimation
		else if(!C[0].overlaps(C[1])||!C[0].overlaps(C[2])||!C[1].overlaps(C[2])){
			CircleIncrease();
		}

//overEstimation case2					
		else{
			if((C[0].areaInter(C[1])>C[0].delta(C[1]) || C[0].areaInter(C[2])>C[0].delta(C[2]) || C[1].areaInter(C[2])>C[1].delta(C[2]) ) && !(!C[0].overlaps(C[1])||!C[0].overlaps(C[2])||!C[1].overlaps(C[2])) ){
			// this and underestimation is false then overestimation 2 is true
			//as its in else case its shouldv'e already been covered
			//but as the radius is dynamically reduced it needs to be checked again
					
				while((C[0].areaInter(C[1])>C[0].delta(C[1]) || C[0].areaInter(C[2])>C[0].delta(C[2]) || C[1].areaInter(C[2])>C[1].delta(C[2]) ) && !(!C[0].overlaps(C[1])||!C[0].overlaps(C[2])||!C[1].overlaps(C[2])) ){
					
					C[0].R1=C[0].R1 - beta[0]* C[0].R;
					System.out.println("increase c[0]");
					C[1].R1=C[1].R1 - beta[1]* C[1].R;
					System.out.println("increase c[1]");
				}
			}
		}

		System.out.println("done dce");
		
		
		/*************************************************************************
								Location estimation
		*************************************************************************/
		float  W[] = new float[360] ;
			
		for(int i=0;i<3;i++){
				
			int t=0;
			for(int k=0;k<C[i].R1;k++){
				W[k]=(float) ((1/C[i].R)*(Math.exp(k/C[i].R)));
				System.out.println(W[k]);
				//for every point "j" on the varying circumference of radius "k"
				float x_co=(float) (C[i].X+k*Math.cos(t*3.14/180));
				float y_co=(float) (C[i].Y+k*Math.sin(t*3.14/180));
				point j= new point();
				j.x=x_co;
				j.y=y_co;
				// point on the varying k
				//check if it lies inside other circles
				if(C[(i+1)%3].hasPoint(x_co, y_co)||C[(i+2)%3].hasPoint(x_co, y_co)){
					if(j.isTrilaterable()){// checking if the point is trilaterable
						j.weight=j.weight+W[k];
					}
					else{
						j.weight=Math.abs(j.weight-W[k]);
					}
				}
				else{
					j.weight=W[k];
				}
			}
			
			for(float s:W){
				System.out.println(s);
			}
				
		}
	}		
		
}	

	
