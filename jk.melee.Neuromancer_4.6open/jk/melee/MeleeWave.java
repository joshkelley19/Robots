package jk.melee;
import java.awt.geom.*;
import java.util.*;
import jk.mega.FastTrig;
import jk.mega.KDTree;
import robocode.util.*;
 
public class MeleeWave{
 
   double waveWeight;
 
   String firedBy;
   EnemyInfo firer;
 
   Point2D.Double fireLocation;

   Hashtable<String,EnemyInfo> snapshot;
   
   long fireTime;
	
   double bulletPower, bulletVelocity, bulletDamage;
	
   double[] bins;
   double[] botShadowBins;
   
   boolean surfable;
// 	double[] bulletShadowBins;

   boolean checkForBonus = false;

   public void calcDangers(Point2D.Double myLocation){
      surfable = false;
      Enumeration<EnemyInfo> en = snapshot.elements();               
      while(en.hasMoreElements()){
         EnemyInfo target = en.nextElement();
         KDTree<MeleeScan> tree = firer.targets.get(target.name);
                  //// - weight top 3 scans by inverse distance from 'actual scan', 
                  ////    based on what they are doing from his perspective, 
                  ////    weight each bot by inverse distance-to-him squared
                  
         if(target.name.equals(firer.name))
            continue;
               	
         double targetBearing = MeleeSurf.absoluteBearing(fireLocation,target.location);
         double latVel = target.velocity*FastTrig.sin(target.heading - targetBearing);
         double advVel = target.velocity*FastTrig.cos(target.heading - targetBearing);
         double distToE = target.location.distance(fireLocation);
         double meBearing = MeleeSurf.absoluteBearing(fireLocation,myLocation);
                  
         int k = 0;
         if(tree == null)
            if(firer.defaultAim != null){
               tree = firer.defaultAim;
               k = 1;     
            }
            else{
               tree = MeleeSurf.GF_0_tree;
               k = 1;     
            }
                           
         double MEA = MeleeSurf.maxEscapeAngle(bulletVelocity);
         
         if(Math.abs(Utils.normalRelativeAngle(targetBearing - meBearing)) > 3*MEA)
            continue;
                           
         double GFcorrection = Math.signum(latVel)*MEA;
                  
         double botWeight = 1/(distToE*distToE);//+10*target.energy);
         double[] bins = new double[360];
                  
         do{
            double[] treeLoc = target.treeLocation;
            if(tree == null){
               System.out.println("tree null, k = " + k);
            
            }
         				
            List<KDTree.SearchResult<MeleeScan>> cluster = tree.nearestNeighbours(
                           treeLoc,
                           Math.min(10,tree.size())
                           );
                     
                     
            Iterator<KDTree.SearchResult<MeleeScan>> it = cluster.iterator();
            double weight = botWeight*Math.exp(-k);      
            while(it.hasNext()){
               KDTree.SearchResult<MeleeScan> v = it.next();
               double fireAngle = Utils.normalAbsoluteAngle
                              (v.payload.GF*GFcorrection + targetBearing);
                        
                        // w.bins[((int)(0.5 + fireAngle*(180/Math.PI)))%360] +=botWeight/(1e-5 + v.distance);
               if(Math.abs(Utils.normalRelativeAngle(fireAngle - meBearing)) < 2*MEA){
                  MeleeSurf.smoothAround(bins,((int)(fireAngle*(180/Math.PI)))%360,
                              18,v.payload.weight*weight/(1e-15 + v.distance));
                  surfable = true;              
               }
            }
            k++;
                     
                     // botWeight = botWeight*Math.exp(-tree.size());
            tree = firer.defaultAim;
                     
         }while(k == 1);
                  
                  
         MeleeSurf.areaNormalize(bins);
         for(int i = 0; i < bins.length; i++)
            this.bins[i] += botWeight*bins[i];
               	
                  //// - smooth these into the 360* buffer
      }
              
      MeleeSurf.areaNormalize(this.bins);
   }

}