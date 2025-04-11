package com.example.queuemod;

import net.minecraft.server.level.ServerPlayer;

import java.util.LinkedList;
import java.util.UUID;

public class PlayerQueue {
    // FIFO queue - LinkedList is great for this
    public static final LinkedList<UUID> QUEUE = new LinkedList<>();

    public static void addPlayer(ServerPlayer player) {
        if (!QUEUE.contains(player.getUUID())) {
            QUEUE.add(player.getUUID());
        }
    }

    public static void removePlayer(ServerPlayer player) {
        QUEUE.remove(player.getUUID());
    }

    public static int getPlayerPosition(ServerPlayer player) {
        return QUEUE.indexOf(player.getUUID()) + 1;
    }

    public static UUID peekFirst() {
        return QUEUE.peekFirst();
    }

    public static boolean isFirst(ServerPlayer player) {
        return player.getUUID().equals(peekFirst());
    }
}