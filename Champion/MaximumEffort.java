package Champion;
import robocode.*;
import java.awt.Color;

// API help : http://robocode.sourceforge.net/docs/robocode/robocode/Robot.html

/**
 * MaximumEffort - a robot by (your name here)
 */
public class MaximumEffort extends AdvancedRobot
{
	/**
	 * run: MaximumEffort's default behavior
	 */
	public void run() {
		// Initialization of the robot should be put here

		// After trying out your robot, try uncommenting the import at the top,
		// and the next line:

		setColors(Color.red,Color.black,Color.red); // body,gun,radar

		// Robot main loop
		while(true) {
			// Replace the next 4 lines with any behavior you would like
			ahead(100);
			turnGunRight(360);
			back(100);
			turnGunRight(360);
		}
	}

	/**
	 * onScannedRobot: What to do when you see another robot
	 */
	public void onScannedRobot(ScannedRobotEvent e) {
		// Replace the next line with any behavior you would like
		fire(1);
	}

	/**
	 * onHitByBullet: What to do when you're hit by a bullet
	 */
	public void onHitByBullet(HitByBulletEvent e) {
		// Replace the next line with any behavior you would like
		back(10);
	}
	
	/**
	 * onHitWall: What to do when you hit a wall
	 */
	public void onHitWall(HitWallEvent e) {
		// Replace the next line with any behavior you would like
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
}
