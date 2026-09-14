package com.darkcontinent.nenfoundation.worldtree;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.HashSet;
import java.util.Set;

/** Estado global da unica World Tree, salvo no Overworld. */
public final class WorldTreeSavedData extends SavedData {
    public static final String DATA_ID = "world_tree";
    public static final int CURRENT_VERSION = 1;

    private static final String VERSION = "generation_version";
    private static final String TREE_GENERATED = "tree_generated";
    private static final String ORIGIN_X = "overworld_origin_x";
    private static final String ORIGIN_Z = "overworld_origin_z";
    private static final String DIMENSION_ORIGIN_X = "tree_dimension_origin_x";
    private static final String DIMENSION_ORIGIN_Z = "tree_dimension_origin_z";
    private static final String DISCOVERED = "discovered";
    private static final String CLOUD_REACHED = "first_reached_cloud_layer";
    private static final String CROWN_REACHED = "first_reached_crown";
    private static final String SUMMIT_REACHED = "summit_reached";
    private static final String GENERATED_CHUNKS = "generated_base_chunks";

    private int generationVersion = CURRENT_VERSION;
    private boolean treeGenerated;
    private int overworldOriginX;
    private int overworldOriginZ;
    private int treeDimensionOriginX;
    private int treeDimensionOriginZ;
    private boolean discovered;
    private boolean firstReachedCloudLayer;
    private boolean firstReachedCrown;
    private boolean summitReached;
    private final Set<Long> generatedBaseChunks = new HashSet<>();
    private transient long cachedLayoutSeed = Long.MIN_VALUE;
    private transient WorldTreeLayout cachedLayout;

    public static SavedData.Factory<WorldTreeSavedData> factory() {
        return new SavedData.Factory<>(WorldTreeSavedData::new, WorldTreeSavedData::load, null);
    }

    public static WorldTreeSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        tag = WorldTreeSaveMigration.migrate(tag);
        int version = tag.getInt(VERSION);
        if (version <= 0 || version > CURRENT_VERSION) {
            throw new IllegalArgumentException("versao de World Tree nao suportada: " + version);
        }
        WorldTreeSavedData data = new WorldTreeSavedData();
        data.generationVersion = version;
        data.treeGenerated = tag.getBoolean(TREE_GENERATED);
        data.overworldOriginX = tag.getInt(ORIGIN_X);
        data.overworldOriginZ = tag.getInt(ORIGIN_Z);
        data.treeDimensionOriginX = tag.getInt(DIMENSION_ORIGIN_X);
        data.treeDimensionOriginZ = tag.getInt(DIMENSION_ORIGIN_Z);
        data.discovered = tag.getBoolean(DISCOVERED);
        data.firstReachedCloudLayer = tag.getBoolean(CLOUD_REACHED);
        data.firstReachedCrown = tag.getBoolean(CROWN_REACHED);
        data.summitReached = tag.getBoolean(SUMMIT_REACHED);
        for (long chunk : tag.getLongArray(GENERATED_CHUNKS)) {
            data.generatedBaseChunks.add(chunk);
        }
        return data;
    }

    public void initialize(long seed) {
        if (treeGenerated) {
            return;
        }
        long mixed = seed ^ 0x9E3779B97F4A7C15L;
        mixed = (mixed ^ (mixed >>> 30)) * 0xBF58476D1CE4E5B9L;
        mixed = (mixed ^ (mixed >>> 27)) * 0x94D049BB133111EBL;
        mixed ^= mixed >>> 31;
        int distance = 2500 + Math.floorMod((int) mixed, 4501);
        int angle = Math.floorMod((int) (mixed >>> 32), 360);
        double radians = Math.toRadians(angle);
        overworldOriginX = (int) Math.round(Math.cos(radians) * distance);
        overworldOriginZ = (int) Math.round(Math.sin(radians) * distance);
        treeDimensionOriginX = 0;
        treeDimensionOriginZ = 0;
        treeGenerated = true;
        setDirty();
    }

    public int generationVersion() { return generationVersion; }
    public boolean treeGenerated() { return treeGenerated; }
    public int overworldOriginX() { return overworldOriginX; }
    public int overworldOriginZ() { return overworldOriginZ; }
    public int treeDimensionOriginX() { return treeDimensionOriginX; }
    public int treeDimensionOriginZ() { return treeDimensionOriginZ; }
    public boolean discovered() { return discovered; }
    public boolean firstReachedCloudLayer() { return firstReachedCloudLayer; }
    public boolean firstReachedCrown() { return firstReachedCrown; }
    public boolean summitReached() { return summitReached; }
    public boolean isBaseChunkGenerated(long chunk) { return generatedBaseChunks.contains(chunk); }

    /**
     * Layout is derived from saved identity and seed, so it is cached only in
     * memory and never serialized as a second source of world truth.
     */
    public synchronized WorldTreeLayout layoutForGeneration(long seed) {
        if (cachedLayout == null || cachedLayoutSeed != seed) {
            cachedLayout = WorldTreeLayoutGenerator.generate(seed, generationVersion,
                    overworldOriginX, overworldOriginZ);
            cachedLayoutSeed = seed;
        }
        return cachedLayout;
    }

    public boolean markBaseChunkGenerated(long chunk) {
        if (!generatedBaseChunks.add(chunk)) {
            return false;
        }
        setDirty();
        return true;
    }

    public void markDiscovered() { discovered = true; setDirty(); }
    public void markCloudLayerReached() { firstReachedCloudLayer = true; setDirty(); }
    public void markCrownReached() { firstReachedCrown = true; setDirty(); }
    public void markSummitReached() { summitReached = true; setDirty(); }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt(VERSION, generationVersion);
        tag.putBoolean(TREE_GENERATED, treeGenerated);
        tag.putInt(ORIGIN_X, overworldOriginX);
        tag.putInt(ORIGIN_Z, overworldOriginZ);
        tag.putInt(DIMENSION_ORIGIN_X, treeDimensionOriginX);
        tag.putInt(DIMENSION_ORIGIN_Z, treeDimensionOriginZ);
        tag.putBoolean(DISCOVERED, discovered);
        tag.putBoolean(CLOUD_REACHED, firstReachedCloudLayer);
        tag.putBoolean(CROWN_REACHED, firstReachedCrown);
        tag.putBoolean(SUMMIT_REACHED, summitReached);
        long[] chunks = new long[generatedBaseChunks.size()];
        int index = 0;
        for (long chunk : generatedBaseChunks) {
            chunks[index++] = chunk;
        }
        tag.put(GENERATED_CHUNKS, new LongArrayTag(chunks));
        return tag;
    }
}
