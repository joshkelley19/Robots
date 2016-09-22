package Champion;
import robocode.*;
import java.awt.Color;

// API help : http://robocode.sourceforge.net/docs/robocode/robocode/Robot.html

/**
 * Champion - a robot by (your name here)
 */
public class Champion extends AdvancedRobot
{
	public byte scanDirection= 1;
	/**
	 * run: Champion's default behavior
	 */
	public void run() {
		// Initialization of the robot should be put here
		setAdjustRadarForGunTurn(true);
		setAdjustGunForRobotTurn(true);
		// After trying out your robot, try uncommenting the import at the top,
		// and the next line:

		setColors(Color.red,Color.blue,Color.green); // body,gun,radar

		// Robot main loop
		while(true) {

			
			// Replace the next 4 lines with any behavior you would like
			setTurnRadarRight(90);
			if(getEnergy()<50){
				evasiveManeuvers();
			}else standardMovement();
			//turnRadarRight(360);

			
			
		}
	}

	/**
	 * onScannedRobot: What to do when you see another robot
	 */
	public void onScannedRobot(ScannedRobotEvent e) {
		scanDirection *= -1; // changes value from 1 to -1
		setTurnRadarRight(360 * scanDirection);//full rotation scans left to right//mark.random-article.com
		
		//setTurnRadarRight(e.getBearing());
		setTurnRight(e.getBearing());
		setTurnGunRight(getHeading() - getGunHeading() + e.getBearing());//mark.random-article.com
		setFire(Math.min(500/e.getDistance(),3));
	}

	/**
	 * onHitByBullet: What to do when you're hit by a bullet
	 */
	public void onHitByBullet(HitByBulletEvent e) {

		evasiveManeuvers();
	}
	
	/**
	 * onHitWall: What to do when you hit a wall
	 */
	public void onHitWall(HitWallEvent e) {

		back(100);
	
	}	
	public void standardMovement(){
		setTurnRadarRight(360);
		ahead(Math.random()*400);
		//setTurnRadarRight(360);
		back(Math.random()*400);
	}
	public void evasiveManeuvers(){
		boolean decision=((Math.random()*100)>50)?true:false;
		if(decision){
			setTurnRight(Math.random()*300);
			setAhead(Math.random()*300);
		}else{
			setTurnLeft(Math.random()*300);
			setAhead(Math.random()*300);
		}

	}
	public void approachAndShoot(){
		
	}
}
