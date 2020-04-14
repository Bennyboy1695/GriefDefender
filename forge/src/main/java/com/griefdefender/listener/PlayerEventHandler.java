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

import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3i;
import com.google.common.collect.ImmutableMap;
import com.google.common.reflect.TypeToken;
import com.griefdefender.GDPlayerData;
import com.griefdefender.GDTimings;
import com.griefdefender.GriefDefenderPlugin;
import com.griefdefender.api.Tristate;
import com.griefdefender.api.claim.Claim;
import com.griefdefender.api.claim.ClaimBlockSystem;
import com.griefdefender.api.claim.ClaimResult;
import com.griefdefender.api.claim.ClaimResultType;
import com.griefdefender.api.claim.ClaimType;
import com.griefdefender.api.claim.ClaimTypes;
import com.griefdefender.api.claim.ShovelTypes;
import com.griefdefender.api.claim.TrustType;
import com.griefdefender.api.claim.TrustTypes;
import com.griefdefender.api.permission.flag.Flag;
import com.griefdefender.api.permission.flag.Flags;
import com.griefdefender.api.permission.option.Options;
import com.griefdefender.api.permission.option.type.CreateModeTypes;
import com.griefdefender.cache.MessageCache;
import com.griefdefender.cache.PermissionHolderCache;
import com.griefdefender.claim.GDClaim;
import com.griefdefender.claim.GDClaimManager;
import com.griefdefender.command.CommandHelper;
import com.griefdefender.configuration.MessageStorage;
import com.griefdefender.event.GDCauseStackManager;
import com.griefdefender.internal.provider.WorldEditProvider;
import com.griefdefender.internal.provider.WorldGuardProvider;
import com.griefdefender.internal.registry.BlockTypeRegistryModule;
import com.griefdefender.internal.registry.ItemTypeRegistryModule;
import com.griefdefender.internal.util.BlockUtil;
import com.griefdefender.internal.util.NMSUtil;
import com.griefdefender.internal.util.VecHelper;
import com.griefdefender.internal.visual.ClaimVisual;
import com.griefdefender.permission.GDPermissionManager;
import com.griefdefender.permission.GDPermissionUser;
import com.griefdefender.permission.GDPermissions;
import com.griefdefender.permission.flag.GDFlags;
import com.griefdefender.provider.VaultProvider;
import com.griefdefender.storage.BaseStorage;
import com.griefdefender.util.*;

import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.adapter.bukkit.TextAdapter;
import net.kyori.text.format.TextColor;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.ArmorStandEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PlayerEventHandler {

    private final BaseStorage dataStore;
    private final WorldEditProvider worldEditProvider;
    private boolean lastInteractItemCancelled = false;

    public PlayerEventHandler(BaseStorage dataStore) {
        this.dataStore = dataStore;
        this.worldEditProvider = GriefDefenderPlugin.getInstance().getWorldEditProvider();
        // this.banService = Sponge.getServiceManager().getRegistration(BanService.class).get().getProvider();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        GDTimings.PLAYER_LOGIN_EVENT.startTiming();
        final ServerPlayerEntity player = (ServerPlayerEntity) event.getPlayer();
        if (!GriefDefenderPlugin.getInstance().claimsEnabledForWorld(UUID.nameUUIDFromBytes(player.world.getWorldInfo().getWorldName().getBytes()))) {
            GDTimings.PLAYER_LOGIN_EVENT.stopTiming();
            return;
        }

        final UUID worldUniqueId = UUID.nameUUIDFromBytes(player.world.getWorldInfo().getWorldName().getBytes());
        final UUID playerUniqueId = player.getUniqueID();
        final GDClaimManager claimWorldManager = this.dataStore.getClaimWorldManager(worldUniqueId);
        final Instant dateNow = Instant.now();
        for (Claim claim : claimWorldManager.getWorldClaims()) {
            if (claim.getType() != ClaimTypes.ADMIN && claim.getOwnerUniqueId().equals(playerUniqueId)) {
                claim.getData().setDateLastActive(dateNow);
                for (Claim subdivision : ((GDClaim) claim).children) {
                    subdivision.getData().setDateLastActive(dateNow);
                }
                ((GDClaim) claim).getInternalClaimData().setRequiresSave(true);
            }
        }
        GDTimings.PLAYER_LOGIN_EVENT.stopTiming();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(EntityJoinWorldEvent event) {
        if (event.getEntity() instanceof ServerPlayerEntity) {
            GDTimings.PLAYER_JOIN_EVENT.startTiming();
            ServerPlayerEntity player = (ServerPlayerEntity) event.getEntity();
            if (!GriefDefenderPlugin.getInstance().claimsEnabledForWorld(UUID.nameUUIDFromBytes(player.world.getWorldInfo().getWorldName().getBytes()))) {
                GDTimings.PLAYER_JOIN_EVENT.stopTiming();
                return;
            }

            UUID playerID = player.getUniqueID();
            final GDPlayerData playerData = this.dataStore.getOrCreatePlayerData(player.world, playerID);

            final GDClaim claim = this.dataStore.getClaimAtPlayer(playerData, player.getPositionVector());
            if (claim.isInTown()) {
                playerData.inTown = true;
            }
            GDTimings.PLAYER_JOIN_EVENT.stopTiming();
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerEvent.PlayerLoggedOutEvent event) {
        final ServerPlayerEntity player = event.getPlayer();
        if (!GriefDefenderPlugin.getInstance().claimsEnabledForWorld(UUID.nameUUIDFromBytes(player.world.getWorldInfo().getWorldName().getBytes()))) {
            return;
        }

        GDTimings.PLAYER_QUIT_EVENT.startTiming();
        UUID playerID = player.getUniqueID();
        GDPlayerData playerData = this.dataStore.getOrCreatePlayerData(player.world, playerID);

        playerData.onDisconnect();
        PaginationUtil.getInstance().removeActivePageData(player.getUniqueID());
        if (playerData.getClaims().isEmpty()) {
            this.dataStore.clearCachedPlayerData(UUID.nameUUIDFromBytes(player.world.getWorldInfo().getWorldName().getBytes()), playerID);
        }

        GDTimings.PLAYER_QUIT_EVENT.stopTiming();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        ServerPlayerEntity player = (ServerPlayerEntity) event.getPlayer();
        if (!GriefDefenderPlugin.getInstance().claimsEnabledForWorld(UUID.nameUUIDFromBytes(player.world.getWorldInfo().getWorldName().getBytes()))) {
            return;
        }

        final GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(player.world, player.getUniqueID());
        playerData.lastPvpTimestamp = null;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayerEntity) {
            final ServerPlayerEntity player = (ServerPlayerEntity) event.getEntity();
            GDCauseStackManager.getInstance().pushCause(player);
            final GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(player.world, player.getUniqueID());
            final GDClaim claim = GriefDefenderPlugin.getInstance().dataStore.getClaimAtPlayer(playerData, player.getPositionVector());
            final Tristate keepInventory = GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Tristate.class), playerData.getSubject(), Options.PLAYER_KEEP_INVENTORY, claim);
            final Tristate keepLevel = GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Tristate.class), playerData.getSubject(), Options.PLAYER_KEEP_LEVEL, claim);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPlayerFoodLevelChange(LivingEntityUseItemEvent.Finish event) {
        final ServerPlayerEntity player = event.getEntity() instanceof ServerPlayerEntity ? (ServerPlayerEntity) event.getEntity() : null;
        if (player == null) {
            return;
        }
        if (!event.getResultStack().isFood()) {
            return;
        }
        GDCauseStackManager.getInstance().pushCause(player);
        final GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(player.world, player.getUniqueID());
        final GDClaim claim = GriefDefenderPlugin.getInstance().dataStore.getClaimAtPlayer(playerData, player.getPositionVector());
        final Boolean denyHunger = GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Boolean.class), playerData.getSubject(), Options.PLAYER_DENY_HUNGER, claim);
        if (denyHunger != null && denyHunger) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPlayerChangeHeldItem(LivingEquipmentChangeEvent event) {
        if (event.getEntity() instanceof ServerPlayerEntity) {
            final ServerPlayerEntity player = (ServerPlayerEntity) event.getEntity();
            GDCauseStackManager.getInstance().pushCause(player);
            if (!GriefDefenderPlugin.getInstance().claimsEnabledForWorld(UUID.nameUUIDFromBytes(player.world.getWorldInfo().getWorldName().getBytes()))) {
                return;
            }

            GDTimings.PLAYER_CHANGE_HELD_ITEM_EVENT.startTiming();
            GDPlayerData playerData = this.dataStore.getOrCreatePlayerData(player.world, player.getUniqueID());

            ItemStack newItemStack = event.getTo();
            if (newItemStack != null && GriefDefenderPlugin.getInstance().modificationTool != null && NMSUtil.getInstance().itemsEqual(newItemStack, GriefDefenderPlugin.getInstance().modificationTool)) {
                playerData.lastShovelLocation = null;
                playerData.endShovelLocation = null;
                playerData.claimResizing = null;
                // always reset to basic claims mode
                if (playerData.shovelMode != ShovelTypes.BASIC) {
                    playerData.shovelMode = ShovelTypes.BASIC;
                    GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().MODE_BASIC);
                }

                if (GriefDefenderPlugin.CLAIM_BLOCK_SYSTEM == ClaimBlockSystem.VOLUME) {
                    final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.PLAYER_REMAINING_BLOCKS_3D,
                            ImmutableMap.of(
                                    "block-amount", playerData.getRemainingClaimBlocks(),
                                    "chunk-amount", playerData.getRemainingChunks()));
                    GriefDefenderPlugin.sendMessage(player, message);
                } else {
                    final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.PLAYER_REMAINING_BLOCKS_2D,
                            ImmutableMap.of(
                                    "block-amount", playerData.getRemainingClaimBlocks()));
                    GriefDefenderPlugin.sendMessage(player, message);
                }
            }
            GDTimings.PLAYER_CHANGE_HELD_ITEM_EVENT.stopTiming();
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPlayerDropItem(ItemTossEvent event) {
        final ServerPlayerEntity player = (ServerPlayerEntity) event.getPlayer();
        GDCauseStackManager.getInstance().pushCause(player);
        if (!GDFlags.ITEM_DROP) {
            return;
        }

        final World world = event.getPlayer().world;
        if (!GriefDefenderPlugin.getInstance().claimsEnabledForWorld(UUID.nameUUIDFromBytes(player.world.getWorldInfo().getWorldName().getBytes()))) {
            return;
        }
        if (GriefDefenderPlugin.isSourceIdBlacklisted(Flags.ITEM_DROP.getName(), player, UUID.nameUUIDFromBytes(player.world.getWorldInfo().getWorldName().getBytes()))) {
            return;
        }

        if (GriefDefenderPlugin.isTargetIdBlacklisted(Flags.ITEM_DROP.getName(), event.getEntityItem(), UUID.nameUUIDFromBytes(player.world.getWorldInfo().getWorldName().getBytes()))) {
            return;
        }

        final Vec3d location = event.getEntityItem().getPositionVector();
        final GDClaim targetClaim = GriefDefenderPlugin.getInstance().dataStore.getClaimAt(location);

        if (GDPermissionManager.getInstance().getFinalPermission(event, location, targetClaim, Flags.ITEM_DROP, player, event.getEntityItem(), player, TrustTypes.ACCESSOR, true) == Tristate.FALSE) {
            event.setCanceled(true);
        }
    }

    // Older MC versions do not have EntityPickupItemEvent so keep this in common
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPlayerPickupItem(EntityItemPickupEvent event) {
        if (!GDFlags.ITEM_PICKUP) {
            return;
        }

        final ServerPlayerEntity player = event.getPlayer();
        GDCauseStackManager.getInstance().pushCause(player);
        final World world = player.world;
        if (!GriefDefenderPlugin.getInstance().claimsEnabledForWorld(UUID.nameUUIDFromBytes(player.world.getWorldInfo().getWorldName().getBytes()))) {
            return;
        }
        if (GriefDefenderPlugin.isSourceIdBlacklisted(Flags.ITEM_PICKUP.getName(), player, UUID.nameUUIDFromBytes(player.world.getWorldInfo().getWorldName().getBytes()))) {
            return;
        }

        if (GriefDefenderPlugin.isTargetIdBlacklisted(Flags.ITEM_PICKUP.getName(), event.getItem(), UUID.nameUUIDFromBytes(player.world.getWorldInfo().getWorldName().getBytes()))) {
            return;
        }

        final Vec3d location = event.getItem().getPositionVector();
        final GDClaim targetClaim = GriefDefenderPlugin.getInstance().dataStore.getClaimAt(location);
        if (GDPermissionManager.getInstance().getFinalPermission(event, location, targetClaim, Flags.ITEM_PICKUP, player, event.getItem(), player, TrustTypes.ACCESSOR, true) == Tristate.FALSE) {
            event.setCanceled(true);
        }
    }

    private void onInventoryOpen(Event event, Vec3d location, Object target, ServerPlayerEntity player) {
        GDCauseStackManager.getInstance().pushCause(player);
        if (event instanceof PlayerContainerEvent.Open) {
            final PlayerContainerEvent.Open inventoryEvent = (PlayerContainerEvent.Open) event;
            target = inventoryEvent.getContainer().getType();
        }
        if (!GDFlags.INTERACT_INVENTORY || !GriefDefenderPlugin.getInstance().claimsEnabledForWorld(UUID.nameUUIDFromBytes(player.world.getWorldInfo().getWorldName().getBytes()))) {
            return;
        }

        if (GriefDefenderPlugin.isTargetIdBlacklisted(Flags.INTERACT_INVENTORY.getName(), target, UUID.nameUUIDFromBytes(player.world.getWorldInfo().getWorldName().getBytes()))) {
            return;
        }

        String targetId = GDPermissionManager.getInstance().getPermissionIdentifier(target);
        GDTimings.PLAYER_INTERACT_INVENTORY_OPEN_EVENT.startTiming();
        final GDClaim claim = this.dataStore.getClaimAt(location);
        final GDPermissionUser user = PermissionHolderCache.getInstance().getOrCreateUser(player.getUniqueID());
        if (user.getInternalPlayerData() != null && user.getInternalPlayerData().eventResultCache != null && user.getInternalPlayerData().eventResultCache.checkEventResultCache(claim, Flags.INTERACT_BLOCK_SECONDARY.getName()) == Tristate.TRUE) {
            GDPermissionManager.getInstance().processResult(claim, Flags.INTERACT_INVENTORY.getPermission(), "cache", Tristate.TRUE, user);
            GDTimings.PLAYER_INTERACT_INVENTORY_OPEN_EVENT.stopTiming();
            return;
        }
        final Tristate result = GDPermissionManager.getInstance().getFinalPermission(event, location, claim, Flags.INTERACT_INVENTORY, player, target, user, TrustTypes.CONTAINER, true);
        if (result == Tristate.FALSE) {
            final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.PERMISSION_INVENTORY_OPEN,
                    ImmutableMap.of(
                            "player", claim.getOwnerName(),
                            "block", targetId));
            GriefDefenderPlugin.sendClaimDenyMessage(claim, player, message);
            event.setCanceled(true);
        }

        GDTimings.PLAYER_INTERACT_INVENTORY_OPEN_EVENT.stopTiming();
    }

