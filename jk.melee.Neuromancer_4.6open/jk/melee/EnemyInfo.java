package jk.melee;
 
import java.awt.geom.*;
import java.util.*;
import jk.mega.KDTree;
public class EnemyInfo{
   String name;
   double heading, velocity;
   int lastScanTime;
   int lastHitByTime;//, lastHitIndex, offsetIndex;
   double energy, lastEnergy, gunHeat;
   Point2D.Double location;
   
   Hashtable<String,KDTree<MeleeScan>> targets;
   KDTree<MeleeScan> defaultAim;
   
   double[] treeLocation;
}