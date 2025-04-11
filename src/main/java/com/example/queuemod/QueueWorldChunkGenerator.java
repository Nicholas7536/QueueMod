package com.example.queuemod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.StructureFeatureManager;
import com.mojang.serialization.Codec;
import net.minecraft.world.level.levelgen.structure.StructureSet;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class QueueWorldChunkGenerator extends ChunkGenerator {

    public QueueWorldChunkGenerator(Registry<StructureSet> p_207960_, Optional<HolderSet<StructureSet>> p_207961_, BiomeSource p_207962_) {
        super(p_207960_, p_207961_, p_207962_);
    }

    // Method to fill the chunk with noise (no noise here, just air everywhere)
    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Executor executor, Blender blender, StructureFeatureManager featureManager, ChunkAccess chunk) {
        return CompletableFuture.completedFuture(chunk);
    }

    // Method to get the generation depth for the dimension (set to 384 for this example)
    @Override
    public int getGenDepth() {
        return 384; // Maximum Y-level (adjust as needed)
    }

    // Set the sea level (no sea level since it's all air)
    @Override
    public int getSeaLevel() {
        return 0;
    }

    // Set the minimum Y value for the chunk generation (set to 0 for an empty world)
    @Override
    public int getMinY() {
        return 0;
    }

    // Get the base height at a given coordinate (we set it to 0 because there's no terrain)
    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor heightAccessor) {
        return 0; // No terrain, everything is air
    }

    // This method returns a column of blocks (we return null, as we generate air everywhere)
    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor heightAccessor) {
        return null; // No base column, everything is air
    }

    // Debug info for chunk generation (we can leave this empty or add any debugging if needed)
    @Override
    public void addDebugScreenInfo(List<String> info, BlockPos pos) {
        // Empty since we're not generating any terrain
    }

    // Codec for this chunk generator (optional)
    @Override
    public Codec<? extends ChunkGenerator> codec() {
        return null; // Optionally implement codec for serialization, but not needed for this case
    }

    @Override
    public ChunkGenerator withSeed(long p_62156_) {
        return null;
    }

    // Override method to apply world carving (but there will be no carving since it's all air)
    @Override
    public void applyCarvers(WorldGenRegion region, long seed, BiomeManager biomeManager, StructureFeatureManager featureManager, ChunkAccess chunkAccess, GenerationStep.Carving carvingStep) {
        // No carvers, just air
    }

    @Override
    public void buildSurface(WorldGenRegion p_187697_, StructureFeatureManager p_187698_, ChunkAccess p_187699_) {

    }

    // Override method for spawning mobs (no mobs will spawn in an air-filled world)
    @Override
    public void spawnOriginalMobs(WorldGenRegion region) {
        // No mobs to spawn
    }


    // Optional method for climate sampling
    @Override
    public Climate.Sampler climateSampler() {
        return null; // No climate sampling needed for an air-filled world
    }
}
