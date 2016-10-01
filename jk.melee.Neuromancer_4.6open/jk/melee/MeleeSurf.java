package jk.melee;

import robocode.*;
import robocode.util.*;
import java.util.*;
import java.awt.geom.*;
import jk.mega.FastTrig;
import jk.mega.KDTree;
import java.awt.Color;
	
public class MeleeSurf{


   static  KDTree<MeleeScan> GF_0_tree = new KDTree.Manhattan<MeleeScan>(6);
   static{
      //FastTrig.init();
      GF_0_tree.addPoint(new double[]{0,0,0,0,0,0},new MeleeScan());
   }
   static int LOCATION_DIMS = 7;
   static Hashtable<String,EnemyInfo> enemies = new Hashtable<String,EnemyInfo>();
   static Hashtable<String,EnemyInfo> deadEnemies = new Hashtable<String,EnemyInfo>();
   ArrayList<MeleeWave> waves = new ArrayList<MeleeWave>();
   AdvancedRobot bot;
   public static Rectangle2D.Double fieldRect;
   double MAX_X, MAX_Y;
   ArrayList<Point2D.Double> testPoints = new ArrayList<Point2D.Double>();
   ArrayList<Point2D.Double> bestHitPoints;
   EnemyInfo me, lastMe, lastLastMe; 
   Point2D.Double[] pts;
   double[] dangers;
   ArrayList<Point2D.Double>[] hitPoints;
   Point2D.Double bestGoPoint;
   
