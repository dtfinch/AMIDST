
package amidst;

import java.util.Random;
import java.security.SecureRandom;
import java.lang.Math;
import java.io.PrintStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.awt.Point;

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

    public SeedTester() {
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
        while(count-- > 0) {
            testSeed(rnd.nextLong());
            
            display();
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
    }
    
    /* unused now
    private boolean hasAnyBiome(int[] types) {
        for(int i=0; i<types.length; i++) {
            int type = types[i];
            if(foundBiomes[type]>0 || foundBiomes[type|128]>0) { //also check for variant
                return true;
            }
        }
        return false;
    }*/

    private void checkSpecialBiome(boolean require, int... types) {
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
        } else if(require) {
            qualified = false;
        }
    }
    
    public void testChunks(int midX, int midY, int range, int minRange, boolean refine) {
        int limit = range*(range+1); //square of distance for circular range. lazy integer approximation of (range+0.5)**2
        int minLimit = minRange*(minRange+1);
        
        for(int dy = -range; dy<=range; dy++) {
            int y = dy+midY;
            for(int dx = -range; dx<=range; dx++) {
                int sqDist = dx*dx+dy*dy;
                if(sqDist>limit || sqDist<minLimit) continue; //range is circular, donut
                
                int x = dx+midX;
                
                if(!refine) { //don't recount on refine pass
                
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
                
                int spacing;
                boolean checkBiome;

                checkBiome = (y&1)==0 && ((x+y)&3)==0; //check 1/8th of chunks. diagonal grid pattern.
                /*
                #   #   #
                
                  #   #   #
                  
                */
                
                if(refine) checkBiome = !checkBiome; //if refining (qualified seeds) check all the chunks that weren't examined on the first pass
                
                if(checkBiome) { 
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
            if(dy%10==0) {
                doSleep();
            }
        }
    }

    public void runTest(int midX, int midY, int range, int minRange) {
        testChunks(midX, midY, range, minRange, false);
        
        checkSpecialBiome(true, 14, 15); //mushroom, for mycelium
        checkSpecialBiome(true, 21, 22, 23); //jungle, for cookies and cats
        checkSpecialBiome(true, 32, 33); //mega taiga, for podzol
        checkSpecialBiome(true, 37, 38, 39); //mesa, for colored sand
        
        //if any of the above don't match, checkSpecialBiome sets qualified=false
        
        // With range=125, about 1 in 550 qualify by containing the above 4 biome types
        if(qualified) testChunks(midX, midY, range, minRange, true); //expensive
        
        //the following biomes are nice to have, but don't disqualify a seed
        
        checkSpecialBiome(false, 6); //swampland, for lily pads
        checkSpecialBiome(false, 132); //flower forest, for dyes
        checkSpecialBiome(false, 140); //ice spikes, for packed ice
        checkSpecialBiome(false, 35, 36); //savanna, for diagonal trees and (also on plains) horses

        checkSpecialBiome(false, 24); //deep ocean, for possible ocean monument
        checkSpecialBiome(false, 2, 17); //desert, for cactus
        
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
