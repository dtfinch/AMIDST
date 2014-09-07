
package amidst;

import java.util.Random;
import java.security.SecureRandom;
import java.lang.Math;
import java.io.PrintStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.awt.Point;

import amidst.logging.Log;
import amidst.Options;
import amidst.map.layers.*;
import amidst.minecraft.MinecraftUtil;


public class SeedTester {
    public int mines;
    public int fortresses;
    public int villages;
    public int temples;
    public int monuments;
    public int biomes;
    public int biomeScore;
    public int specialBiomes; //those with unique resources
    public int specialScore;
    public boolean qualified; //has all the rarest biomes
    public long score;
    public Point spawn;
    
    
    public int[] foundBiomes;
    public int[] biomeScores;
    
    
    private MineshaftLayer mineLayer;
    private NetherFortressLayer fortressLayer;
    private VillageLayer villageLayer;
    private TempleLayer templeLayer;
    private SpawnLayer spawnLayer;
    private OceanMonumentLayer monumentLayer;
    
    private PrintStream seedFile;
    
    private int numSkipped, numChecked, numQualified;

    public SeedTester() {
        Log.isShowingDebug = false; //block extra messages from world generation and spawn finding
        
        if(Options.instance.pruneSeeds) {
            Log.i("Will skip low quality seeds for better performance.");
        } else {
            Log.i("Will slowly examine all seeds. Use -pruneseeds to skip low quality seeds for performance.");
        }
    
        mineLayer = new MineshaftLayer();
        fortressLayer = new NetherFortressLayer();
        villageLayer = new VillageLayer();
        templeLayer = new TempleLayer();
        spawnLayer = new SpawnLayer();
        monumentLayer = new OceanMonumentLayer();
        
        //TODO support supplying a list of seeds to retest (like a text file of seed per line, ignoring stuff after tabs)
        //TODO output detailed information on "qualified" seeds (like per-biome distances) to support future querying
        
        
        String path = Options.instance.historyPath;
        if(path!=null && !path.isEmpty()) {
            try {
                seedFile = new PrintStream(new FileOutputStream(path, true)); // append
            } catch(FileNotFoundException e) { //forced to catch this
                //output directory probably missing
                System.err.println("Could not open "+path+" for writing. Maybe directory missing.\n"+e.toString());
                return;
            }
        } else {
            seedFile = System.out;
        }
        
        seedFile.println("seed\tmines\tfortresses\tvillages\ttemples\tmonuments\tbiomes\tspecialBiomes\tbiomeScore\tspecialScore\tscore\tqualified");
    
    
        Random rnd = new SecureRandom();
        int count = Options.instance.trySeeds;
        for(int i=0; i<count; i++) {
            testSeed(rnd.nextLong());
            
            if(qualified || score>0) {
                numChecked++;
                if(qualified) numQualified++;
                display();
                
                if(Options.instance.pruneSeeds || (i%20)==0) {
                    Log.i("Total: "+(numChecked+numSkipped)+"  Pruned: "+numSkipped+"  Checked: "+numChecked+"  Qualified: "+numQualified);
                }
            } else {
                numSkipped++;
            }
        }
        if(seedFile!=System.out) seedFile.close();
    }
    
    public void testSeed(long seed) {
        doSleep();
        Options.instance.seed = seed;
        MinecraftUtil.createWorld(Options.instance.seed, "default");
        doSleep();        

        reset();
        
        spawn = spawnLayer.getSpawnPosition(); //occasionally fails and returns 0,0. Don't know what minecraft itself does in these situations.
        
        runTest(spawn.x/16, spawn.y/16, 125, 0);
    }


    public void reset() {
        mines = 0;
        fortresses = 0;
        villages = 0;
        temples = 0;
        monuments = 0;
        biomes = 0;
        specialBiomes = 0;
        biomeScore = 0;
        specialScore = 0;
        foundBiomes = new int[256];
        biomeScores = new int[256];
        qualified = true;
        score = 0;
    }
    
    /* unused now, but maybe handy later
    private boolean hasAnyBiome(int[] types) {
        for(int i=0; i<types.length; i++) {
            int type = types[i];
            if(foundBiomes[type]>0 || foundBiomes[type|128]>0) { //also check for variant
                return true;
            }
        }
        return false;
    }*/

    private boolean checkSpecialBiome(int... types) {
        int bestScore=0;
        boolean found=false;
        for(int i=0; i<types.length; i++) {
            int type = types[i];
            if(foundBiomes[type]>0 || foundBiomes[type|128]>0) { //also check for variant
                found = true;
                int score = biomeScores[type];
                if(score>bestScore) bestScore=score;
                score = biomeScores[type|128];
                if(score>bestScore) bestScore=score;
            }
        }
        if(found) {
            specialBiomes++;
            specialScore+=bestScore;
            return true;
        }
        return false;
    }
    