   // Hashtable<Long,Snapshot> snapshots = new Hashtable<Long,Snapshot>();

	
   public MeleeSurf(AdvancedRobot abot){
      bot = abot;
      
      enemies.putAll(deadEnemies);
      deadEnemies.clear();  
      
      if(enemies.get(bot.getName()) == null){
         EnemyInfo me = new EnemyInfo();
         me.name = bot.getName();
         enemies.put(me.name,me);
      }
   	
      Enumeration<EnemyInfo> e = enemies.elements();
      // System.out.println("Simulated enemy gun enemies: " );
      while(e.hasMoreElements()){
         EnemyInfo eInfo = e.nextElement();
         eInfo.lastScanTime =0;
         // if(eInfo.targets != null){// 
            // System.out.print(eInfo.targets.size() + " ");
           //  Enumeration<String> keys = eInfo.targets.keys();
         //       while(keys.hasMoreElements())
         //          System.out.println(keys.nextElement());  
         // }
      }
      // System.out.println();
      MAX_X = bot.getBattleFieldWidth();
      MAX_Y = bot.getBattleFieldHeight();
      fieldRect = new Rectangle2D.Double(18,18,MAX_X - 36,MAX_Y - 36);
      
   }  
   public void onTick(){
      // lastLastMe = lastMe;
      lastMe = me;
    
      //me = enemies.get(bot.getName());
      //if(me == null){
      me = new EnemyInfo();
      me.name = bot.getName();
         //enemies.put(me.name,me);
      //}
   
   
      me.location = new Point2D.Double(bot.getX(), bot.getY());
      me.heading = bot.getHeadingRadians();
      me.velocity = bot.getVelocity();
      me.energy = bot.getEnergy();
      me.lastScanTime = (int)bot.getTime();
      me.name = bot.getName();
   
      if(lastMe != null)
         enemies.put(me.name,lastMe);  
      else
         enemies.put(me.name,me);  
   	
      // Snapshot snap = new Snapshot(bot.getTime(),enemies);
      // snapshots.put(snap.time,snap);
   	
      long time = bot.getTime();
      Iterator<MeleeWave> wit = waves.iterator();
      while(wit.hasNext()){
         MeleeWave mw = wit.next();
         double radius = mw.bulletVelocity*(time - mw.fireTime);
         Point2D.Double centre = mw.fireLocation;
         if( radius - 50 > centre.distance(new Point2D.Double(0,0))
         && radius > centre.distance(new Point2D.Double(0,MAX_Y))
         && radius > centre.distance(new Point2D.Double(MAX_X,0))
         && radius > centre.distance(new Point2D.Double(MAX_X,MAX_Y)))
            wit.remove();
      
      }
   //somehow work out which movement option intersects all waves the safest....
   
   //generate directions to check distance/parallism with all bots as this is a first criteria
      double closestEnemy = Double.POSITIVE_INFINITY;
      Enumeration<EnemyInfo> clen = enemies.elements();
      while(clen.hasMoreElements()){
         EnemyInfo ei = clen.nextElement();
         if(ei == me || ei == lastMe)
            continue;
         double d = ei.location.distance(me.location);
         if(d < closestEnemy)
            closestEnemy = d;
      }
      pts = new Point2D.Double[32];
      for(int i = 0; i < pts.length; i++)
         pts[i] = project(me.location,(i)*2*Math.PI/(double)(pts.length),Math.min(160,closestEnemy*0.75));
      if(bestGoPoint == null)
         bestGoPoint = me.location;
      //pts[pts.length - 1] = bestGoPoint;
         
      //   System.out.println(closestEnemy);
      dangers = new double[pts.length];
      testPoints.clear();
      
   //do some surfing!
   
      double minDanger = Double.POSITIVE_INFINITY;
      double goDir = 0;
      
      double[] enemyDangers = new double[pts.length];
      double[] waveDangers = new double[pts.length];
      hitPoints = new ArrayList[pts.length];
      
      boolean melee = bot.getOthers() > 1;
      
      points:
      for(int i = 0; i < pts.length; i++){
      
         if(fieldRect.contains(pts[i]))
            testPoints.add(pts[i]);
         else{
            enemyDangers[i] = waveDangers[i] = Double.POSITIVE_INFINITY;
            continue;
         }
         if(melee){
            boolean oldMethodErroredFast = oldMethodCheckFast(me.location,pts[i],me.velocity,me.heading);
            if(oldMethodErroredFast){
               waveDangers[i] = Double.POSITIVE_INFINITY;
               continue points;  
            }
         // boolean oldMethodErrored = false;
         }
         ArrayList<Point2D.Double> path = futureStatus(me.location,pts[i],me.velocity,me.heading);
         
           
         Enumeration<EnemyInfo> en = enemies.elements();
         while(en.hasMoreElements()){
            EnemyInfo ei = en.nextElement();
            if(ei.name.equals(me.name))
               continue;
            // double dist = 
         //    new Line2D.Double(pts[i],me.location).ptSegDist(ei.location);
         	
            double dist = Math.max(0,pts[i].distance(ei.location)-30);
            double closest = 1;	
            Enumeration<EnemyInfo> ens = enemies.elements();
            double minEDist = Double.POSITIVE_INFINITY;
            while(ens.hasMoreElements()){
               EnemyInfo eni = ens.nextElement();
               if(eni.name.equals(me.name) | eni == ei)
                  continue;
               double eDist = eni.location.distance(ei.location);
               if(eDist  < minEDist)
                  minEDist = eDist;
               if(dist < eDist)
                  closest++;    
               
            }
            closest = limit(1,closest - enemies.size() + 1, 3);
            if(new Line2D.Double(pts[i],me.location).ptSegDist(ei.location) < 0.9*minEDist){
            
               double anglediff = absoluteBearing(me.location,pts[i]) - absoluteBearing(ei.location,me.location);
            
               enemyDangers[i] += ei.energy/(dist*dist ) * closest * (1 + Math.abs(Math.cos(anglediff)));
            
            }
            else{
               enemyDangers[i] += ei.energy/(dist*dist )* closest ;
            
            }
         
         }
         //enemyDangers[i] *= closest;
         hitPoints[i] = new ArrayList<Point2D.Double>();
      		
         Iterator<MeleeWave> it = waves.iterator();
         while(it.hasNext()){
            MeleeWave w = it.next();
            
            if(
             !w.surfable
             ||
            me.location.distance(w.fireLocation) - 18 < w.bulletVelocity*(time - w.fireTime) )
               continue;  
         	
            Point2D.Double hitPoint = futureStatus(path,time,w);
         	
            if(!fieldRect.contains(hitPoint)){
               waveDangers[i] = Double.POSITIVE_INFINITY;
               continue points;
            }
            
            
            double bearing = Utils.normalAbsoluteAngle(absoluteBearing(w.fireLocation,hitPoint));
                  
            double botWidth = 40/w.fireLocation.distance(hitPoint);
                  
            double scale = 180/Math.PI;
            double a1 = Utils.normalAbsoluteAngle(bearing - botWidth*0.5);
            double a2 = Utils.normalAbsoluteAngle(bearing + botWidth*0.5);
            double waveDanger =  botWidth*averageDanger((int)(scale*a1), (int)(scale*a2), w.bins, w.botShadowBins);
            // double waveDanger = botWidth*w.bins[(int)(scale*bearing)];
         	
            double waveWeight = w.bulletDamage/
               Math.abs(me.location.distance(w.fireLocation)/w.bulletVelocity - (time - w.fireTime));
         	
         	
            waveDangers[i] += waveWeight*waveDanger;
         
         }
      
      }
    
      medianNormalize(enemyDangers);
      medianNormalize(waveDangers);
   	
      double[] dangerWeights = {1,3};//{bot.getOthers(),bot.getOthers() + waves.size()};
      for(int i = 0; i < pts.length; i++){
         dangers[i] =  dangerWeights[0]*enemyDangers[i] + 
            dangerWeights[1]*waveDangers[i];
      
         if(dangers[i] < minDanger){
            minDanger = dangers[i];
            
            bestGoPoint = pts[i];
            bestHitPoints = hitPoints[i];
         }
      }
      goDir = absoluteBearing(me.location,bestGoPoint);
      double headingDiff = goDir - bot.getHeadingRadians();
      bot.setTurnRightRadians(Math.tan(headingDiff));
      bot.setAhead(Double.POSITIVE_INFINITY*Math.cos(headingDiff));
      
   }
   public double averageDanger(int i1, int i2, double[] bins, double[] enableBins){
      double d = 0;
      if(i1 < i2){
         for(int i = i1; i <= i2; i++)
            d += bins[i]*enableBins[i];
         d /= i2 - i1 + 1;
      }
      else{
         for(int i = i1; i < bins.length; i++)
            d += bins[i]*enableBins[i];
         for(int i = 0; i <= i2; i++)
            d += bins[i]*enableBins[i];
         d /= bins.length - i1 + i2 + 1;
      }
   
      return d;
   }
   public static void areaNormalize(double[] bins){
      double total = 0;
      //double min = Double.POSITIVE_INFINITY;
      for(int i = 0; i < bins.length; i++){
         if(bins[i] != Double.POSITIVE_INFINITY){
            total += bins[i];
            //if(bins[i] < min)
               //min = bins[i];   
         }
      }
      if(total != 0){
         total = 1/total;
         for(int i = 0; i < bins.length; i++)
            if(bins[i] != Double.POSITIVE_INFINITY){
               //bins[i] = (bins[i] - min)* total;
               bins[i] *= total;
            }
      }
   }
   public static void medianNormalize(double[] bins){
      double[] sortedBins = new double[bins.length];
      System.arraycopy(bins,0,sortedBins,0,bins.length);
      Arrays.sort(sortedBins);
      int minReal = 0;
      while(minReal < bins.length && sortedBins[minReal] == 0)minReal++;
      int maxReal = minReal;
      while(maxReal < bins.length && !Double.isInfinite(sortedBins[maxReal]))maxReal++;
   	
   	
      double scale = 1/(1e-30 + sortedBins[(int)limit(0,(minReal + maxReal)/2 - 1,bins.length - 1)]);
   
      for(int i = 0; i < bins.length; i++)
         if(bins[i] != Double.POSITIVE_INFINITY){
               //bins[i] = (bins[i] - min)* total;
            bins[i] *= scale;
         }
      
   }
   public static void normalize(double[] bins){
      double max = 0, min = Double.POSITIVE_INFINITY;
      for(int i = 0; i < bins.length; i++){
         if(bins[i] != Double.POSITIVE_INFINITY && bins[i] > max)
            max = bins[i];
         if(bins[i] < min)
            min = bins[i];
      }
      if(max != min){
         max = 1/(max-min);
         for(int i = 0; i < bins.length; i++)
            if(bins[i] != Double.POSITIVE_INFINITY)
               bins[i] = (bins[i]-min)*max;
      }
   }
   public void onPaint(java.awt.Graphics2D g){
   
      g.setColor(Color.red);
   
      Iterator<Point2D.Double> pi = testPoints.iterator();
      while(pi.hasNext()){
         Point2D.Double p = pi.next();
         g.drawOval((int)p.x - 3, (int)p.y - 3, 6,6);
      
      }
   
      g.setColor(Color.green);
      g.drawOval((int)me.location.x - 20, (int)me.location.y - 20, 40,40);
      long time = bot.getTime();
      Iterator<MeleeWave> wit = waves.iterator();
      while(wit.hasNext()){
         MeleeWave w = wit.next();
         double bDist = w.bulletVelocity*(time - w.fireTime);
        
         g.setColor(Color.orange);
         
      	
        // // for(int i = 0; i < w.bins.length; i++){
            // Point2D.Double p = project(w.startFiredFrom,i*(Math.PI*2), sDist);
            // g.drawOval((int)p.x - 1, (int)p.y - 1, 2,2);
         // }
         g.drawOval((int)w.fireLocation.x - (int)bDist, (int)w.fireLocation.y - (int)bDist, 2*(int)bDist, 2*(int)bDist);
         g.setColor(Color.white);
         
         
         EnemyInfo meInfoAtFire = w.snapshot.get(me.name);
         
         // if(meInfoAtFire == null)
            // System.out.println("me not in snapshot, snapshot size = " + w.snapshot.size());
         Point2D.Double meAtFire = meInfoAtFire.location;
       
         double GF0 = absoluteBearing(w.fireLocation, meAtFire);
         double MEA = maxEscapeAngle(w.bulletVelocity);
         double a1 = GF0 - MEA;
         double a2 = GF0 + MEA;
         
         int index1 = (int)((180/Math.PI)*a1 + 360)%360;
         int index2 = (int)((180/Math.PI)*a2 + 360)%360;
         
         if(index1 < index2){
            double maxD = 0;
            for(int i = 0; i < w.bins.length; i++){
               if(w.bins[i]*w.botShadowBins[i] > maxD)
                  maxD = w.bins[i]*w.botShadowBins[i];
            }
            for(int i = index1; i < index2; i++){
               double relDanger = w.bins[i]*w.botShadowBins[i]/maxD;
               
               boolean paint = false;
               if(relDanger == 1){
                  g.setColor(Color.red);
                  paint =true;}
               else if(relDanger > 0.8){
                  g.setColor(Color.orange);
                  paint =true;}
               else if(relDanger > 0.6){
                  g.setColor(Color.yellow);
                  paint =true;}
               else if(relDanger > 0.4){
                  g.setColor(Color.green);
                  paint =true;}
               else if(relDanger > 0.2)
                  g.setColor(Color.blue);
               else
                  g.setColor(Color.black);
               if(paint){
                  Point2D.Double p = project(w.fireLocation,i*Math.PI/180,bDist);
                  g.drawOval((int)p.x - 3, (int)p.y - 3, 6, 6);
               }
               
            }
         }
         else{
            double maxD = 0;
            for(int i = 0; i < w.bins.length; i++){
               if(w.bins[i] > maxD)
                  maxD = w.bins[i];
            }
            for(int i = index1; i <w.bins.length; i++){
               double relDanger = w.bins[i]/maxD;
               boolean paint = false;
               if(relDanger == 1){
                  g.setColor(Color.red);
                  paint =true;}
               else if(relDanger > 0.8){
                  g.setColor(Color.orange);
                  paint =true;}
               else if(relDanger > 0.6){
                  g.setColor(Color.yellow);
                  paint =true;}
               else if(relDanger > 0.4){
                  g.setColor(Color.green);
                  paint =true;}
               else if(relDanger > 0.2)
                  g.setColor(Color.blue);
               else
                  g.setColor(Color.black);
               if(paint){
                  Point2D.Double p = project(w.fireLocation,i*Math.PI/180,bDist);
                  g.drawOval((int)p.x - 3, (int)p.y - 3, 6, 6);
               }
            }
            for(int i = 0; i <= index2; i++){
               double relDanger = w.bins[i]/maxD;
               
               boolean paint = false;
               if(relDanger == 1){
                  g.setColor(Color.red);
                  paint =true;}
               else if(relDanger > 0.8){
                  g.setColor(Color.orange);
                  paint =true;}
               else if(relDanger > 0.6){
                  g.setColor(Color.yellow);
                  paint =true;}
               else if(relDanger > 0.4){
                  g.setColor(Color.green);
                  paint =true;}
               else if(relDanger > 0.2)
                  g.setColor(Color.blue);
               else
                  g.setColor(Color.black);
               if(paint){
                  Point2D.Double p = project(w.fireLocation,i*Math.PI/180,bDist);
                  g.drawOval((int)p.x - 3, (int)p.y - 3, 6, 6);
               }               }
         }
      	
      }
      double maxDanger = 0;
      for(int i = 0; i < dangers.length; i++)
         if(dangers[i] > maxDanger && dangers[i] != Double.POSITIVE_INFINITY)
            maxDanger = dangers[i];
      for(int i = 0; i < pts.length; i++){
         Point2D.Double p = pts[i];
         double relDanger = dangers[i]/maxDanger;
         if(relDanger == Double.POSITIVE_INFINITY)
            g.setColor(Color.magenta);
         else if(relDanger == 1)
            g.setColor(Color.red);
         else if(relDanger > 0.8)
            g.setColor(Color.orange);
         else if(relDanger > 0.6)
            g.setColor(Color.yellow);
         else if(relDanger > 0.4)
            g.setColor(Color.green);
         else if(relDanger > 0.2)
            g.setColor(Color.blue);
         else
            g.setColor(Color.black);
      
         g.drawLine((int)p.x, (int)p.y, (int)me.location.x, (int)me.location.y);
      }
      
      g.setColor(Color.white);
      if(hitPoints != null)
         for(int i = 0; i < hitPoints.length; i++)
            for(int j = 0; hitPoints[i] != null && j < hitPoints[i].size(); j++){
               Point2D.Double p = hitPoints[i].get(j);
               g.drawOval((int)p.x - 1, (int)p.y - 1, 2, 2);
            }
         
      // g.setColor(Color.green);
      // ArrayList<MoveGenerator.MoveSequence> seqs = MoveGenerator.getRandomSequences(50,me.location,me.heading,me.velocity,50);
      // for(MoveGenerator.MoveSequence seq : seqs){
         // Point2D.Double lp = me.location;
         // for(MoveGenerator.Move m : seq.sequence){
            // Point2D.Double p = m.loc;
            // g.drawLine((int)p.x, (int)p.y, (int)lp.x, (int)lp.y);
            // lp = p;
         // }
      // 
      // }
   // 		
   }
   public void onScannedRobot(ScannedRobotEvent e){
   
      // me.location = new Point2D.Double(bot.getX(), bot.getY());
      // me.heading = bot.getHeadingRadians();
      // me.velocity = bot.getVelocity();
      // me.energy = bot.getEnergy();
      // me.lastScanTime = (int)bot.getTime();
      // me.name = bot.getName();
   // 
   
      EnemyInfo eInfo = enemies.get(e.getName());
      double absBearing = e.getBearingRadians() + bot.getHeadingRadians();
      Point2D.Double newELocation =project(new Point2D.Double(bot.getX(), bot.getY()),absBearing,e.getDistance());
      
      if(eInfo == null){
         enemies.put(e.getName(), eInfo = new EnemyInfo());
         eInfo.name = e.getName();
         eInfo.gunHeat = bot.getGunHeat() - bot.getGunCoolingRate();
         eInfo.defaultAim = new KDTree.Manhattan(LOCATION_DIMS);
         
      //    add a default GF0 shot
         MeleeScan ms = new MeleeScan();
         ms.GF = 0;
         ms.weight = 1e-20; 
         eInfo.defaultAim.addPoint(new double[7],ms);
         
         eInfo.targets = new Hashtable<String,KDTree<MeleeScan>>();
         eInfo.heading = e.getHeadingRadians();
         eInfo.velocity = e.getVelocity();
      
         // System.out.println("adding " + eInfo.name);
      }
      else{
         if(eInfo.lastScanTime == 0)
            eInfo.gunHeat = bot.getGunHeat()  - bot.getGunCoolingRate();
      
         double lastGunHeat = eInfo.gunHeat;
         double deltaE = eInfo.energy - e.getEnergy();
         long deltaT = Math.max(0,bot.getTime() -eInfo.lastScanTime);
         eInfo.heading = e.getHeadingRadians();
         eInfo.velocity = e.getVelocity();
      
         if(eInfo.lastScanTime != 0 && deltaT != 0 ){
         
         
         //check gunheat: if (gunheat> 0) fired = false;
            double gunHeat = eInfo.gunHeat -= deltaT*bot.getGunCoolingRate();
            boolean fired = gunHeat <= bot.getGunCoolingRate();
         
         //check for overlapping waves
            MeleeWave bestOverlap = getBestOverlapExcluding(newELocation,deltaE,eInfo.name,false);            
            if (bestOverlap != null && Math.abs(deltaE-bestOverlap.bulletDamage) < 0.01){
               logWaveHit(bestOverlap, newELocation, eInfo);
            }
            
            else if(
            (bestOverlap == null || deltaE < bestOverlap.bulletDamage)
                && fired && 0 < deltaE && deltaE <= 3 )//they fired!
               addWave(eInfo,newELocation,deltaE, lastGunHeat);
               
            else{
               bestOverlap = getBestOverlapExcluding(newELocation,deltaE,eInfo.name,true); 
            
               if(fired && bestOverlap != null &&  bestOverlap.bulletDamage < deltaE && deltaE <= bestOverlap.bulletDamage + 3){
               
                  addWave(eInfo,newELocation,deltaE - bestOverlap.bulletDamage, lastGunHeat);
                  logWaveHit(bestOverlap, newELocation ,eInfo);
               
               
               }
               
               else if(bestOverlap == null);//can't find wave!
            }
         }
         
         //bot shadows... like bullet shadows but bigger!
         if(eInfo.location == null){
            eInfo.location = project(newELocation,e.getHeadingRadians(),-e.getVelocity());
            eInfo.lastScanTime = (int)bot.getTime() - 1;
         }
         int endTime = (int)bot.getTime();         	   
         int interpolateTime = endTime - eInfo.lastScanTime + 1;
         Point2D.Double[] interpPoints = new Point2D.Double[interpolateTime];
         double startx = eInfo.location.x, starty = eInfo.location.y;
         double dx = (newELocation.x - startx)/interpolateTime;
         double dy = (newELocation.y - starty)/interpolateTime;   
         for(int i = 0; i < interpolateTime; i++)
            interpPoints[i] = new Point2D.Double(startx + i*dx, starty + i*dy);
         for(MeleeWave w : waves){
            if(!w.firedBy.equals(eInfo.name))
               for(int i = 0; i < interpolateTime; i++){
                  Point2D.Double testPoint = interpPoints[i];
                  int testTime = eInfo.lastScanTime + i;
                  double testDist = testPoint.distance(w.fireLocation);
                  double waveRadius = w.bulletVelocity*(testTime - w.fireTime);
                  if(waveRadius > testDist + 18 || 
                  (i == 0 && waveRadius < testDist - 18 - (interpolateTime - i)*(8 + w.bulletVelocity)))
                     break;
                  
                  if(waveRadius < testDist - 18)
                     continue;
                  
               //must be intersecting!  
                  double angle = absoluteBearing(w.fireLocation,testPoint);
                  logShadow(w,angle,36/testDist);
               
               }
         }
      
         
      }
      eInfo.lastEnergy = eInfo.energy;
      eInfo.energy = e.getEnergy();
      eInfo.lastScanTime = (int)bot.getTime();
      eInfo.location = newELocation;
   }
   public void onRobotDeath(RobotDeathEvent ev){
      EnemyInfo e = enemies.remove(ev.getName());
      if(e != null)
         deadEnemies.put(e.name,e);
   }
   public void onHitByBullet(HitByBulletEvent ev){
      Bullet b = ev.getBullet();
      logBullet(b);
      EnemyInfo e = enemies.get(b.getName());
      if(e != null){
         e.energy += b.getPower()*3;
      }
   
   }
   public void onBulletHitBullet(BulletHitBulletEvent e){
      logBullet(e.getHitBullet());
   }
   public void onBulletHit(BulletHitEvent ev){
      Bullet b = ev.getBullet();
      EnemyInfo e = enemies.get(ev.getName());
      if(e != null){
         double power = b.getPower();
         double damage = 4*power;
         if(power > 1)
            damage += 2*(power - 1);
         e.energy -= damage;
         double deltaE = e.energy - ev.getEnergy();
         double nowGunHeat = e.gunHeat - bot.getGunCoolingRate()*(bot.getTime() - e.lastScanTime);
         if(deltaE <= 3 && deltaE > 0.01 &&  nowGunHeat <= 0){
            Point2D.Double newLocation = new Point2D.Double(b.getX(),b.getY());
            addWave(e,newLocation,deltaE, e.gunHeat);
         }
         e.energy = ev.getEnergy();
      }
   
   }
   public void logShadow(MeleeWave w, double angle, double width){
   
      int lowIndex = (int)Math.round(w.bins.length*Utils.normalAbsoluteAngle(angle - 0.5*width)*(0.5/Math.PI))%w.bins.length;
      int highIndex = (int)Math.round(w.bins.length*Utils.normalAbsoluteAngle(angle + 0.5*width)*(0.5/Math.PI))%w.bins.length;
      if(lowIndex <= highIndex)
         for(int i = lowIndex; i <= highIndex; i++)
            w.botShadowBins[i] = 0;
      else {
         for(int i = highIndex; i < w.bins.length; i++)
            w.botShadowBins[i] = 0;
         for(int i = 0; i <= lowIndex; i++)
            w.botShadowBins[i] = 0;
      }
   
   }
   public void addWave(EnemyInfo firer, Point2D.Double newELocation, double deltaE, double lastGunHeat){
     
     
      firer.gunHeat = Rules.getGunHeat(deltaE) - bot.getGunCoolingRate()*(bot.getTime() - firer.lastScanTime);
      MeleeWave w = new MeleeWave();
            //take a "snapshot" of where everybody is, and their direction and energy etc
            //and store it in the wave
      w.snapshot = new Hashtable<String,EnemyInfo>();
      Enumeration<EnemyInfo> en = enemies.elements();
      while(en.hasMoreElements()){
         EnemyInfo ei = en.nextElement();
         if(ei.name == firer.name)
            continue;
         EnemyInfo cp = new EnemyInfo();
         cp.location = ei.location;
         cp.energy = ei.energy;
         cp.name = ei.name;
         cp.heading = ei.heading;
         cp.velocity = ei.velocity;
         w.snapshot.put(cp.name,cp);
                  
         double targetBearing = absoluteBearing(firer.location,cp.location);
         double latVel = cp.velocity*FastTrig.sin(cp.heading - targetBearing);
         double advVel = cp.velocity*FastTrig.cos(cp.heading - targetBearing);
         double distToE = cp.location.distance(firer.location);
         double distToNearest = distToE * distToE;
         Enumeration<EnemyInfo> all = enemies.elements();
         while(all.hasMoreElements()){
            EnemyInfo ein = all.nextElement();
            if(ein.name.equals(cp.name))
               continue;
            double distSq = ein.location.distanceSq(cp.location);
            if(distSq < distToNearest)
               distToNearest = distSq;
         }
         distToNearest = Math.sqrt(distToNearest);
         double distToWall = Math.min(
                        Math.min(cp.location.x - 18,cp.location.y - 18),
                        Math.min(MAX_X - 18 - cp.location.x, MAX_Y - 18 - cp.location.y)); 
         double enemiesAlive = bot.getOthers();
         Point2D.Double[] corners = new Point2D.Double[]{
               new Point2D.Double(0,0),
               new Point2D.Double(0,MAX_Y),
               new Point2D.Double(MAX_X,0),
               new Point2D.Double(MAX_X,MAX_Y)
               };
         
         double distToCorner = Double.POSITIVE_INFINITY;
         for(int i = 0; i < 4; i++)
            distToCorner = Math.min(cp.location.distanceSq(corners[i]),distToCorner);
         distToCorner = Math.sqrt(distToCorner);
                  
         double[] location = { 
                           latVel *(20/8.0),
                           advVel *(12/8.0),
                           distToE * (3/1200.0),
                           distToNearest * (10/1200.0),
                           distToWall * (4/600.0),
                           enemiesAlive*2,
                           distToCorner*(5/700.0)
                             };

         LOCATION_DIMS = location.length;
         cp.treeLocation = location;
                  
      }
               // System.out.println("Snapshot size: " + w.snapshot.size() + " Enemies: " + enemies.size());
      w.firedBy = firer.name;
   
      long gunDelay = (long)Math.max(0,Math.round(lastGunHeat/bot.getGunCoolingRate()));
      //w.fireTime = (long)Math.round((firer.lastScanTime + gunDelay + bot.getTime() + 1)*0.5 - 2);
      long earliestFire = firer.lastScanTime + gunDelay;  
      long latestFire = bot.getTime() - 1;
      w.fireTime = (earliestFire + latestFire)/2 - 1;
      
      double travelTick = firer.location.distance(newELocation)/(bot.getTime() - firer.lastScanTime);
      double headingTick = absoluteBearing(firer.location,newELocation);
      Point2D.Double earliestLoc = project(firer.location,headingTick,travelTick*gunDelay);
      
      Point2D.Double latestLoc = project(newELocation, firer.heading, -firer.velocity);
      
      w.fireLocation =// 
                  new Point2D.Double(0.5*(earliestLoc.x + latestLoc.x),
                  0.5*(earliestLoc.y + latestLoc.y));
         			
      w.bulletPower = deltaE;
      w.bulletVelocity = 20 - 3*deltaE;
      w.bulletDamage = Rules.getBulletDamage(deltaE);
            //create a set of bins that cover 360 degrees
      w.bins = new double[360];
      w.botShadowBins = new double[360];
      for(int i = w.botShadowBins.length - 1; i >= 0; i--)
         w.botShadowBins[i] = 1;
            //for each enemy that i have targeting data from this bot for:
   
      if(firer.targets == null)
         firer.targets = new Hashtable<String,KDTree<MeleeScan>>();
   
      w.firer = new EnemyInfo();
      w.firer.location = (Point2D.Double)firer.location.clone();
      w.firer.energy = firer.energy;
      w.firer.velocity = firer.velocity;
      w.firer.heading = firer.heading;
      w.firer.name = firer.name;
      w.firer.targets = firer.targets;
      w.firer.defaultAim = firer.defaultAim;
      
      w.calcDangers(me.location);
   
      
            
            //add wave!
      waves.add(w);
   }  
   public void logWaveHit(MeleeWave bestOverlap, Point2D.Double newELocation, EnemyInfo eInfo){
   
              //log hit
            ////- find offset from who we think was being targeted (closest bot within GF +-1)
            //increase energy of targeter bot
            
            //assume they were targeting the bot that got hit for now...
   			
      EnemyInfo firer = enemies.get(bestOverlap.firedBy);
      if(firer == null)
         firer = deadEnemies.get(bestOverlap.firedBy);
      if(firer == null)
         return;
      // if(firer.defaultAim == null )
         // firer.defaultAim = new KDTree.Manhattan(LOCATION_DIMS,new Integer(10000));
            
      double closest = Double.POSITIVE_INFINITY;
            
      Enumeration<EnemyInfo> it = bestOverlap.snapshot.elements();
      while(it.hasMoreElements()){
               
         EnemyInfo firedTargetInfo = it.nextElement();
         double cdist = firedTargetInfo.location.distance(bestOverlap.fireLocation);
         if(cdist < closest)
            closest =cdist;
      }
      double inv_closest = 1/closest;
            
            
      it = bestOverlap.snapshot.elements();
      while(it.hasMoreElements()){
               
         EnemyInfo firedTargetInfo = it.nextElement();
         double distRatio = firedTargetInfo.location.distance(bestOverlap.fireLocation)*inv_closest;
         if(distRatio > 1.2)
            continue;
               
         double fireBearing = absoluteBearing(bestOverlap.fireLocation,
                        firedTargetInfo.location);
         double hitBearing = absoluteBearing(bestOverlap.fireLocation,
                        newELocation);
                        
         double offset = Utils.normalRelativeAngle(hitBearing - fireBearing);
                  
               
         double latVel = firedTargetInfo.velocity*
                     FastTrig.sin(firedTargetInfo.heading - fireBearing);
               
         double GF = offset*Math.signum(latVel)/maxEscapeAngle(bestOverlap.bulletVelocity);
         if(GF > 1 || GF < -1)
            continue;
      
         if(firedTargetInfo.treeLocation == null){
            System.out.println("tree location is null!");
            continue;
         }
         KDTree firerTree = firer.targets.get(eInfo.name);
      
           
         if(firerTree == null ){
            firerTree = new KDTree.Manhattan(LOCATION_DIMS);
            firer.targets.put(eInfo.name,firerTree);
         }
      
      		
         MeleeScan ms = new MeleeScan();
         ms.GF = GF;
         ms.weight = Math.exp(-2*distRatio);
                  
         firerTree.addPoint(firedTargetInfo.treeLocation,ms);
            // System.out.println("adding target info on " + eInfo.name + " to " + firer.name);   
         firer.defaultAim.addPoint(firedTargetInfo.treeLocation,ms);
         
      }
               
   
      if(firer.energy - firer.lastEnergy - bestOverlap.bulletPower*3 < -0.01)
         //firer = deadEnemies.get(bestOverlap.firedBy);
         firer.energy += bestOverlap.bulletPower*3;
   
      waves.remove(bestOverlap);
      recalcWaveDangersFor(firer.name);
   
   }
   void recalcWaveDangersFor(String name){
      for(MeleeWave w : waves){
         if(w.firedBy.equals(name))
            w.calcDangers(me.location);
      }
   
   }
	
