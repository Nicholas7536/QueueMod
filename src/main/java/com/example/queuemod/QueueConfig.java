package com.example.queuemod;

import net.minecraftforge.common.ForgeConfigSpec;

public class QueueConfig {
    public static final ForgeConfigSpec COMMON_CONFIG;
    public static final ForgeConfigSpec.IntValue MAX_PLAYERS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("Queue Settings");
        MAX_PLAYERS = builder
                .comment("Maximum players allowed in the main world before being sent to the queue")
                .defineInRange("maxPlayersInMainWorld", 5, 1, 100);
        builder.pop();

        COMMON_CONFIG = builder.build();
    }
}