    public void testChunks(int midX, int midY, int range, int minRange, int refine) {
        int limit = range*(range+1); //square of distance for circular range. lazy integer approximation of (range+0.5)**2
        int minLimit = minRange*(minRange+1);
        
        int step = 1;
        if(refine==0) step=15; //1 in 225
        else if(refine==1) step=7; //1 in 49
        // refine 3: check structures on all chunks, but biomes on 1/8th
        // refine 4: check no structures (already checked), but check other 7/8th of biomes
        
        for(int dy = -range; dy<=range; dy+=step) {
            int y = dy+midY;
            for(int dx = -range; dx<=range; dx+=step) { //todo stagger by half of step
                int sqDist = dx*dx+dy*dy;
                if(sqDist>limit || sqDist<minLimit) continue; //range is circular, donut
                
                int x = dx+midX;
                
                if(refine==2) { //only count on refine pass
                
                    // TODO we probably don't need to count mines and fortresses, since density is pretty uniform.
                
                    //count structures
                    if(mineLayer.checkChunk(x, y)) {
                        mines++; //is this accurate? The count is very high.
                    }
                    if(fortressLayer.checkChunk(x, y)) {
                        fortresses++; //nether
                    }
                    if(villageLayer.checkChunk(x, y)) {
                        villages++;
                    }
                    if(templeLayer.checkChunk(x, y)) {
                        //TODO distinguish witchhuts from temples
                        temples++;
                    }
                    if(monumentLayer.checkChunk(x, y)) {
                        monuments++;
                    }
                    //TODO use structure distance in score
                }
                
                boolean check;
                if(refine<2) {
                    check=true;
                } else {
                    check = (x&1)==0 && ((x+y)&3)==0; //1/8th in a staggered grid pattern
                    if(refine==3) check = !check; //the remaining 7/8ths
                }
                
                if(check) {

                    //getBiomeData seems to have a 4x4 resolution, even though anvil files store the full resolution.
                    int biome = MinecraftUtil.getBiomeData(x*4+2, y*4+2, 1, 1)[0]; // *4 because 16/4==4. +2 to get center sample (probably doesn't matter)
     
                    if(foundBiomes[biome]++ == 0) {
                        biomes++;
                    }
                    
                    //also track how close it is
                    int dist = (int)Math.round(Math.sqrt(sqDist));
                    int score = range+1-dist;
                    if(score>biomeScores[biome]) biomeScores[biome]=score;
                }
            }
            if(refine>=2 && dy%10==0) {
                doSleep();
            }
        }
    }
    
    private void checkRequiredBiomes() {
        specialBiomes = specialScore = 0; //reset because this may be called multiple times, before the non-required ones are checked
        checkSpecialBiome(14, 15); //mushroom, for mycelium
        checkSpecialBiome(21, 22, 23); //jungle, for cookies and cats
        checkSpecialBiome(32, 33); //mega taiga, for podzol
        checkSpecialBiome(37, 38, 39); //mesa, for colored sand
    }

    public void runTest(int midX, int midY, int range, int minRange) {
        if(Options.instance.pruneSeeds) {
            qualified = false;
            
            // fastest test, requiring 2 of 4 biomes
            testChunks(midX, midY, range, minRange, 0); 
            checkRequiredBiomes(); //updates specialBiomes count
            if(specialBiomes<2) return;
            
            // a little slower, requiring 3 of 4 to proceed
            testChunks(midX, midY, range, minRange, 1);
            checkRequiredBiomes();
            if(specialBiomes<3) return;   
        }
        
        //normal scan. check structures in all chunks, but biomes in only 1/8 of chunks.
        testChunks(midX, midY, range, minRange, 2);
        
        checkRequiredBiomes();
        
        qualified = (specialBiomes>=4);
        
        if(qualified) {
            //refine further for more precise biome stats
            testChunks(midX, midY, range, minRange, 3); //expensive biome test, checking all chunks

            checkRequiredBiomes();
        
        }
      
        
        //the following biomes are nice to have, but don't disqualify a seed
        
        checkSpecialBiome(6); //swampland, for lily pads
        checkSpecialBiome(132); //flower forest, for dyes
        checkSpecialBiome(140); //ice spikes, for packed ice
        checkSpecialBiome(35, 36); //savanna, for diagonal trees and (also on plains) horses

        checkSpecialBiome(24); //deep ocean, for possible ocean monument
        checkSpecialBiome(2, 17); //desert, for cactus
        
        // of seeds that "qualify", about 1 in 12 contain all the above biomes as well.
        
        
        for(int i=0; i<256;i++) biomeScore += biomeScores[i];
        
        //arbitrary scoring
        score = mines
            +fortresses
            +10*villages
            +5*temples
            +7*monuments // increase when more reliable
            +5*biomes
            +20*specialBiomes
            +specialScore/5
            +biomeScore/50
            +(qualified?50:0);
        
        
    }


    public void display() {

        seedFile.println(Options.instance.seed
            +"\t"+mines
            +"\t"+fortresses
            +"\t"+villages
            +"\t"+temples
            +"\t"+monuments // overestimated at the moment
            +"\t"+biomes
            +"\t"+specialBiomes
            +"\t"+biomeScore
            +"\t"+specialScore
            +"\t"+score
            +"\t"+qualified
        );
        
    }
    
    private long lastSleep=0;
    private void doSleep() {
        //don't want to set off alarms using 100% on a VPS
        //or cause my old mac mini to catch fire
        
        int sleep_percent = Options.instance.sleepPct;
        
        if(sleep_percent<=0 || sleep_percent>=100) return;
    
        long now = System.currentTimeMillis();
        long delay = (now - lastSleep)*sleep_percent/(100-sleep_percent); 
        
        if(lastSleep==0 || delay<0) delay = 100; // first-run or time travel
        else if(delay>5000) delay = 5000; //hiccup
        else if(delay<20) return; //put it off until later
        
        try {
            Thread.sleep(delay);
        } catch (InterruptedException ex) {
            //I hate java sometimes
        }
        lastSleep = now + delay;
    }

}