   public void logBullet(Bullet b){
   
      MeleeWave bestOverlap = getBestOverlapBy(new Point2D.Double(b.getX(), b.getY()),
         Rules.getBulletDamage(b.getPower()), b.getName());
      EnemyInfo eInfo = enemies.get(bot.getName());
      double closest = Double.POSITIVE_INFINITY;
      if(bestOverlap == null || bestOverlap.snapshot == null){
         // System.out.println("Can't find wave for bullet hit");
         return;
      }
         
      Enumeration<EnemyInfo> it = bestOverlap.snapshot.elements();
      while(it.hasMoreElements()){
               
         EnemyInfo firedTargetInfo = it.nextElement();
         double cdist = firedTargetInfo.location.distance(bestOverlap.fireLocation);
         if(cdist < closest)
            closest =cdist;
      }
      double inv_closest = 1/closest;
         
      double hitBearing = b.getHeadingRadians();//absoluteBearing(bestOverlap.fireLocation, eInfo.location);   
            
      it = bestOverlap.snapshot.elements();
      while(it.hasMoreElements()){
               
         EnemyInfo firedTargetInfo = it.nextElement();
         if(firedTargetInfo.name == bestOverlap.firedBy)
            continue;
         double distRatio = firedTargetInfo.location.distance(bestOverlap.fireLocation)*inv_closest;
         if(distRatio > 1.2)
            continue;
            
         double fireBearing = absoluteBearing(bestOverlap.fireLocation,
                        firedTargetInfo.location);
      
         double offset = Utils.normalRelativeAngle(hitBearing - fireBearing);
               
         double latVel = firedTargetInfo.velocity*FastTrig.sin(firedTargetInfo.heading - fireBearing);
               
         double GF = offset*Math.signum(latVel)/maxEscapeAngle(bestOverlap.bulletVelocity);
         if(GF > 1 || GF < -1){
            // System.out.println("hitwave out of gf range for " + firedTargetInfo.name);
            continue;
         }
         EnemyInfo firer = enemies.get(bestOverlap.firedBy);
         if(firer == null)
            firer = deadEnemies.get(bestOverlap.firedBy);
         if(firer != null ){
            if(firer.targets == null)
               firer.targets = new Hashtable<String,KDTree<MeleeScan>>();
            KDTree firerTree = firer.targets.get(eInfo.name);
            if(firerTree == null && firedTargetInfo.treeLocation != null){
               firerTree = new KDTree.Manhattan(LOCATION_DIMS);
               firer.targets.put(eInfo.name,firerTree);
            }
            if(firedTargetInfo.treeLocation == null)
               System.out.println("Fired Target Info NULL");
            MeleeScan ms = new MeleeScan();
            ms.GF = GF;
            ms.weight = Math.exp(-2*distRatio);
            if(firedTargetInfo.treeLocation != null)
               firerTree.addPoint(firedTargetInfo.treeLocation,ms);
           
            if(firer.defaultAim == null && firedTargetInfo.treeLocation != null)
               firer.defaultAim = new KDTree.Manhattan(LOCATION_DIMS);
            if(firedTargetInfo.treeLocation != null)
               firer.defaultAim.addPoint(firedTargetInfo.treeLocation,ms);
         }
         else
            System.out.println("Got wave but not firer...");
      }
      
      //System.out.println("firer: " + bestOverlap.firedBy);
      EnemyInfo firer = enemies.get(bestOverlap.firedBy);
      if(firer != null){
         if(firer.energy - firer.lastEnergy - bestOverlap.bulletPower*3 < -0.01)
         //firer = deadEnemies.get(bestOverlap.firedBy);
            firer.energy += bestOverlap.bulletPower*3;
      }
      
      waves.remove(bestOverlap);
      if(firer != null)
         recalcWaveDangersFor(firer.name);
   }
   MeleeWave getBestOverlapBy(Point2D.Double location, double bulletDamage, String name){
    
      
      MeleeWave bestOverlap = null;
      double dist = Double.POSITIVE_INFINITY;
      for(int i = 0, k = waves.size(); i < k; i++){
         MeleeWave overlap = waves.get(i);
         if(overlap.firedBy == name
         &&
         Math.abs(overlap.fireLocation.distance(location) -
               (bot.getTime() - overlap.fireTime)*overlap.bulletVelocity) 
         		< 8*overlap.bulletVelocity
               //&& sqr(overlap.bulletDamage - bulletDamage) < 0.01
               ){
               //rank overlapping waves in order of likelyhood, incorporating:
               ////- how near deltaE is to this wave's damage (mostly)
               ////- how long the wave had to travel (less is better) (weighted just as tie-breaker)
                    ////TODO: use all, and eliminate the ones whose bots do not get a hit bonus
            double thisDist =  sqr(overlap.bulletDamage - bulletDamage) + (bot.getTime() - overlap.fireTime)*0.001;
            if(thisDist < dist){
               bestOverlap = overlap;
               dist = thisDist;
            }
                  
         }
      }
      return bestOverlap;
    
   }
   MeleeWave getBestOverlapExcluding(Point2D.Double location, double bulletDamage, String excludeName, boolean allow1Fire){
    
      
      MeleeWave bestOverlap = null;
      double dist = Double.POSITIVE_INFINITY;
      for(int i = 0, k = waves.size(); i < k; i++){
         MeleeWave overlap = waves.get(i);
         if(!overlap.firedBy.equals(excludeName)
         &&
         Math.abs(overlap.fireLocation.distance(location) -
               (bot.getTime() - overlap.fireTime)*overlap.bulletVelocity) 
         		< 8*overlap.bulletVelocity
               //&& sqr(overlap.bulletDamage - bulletDamage) < 0.01
               ){
               //rank overlapping waves in order of likelyhood, incorporating:
               ////- how near deltaE is to this wave's damage (mostly)
               ////- how long the wave had to travel (less is better) (weighted just as tie-breaker)
                    ////TODO: use all, and eliminate the ones whose bots do not get a hit bonus
            double thisDist =  
               
               ((0 < (bulletDamage - overlap.bulletDamage)
               && (bulletDamage - overlap.bulletDamage) <= 3
               && allow1Fire)
               ?0:Math.abs(bulletDamage - overlap.bulletDamage))
               
               + (bot.getTime() - overlap.fireTime)*0.01;
            if(thisDist < dist){
               bestOverlap = overlap;
               dist = thisDist;
            }
                  
         }
      }
      return bestOverlap;
    
   }