/*    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPlayerInteractInventoryClick(PlayerContainerEvent event) {
        final HumanEntity player = event.getWhoClicked();
        GDCauseStackManager.getInstance().pushCause(player);
        if (!GDFlags.INTERACT_INVENTORY_CLICK || event.getClickedInventory() == null || !GriefDefenderPlugin.getInstance().claimsEnabledForWorld(player.getWorld().getUID())) {
            return;
        }

        GDTimings.PLAYER_INTERACT_INVENTORY_CLICK_EVENT.startTiming();
        final Location location = player.getLocation();
        final GDClaim claim = this.dataStore.getClaimAt(location);
        final ItemStack cursorItem = event.getCursor();
        final Inventory source = event.getInventory();
        final ItemStack target = event.getCurrentItem();

        if (GriefDefenderPlugin.isTargetIdBlacklisted(Flags.INTERACT_INVENTORY_CLICK.getName(), target, player.getWorld().getUID())) {
            GDTimings.PLAYER_INTERACT_INVENTORY_CLICK_EVENT.stopTiming();
            return;
        }

        final GDPermissionUser user = PermissionHolderCache.getInstance().getOrCreateUser(player.getUniqueId());
        final Tristate result = GDPermissionManager.getInstance().getFinalPermission(event, location, claim, Flags.INTERACT_INVENTORY_CLICK, source, target, user, TrustTypes.CONTAINER, true);
        if (result == Tristate.FALSE) {
            final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.PERMISSION_INTERACT_ITEM,
                    ImmutableMap.of(
                    "player", claim.getOwnerName(),
                    "item", ItemTypeRegistryModule.getInstance().getNMSKey(target)));
            GriefDefenderPlugin.sendClaimDenyMessage(claim, player, message);
            event.setCancelled(true);
        }
        GDTimings.PLAYER_INTERACT_INVENTORY_CLICK_EVENT.stopTiming();
    }*/

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPlayerConsumeItem(LivingEntityUseItemEvent event) {
        if (event.getEntity() instanceof ServerPlayerEntity) {
            final ServerPlayerEntity player = event.getPlayer();
            final ItemStack itemInUse = event.getItem();
            GDCauseStackManager.getInstance().pushCause(player);
            if (!GDFlags.ITEM_USE || !GriefDefenderPlugin.getInstance().claimsEnabledForWorld(UUID.nameUUIDFromBytes(player.world.getWorldInfo().getWorldName().getBytes()))) {
                return;
            }
            if (GriefDefenderPlugin.isTargetIdBlacklisted(Flags.ITEM_USE.toString(), itemInUse, UUID.nameUUIDFromBytes(player.world.getWorldInfo().getWorldName().getBytes()))) {
                return;
            }

            GDTimings.PLAYER_USE_ITEM_EVENT.startTiming();
            Vec3d location = player.getPositionVector();
            GDPlayerData playerData = this.dataStore.getOrCreatePlayerData(player.world, player.getUniqueID());
            GDClaim claim = this.dataStore.getClaimAtPlayer(playerData, location);

            final Tristate result = GDPermissionManager.getInstance().getFinalPermission(event, location, claim, Flags.ITEM_USE, player, itemInUse, player, TrustTypes.ACCESSOR, true);
            if (result == Tristate.FALSE) {
                final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.PERMISSION_ITEM_USE,
                        ImmutableMap.of("item", ItemTypeRegistryModule.getInstance().getNMSKey(itemInUse)));
                GriefDefenderPlugin.sendClaimDenyMessage(claim, player, message);
                event.setCanceled(true);
            }
            GDTimings.PLAYER_USE_ITEM_EVENT.stopTiming();
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPlayerInteractItem(PlayerInteractEvent event) {
        final World world = event.getPlayer().world;
        final BlockState clickedBlock = event.getWorld().getBlockState(event.getPos());
        final ItemStack itemInHand = event.getItemStack();
        final ServerPlayerEntity player = (ServerPlayerEntity) event.getPlayer();
        final GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(player.world, player.getUniqueID());
        GDCauseStackManager.getInstance().pushCause(player);
        if (!playerData.claimMode && (itemInHand == null || itemInHand.isFood())) {
            return;
        }

        if ((!GDFlags.INTERACT_ITEM_PRIMARY && !GDFlags.INTERACT_ITEM_SECONDARY) || !GriefDefenderPlugin.getInstance().claimsEnabledForWorld(UUID.nameUUIDFromBytes(player.world.getWorldInfo().getWorldName().getBytes()))) {
            return;
        }

        if (playerData.claimMode || (GriefDefenderPlugin.getInstance().modificationTool != null && NMSUtil.getInstance().itemsEqual(itemInHand, GriefDefenderPlugin.getInstance().modificationTool) ||
                GriefDefenderPlugin.getInstance().investigationTool != null && NMSUtil.getInstance().itemsEqual(itemInHand, GriefDefenderPlugin.getInstance().investigationTool))) {
            investigateClaim(event, player, clickedBlock, itemInHand);
            event.setCanceled(true);
            return;
        }

        final boolean itemPrimaryBlacklisted = GriefDefenderPlugin.isTargetIdBlacklisted(Flags.INTERACT_ITEM_PRIMARY.getName(), itemInHand, UUID.nameUUIDFromBytes(player.world.getWorldInfo().getWorldName().getBytes()));
        final boolean itemSecondaryBlacklisted = GriefDefenderPlugin.isTargetIdBlacklisted(Flags.INTERACT_ITEM_SECONDARY.getName(), itemInHand, UUID.nameUUIDFromBytes(player.world.getWorldInfo().getWorldName().getBytes()));
        if (itemPrimaryBlacklisted && itemSecondaryBlacklisted) {
            return;
        }

        final boolean primaryEvent = event.getHand() == Hand.OFF_HAND ? true : false;
        final Vec3d location = clickedBlock == null ? event.getPlayer().getPositionVector() : new Vec3d(event.getPos().getX(), event.getPos().getY(), event.getPos().getZ());

        final GDClaim claim = this.dataStore.getClaimAt(location);
        final Flag flag = primaryEvent ? Flags.INTERACT_ITEM_PRIMARY : Flags.INTERACT_ITEM_SECONDARY;
        if ((itemPrimaryBlacklisted && flag == Flags.INTERACT_ITEM_PRIMARY) || (itemSecondaryBlacklisted && flag == Flags.INTERACT_ITEM_SECONDARY)) {
            return;
        }

        if (GDPermissionManager.getInstance().getFinalPermission(event, location, claim, flag, player, itemInHand, player, TrustTypes.ACCESSOR, true) == Tristate.FALSE) {
            Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.PERMISSION_INTERACT_ITEM,
                    ImmutableMap.of(
                            "player", claim.getOwnerName(),
                            "item", ItemTypeRegistryModule.getInstance().getNMSKey(itemInHand)));
            GriefDefenderPlugin.sendClaimDenyMessage(claim, player, message);
            event.setCanceled(true);
            lastInteractItemCancelled = true;
            return;
        }
    }

