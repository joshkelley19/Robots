package FOF;
import robocode.*;
import java.awt.Color;

// API help : http://robocode.sourceforge.net/docs/robocode/robocode/Robot.html

/**
 * Champion - a robot by (your name here)
 */
public class Emerald extends TeamRobot
{
	public byte scanDirection= 1;
	/**
	 * run: Champion's default behavior
	 */
	public void run() {

		setAdjustRadarForGunTurn(true);
		setAdjustGunForRobotTurn(true);
		setAdjustRadarForRobotTurn(true);
		setColors(Color.green,Color.green,Color.green); // body,gun,radar

		// Robot main loop
		while(true) {

			turnRadarRight(90);
			if(getEnergy()<50){
				evasiveManeuvers();
			}else standardMovement();
			turnRadarRight(360);
			
		}
	}

	/**
	 * onScannedRobot: What to do when you see another robot
	 */
	public void onScannedRobot(ScannedRobotEvent e) {
		if(e.getName().contains("FOF")){
			return;
		}		
		turnGunRight(getHeading() - getGunHeading() + 				e.getBearing());//mark.random-article.com
		scanDirection *= -1; // changes value from 1 to -1
		setTurnRadarRight(360 * scanDirection);//full rotation scans left to right//mark.random-article.com
		setTurnRadarRight(e.getBearing());
		execute();
		//turnGunRight(e.getBearing());
		fire(1);

		ahead(100);
		//setFire(Math.min(500/e.getDistance(),3));
		fire(3);
		
		
	}

	/**
	 * onHitByBullet: What to do when you're hit by a bullet
	 */
	public void onHitByBullet(HitByBulletEvent e) {
		evasiveManeuvers();
		evasiveManeuvers();
	}
	
	/**
	 * onHitWall: What to do when you hit a wall
	 */
	public void onHitWall(HitWallEvent e) {
		setTurnRight(e.getBearing());
		back(300);
	
	}	
	public void onHitRobot(HitRobotEvent e){
		turnRight(e.getBearing());

		ahead(100);
	}
	public void standardMovement(){
		ahead(Math.random()*400);
		setTurnRadarRight(360);
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
//	public void approachAndShoot(){
//		
//	}
}