   public static void smoothAround(double[] bins, int index, int width, double weight){
    
      int minIndex = (index - 2*width + 2*bins.length)%bins.length, maxIndex = (index + 2*width)%bins.length;
      double invWidth = 1.0/width;
   
      if(minIndex > index){
         for(int i = minIndex; i < bins.length ; i++)
            bins[i] += weight/(sqr(i - bins.length - index)*invWidth + 1);
               
         for(int i = 0; i < index; i++)
            bins[i] += weight/(sqr(i - index)*invWidth + 1);
      }
      else
         for(int i = minIndex; i < index; i++)
            bins[i] += weight/(sqr(i - index)*invWidth + 1);
               
      if(maxIndex < index){
         for(int i = index; i < bins.length; i++)
            bins[i] += weight/(sqr(i - index)*invWidth + 1);
         
         for(int i = 0; i <= maxIndex; i++)
            bins[i] += weight/(sqr(i + bins.length - index)*invWidth + 1);
      }
      else
         for(int i = index; i <= maxIndex; i++)
            bins[i] += weight/(sqr(i - index)*invWidth + 1);
     
      
   	
    
      // bins[index] += weight;
      // double invWidth = 1.0/width;
   //       
      // int oppIndex = index + bins.length>>1;
      // if(oppIndex > bins.length){
      // 
         // for(int i = oppIndex - bins.length; i >= 0; i--)
            // bins[i] += weight/(sqr(i + bins.length - index)*invWidth + 1);
      //       
         // for(int i = oppIndex- bins.length; i < bins.length; i++)
            // bins[i] += weight/(sqr(i - index)*invWidth + 1);
      //      
      // }
      // else{
      // 
         // for(int i = oppIndex; i >= 0; i--)
            // bins[i] += weight/(sqr(i - index)*invWidth + 1);
      //       
         // for(int i = oppIndex; i < bins.length; i++)
            // bins[i] += weight/(sqr(i - bins.length - index)*invWidth + 1);
      //       
      // 
      // }
   }
   public static int sqr(int i){
      return i*i;}
   public static double sqr(double d){
      return d*d;}

