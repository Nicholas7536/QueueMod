package com.example.queuemod;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.Blocks;

import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.UUID;

// The value here should match an entry in the META-INF/mods.toml file
@Mod("queuemod")
public class QueueMod {
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();

    // Placeholder dimension ID
    public static final ResourceKey<Level> QUEUE_DIMENSION = ResourceKey.create(Registry.DIMENSION_REGISTRY, new ResourceLocation("queuemod", "queue"));

    public QueueMod() {
        // Register the setup method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void setup(final FMLCommonSetupEvent event) {
        // Log for debugging
        LOGGER.info("HELLO FROM PREINIT");
        LOGGER.info("DIRT BLOCK >> {}", Blocks.DIRT.getRegistryName());
    }

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        ServerPlayer player = (ServerPlayer) event.getPlayer();
        MinecraftServer server = player.getServer();

        if (server == null) return;

        PlayerQueue.addPlayer(player);

        int position = PlayerQueue.getPlayerPosition(player);

        if (position == 1) {
            player.sendMessage(new TextComponent("OnPlayerJoin, Queue Finished"), player.getUUID());
        } else {
            player.sendMessage(new TextComponent("You are #" + (position - 1) + " in queue."), player.getUUID());

            // Send them to the queue dimension
            ServerLevel queueLevel = server.getLevel(QueueMod.QUEUE_DIMENSION);
            if (queueLevel != null) {
                // Freeze the player if they're not already frozen
                if (player.level != queueLevel) {
                    freezePlayer(player);
                }
                BlockPos spawnPos = new BlockPos(0, 40, 0);
                player.teleportTo(queueLevel, spawnPos.getX() + 0.5, 200, spawnPos.getZ() + 0.5,
                        player.getYRot(), player.getXRot());
                LOGGER.info("Sent {} to the queue dimension", player.getName().getString());
            }
        }
    }


    public void savePlayerPosition(ServerPlayer player) {
        String dimension = player.level.dimension().location().toString();
        if (!dimension.equals("queuemod:queue")) {
            CompoundTag nbt = player.getPersistentData();

            // Save current position
            BlockPos pos = player.blockPosition();


            CompoundTag queueData = new CompoundTag();
            queueData.putInt("lastX", pos.getX());
            queueData.putInt("lastY", pos.getY());
            queueData.putInt("lastZ", pos.getZ());
            queueData.putString("dimension", dimension);

            // Save the frozen state (for later use when reloading the position)
            queueData.putBoolean("frozen", player.isInvisible());  // Assuming freeze = invisible

            nbt.put("queueModData", queueData);
        }
    }

    public void loadPlayerPosition(ServerPlayer player) {
        CompoundTag nbt = player.getPersistentData().getCompound("queueModData");

        if (nbt.contains("lastX") && nbt.contains("lastY") && nbt.contains("lastZ") && nbt.contains("dimension")) {
            int x = nbt.getInt("lastX");
            int y = nbt.getInt("lastY");
            int z = nbt.getInt("lastZ");
            String dimensionName = nbt.getString("dimension");
            BlockPos savedPos = new BlockPos(x, y, z);

            // Retrieve the frozen state and apply it
            boolean isFrozen = nbt.getBoolean("frozen");

            // Get the saved dimension
            ResourceKey<Level> dimensionKey = ResourceKey.create(Registry.DIMENSION_REGISTRY, new ResourceLocation(dimensionName));
            ServerLevel targetDimension = player.getServer().getLevel(dimensionKey);

            if (targetDimension != null) {
                player.teleportTo(targetDimension, savedPos.getX() + 0.5, savedPos.getY(), savedPos.getZ() + 0.5,
                        player.getYRot(), player.getXRot());

                if (targetDimension == player.getServer().getLevel(QueueMod.QUEUE_DIMENSION)) {
                    // Freeze the player if they are in the queue world
                    freezePlayer(player);
                } else {
                    // Unfreeze the player if they are NOT in the queue world
                    unfreezePlayer(player);
                }
            } else {
                // If no position or dimension data exists, teleport the player to spawn or some default location
                ServerLevel overworld = player.getServer().getLevel(Level.OVERWORLD);
                if (overworld != null) {
                    BlockPos spawnPos = overworld.getSharedSpawnPos();
                    player.teleportTo(overworld, spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5,
                            player.getYRot(), player.getXRot());
                    LOGGER.info("Player {} does not have saved position or dimension data, teleporting to spawn.", player.getName().getString());
                }
            }
        } else {
            // If no saved position, teleport the player to spawn
            ServerLevel overworld = player.getServer().getLevel(Level.OVERWORLD);
            if (overworld != null) {
                BlockPos spawnPos = overworld.getSharedSpawnPos();
                player.teleportTo(overworld, spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5,
                        player.getYRot(), player.getXRot());
                LOGGER.info("Player {} does not have saved position or dimension data, teleporting to spawn.", player.getName().getString());
            }
        }
    }


    @SubscribeEvent
    public void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        ServerPlayer player = (ServerPlayer) event.getPlayer();

        savePlayerPosition(player);
        boolean wasFirst = PlayerQueue.isFirst(player);

        PlayerQueue.removePlayer(player);

        if (wasFirst) {
            promoteNextPlayer(player.getServer());
        }
    }

    private void promoteNextPlayer(MinecraftServer server) {
        UUID nextUUID = PlayerQueue.peekFirst();
        if (nextUUID == null) return;

        ServerPlayer nextPlayer = server.getPlayerList().getPlayer(nextUUID);
        if (nextPlayer != null) {
            // Send a message about their promotion
            int newPosition = PlayerQueue.getPlayerPosition(nextPlayer);
            nextPlayer.sendMessage(new TextComponent("You have been promoted to position " + newPosition + " in the queue."), nextUUID);

            // Optional: teleport them out of the queue dimension
            loadPlayerPosition(nextPlayer);
            unfreezePlayer(nextPlayer);  // Unfreeze the player as they are no longer in the queue
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        MinecraftServer server = event.getServer();
        ServerLevel queueWorld = server.getLevel(QueueMod.QUEUE_DIMENSION);
        if (queueWorld == null) {
            LOGGER.warn("QueueWorld not found on server start!");
        }
    }

    private void freezePlayer(ServerPlayer player) {
        player.setInvisible(true);
        player.setInvulnerable(true);
        player.getAbilities().mayBuild = false;
        player.getAbilities().setWalkingSpeed(0.0F);
        player.getAbilities().setFlyingSpeed(0.0F);
        player.noPhysics = true;
        player.onUpdateAbilities();
    }

    private void unfreezePlayer(ServerPlayer player) {
        player.setInvisible(false);
        player.setInvulnerable(false);
        player.getAbilities().mayBuild = true;
        player.getAbilities().setWalkingSpeed(0.1F); // Reset to normal walking speed
        player.getAbilities().setFlyingSpeed(0.05F); // Reset to normal flying speed
        player.noPhysics = false;
        player.onUpdateAbilities();
    }
}
