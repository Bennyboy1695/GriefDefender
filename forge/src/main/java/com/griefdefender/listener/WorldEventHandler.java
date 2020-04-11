/*
 * This file is part of GriefDefender, licensed under the MIT License (MIT).
 *
 * Copyright (c) bloodmc
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.griefdefender.listener;

import com.griefdefender.GDTimings;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.claim.GDClaimManager;
import com.griefdefender.internal.tracking.chunk.GDChunk;

import java.io.IOException;
import java.util.UUID;

import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class WorldEventHandler {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onWorldUnload(WorldEvent.Unload event) {
        if (!GriefDefenderPlugin.getInstance().claimsEnabledForWorld(UUID.nameUUIDFromBytes(event.getWorld().getWorldInfo().getWorldName().getBytes()))) {
            return;
        }

        GriefDefenderPlugin.getInstance().dataStore.removeClaimWorldManager(UUID.nameUUIDFromBytes(event.getWorld().getWorldInfo().getWorldName().getBytes()));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onWorldSave(WorldEvent.Save event) {
        if (!GriefDefenderPlugin.getInstance().claimsEnabledForWorld(UUID.nameUUIDFromBytes(event.getWorld().getWorldInfo().getWorldName().getBytes()))) {
            return;
        }

        GDTimings.WORLD_SAVE_EVENT.startTiming();
        GDClaimManager claimWorldManager = GriefDefenderPlugin.getInstance().dataStore.getClaimWorldManager(UUID.nameUUIDFromBytes(event.getWorld().getWorldInfo().getWorldName().getBytes()));
        if (claimWorldManager == null) {
            GDTimings.WORLD_SAVE_EVENT.stopTiming();
            return;
        }

        claimWorldManager.save();
        claimWorldManager.playerIndexStorage.savePlayerDatData();

        GDTimings.WORLD_SAVE_EVENT.stopTiming();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onChunkLoad(ChunkEvent.Load event) {
        final GDClaimManager claimWorldManager = GriefDefenderPlugin.getInstance().dataStore.getClaimWorldManager(UUID.nameUUIDFromBytes(event.getWorld().getWorldInfo().getWorldName().getBytes()));
        final GDChunk gpChunk = claimWorldManager.getChunk(event.getChunk());
        if (gpChunk != null) {
            try {
                gpChunk.loadChunkTrackingData();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onChunkUnload(ChunkEvent.Unload event) {
        final GDClaimManager claimWorldManager = GriefDefenderPlugin.getInstance().dataStore.getClaimWorldManager(UUID.nameUUIDFromBytes(event.getWorld().getWorldInfo().getWorldName().getBytes()));
        final GDChunk gpChunk = claimWorldManager.getChunk(event.getChunk());
        if (gpChunk != null) {
            if (gpChunk.getTrackedShortPlayerPositions().size() > 0) {
                gpChunk.saveChunkTrackingData();
            }
            claimWorldManager.removeChunk(event.getChunk());
        }
    }
}