   static class PredictionStatus{  
      double  finalHeading, finalVelocity, distanceRemaining;
      long time;
      Point2D.Double endPoint;
      boolean debug;
   }
      	//3 optimized methods from the new robocode engine
   private static double getNewVelocity(double velocity, double distance) {
      final double goalVel = Math.min(getMaxVelocity(distance), 8);
   
      if(velocity >= 0)
         return limit(velocity - 2,
            goalVel, velocity + 1);
     
      return limit(velocity - 1,
         goalVel, velocity + maxDecel(-velocity));
   }

   final static double getMaxVelocity(double distance) {
      final double decelTime =  Math.max(1,Math.ceil(
         //sum of 0... decelTime, solving for decelTime 
         //using quadratic formula, then simplified a lot
         Math.sqrt(distance + 1) - 0.5));
   
      final double decelDist = (decelTime) * (decelTime-1) ;
         // sum of 0..(decelTime-1)
         // * Rules.DECELERATION*0.5;
   
      return ((decelTime - 1) * 2) + ((distance - decelDist) / decelTime);
   }
 
   private static final double maxDecel(double speed) {
      return limit(1,speed*0.5 + 1, 2);
   }
   public static ArrayList<Point2D.Double> futureStatus(Point2D.Double fromLocation, Point2D.Double toLocation, double initialVelocity, double initialHeading){
      ArrayList<Point2D.Double> path = new ArrayList<Point2D.Double>();
      double bearing = absoluteBearing(fromLocation,toLocation);
      double velocity = initialVelocity;
      double distanceRemaining = fromLocation.distance(toLocation);;
      //long time = currentTime - wave.fireTime;
      double heading = initialHeading;
      
      Point2D.Double endPoint = (Point2D.Double)fromLocation.clone();  
      int counter = 91;//5 + (int)Math.ceil(endPoint.distance(wave.fireLocation)/(wave.bulletVelocity-8)) - time;;
      double sinVal = 0, cosVal=0;
      boolean inline = false;
      do{
      
         if(!inline & (distanceRemaining > 1 | Math.abs(velocity) > 0.1))
         {
            double maxTurn = Math.PI/18 - Math.PI/240*Math.abs(velocity);
            bearing = absoluteBearing(endPoint,toLocation);
            double offset = FastTrig.normalRelativeAngle(bearing - heading);
            if(-Math.PI/2 > offset | offset > Math.PI/2){
               offset = FastTrig.normalRelativeAngle(offset + Math.PI);
               velocity = -velocity;
               heading += Math.PI;
            }
            offset = limit(-maxTurn,offset,maxTurn);
            heading += offset;
            sinVal = FastTrig.sin(heading);
            cosVal = FastTrig.cos(heading);
            if(-0.0001 < offset & offset < 0.0001)
               inline = true;
         }
         
      
      // for some reason old way works better? Or maybe not now that it is on the correct tick...
         // velocity = getNewVelocity(velocity, distanceRemaining);
         	
         if(velocity >= 0 & distanceRemaining >= decelDistance(velocity))
            velocity = Math.min(velocity + 1, 8);
         else
            velocity = limit(-1.9999999999, Math.abs(velocity) - Math.min(Math.max(Math.abs(velocity),distanceRemaining),2), 6)*(velocity<0?-1:1);
      		
         endPoint.x += sinVal*velocity;
         endPoint.y += cosVal*velocity;
         
         if(endPoint.x < W
         | endPoint.x > E
         | endPoint.y < S
         | endPoint.y > N){
         
            velocity = 0;
         
         }
         path.add((Point2D.Double)endPoint.clone());
         
         if(velocity > distanceRemaining)
            inline = false;
         if(inline)
            distanceRemaining = Math.abs(distanceRemaining - velocity);
         else
            distanceRemaining = endPoint.distance(toLocation);
                
      } while( (Math.abs(distanceRemaining) > 0.1 || Math.abs(velocity) > 0.1) & --counter != 0 );
      
      return path;
   }   
   public static boolean oldMethodCheckFast(Point2D.Double fromLocation, Point2D.Double toLocation, double initialVelocity, double initialHeading){
   
      double wantedHeading = absoluteBearing(fromLocation,toLocation);
      double velocity = initialVelocity;
      double distanceRemaining = fromLocation.distance(toLocation);;
      double theta = Utils.normalRelativeAngle(wantedHeading - initialHeading);
      double offsetSign = Math.signum(theta);
      theta = Math.abs(theta);
   	
      PredictionStatus status = new PredictionStatus();
      status.finalHeading = initialHeading;
      
      if(theta > Math.PI/2){
         theta = Math.PI - theta;
         velocity = -velocity;
         offsetSign = -offsetSign;
         status.finalHeading = Utils.normalAbsoluteAngle(initialHeading + Math.PI);
      }
      
      
      do{
         double deltaHeading = (theta - (theta = Math.max(0,theta - (Math.PI/18) + (Math.PI/240)*Math.abs(velocity))))*offsetSign;
         status.finalHeading += deltaHeading;
      // velocity = getNewVelocity(velocity, distanceRemaining);
      
      // 	/*
         if(velocity >= 0 && distanceRemaining >= decelDistance(velocity))
            velocity = Math.min(velocity + 1, 8);
         else
            velocity = limit(-1.9999999999, Math.abs(velocity) - Math.min(Math.max(Math.abs(velocity),distanceRemaining),2), 6)*(velocity<0?-1:1);
      //    */
         if(velocity == 0.0 && theta != 0.0)  
            return true; 
         if(theta == 0){
            return false;
            //distanceRemaining -= velocity;
         }
         else{ //rule of cosines
            double oldDistRemSq = distanceRemaining*distanceRemaining;
            double distRemSq = velocity*velocity + oldDistRemSq - 2*velocity*distanceRemaining*FastTrig.cos(theta);
            if(distRemSq <= 0.1)
               distanceRemaining = 0;
            else{
               distanceRemaining = Math.sqrt(distRemSq);
               
               if(velocity == 0.0){
                  //System.out.println("exiting due to velocity");
                  return true;}
               
               double acosVal = (velocity*velocity + distRemSq - oldDistRemSq)/(2*velocity*distanceRemaining);
               
               if(Double.isNaN(acosVal)){
                  //System.out.println("exiting due to acosVal");
                  return true;
               }   
               
               if(acosVal < -1)
                  acosVal = -1;
               else if(acosVal > 1)
                  acosVal = 1;
               theta =  Math.PI - Math.acos(acosVal);
               if(theta > Math.PI/2){//in case of overshoot
                  theta = Math.PI - theta;  
                  velocity = -velocity;
                  offsetSign = -offsetSign;
                  status.finalHeading += Math.PI;
                     
               }
            }
         }
         
        
      
         	
      }while(!(distanceRemaining == 0.0 && velocity == 0));
      
   
      
      return Double.isNaN(theta);
         // System.out.println("waveBearing: " + waveBearing + "   waveCenterDist: " + waveCenterDist);
      
      
   }  
   public static boolean oldMethodCheck(Point2D.Double fromLocation, Point2D.Double toLocation, double initialVelocity, double initialHeading, long currentTime, MeleeWave wave){
   
      double wantedHeading = absoluteBearing(fromLocation,toLocation);
      double velocity = initialVelocity;
      double distanceRemaining = fromLocation.distance(toLocation);;
      long time = currentTime;
      double theta = Utils.normalRelativeAngle(wantedHeading - initialHeading);
      double offsetSign = Math.signum(theta);
      theta = Math.abs(theta);
   	
      PredictionStatus status = new PredictionStatus();
      status.finalHeading = initialHeading;
      
      if(theta > Math.PI/2){
         theta = Math.PI - theta;
         velocity = -velocity;
         offsetSign = -offsetSign;
         status.finalHeading = Utils.normalAbsoluteAngle(initialHeading + Math.PI);
      }
      
      double waveCenterDist = wave.fireLocation.distance(fromLocation);
      double waveBearing =  absoluteBearing(fromLocation,wave.fireLocation);
      double waveOffset = Utils.normalAbsoluteAngle(waveBearing - status.finalHeading);
      double waveOffsetSign = 1;
      if(waveOffset > Math.PI){
         waveOffset = 2*Math.PI - waveOffset;
         waveOffsetSign = -waveOffsetSign;  
      }
      
      do{
         double deltaHeading = (theta - (theta = Math.max(0,theta - (Math.PI/18) + (Math.PI/240)*Math.abs(velocity))))*offsetSign;
         status.finalHeading += deltaHeading;
         waveOffset -= deltaHeading*waveOffsetSign;
         if(waveOffset > Math.PI){
            waveOffset = 2*Math.PI - waveOffset;
            waveOffsetSign = -waveOffsetSign;  
         }
         else if(waveOffset < 0){
            waveOffset = -waveOffset;
            waveOffsetSign = -waveOffsetSign;
         
         }
         // velocity = getNewVelocity(velocity, distanceRemaining);
      
      // 	/*
         if(velocity >= 0 && distanceRemaining >= decelDistance(velocity))
            velocity = Math.min(velocity + 1, 8);
         else
            velocity = limit(-1.9999999999, Math.abs(velocity) - Math.min(Math.max(Math.abs(velocity),distanceRemaining),2), 6)*(velocity<0?-1:1);
      //    */
         //if(velocity != 0.0)   
         if(theta == 0)
            distanceRemaining -= velocity;
         else{ //rule of cosines
            double oldDistRemSq = distanceRemaining*distanceRemaining;
            double distRemSq = velocity*velocity + oldDistRemSq - 2*velocity*distanceRemaining*FastTrig.cos(theta);
            if(distRemSq <= 0.1)
               distanceRemaining = 0;
            else{
               distanceRemaining = Math.sqrt(distRemSq);
               
               if(velocity == 0.0){
                  //System.out.println("exiting due to velocity");
                  return true;}
               
               double acosVal = (velocity*velocity + distRemSq - oldDistRemSq)/(2*velocity*distanceRemaining);
               
               if(Double.isNaN(acosVal)){
                  //System.out.println("exiting due to acosVal");
                  return true;
               }   
               
               if(acosVal < -1)
                  acosVal = -1;
               else if(acosVal > 1)
                  acosVal = 1;
               theta =  Math.PI - Math.acos(acosVal);
               if(theta > Math.PI/2){//in case of overshoot
                  theta = Math.PI - theta;  
                  velocity = -velocity;
                  offsetSign = -offsetSign;
                  status.finalHeading += Math.PI;
                  waveOffset = Math.PI - waveOffset;
                  waveOffsetSign = -waveOffsetSign;
                  
               }
            }
         }
         
         if(velocity > 0.01 || velocity < -0.01){
            
            double newWaveDSq = (velocity*velocity + waveCenterDist*waveCenterDist -
                  2*velocity*waveCenterDist*Math.cos(waveOffset));
                  
            if(Double.isNaN(waveOffset)){
               //System.out.println("exiting due to waveOffset");
               return true;
            }   
            double newWaveD = Math.sqrt(newWaveDSq);
         
            
            double acosVal =  (waveCenterDist*waveCenterDist 
                  - velocity*velocity - newWaveDSq)/(2*velocity*newWaveD);
            
            if(acosVal < -1)
               acosVal = -1;
            else if(acosVal > 1)
               acosVal = 1;
         
            
            double newWaveOffset = Math.acos(acosVal);
           
            double alpha = newWaveOffset - waveOffset;
                
            waveBearing += alpha*waveOffsetSign;
         		
         		
            waveOffset = newWaveOffset;
            waveCenterDist = newWaveD;
         }
         if(Double.isNaN(waveBearing)){
            //System.out.println("exiting due to waveBearing");
            return true;
         }   
         if(Double.isNaN(waveCenterDist)){
            //System.out.println("exiting due to waveCenterDist");
            return true;
         }
      
         time++;
         	
      }while(wave.bulletVelocity*(time + 1 - wave.fireTime) < waveCenterDist
         && !(distanceRemaining == 0.0 && velocity == 0));
      
      time = (long)(waveCenterDist/wave.bulletVelocity) + wave.fireTime - 1;
   
      
      Point2D.Double endPoint = project(wave.fireLocation, waveBearing, -waveCenterDist);
      return Double.isNaN(endPoint.x);
         // System.out.println("waveBearing: " + waveBearing + "   waveCenterDist: " + waveCenterDist);
      
      
   }  