/*    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerArmorStandManipulateEvent(PlayerArmorStandManipulateEvent event) {
        onPlayerInteractEntity(event);
    }*/

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPlayerInteractEntity(PlayerInteractEvent.EntityInteract event) {
        final ServerPlayerEntity player = (ServerPlayerEntity) event.getPlayer();
        final World world = player.world;
        GDCauseStackManager.getInstance().pushCause(player);
        if (!GDFlags.INTERACT_ENTITY_SECONDARY || !GriefDefenderPlugin.getInstance().claimsEnabledForWorld(UUID.nameUUIDFromBytes(player.world.getWorldInfo().getWorldName().getBytes()))) {
            return;
        }

        final Entity targetEntity = event.getTarget();
        final Vec3d location = targetEntity.getPositionVector();
        final ItemStack activeItem = NMSUtil.getInstance().getActiveItem(player, event);
        final GDPlayerData playerData = this.dataStore.getOrCreatePlayerData(player.world, player.getUniqueID());

        if (targetEntity instanceof TameableEntity) {
            if (playerData.petRecipientUniqueId != null) {
                final TameableEntity tameableEntity = (TameableEntity) targetEntity;
                final GDPermissionUser recipientUser = PermissionHolderCache.getInstance().getOrCreateUser(playerData.petRecipientUniqueId);
                tameableEntity.setOwnerId(recipientUser.getOfflinePlayer().getUniqueID());
                playerData.petRecipientUniqueId = null;
                TextAdapter.sendComponent(player, MessageCache.getInstance().COMMAND_PET_CONFIRMATION);
                event.setCanceled(true);
                return;
            }

            final UUID uuid = NMSUtil.getInstance().getTameableOwnerUUID(targetEntity);
            if (uuid != null) {
                // always allow owner to interact with their pets
                if (player.getUniqueID().equals(uuid)) {
                    return;
                }
                // If pet protection is enabled, deny the interaction
                if (GriefDefenderPlugin.getActiveConfig(UUID.nameUUIDFromBytes(player.world.getWorldInfo().getWorldName().getBytes())).getConfig().claim.protectTamedEntities) {
                    final GDPermissionUser user = PermissionHolderCache.getInstance().getOrCreateUser(uuid);
                    final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.CLAIM_PROTECTED_ENTITY,
                            ImmutableMap.of(
                                    "player", user.getName()));
                    GriefDefenderPlugin.sendMessage(player, message);
                    event.setCanceled(true);
                    return;
                }
            }
        }

        if (activeItem != null && activeItem.getItem() != Items.AIR) {
            // handle item usage
            if (!GDFlags.INTERACT_ITEM_SECONDARY || !GriefDefenderPlugin.getInstance().claimsEnabledForWorld(UUID.nameUUIDFromBytes(player.world.getWorldInfo().getWorldName().getBytes()))) {
                return;
            }

            if (GriefDefenderPlugin.isTargetIdBlacklisted(Flags.INTERACT_ITEM_SECONDARY.getName(), activeItem, UUID.nameUUIDFromBytes(player.world.getWorldInfo().getWorldName().getBytes()))) {
                return;
            }

            final GDClaim claim = this.dataStore.getClaimAt(location);
            if (GDPermissionManager.getInstance().getFinalPermission(event, location, claim, Flags.INTERACT_ITEM_SECONDARY, player, activeItem, player, TrustTypes.ACCESSOR, true) == Tristate.FALSE) {
                Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.PERMISSION_INTERACT_ITEM,
                        ImmutableMap.of(
                                "player", claim.getOwnerName(),
                                "item", ItemTypeRegistryModule.getInstance().getNMSKey(activeItem)));
                GriefDefenderPlugin.sendClaimDenyMessage(claim, player, message);
                event.setCanceled(true);
                lastInteractItemCancelled = true;
                return;
            }
        }

        // Item permission checks passed, check entity
        final Object source = activeItem != null && activeItem.getItem() != Items.AIR ? activeItem : player;
        if (GriefDefenderPlugin.isTargetIdBlacklisted(Flags.INTERACT_ENTITY_SECONDARY.getName(), targetEntity, UUID.nameUUIDFromBytes(player.world.getWorldInfo().getWorldName().getBytes()))) {
            return;
        }

        GDTimings.PLAYER_INTERACT_ENTITY_SECONDARY_EVENT.startTiming();
        final GDClaim claim = this.dataStore.getClaimAt(location);

        Tristate result = GDPermissionManager.getInstance().getFinalPermission(event, location, claim, Flags.INTERACT_ENTITY_SECONDARY, source, targetEntity, player, TrustTypes.ACCESSOR, true);
        if (result == Tristate.TRUE && targetEntity instanceof ArmorStandEntity) {
            result = GDPermissionManager.getInstance().getFinalPermission(event, location, claim, Flags.INTERACT_INVENTORY, source, targetEntity, player, TrustTypes.CONTAINER, false);
        }
        if (result == Tristate.FALSE) {
            event.setCanceled(true);
            CommonEntityEventHandler.getInstance().sendInteractEntityDenyMessage(activeItem, targetEntity, claim, player);
        }
        GDTimings.PLAYER_INTERACT_ENTITY_SECONDARY_EVENT.stopTiming();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPlayerBucketFillEvent(FillBucketEvent event) {
        onPlayerBucketEvent(event);
    }

    public void onPlayerBucketEvent(FillBucketEvent event) {
        final ServerPlayerEntity player = (ServerPlayerEntity) event.getPlayer();
        final Block clickedBlock = (Block) event.getTarget().hitInfo;
        GDCauseStackManager.getInstance().pushCause(player);
        if (!GriefDefenderPlugin.getInstance().claimsEnabledForWorld(UUID.nameUUIDFromBytes(player.world.getWorldInfo().getWorldName().getBytes()))) {
            return;
        }
        if (GriefDefenderPlugin.isTargetIdBlacklisted(Flags.INTERACT_BLOCK_SECONDARY.getName(), clickedBlock, UUID.nameUUIDFromBytes(player.world.getWorldInfo().getWorldName().getBytes()))) {
            return;
        }

        GDTimings.PLAYER_INTERACT_BLOCK_SECONDARY_EVENT.startTiming();
        final Object source = player;
        final Vec3d location = event.getTarget().getHitVec();
        final GDClaim claim = this.dataStore.getClaimAt(location);
        final TrustType trustType = NMSUtil.getInstance().hasBlockTileEntity(location) ? TrustTypes.CONTAINER : TrustTypes.ACCESSOR;

        Tristate result = GDPermissionManager.getInstance().getFinalPermission(event, location, claim, Flags.INTERACT_BLOCK_SECONDARY, source, clickedBlock, player, trustType, true);
        if (result == Tristate.FALSE) {
            event.setCanceled(true);
            GDTimings.PLAYER_INTERACT_BLOCK_SECONDARY_EVENT.stopTiming();
            return;
        }

        GDTimings.PLAYER_INTERACT_BLOCK_SECONDARY_EVENT.stopTiming();
    }

    public void onPlayerInteractBlockPrimary(PlayerInteractEvent event, ServerPlayerEntity player) {
        if (!GDFlags.INTERACT_BLOCK_PRIMARY || !GriefDefenderPlugin.getInstance().claimsEnabledForWorld(UUID.nameUUIDFromBytes(player.world.getWorldInfo().getWorldName().getBytes()))) {
            return;
        }
        if (GriefDefenderPlugin.isTargetIdBlacklisted(Flags.INTERACT_BLOCK_PRIMARY.getName(), event.getWorld().getBlockState(event.getPos()), UUID.nameUUIDFromBytes(player.world.getWorldInfo().getWorldName().getBytes()))) {
            return;
        }

        final Block clickedBlock = event.getWorld().getBlockState(event.getPos()).getBlock();
        final ItemStack itemInHand = event.getItemStack();
        final Vec3d location = clickedBlock == null ? null : new Vec3d(event.getPos().getX(), event.getPos().getY(), event.getPos().getZ());
        final GDPlayerData playerData = this.dataStore.getOrCreateGlobalPlayerData(player.getUniqueID());
        final Object source = itemInHand != null ? itemInHand : player;
        if (playerData.claimMode) {
            return;
        }
        // check give pet
        if (playerData.petRecipientUniqueId != null) {
            playerData.petRecipientUniqueId = null;
            GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().COMMAND_PET_TRANSFER_CANCEL);
            event.setCanceled(true);
            return;
        }

        if (location == null) {
            return;
        }

        GDTimings.PLAYER_INTERACT_BLOCK_PRIMARY_EVENT.startTiming();
        final GDClaim claim = this.dataStore.getClaimAt(location);
        final Tristate result = GDPermissionManager.getInstance().getFinalPermission(event, location, claim, Flags.INTERACT_BLOCK_PRIMARY, source, clickedBlock.getDefaultState(), player, TrustTypes.BUILDER, true);
        if (result == Tristate.FALSE) {
            if (GriefDefenderPlugin.isTargetIdBlacklisted(Flags.BLOCK_BREAK.toString(), clickedBlock.getDefaultState(), UUID.nameUUIDFromBytes(player.world.getWorldInfo().getWorldName().getBytes()))) {
                GDTimings.PLAYER_INTERACT_BLOCK_PRIMARY_EVENT.stopTiming();
                return;
            }
            if (GDPermissionManager.getInstance().getFinalPermission(event, location, claim, Flags.BLOCK_BREAK, player, clickedBlock.getDefaultState(), player, TrustTypes.BUILDER, true) == Tristate.TRUE) {
                GDTimings.PLAYER_INTERACT_BLOCK_PRIMARY_EVENT.stopTiming();
                return;
            }

            // Don't send a deny message if the player is in claim mode or is holding an investigation tool
            if (!playerData.claimMode && (GriefDefenderPlugin.getInstance().investigationTool == null || !NMSUtil.getInstance().hasItemInOneHand(player, GriefDefenderPlugin.getInstance().investigationTool))) {
                this.sendInteractBlockDenyMessage(itemInHand, clickedBlock, claim, player, playerData);
            }
            event.setCanceled(true);
            GDTimings.PLAYER_INTERACT_BLOCK_PRIMARY_EVENT.stopTiming();
            return;
        }
        GDTimings.PLAYER_INTERACT_BLOCK_PRIMARY_EVENT.stopTiming();
    }

