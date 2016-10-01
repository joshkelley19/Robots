package jk.melee;
  
import robocode.*;
import java.util.*;
import java.io.*;
import java.awt.Color;
public class Neuromancer extends AdvancedRobot{

   public static final boolean MC = false;
   public static final boolean TC = false;
   //public static final boolean MC2K7 = true;

   MeleeSurf move;
   MeleeRadar radar;
   MeleeGun gun;

   static int[] finishes;

   public void run(){
      if(finishes == null)
         finishes = new int[getOthers()+1];
      setAdjustRadarForGunTurn(true);
      setAdjustRadarForRobotTurn(true);
      setAdjustGunForRobotTurn(true);
      setColors( new Color(63, 0, 127), Color.YELLOW,  Color.BLACK);
      if(!TC)
         try{
            move = new MeleeSurf(this);
         }
         catch(Exception ex){contain(ex);}
      try{
         radar = new MeleeRadar(this);
      }
      catch(Exception ex){contain(ex);}
      try{
         gun = new MeleeGun(this);
      }
      catch(Exception ex){contain(ex);}
   
      while(true){
         if(!TC)
            try{
               move.onTick();
            }
            catch(Exception ex){contain(ex);}
         try{
            radar.onTick();
         }
         catch(Exception ex){contain(ex);}
         try{
            gun.onTick();
         }
         catch(Exception ex){contain(ex);}
         execute();
      }
   }

   public void onScannedRobot(ScannedRobotEvent e){
      if(!TC)
         try{
            move.onScannedRobot(e);
         }
         catch(Exception ex){contain(ex);}
      try{
         radar.onScannedRobot(e);
      }
      catch(Exception ex){contain(ex);}
      try{
         gun.onScannedRobot(e);
      }
      catch(Exception ex){contain(ex);}
   }
   public void onRobotDeath(RobotDeathEvent e){
      if(!TC)
         try{
            move.onRobotDeath(e);
         }
         catch(Exception ex){contain(ex);}
      try{
         radar.onRobotDeath(e);
      }
      catch(Exception ex){contain(ex);}
      try{
         gun.onRobotDeath(e);
      }
      catch(Exception ex){contain(ex);}
      
   }
   public void onHitByBullet(HitByBulletEvent e){
      if(!TC)
         try{
            move.onHitByBullet(e);
         }
         catch(Exception ex){contain(ex);}
   }
   public void onBulletHitBullet(BulletHitBulletEvent e){
      if(!TC)
         try{  move.onBulletHitBullet(e);
         }
         catch(Exception ex){contain(ex);}
   }
   public void onBulletHit(BulletHitEvent e){
      if(!TC)
         try{ move.onBulletHit(e);
         }
         catch(Exception ex){contain(ex);}
   }
   public void onPaint(java.awt.Graphics2D g){
      if(!TC)
         move.onPaint(g);
      gun.onPaint(g);
   }
   public void onDeath(DeathEvent e){
      try{ gun.onDeath(e);
         finishes[getOthers()]++;
         System.out.println("Finishes: " + Arrays.toString(finishes));
      }
      catch(Exception ex){contain(ex);}
   }
   public void onWin(WinEvent e){
      try{
         gun.onWin(e);
         finishes[0]++;
         System.out.println("Finishes: " + Arrays.toString(finishes));
      }
      catch(Exception ex){contain(ex);}
   }

   public void contain(Exception e){
   
      e.printStackTrace();
         
      try{
         PrintStream out = new PrintStream(new RobocodeFileOutputStream(getDataFile((int)(Math.random()*1000) + ".error")));
         e.printStackTrace(out);
         out.flush();
         out.close();
      }
      catch (IOException ioex){}
   }


}