   public static Point2D.Double futureStatus(ArrayList<Point2D.Double> path, long currentTime, MeleeWave wave){
   
      long time = currentTime - wave.fireTime+1;
      int i = 0;
      int max = path.size();
      Point2D.Double endPoint;
      do{
         endPoint = path.get(i++);
         time++;
                   
      } while(endPoint.distanceSq(wave.fireLocation) > sqr(wave.bulletVelocity*(time)) & i < max);
      
      return endPoint;
   }
   // CREDIT: from CassiusClay, by PEZ
   //   - returns point length away from sourceLocation, at angle
   // robowiki.net?CassiusClay
   public static Point2D.Double project(Point2D.Double sourceLocation, double angle, double length) {
      return new Point2D.Double(sourceLocation.x + FastTrig.sin(angle) * length,
            sourceLocation.y + FastTrig.cos(angle) * length);
   }
   
   // got this from RaikoMicro, by Jamougha, but I think it's used by many authors
   //  - returns the absolute angle (in radians) from source to target points
   public static double absoluteBearing(Point2D.Double source, Point2D.Double target) {
      return FastTrig.atan2(target.x - source.x, target.y - source.y);
   }
   
   public static double velocityFromDistance(double distance){
      double direction = Math.signum(distance);
      distance = Math.abs(distance);
      double speed = 0;
      if(distance <= 2)
         speed = distance;
      else if (distance <= 4)
         speed = 3;
      else if(distance <= 6)
         speed = 4;
      else if(distance <= 9)
         speed = 5;
      else if(distance <= 12)
         speed = 6;
      else if(distance <= 16)
         speed = 7;
      else 
         speed = 8;
      
      return speed*direction;
   }
	
	
   public static double limit(double min, double value, double max) {
      if(value > max)
         return max;
      if(value < min)
         return min;
      
      return value;
   }
   