/*    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteractBlockSecondary(PlayerInteractEvent event) {
        final Player player = event.getPlayer();
        final GDPlayerData playerData = this.dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.PHYSICAL) {
            onPlayerInteractBlockPrimary(event, player);
            return;
        }

        final Block clickedBlock = event.getClickedBlock();
        BlockState state = null;
        if (clickedBlock != null) {
            state = clickedBlock.getState();
        }
        final ItemStack itemInHand = event.getItem();
        final boolean hasTileEntity = clickedBlock != null ? NMSUtil.getInstance().hasBlockTileEntity(clickedBlock.getLocation()) : false;
        if (hasTileEntity && !(state instanceof Sign)) {
            onInventoryOpen(event, state.getLocation(), state, player);
            return;
        }
        GDCauseStackManager.getInstance().pushCause(player);
        if (!GriefDefenderPlugin.getInstance().claimsEnabledForWorld(player.getWorld().getUID())) {
            return;
        }
        if (GriefDefenderPlugin.isTargetIdBlacklisted(Flags.INTERACT_BLOCK_SECONDARY.getName(), event.getClickedBlock(), player.getWorld().getUID())) {
            return;
        }

        GDTimings.PLAYER_INTERACT_BLOCK_SECONDARY_EVENT.startTiming();
        final Object source = player;
        final Location location = event.getClickedBlock() != null ? event.getClickedBlock().getLocation() : null;

        if (NMSUtil.getInstance().isMainHandSlot(event.getHand()) && (playerData.claimMode || (itemInHand != null && GriefDefenderPlugin.getInstance().modificationTool != null && NMSUtil.getInstance().itemsEqual(itemInHand, GriefDefenderPlugin.getInstance().modificationTool)))) {
            onPlayerHandleClaimCreateAction(event, clickedBlock, player, itemInHand, playerData);
            // avoid changing blocks after using a shovel
            event.setUseInteractedBlock(Result.DENY);
            GDTimings.PLAYER_INTERACT_BLOCK_SECONDARY_EVENT.stopTiming();
            return;
        }

        if (location == null) {
            GDTimings.PLAYER_INTERACT_BLOCK_SECONDARY_EVENT.stopTiming();
            return;
        }
        final GDClaim claim = this.dataStore.getClaimAt(location);
        TrustType trustType = event.isBlockInHand() && event.getAction() != Action.PHYSICAL ? TrustTypes.BUILDER : TrustTypes.ACCESSOR;
        if (clickedBlock != null && clickedBlock.getType().toString().contains("DOOR")) {
            trustType = TrustTypes.ACCESSOR;
        }
        if (GDFlags.INTERACT_BLOCK_SECONDARY && playerData != null) {
            Flag flag = Flags.INTERACT_BLOCK_SECONDARY;
            if (event.getAction() == Action.PHYSICAL) {
                flag = Flags.COLLIDE_BLOCK;
            }
            Tristate result = GDPermissionManager.getInstance().getFinalPermission(event, location, claim, flag, source, clickedBlock, player, trustType, true);
            if (result == Tristate.FALSE) {
                // if player is holding an item, check if it can be placed
                if (GDFlags.BLOCK_PLACE && itemInHand != null && itemInHand.getType().isBlock()) {
                    if (GriefDefenderPlugin.isTargetIdBlacklisted(Flags.BLOCK_PLACE.getName(), itemInHand, player.getWorld().getUID())) {
                        GDTimings.PLAYER_INTERACT_BLOCK_SECONDARY_EVENT.stopTiming();
                        return;
                    }
                    if (GDPermissionManager.getInstance().getFinalPermission(event, location, claim, Flags.BLOCK_PLACE, source, itemInHand, player, TrustTypes.BUILDER, true) == Tristate.TRUE) {
                        GDTimings.PLAYER_INTERACT_BLOCK_SECONDARY_EVENT.stopTiming();
                        return;
                    }
                }
                // Don't send a deny message if the player is in claim mode or is holding an investigation tool
                if (lastInteractItemCancelled != true) {
                    if (!playerData.claimMode && (GriefDefenderPlugin.getInstance().investigationTool == null || !NMSUtil.getInstance().hasItemInOneHand(player, GriefDefenderPlugin.getInstance().investigationTool))) {
                        if (event.getAction() == Action.PHYSICAL) {
                            if (player.getWorld().getTime() % 100 == 0L) {
                                this.sendInteractBlockDenyMessage(itemInHand, clickedBlock, claim, player, playerData);
                            }
                        } else {
                            this.sendInteractBlockDenyMessage(itemInHand, clickedBlock, claim, player, playerData);
                        }
                    }
                }

                event.setCancelled(true);
                GDTimings.PLAYER_INTERACT_BLOCK_SECONDARY_EVENT.stopTiming();
                return;
            }
        }

        GDTimings.PLAYER_INTERACT_BLOCK_SECONDARY_EVENT.stopTiming();
    }*/

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPlayerTeleport(EnderTeleportEvent event) {
        if (event.getEntity() instanceof ServerPlayerEntity) {
            final ServerPlayerEntity player = (ServerPlayerEntity) event.getEntity();
            if (VecHelper.toVector3i(player.getPositionVector()).equals(VecHelper.toVector3i(new Vec3d(event.getTargetX(), event.getTargetY(), event.getTargetZ())))) {
                // Ignore teleports that have the same block position
                // This prevents players from getting through doors without permission
                return;
            }
            GDCauseStackManager.getInstance().pushCause(player);
            if (!GDFlags.ENTITY_TELEPORT_FROM && !GDFlags.ENTITY_TELEPORT_TO) {
                return;
            }

            if (!GriefDefenderPlugin.getInstance().claimsEnabledForWorld(UUID.nameUUIDFromBytes(player.world.getWorldInfo().getWorldName().getBytes()))) {
                return;
            }
            final boolean teleportFromBlacklisted = GriefDefenderPlugin.isSourceIdBlacklisted(Flags.ENTITY_TELEPORT_FROM.getName(), player, UUID.nameUUIDFromBytes(player.world.getWorldInfo().getWorldName().getBytes()));
            final boolean teleportToBlacklisted = GriefDefenderPlugin.isSourceIdBlacklisted(Flags.ENTITY_TELEPORT_TO.getName(), player, UUID.nameUUIDFromBytes(player.world.getWorldInfo().getWorldName().getBytes()));
            if (teleportFromBlacklisted && teleportToBlacklisted) {
                return;
            }

            GDTimings.ENTITY_TELEPORT_EVENT.startTiming();

            final Vec3d sourceLocation = player.getPositionVector();
            final Vec3d destination = new Vec3d(event.getTargetX(), event.getTargetY(), event.getTargetZ());
            final GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(player.world, UUID.nameUUIDFromBytes(player.world.getWorldInfo().getWorldName().getBytes()));
            final GDClaim sourceClaim = this.dataStore.getClaimAtPlayer(playerData, player.getPositionVector());
            // Cancel event if player is unable to teleport during PvP combat
            final boolean pvpCombatTeleport = GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Boolean.class), player, Options.PVP_COMBAT_TELEPORT, sourceClaim);
            if (!pvpCombatTeleport) {
                final int combatTimeRemaining = playerData.getPvpCombatTimeRemaining();
                if (combatTimeRemaining > 0) {
                    final Component denyMessage = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.PVP_IN_COMBAT_NOT_ALLOWED,
                            ImmutableMap.of(
                                    "time-remaining", combatTimeRemaining));
                    GriefDefenderPlugin.sendMessage(player, denyMessage);
                    event.setCanceled(true);
                    GDTimings.ENTITY_TELEPORT_EVENT.stopTiming();
                    return;
                }
            }

            // Handle BorderClaimEvent
            if (!CommonEntityEventHandler.getInstance().onEntityMove(event, sourceLocation, destination, player)) {
                event.setCanceled(true);
                GDTimings.ENTITY_TELEPORT_EVENT.stopTiming();
                return;
            }

            if (sourceClaim != null) {
                final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.PERMISSION_PORTAL_EXIT,
                        ImmutableMap.of(
                                "player", sourceClaim.getOwnerName()));
                if (GDFlags.ENTITY_TELEPORT_FROM && !teleportFromBlacklisted && GDPermissionManager.getInstance().getFinalPermission(event, sourceLocation, sourceClaim, Flags.ENTITY_TELEPORT_FROM, player, player, TrustTypes.ACCESSOR, true) == Tristate.FALSE) {
                    if (player != null) {
                        GriefDefenderPlugin.sendMessage(player, message);
                    }

                    event.setCanceled(true);
                    GDTimings.ENTITY_TELEPORT_EVENT.stopTiming();
                    return;
                } else if (GDFlags.EXIT_CLAIM && !teleportFromBlacklisted && GDPermissionManager.getInstance().getFinalPermission(event, sourceLocation, sourceClaim, Flags.EXIT_CLAIM, player, player, TrustTypes.ACCESSOR, true) == Tristate.FALSE) {
                    if (player != null) {
                        GriefDefenderPlugin.sendMessage(player, message);
                    }

                    event.setCanceled(true);
                    GDTimings.ENTITY_TELEPORT_EVENT.stopTiming();
                    return;
                }
            }

            // check if destination world is enabled
            final World toWorld = player.world;
            if (!GriefDefenderPlugin.getInstance().claimsEnabledForWorld(UUID.nameUUIDFromBytes(player.world.getWorldInfo().getWorldName().getBytes()))) {
                GDTimings.ENTITY_TELEPORT_EVENT.stopTiming();
                return;
            }

            final GDClaim toClaim = this.dataStore.getClaimAt(destination);
            if (toClaim != null) {
                final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.PERMISSION_PORTAL_ENTER,
                        ImmutableMap.of(
                                "player", toClaim.getOwnerName()));
                if (GDFlags.ENTITY_TELEPORT_TO && !teleportToBlacklisted && GDPermissionManager.getInstance().getFinalPermission(event, destination, toClaim, Flags.ENTITY_TELEPORT_TO, player, player, TrustTypes.ACCESSOR, true) == Tristate.FALSE) {
                    if (player != null) {
                        GriefDefenderPlugin.sendMessage(player, message);
                    }

                    event.setCanceled(true);
                    GDTimings.ENTITY_TELEPORT_EVENT.stopTiming();
                    return;
                } else if (GDFlags.ENTER_CLAIM && !teleportToBlacklisted && GDPermissionManager.getInstance().getFinalPermission(event, destination, toClaim, Flags.ENTER_CLAIM, player, player, TrustTypes.ACCESSOR, true) == Tristate.FALSE) {
                    if (player != null) {
                        GriefDefenderPlugin.sendMessage(player, message);
                    }

                    event.setCanceled(true);
                    GDTimings.ENTITY_TELEPORT_EVENT.stopTiming();
                    return;
                }
            }

            if (player != null && !UUID.nameUUIDFromBytes(player.world.getWorldInfo().getWorldName().getBytes()).equals(UUID.nameUUIDFromBytes(player.world.getWorldInfo().getWorldName().getBytes()))) {
                // new world, check if player has world storage for it
                GDClaimManager claimWorldManager = GriefDefenderPlugin.getInstance().dataStore.getClaimWorldManager(UUID.nameUUIDFromBytes(player.world.getWorldInfo().getWorldName().getBytes()));

                // update lastActive timestamps for claims this player owns
                UUID playerUniqueId = player.getUniqueID();
                for (Claim claim : this.dataStore.getClaimWorldManager(UUID.nameUUIDFromBytes(player.world.getWorldInfo().getWorldName().getBytes())).getWorldClaims()) {
                    if (claim.getOwnerUniqueId().equals(playerUniqueId)) {
                        // update lastActive timestamp for claim
                        claim.getData().setDateLastActive(Instant.now());
                        claimWorldManager.addClaim(claim);
                    } else if (claim.getParent().isPresent() && claim.getParent().get().getOwnerUniqueId().equals(playerUniqueId)) {
                        // update lastActive timestamp for subdivisions if parent owner logs on
                        claim.getData().setDateLastActive(Instant.now());
                        claimWorldManager.addClaim(claim);
                    }
                }
            }

            if (playerData != null) {
                if (toClaim.isTown()) {
                    playerData.inTown = true;
                } else {
                    playerData.inTown = false;
                }
            }
            // TODO
        /*if (event.getCause().first(PortalTeleportCause.class).isPresent()) {
            // FEATURE: when players get trapped in a nether portal, send them back through to the other side
            CheckForPortalTrapTask task = new CheckForPortalTrapTask(player, event.getFromTransform().getLocation());
            Sponge.getGame().getScheduler().createTaskBuilder().delayTicks(200).execute(task).submit(GriefDefender.instance);
        }*/
            GDTimings.ENTITY_TELEPORT_EVENT.stopTiming();
        }
    }

    private void onPlayerHandleClaimCreateAction(PlayerInteractEvent event, Block clickedBlock, ServerPlayerEntity player, ItemStack itemInHand, GDPlayerData playerData) {
        if (player.isSneaking() && (event instanceof PlayerInteractEvent.RightClickBlock || event instanceof PlayerInteractEvent.RightClickEmpty)) {
            playerData.revertActiveVisual(player);
            playerData.lastShovelLocation = null;
            playerData.endShovelLocation = null;
            playerData.claimResizing = null;
            playerData.shovelMode = ShovelTypes.BASIC;
            return;
        }

        GDTimings.PLAYER_HANDLE_SHOVEL_ACTION.startTiming();
        Vec3d location = clickedBlock != null ? new Vec3d(event.getPos().getX(), event.getPos().getY(), event.getPos().getZ()) : null;

        if (location == null) {
            boolean ignoreAir = false;
            final int distance = !ignoreAir ? 100 : 5;
            location = BlockUtil.getInstance().getTargetBlock(player, playerData, distance, ignoreAir).orElse(null);
            if (location == null) {
                GDTimings.PLAYER_HANDLE_SHOVEL_ACTION.stopTiming();
                return;
            }
        }

        // Always cancel to avoid breaking blocks such as grass
        event.setCanceled(true);
        playerData = this.dataStore.getOrCreatePlayerData(player.world, player.getUniqueID());

        if (!playerData.canCreateClaim(player, true)) {
            GDTimings.PLAYER_HANDLE_SHOVEL_ACTION.stopTiming();
            return;
        }

        if (playerData.claimResizing != null && playerData.lastShovelLocation != null) {
            handleResizeFinish(event, player, location, playerData);
            GDTimings.PLAYER_HANDLE_SHOVEL_ACTION.stopTiming();
            return;
        }

        GDClaim claim = this.dataStore.getClaimAtPlayer(location, playerData, true);
        if (!claim.isWilderness()) {
            Component noEditReason = claim.allowEdit(player);
            if (noEditReason != null) {
                final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.CREATE_OVERLAP_PLAYER,
                        ImmutableMap.of(
                                "player", claim.getOwnerName()));
                GriefDefenderPlugin.sendMessage(player, message);
                ClaimVisual visualization = new ClaimVisual(claim, ClaimVisual.ERROR);
                visualization.createClaimBlockVisuals(location.y, player.getPositionVector(), playerData);
                visualization.apply(player);
                Set<Claim> claims = new HashSet<>();
                claims.add(claim);
                CommandHelper.showClaims(player, claims, location.y, true);
            } else if (playerData.lastShovelLocation == null && BlockUtil.getInstance().clickedClaimCorner(claim, VecHelper.toVector3i(location))) {
                handleResizeStart(event, player, location, playerData, claim);
            } else if ((playerData.shovelMode == ShovelTypes.SUBDIVISION
                    || ((claim.isTown() || claim.isAdminClaim()) && (playerData.lastShovelLocation == null || playerData.claimSubdividing != null)) && playerData.shovelMode != ShovelTypes.TOWN)) {
                if (claim.getTownClaim() != null && playerData.shovelMode == ShovelTypes.TOWN) {
                    GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().CREATE_OVERLAP_SHORT);
                    Set<Claim> claims = new HashSet<>();
                    claims.add(claim);
                    CommandHelper.showClaims(player, claims, location.y, true);
                } else if (playerData.lastShovelLocation == null) {
                    createSubdivisionStart(event, player, location, playerData, claim);
                } else if (playerData.claimSubdividing != null) {
                    createSubdivisionFinish(event, player, location, playerData, claim);
                }
            } else {
                GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().CREATE_OVERLAP);
                Set<Claim> claims = new HashSet<>();
                claims.add(claim);
                CommandHelper.showClaims(player, claims, location.y, true);
            }
            GDTimings.PLAYER_HANDLE_SHOVEL_ACTION.stopTiming();
            return;
        } else if (playerData.shovelMode == ShovelTypes.SUBDIVISION && playerData.lastShovelLocation != null) {
            GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().CREATE_SUBDIVISION_FAIL);
            playerData.lastShovelLocation = null;
            GDTimings.PLAYER_HANDLE_SHOVEL_ACTION.stopTiming();
            return;
        }

        Vec3d lastShovelLocation = playerData.lastShovelLocation;
        if (lastShovelLocation == null) {
            createClaimStart(event, player, location, playerData, claim);
            GDTimings.PLAYER_HANDLE_SHOVEL_ACTION.stopTiming();
            return;
        }

        createClaimFinish(event, player, location, playerData, claim);
        GDTimings.PLAYER_HANDLE_SHOVEL_ACTION.stopTiming();
    }

    private void createClaimStart(PlayerInteractEvent.RightClickItem event, ServerPlayerEntity player, Vec3d location, GDPlayerData playerData, GDClaim claim) {
        final WorldGuardProvider worldGuardProvider = GriefDefenderPlugin.getInstance().getWorldGuardProvider();

        if (!PermissionUtil.getInstance().holderHasPermission(player, GDPermissions.BYPASS_CLAIM_LIMIT)) {
            int createClaimLimit = -1;
            if (playerData.shovelMode == ShovelTypes.BASIC && (claim.isAdminClaim() || claim.isTown() || claim.isWilderness())) {
                createClaimLimit = GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Integer.class), player, Options.CREATE_LIMIT, claim).intValue();
            } else if (playerData.shovelMode == ShovelTypes.TOWN && (claim.isAdminClaim() || claim.isWilderness())) {
                createClaimLimit = GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Integer.class), player, Options.CREATE_LIMIT, claim).intValue();
            } else if (playerData.shovelMode == ShovelTypes.SUBDIVISION && !claim.isWilderness()) {
                createClaimLimit = GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Integer.class), player, Options.CREATE_LIMIT, claim).intValue();
            }

            if (createClaimLimit > 0 && createClaimLimit < (playerData.getInternalClaims().size() + 1)) {
                GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.CREATE_FAILED_CLAIM_LIMIT));
                return;
            }
        }

        final int minClaimLevel = playerData.getMinClaimLevel();
        if (playerData.shovelMode != ShovelTypes.ADMIN && location.y < minClaimLevel) {
            final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.CLAIM_BELOW_LEVEL,
                    ImmutableMap.of(
                            "limit", minClaimLevel));
            GriefDefenderPlugin.sendMessage(player, message);
            return;
        }
        final int maxClaimLevel = playerData.getMaxClaimLevel();
        if (playerData.shovelMode != ShovelTypes.ADMIN && location.y > maxClaimLevel) {
            final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.CLAIM_ABOVE_LEVEL,
                    ImmutableMap.of(
                            "limit", maxClaimLevel));
            GriefDefenderPlugin.sendMessage(player, message);
            return;
        }

        if (playerData.shovelMode == ShovelTypes.SUBDIVISION && claim.isWilderness()) {
            GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().CREATE_SUBDIVISION_FAIL);
            return;
        }

        final ClaimType type = PlayerUtil.getInstance().getClaimTypeFromShovel(playerData.shovelMode);

        playerData.lastShovelLocation = location;
        Component message = null;
        if (playerData.claimMode) {
            message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.CLAIM_MODE_START,
                    ImmutableMap.of(
                            "type", PlayerUtil.getInstance().getClaimTypeComponentFromShovel(playerData.shovelMode)));
        } else {
            message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.CLAIM_START,
                    ImmutableMap.of(
                            "type", PlayerUtil.getInstance().getClaimTypeComponentFromShovel(playerData.shovelMode),
                            "item", ItemTypeRegistryModule.getInstance().getNMSKey(event.getItemStack())));
        }
        GriefDefenderPlugin.sendMessage(player, message);
        ClaimVisual visual = ClaimVisual.fromClick(location, location.y, PlayerUtil.getInstance().getVisualTypeFromShovel(playerData.shovelMode), player, playerData);
        visual.apply(player, false);
    }

    private void createClaimFinish(PlayerInteractEvent.RightClickItem event, ServerPlayerEntity player, Vec3d location, GDPlayerData playerData, GDClaim claim) {
        final WorldGuardProvider worldGuardProvider = GriefDefenderPlugin.getInstance().getWorldGuardProvider();

        Vec3d lastShovelLocation = playerData.lastShovelLocation;
        final GDClaim firstClaim = GriefDefenderPlugin.getInstance().dataStore.getClaimAtPlayer(playerData.lastShovelLocation, playerData, true);
        final GDClaim clickedClaim = GriefDefenderPlugin.getInstance().dataStore.getClaimAtPlayer(location, playerData, true);
        if (!firstClaim.equals(clickedClaim)) {
            final GDClaim overlapClaim = firstClaim.isWilderness() ? clickedClaim : firstClaim;
            GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().CREATE_OVERLAP_SHORT);
            Set<Claim> claims = new HashSet<>();
            claims.add(overlapClaim);
            CommandHelper.showClaims(player, claims, location.y, true);
            return;
        }

        final boolean cuboid = playerData.getClaimCreateMode() == CreateModeTypes.VOLUME;
        Vector3i lesserBoundaryCorner = new Vector3i(
                lastShovelLocation.x,
                cuboid ? lastShovelLocation.y : playerData.getMinClaimLevel(),
                lastShovelLocation.z);
        Vector3i greaterBoundaryCorner = new Vector3i(
                location.x,
                cuboid ? location.y : playerData.getMaxClaimLevel(),
                location.z);

        final ClaimType type = PlayerUtil.getInstance().getClaimTypeFromShovel(playerData.shovelMode);
        if ((type == ClaimTypes.BASIC || type == ClaimTypes.TOWN) && GriefDefenderPlugin.getGlobalConfig().getConfig().economy.economyMode) {
            EconomyUtil.getInstance().economyCreateClaimConfirmation(player, playerData, location.y, lesserBoundaryCorner, greaterBoundaryCorner, PlayerUtil.getInstance().getClaimTypeFromShovel(playerData.shovelMode),
                    cuboid, playerData.claimSubdividing);
            return;
        }

        ClaimResult result = this.dataStore.createClaim(
                player.world,
                lesserBoundaryCorner,
                greaterBoundaryCorner,
                type, player.getUniqueID(), cuboid);

        GDClaim gdClaim = (GDClaim) result.getClaim().orElse(null);
        if (!result.successful()) {
            if (result.getResultType() == ClaimResultType.OVERLAPPING_CLAIM) {
                GDClaim overlapClaim = (GDClaim) result.getClaim().get();
                GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().CREATE_OVERLAP_SHORT);
                Set<Claim> claims = new HashSet<>();
                claims.add(overlapClaim);
                CommandHelper.showOverlapClaims(player, claims, location.y);
            } else if (result.getResultType() == ClaimResultType.CLAIM_EVENT_CANCELLED) {
                GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().CREATE_CANCEL);
            } else {
                GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.CREATE_FAILED_RESULT,
                        ImmutableMap.of("reason", result.getResultType())));
            }
            return;
        } else {
            playerData.lastShovelLocation = null;
            final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.CREATE_SUCCESS,
                    ImmutableMap.of(
                            "type", gdClaim.getFriendlyNameType(true)));
            GriefDefenderPlugin.sendMessage(player, message);
            if (this.worldEditProvider != null) {
                this.worldEditProvider.stopVisualDrag(player);
                this.worldEditProvider.visualizeClaim(gdClaim, player, playerData, false);
            }
            gdClaim.getVisualizer().createClaimBlockVisuals(location.y, player.getPositionVector(), playerData);
            gdClaim.getVisualizer().apply(player, false);
        }
    }

    private void createSubdivisionStart(PlayerInteractEvent.RightClickItem event, ServerPlayerEntity player, Vec3d location, GDPlayerData playerData, GDClaim claim) {
        final int minClaimLevel = playerData.getMinClaimLevel();
        if (playerData.shovelMode != ShovelTypes.ADMIN && location.y < minClaimLevel) {
            final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.CLAIM_BELOW_LEVEL,
                    ImmutableMap.of(
                            "limit", minClaimLevel));
            GriefDefenderPlugin.sendMessage(player, message);
            return;
        }
        final int maxClaimLevel = playerData.getMaxClaimLevel();
        if (playerData.shovelMode != ShovelTypes.ADMIN && location.y > maxClaimLevel) {
            final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.CLAIM_ABOVE_LEVEL,
                    ImmutableMap.of(
                            "limit", maxClaimLevel));
            GriefDefenderPlugin.sendMessage(player, message);
            return;
        }

        if (claim.isSubdivision()) {
            GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().RESIZE_OVERLAP_SUBDIVISION);
        } else {
            Component message = null;
            if (playerData.claimMode) {
                message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.CLAIM_START,
                        ImmutableMap.of(
                                "type", playerData.shovelMode.getName()));
            } else {
                message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.CLAIM_START,
                        ImmutableMap.of(
                                "type", playerData.shovelMode.getName(),
                                "item", ItemTypeRegistryModule.getInstance().getNMSKey(event.getItemStack())));
            }
            GriefDefenderPlugin.sendMessage(player, message);
            playerData.lastShovelLocation = location;
            playerData.claimSubdividing = claim;
            ClaimVisual visualization = ClaimVisual.fromClick(location, location.y, PlayerUtil.getInstance().getVisualTypeFromShovel(playerData.shovelMode), player, playerData);
            visualization.apply(player, false);
        }
    }

    private void createSubdivisionFinish(PlayerInteractEvent.RightClickItem event, ServerPlayerEntity player, Vec3d location, GDPlayerData playerData, GDClaim claim) {
        final GDClaim clickedClaim = GriefDefenderPlugin.getInstance().dataStore.getClaimAt(location);
        if (clickedClaim == null || !playerData.claimSubdividing.getUniqueId().equals(clickedClaim.getUniqueId())) {
            if (clickedClaim != null) {
                GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().CREATE_OVERLAP_SHORT);
                final GDClaim overlapClaim = playerData.claimSubdividing;
                Set<Claim> claims = new HashSet<>();
                claims.add(overlapClaim);
                CommandHelper.showClaims(player, claims, location.y, true);
            }

            return;
        }

        Vector3i lesserBoundaryCorner = new Vector3i(playerData.lastShovelLocation.x,
                playerData.getClaimCreateMode() == CreateModeTypes.VOLUME ? playerData.lastShovelLocation.y : playerData.getMinClaimLevel(),
                playerData.lastShovelLocation.z);
        Vector3i greaterBoundaryCorner = new Vector3i(location.x,
                playerData.getClaimCreateMode() == CreateModeTypes.VOLUME ? location.y : playerData.getMaxClaimLevel(),
                location.z);

        ClaimResult result = this.dataStore.createClaim(player.world,
                lesserBoundaryCorner, greaterBoundaryCorner, PlayerUtil.getInstance().getClaimTypeFromShovel(playerData.shovelMode),
                player.getUniqueID(), playerData.getClaimCreateMode() == CreateModeTypes.VOLUME, playerData.claimSubdividing);

        GDClaim gdClaim = (GDClaim) result.getClaim().orElse(null);
        if (!result.successful()) {
            if (result.getResultType() == ClaimResultType.OVERLAPPING_CLAIM) {
                GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().CREATE_OVERLAP_SHORT);
                Set<Claim> claims = new HashSet<>();
                claims.add(gdClaim);
                CommandHelper.showOverlapClaims(player, claims, location.y);
            }
            event.setCanceled(true);
            return;
        } else {
            playerData.lastShovelLocation = null;
            playerData.claimSubdividing = null;
            final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.CREATE_SUCCESS, ImmutableMap.of(
                    "type", gdClaim.getFriendlyNameType(true)));
            GriefDefenderPlugin.sendMessage(player, message);
            gdClaim.getVisualizer().createClaimBlockVisuals(location.y, player.getPositionVector(), playerData);
            gdClaim.getVisualizer().apply(player, false);
        }
    }

    private void handleResizeStart(PlayerInteractEvent.RightClickItem event, ServerPlayerEntity player, Vec3d location, GDPlayerData playerData, GDClaim claim) {
        boolean playerCanResize = true;
        if (!PermissionUtil.getInstance().holderHasPermission(player, GDPermissions.CLAIM_RESIZE_ALL)
                && !playerData.canIgnoreClaim(claim)
                && !claim.isUserTrusted(player.getUniqueID(), TrustTypes.MANAGER)) {

            if (claim.isAdminClaim()) {
                if (!playerData.canManageAdminClaims) {
                    playerCanResize = false;
                }
            } else if (!player.getUniqueID().equals(claim.getOwnerUniqueId()) || !PermissionUtil.getInstance().holderHasPermission(player, GDPermissions.CLAIM_RESIZE)) {
                playerCanResize = false;
            }
            if (!playerCanResize) {
                if (claim.parent != null) {
                    if (claim.parent.isAdminClaim() && claim.isSubdivision()) {
                        playerCanResize = PermissionUtil.getInstance().holderHasPermission(player, (GDPermissions.CLAIM_RESIZE_ADMIN_SUBDIVISION);
                    } else if (claim.parent.isBasicClaim() && claim.isSubdivision()) {
                        playerCanResize = PermissionUtil.getInstance().holderHasPermission(player, (GDPermissions.CLAIM_RESIZE_BASIC_SUBDIVISION);
                    } else if (claim.isTown()) {
                        playerCanResize = PermissionUtil.getInstance().holderHasPermission(player, (GDPermissions.CLAIM_RESIZE_TOWN);
                    } else if (claim.isAdminClaim()) {
                        playerCanResize = PermissionUtil.getInstance().holderHasPermission(player, (GDPermissions.CLAIM_RESIZE_ADMIN);
                    } else {
                        playerCanResize = PermissionUtil.getInstance().holderHasPermission(player, (GDPermissions.CLAIM_RESIZE_BASIC);
                    }
                } else if (claim.isTown()) {
                    playerCanResize = PermissionUtil.getInstance().holderHasPermission(player, (GDPermissions.CLAIM_RESIZE_TOWN);
                } else if (claim.isAdminClaim()) {
                    playerCanResize = PermissionUtil.getInstance().holderHasPermission(player, (GDPermissions.CLAIM_RESIZE_ADMIN);
                } else {
                    playerCanResize = PermissionUtil.getInstance().holderHasPermission(player, (GDPermissions.CLAIM_RESIZE_BASIC);
                }
            }
        }

        if (!claim.getInternalClaimData().isResizable() || !playerCanResize) {
            GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().PERMISSION_CLAIM_RESIZE);
            return;
        }

        playerData.claimResizing = claim;
        playerData.lastShovelLocation = location;
        // Show visual block for resize corner click
        ClaimVisual visual = ClaimVisual.fromClick(location, location.y, PlayerUtil.getInstance().getVisualTypeFromShovel(playerData.shovelMode), player, playerData);
        visual.apply(player, false);
        GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().RESIZE_START);
    }

    private void handleResizeFinish(PlayerInteractEvent.RightClickItem event, ServerPlayerEntity player, Vec3d location, GDPlayerData playerData) {
        if (location.equals(playerData.lastShovelLocation)) {
            return;
        }

        playerData.endShovelLocation = location;
        double newx1, newx2, newz1, newz2, newy1, newy2;
        int smallX = 0, smallY = 0, smallZ = 0, bigX = 0, bigY = 0, bigZ = 0;

        newx1 = playerData.lastShovelLocation.getX();
        newx2 = location.x;
        newy1 = playerData.lastShovelLocation.getY();
        newy2 = location.y;
        newz1 = playerData.lastShovelLocation.getZ();
        newz2 = location.z;
        Vector3i lesserBoundaryCorner = playerData.claimResizing.getLesserBoundaryCorner();
        Vector3i greaterBoundaryCorner = playerData.claimResizing.getGreaterBoundaryCorner();
        smallX = lesserBoundaryCorner.getX();
        smallY = lesserBoundaryCorner.getY();
        smallZ = lesserBoundaryCorner.getZ();
        bigX = greaterBoundaryCorner.getX();
        bigY = greaterBoundaryCorner.getY();
        bigZ = greaterBoundaryCorner.getZ();

        if (newx1 == smallX) {
            smallX = newx2;
        } else {
            bigX = newx2;
        }

        if (newy1 == smallY) {
            smallY = newy2;
        } else {
            bigY = newy2;
        }

        if (newz1 == smallZ) {
            smallZ = newz2;
        } else {
            bigZ = newz2;
        }

        ClaimResult claimResult = null;
        claimResult = playerData.claimResizing.resize(smallX, bigX, smallY, bigY, smallZ, bigZ);
        if (claimResult.successful()) {
            Claim claim = (GDClaim) claimResult.getClaim().get();
            int claimBlocksRemaining = playerData.getRemainingClaimBlocks();
            ;
            if (!playerData.claimResizing.isAdminClaim()) {
                UUID ownerID = playerData.claimResizing.getOwnerUniqueId();
                if (playerData.claimResizing.parent != null) {
                    ownerID = playerData.claimResizing.parent.getOwnerUniqueId();
                }

                if (ownerID.equals(player.getUniqueID())) {
                    claimBlocksRemaining = playerData.getRemainingClaimBlocks();
                } else {
                    GDPlayerData ownerData = this.dataStore.getOrCreatePlayerData(player.world, ownerID);
                    claimBlocksRemaining = ownerData.getRemainingClaimBlocks();
                    final ServerPlayerEntity owner = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayerByUUID(ownerID);
                    if (owner == null || !owner.isHandActive()) {
                        this.dataStore.clearCachedPlayerData(UUID.nameUUIDFromBytes(player.world.getWorldInfo().getWorldName().getBytes()), ownerID);
                    }
                }
            }

            playerData.claimResizing = null;
            playerData.lastShovelLocation = null;
            playerData.endShovelLocation = null;
            if (GriefDefenderPlugin.CLAIM_BLOCK_SYSTEM == ClaimBlockSystem.VOLUME) {
                final double claimableChunks = claimBlocksRemaining / 65536.0;
                final Map<String, Object> params = ImmutableMap.of(
                        "chunk-amount", Math.round(claimableChunks * 100.0) / 100.0,
                        "block-amount", claimBlocksRemaining);
                GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.RESIZE_SUCCESS_3D, params));
            } else {
                final Map<String, Object> params = ImmutableMap.of(
                        "block-amount", claimBlocksRemaining);
                GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.RESIZE_SUCCESS_2D, params));
            }
            playerData.revertActiveVisual(player);
            ((GDClaim) claim).getVisualizer().resetVisuals();
            ((GDClaim) claim).getVisualizer().createClaimBlockVisuals(location.y, player.getPositionVector(), playerData);
            ((GDClaim) claim).getVisualizer().apply(player);
            if (this.worldEditProvider != null) {
                this.worldEditProvider.visualizeClaim(claim, player, playerData, false);
            }
        } else {
            if (claimResult.getResultType() == ClaimResultType.OVERLAPPING_CLAIM) {
                GDClaim overlapClaim = (GDClaim) claimResult.getClaim().get();
                GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().RESIZE_OVERLAP);
                Set<Claim> claims = new HashSet<>();
                claims.add(overlapClaim);
                CommandHelper.showOverlapClaims(player, claims, location.y);
            } else {
                if (!claimResult.getMessage().isPresent()) {
                    GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().CLAIM_NOT_YOURS);
                }
            }

            playerData.claimSubdividing = null;
            event.setCanceled(true);
        }
    }

    private boolean investigateClaim(PlayerInteractEvent event, Player player, Block clickedBlock, ItemStack itemInHand) {
        final GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        if (playerData.claimMode && (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)) {
            if (player.isSneaking()) {
                return true;
            }
            // claim mode inspects with left-click
            return false;
        }
        if (!playerData.claimMode && (itemInHand == null || GriefDefenderPlugin.getInstance().investigationTool == null || !NMSUtil.getInstance().itemsEqual(itemInHand, GriefDefenderPlugin.getInstance().investigationTool))) {
            return false;
        }

        GDTimings.PLAYER_INVESTIGATE_CLAIM.startTiming();
        GDClaim claim = null;
        if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_AIR) {
            final int maxDistance = GDPermissionManager.getInstance().getInternalOptionValue(TypeToken.of(Integer.class), player, Options.RADIUS_INSPECT);
            claim = this.findNearbyClaim(player, maxDistance);
            if (player.isSneaking()) {
                if (!playerData.canIgnoreClaim(claim) && !player.hasPermission(GDPermissions.VISUALIZE_CLAIMS_NEARBY)) {
                    GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().PERMISSION_VISUAL_CLAIMS_NEARBY);
                    GDTimings.PLAYER_INVESTIGATE_CLAIM.stopTiming();
                    return false;
                }

                Location nearbyLocation = playerData.lastValidInspectLocation != null ? playerData.lastValidInspectLocation : player.getLocation();
                Set<Claim> claims = BlockUtil.getInstance().getNearbyClaims(nearbyLocation, maxDistance);
                int height = (int) (playerData.lastValidInspectLocation != null ? playerData.lastValidInspectLocation.getBlockY() : PlayerUtil.getInstance().getEyeHeight(player));

                boolean hideBorders = this.worldEditProvider != null &&
                        this.worldEditProvider.hasCUISupport(player) &&
                        GriefDefenderPlugin.getActiveConfig(player.getWorld().getUID()).getConfig().visual.hideBorders;
                if (!hideBorders) {
                    ClaimVisual visualization = ClaimVisual.fromClaims(claims, PlayerUtil.getInstance().getVisualClaimHeight(playerData, height), player.getLocation(), playerData, null);
                    visualization.apply(player);
                }

                final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.CLAIM_SHOW_NEARBY,
                        ImmutableMap.of(
                                "amount", claims.size()));
                GriefDefenderPlugin.sendMessage(player, message);
                if (!claims.isEmpty()) {

                    if (this.worldEditProvider != null) {
                        worldEditProvider.revertVisuals(player, playerData, null);
                        worldEditProvider.visualizeClaims(claims, player, playerData, true);
                    }
                    CommandHelper.showClaims(player, claims);
                }
                GDTimings.PLAYER_INVESTIGATE_CLAIM.stopTiming();
                return true;
            }
            if (claim != null && claim.isWilderness()) {
                playerData.lastValidInspectLocation = null;
                GDTimings.PLAYER_INVESTIGATE_CLAIM.stopTiming();
                return false;
            }
        } else {
            claim = this.dataStore.getClaimAtPlayer(clickedBlock.getLocation(), playerData, true);
            if (claim.isWilderness()) {
                GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.BLOCK_NOT_CLAIMED));
                GDTimings.PLAYER_INVESTIGATE_CLAIM.stopTiming();
                return false;
            }
        }

        if (claim.getUniqueId() != playerData.visualClaimId) {
            int height = playerData.lastValidInspectLocation != null ? playerData.lastValidInspectLocation.getBlockY() : clickedBlock.getLocation().getBlockY();
            claim.getVisualizer().createClaimBlockVisuals(playerData.getClaimCreateMode() == CreateModeTypes.VOLUME ? height : PlayerUtil.getInstance().getEyeHeight(player), player.getLocation(), playerData);
            claim.getVisualizer().apply(player);
            if (this.worldEditProvider != null) {
                worldEditProvider.visualizeClaim(claim, player, playerData, true);
            }
            Set<Claim> claims = new HashSet<>();
            claims.add(claim);
            CommandHelper.showClaims(player, claims);
        }
        Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.BLOCK_CLAIMED,
                ImmutableMap.of(
                        "player", claim.getOwnerName()));
        GriefDefenderPlugin.sendMessage(player, message);

        GDTimings.PLAYER_INVESTIGATE_CLAIM.stopTiming();
        return true;
    }

    private GDClaim findNearbyClaim(Player player, int maxDistance) {
        if (maxDistance <= 0) {
            maxDistance = 100;
        }
        BlockRay blockRay = BlockRay.from(player).distanceLimit(maxDistance).build();
        GDPlayerData playerData = GriefDefenderPlugin.getInstance().dataStore.getOrCreatePlayerData(player.getWorld(), player.getUniqueId());
        GDClaim claim = null;
        int count = 0;

        while (blockRay.hasNext()) {
            BlockRayHit blockRayHit = blockRay.next();
            Location location = blockRayHit.getLocation();
            claim = this.dataStore.getClaimAt(location);
            if (claim != null && !claim.isWilderness() && (playerData.visualBlocks.isEmpty() || (claim.getUniqueId() != playerData.visualClaimId))) {
                playerData.lastValidInspectLocation = location;
                return claim;
            }

            final Block block = location.getBlock();
            if (!block.isEmpty() && !NMSUtil.getInstance().isBlockTransparent(block)) {
                break;
            }
            count++;
        }

        if (count == maxDistance) {
            GriefDefenderPlugin.sendMessage(player, MessageCache.getInstance().CLAIM_TOO_FAR);
        } else if (claim != null && claim.isWilderness()) {
            GriefDefenderPlugin.sendMessage(player, GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.BLOCK_NOT_CLAIMED));
        }

        return claim;
    }

    private void sendInteractBlockDenyMessage(ItemStack playerItem, Block block, GDClaim claim, Player player, GDPlayerData playerData) {
        if (claim.getData() != null && !claim.getData().allowDenyMessages()) {
            return;
        }

        if (claim.getData() != null && claim.getData().isExpired() && GriefDefenderPlugin.getActiveConfig(player.getWorld().getUID()).getConfig().claim.bankTaxSystem) {
            playerData.sendTaxExpireMessage(player, claim);
        } else if (playerItem == null || playerItem.getType() == Material.AIR) {
            final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.PERMISSION_INTERACT_BLOCK,
                    ImmutableMap.of(
                            "player", claim.getOwnerName(),
                            "block", BlockTypeRegistryModule.getInstance().getNMSKey(block)));
            GriefDefenderPlugin.sendClaimDenyMessage(claim, player, message);
        } else {
            final Component message = GriefDefenderPlugin.getInstance().messageData.getMessage(MessageStorage.PERMISSION_INTERACT_ITEM_BLOCK,
                    ImmutableMap.of(
                            "item", ItemTypeRegistryModule.getInstance().getNMSKey(playerItem),
                            "block", BlockTypeRegistryModule.getInstance().getNMSKey(block)));
            GriefDefenderPlugin.sendClaimDenyMessage(claim, player, message);
        }
    }
}