package amidst;

import amidst.preferences.BiomeColorProfile;
import amidst.preferences.BooleanPrefModel;
import amidst.preferences.FilePrefModel;
import amidst.preferences.SelectPrefModel;
import amidst.preferences.StringPreference;

import java.io.File;
import java.util.prefs.Preferences;

import org.kohsuke.args4j.Option;

/** Currently selected options that change AMIDST’s behavior
 */
public enum Options {
	instance;

	//per-run preferences. TODO: store elsewhere?
	public long seed;
	public String seedText;

	//permanent preferences
	public final FilePrefModel jar;
	public final BooleanPrefModel showSlimeChunks;
	public final BooleanPrefModel showGrid;
	public final BooleanPrefModel showNetherFortresses;
	public final BooleanPrefModel showMineshafts;
	public final BooleanPrefModel showTemples, showPlayers, showStrongholds, showVillages, showOceanMonuments, showSpawn;

	public final BooleanPrefModel mapFlicking, mapFading, showFPS, showDebug;
	public final BooleanPrefModel updateToUnstable;
	public final BooleanPrefModel maxZoom;

	public final StringPreference lastProfile;

	public final SelectPrefModel worldType;
	public BiomeColorProfile biomeColorProfile;
	private Preferences preferences;

	//CLI
	@Option (name="-history", usage="Sets the path to seed history file.", metaVar="<file>")
	public String historyPath;

	@Option (name="-log", usage="Sets the path to logging file.", metaVar="<file>")
	public String logPath;

	@Option (name="-mcpath", usage="Sets the path to the .minecraft directory.", metaVar="<path>")
	public String minecraftPath;

	@Option (name="-mcjar", usage="Sets the path to the minecraft .jar", metaVar="<path>")
	public String minecraftJar;

	@Option (name="-mcjson", usage="Sets the path to the minecraft .json", metaVar="<path>")
	public String minecraftJson;
	
	@Option (name="-tryseeds", usage="Print stats on X random seeds and exit. Outputs to console unless -history given.")
	public int trySeeds;

	@Option (name="-sleeppct", usage="Throttle seed finder by percentage.")
	public int sleepPct;
	
	@Option (name="-pruneseeds", usage="Skip low quality seeds for better performance.")
	public boolean pruneSeeds;

	@Option (name="-radius", usage="Chunk radius of seed finder. Default 125.")
	public int radius;
	
	@Option (name="-retest", usage="Seed file to retest.")
	public String retest;

	private Options() {
		seed = 0L;
		seedText = null;
		trySeeds = 0;
		sleepPct = 0;
		pruneSeeds = false;
		retest = null;
		radius = 125;


		Preferences pref = Preferences.userNodeForPackage(Amidst.class);
		preferences = pref;
		jar				     = new FilePrefModel(   pref, "jar", new File(Util.minecraftDirectory, "bin/minecraft.jar"));
		showSlimeChunks	     = new BooleanPrefModel(pref, "slimeChunks",	 	 false);
		showGrid			 = new BooleanPrefModel(pref, "grid",			 	 false);
		showNetherFortresses = new BooleanPrefModel(pref, "netherFortressIcons", false);
		showMineshafts		 = new BooleanPrefModel(pref, "mineshaftIcons",		 false);
		mapFlicking		     = new BooleanPrefModel(pref, "mapFlicking",		 true);
		mapFading		  	 = new BooleanPrefModel(pref, "mapFading",		     true);
		maxZoom			     = new BooleanPrefModel(pref, "maxZoom",			 true);
		showStrongholds	     = new BooleanPrefModel(pref, "strongholdIcons",	 true);
		showPlayers		     = new BooleanPrefModel(pref, "playerIcons",		 true);
		showTemples		     = new BooleanPrefModel(pref, "templeIcons",		 true);
		showVillages		 = new BooleanPrefModel(pref, "villageIcons",		 true);
		showOceanMonuments	 = new BooleanPrefModel(pref, "oceanMonumentIcons",	 true);
		showSpawn			 = new BooleanPrefModel(pref, "spawnIcon",		     true);
		showFPS			     = new BooleanPrefModel(pref, "showFPS",			 true);
		showDebug			 = new BooleanPrefModel(pref, "showDebug",		     false);
		updateToUnstable     = new BooleanPrefModel(pref, "updateToUnstable",    false);
		lastProfile          = new StringPreference(pref, "profile",             null);
		biomeColorProfile	 = new BiomeColorProfile();
		worldType			 = new SelectPrefModel( pref, "worldType",  "Prompt each time", new String[] { "Prompt each time", "Default", "Flat", "Large Biomes", "Amplified" });
		biomeColorProfile.fillColorArray();


	}

	public Preferences getPreferences() {
		return preferences;
	}

	public File getJar() {
		return jar.get();
	}

	public String getSeedMessage() {
		if (seedText == null)
			return "Seed: " + seed;
		return "Seed: \"" + seedText + "\" (" + seed +  ")";
	}
}