   public static double bulletVelocity(double power) {
      return (20D - (3D*power));
   }
   
   public static double maxEscapeAngle(double velocity) {
      return FastTrig.asin(8.0/velocity);
   }
   
   static double rollingAverage(double value, double newEntry, double depth, double weighting ) {
      return (value * depth + newEntry * weighting)/(depth + weighting);
   } 

   public static int getIndex(double[] slices, double value){
      int index = 0;
      while(index < slices.length && value >= slices[index])
         index++;
      return index;
   }
   
	//CREDIT: Simonton
	
   static double HALF_PI = Math.PI / 2;
   static double WALL_MARGIN = 18;
   static double S = WALL_MARGIN;
   static double W = WALL_MARGIN;
   static double N = 600 - WALL_MARGIN;
   static double E = 800 - WALL_MARGIN;

 // eDist  = the distance from you to the enemy
 // eAngle = the absolute angle from you to the enemy
 // oDir   =  1 for the clockwise orbit distance
 //          -1 for the counter-clockwise orbit distance
 // returns: the positive orbital distance (in radians) the enemy can travel
 //          before hitting a wall (possibly infinity).
   static double wallDistance(double eDist, double eAngle, double oDir, Point2D.Double fireLocation) {
    
      return Math.min(Math.min(Math.min(
         distanceWest(N - fireLocation.y, eDist, eAngle - HALF_PI, oDir),
         distanceWest(E - fireLocation.x, eDist, eAngle + Math.PI, oDir)),
         distanceWest(fireLocation.y - S, eDist, eAngle + HALF_PI, oDir)),
         distanceWest(fireLocation.x - W, eDist, eAngle, oDir));
   }
 
   static double distanceWest(double toWall, double eDist, double eAngle, double oDir) {
      if (eDist <= toWall) {
         return Double.POSITIVE_INFINITY;
      }
      double wallAngle = FastTrig.acos(-oDir * toWall / eDist) + oDir * HALF_PI;
      return Utils.normalAbsoluteAngle(oDir * (wallAngle - eAngle));
   }
   

   
   static double decelDistance(double vel){
   
      int intVel = (int)Math.ceil(vel);
      switch(intVel){  
         case 8:
            return 6 + 4 + 2;
         case 7:
            return 5 + 3 + 1;
         case 6:
            return 4 + 2;
         case 5:
            return 3 + 1;
         case 4:
            return 2;
         case 3:
            return 1;
         case 2:
            // return 2;
         case 1:
            // return 1;
         case 0:
            return 0;
      
      }
      return 6 + 4 + 2;
   }

}
