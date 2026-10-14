package com.sigmayraffykaw.crophelper;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class CropHelperClient implements ClientModInitializer {
    private static boolean farming;
    private static int tickCounter;
    private static BlockPos pendingReplant;
    private static Item pendingSeed;
    private static final int RADIUS = 4;

    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("farm")
                .then(ClientCommandManager.literal("start").executes(context -> {
                    farming = true;
                    context.getSource().sendFeedback(Component.literal("Crop Helper: farming enabled. Stay near your crops; use /farm stop to pause."));
                    return 1;
                }))
                .then(ClientCommandManager.literal("stop").executes(context -> {
                    farming = false;
                    pendingReplant = null;
                    context.getSource().sendFeedback(Component.literal("Crop Helper: farming paused."));
                    return 1;
                }))
                .then(ClientCommandManager.literal("status").executes(context -> {
                    context.getSource().sendFeedback(Component.literal("Crop Helper: " + (farming ? "RUNNING" : "PAUSED") + ". Range: " + RADIUS + " blocks. Use /farm scan to inspect crops."));
                    return 1;
                }))
                .then(ClientCommandManager.literal("scan").executes(context -> {
                    Minecraft client = Minecraft.getInstance();
                    if (client.level == null || client.player == null) {
                        context.getSource().sendFeedback(Component.literal("Join a world first."));
                        return 0;
                    }
                    int mature = 0;
                    int growing = 0;
                    for (BlockPos pos : scanPositions(client.player.blockPosition(), RADIUS)) {
                        BlockState state = client.level.getBlockState(pos);
                        if (state.getBlock() instanceof CropBlock crop) {
                            if (state.getValue(crop.getAgeProperty()) >= crop.getMaxAge()) mature++;
                            else growing++;
                        }
                    }
                    context.getSource().sendFeedback(Component.literal("Crop scan (" + RADIUS + "-block radius): " + mature + " mature crops, " + growing + " still growing. This scan does not include crops outside the area."));
                    return 1;
                }))
                .then(ClientCommandManager.literal("build")
                    .then(ClientCommandManager.argument("schematic", StringArgumentType.word())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(List.of("small_wheat", "carrot_patch", "potato_patch", "beetroot_patch"), builder))
                        .executes(context -> {
                            String name = StringArgumentType.getString(context, "schematic");
                            context.getSource().sendFeedback(Component.literal("Schematic '" + name + "' is not loaded yet. The schematic planner/build placement system is the next feature; no blocks were placed."));
                            return 0;
                        })))
            );
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> tickFarm(client));
    }

    private static void tickFarm(Minecraft client) {
        if (!farming || client.level == null || client.player == null || client.gameMode == null) return;
        if (++tickCounter % 8 != 0) return;

        if (pendingReplant != null) {
            BlockPos cropPos = pendingReplant;
            Item seed = pendingSeed;
            pendingReplant = null;
            pendingSeed = null;
            if (client.level.isEmptyBlock(cropPos) && client.level.getBlockState(cropPos.below()).is(Blocks.FARMLAND)) {
                int slot = findHotbarItem(client, seed);
                if (slot >= 0) {
                    client.player.getInventory().setSelectedSlot(slot);
                    BlockPos soil = cropPos.below();
                    BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(soil), Direction.UP, soil, false);
                    client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, hit);
                }
            }
            return;
        }

        for (BlockPos pos : scanPositions(client.player.blockPosition(), RADIUS)) {
            BlockState state = client.level.getBlockState(pos);
            if (!(state.getBlock() instanceof CropBlock crop)) continue;
            if (state.getValue(crop.getAgeProperty()) < crop.getMaxAge()) continue;
            Item seed = seedFor(state.getBlock());
            if (seed == null) continue;
            // Require the matching replant item before harvesting, to avoid leaving empty farmland.
            if (findAnyMatchingSeed(client, seed) < 0) continue;
            client.gameMode.startDestroyBlock(pos, Direction.UP);
            client.gameMode.continueDestroyBlock(pos, Direction.UP);
            pendingReplant = pos.immutable();
            pendingSeed = seed;
            return; // One crop per interval keeps interaction rate modest.
        }
    }

    private static int findAnyMatchingSeed(Minecraft client, Item item) {
        if (client.player == null) return -1;
        for (int i = 0; i < client.player.getInventory().getContainerSize(); i++) {
            ItemStack stack = client.player.getInventory().getItem(i);
            if (stack.is(item)) return i < 9 ? i : -1;
        }
        return -1;
    }

    private static int findHotbarItem(Minecraft client, Item item) {
        if (client.player == null) return -1;
        for (int i = 0; i < 9; i++) {
            if (client.player.getInventory().getItem(i).is(item)) return i;
        }
        return -1;
    }

    private static Item seedFor(Block block) {
        if (block == Blocks.WHEAT) return Items.WHEAT_SEEDS;
        if (block == Blocks.CARROTS) return Items.CARROT;
        if (block == Blocks.POTATOES) return Items.POTATO;
        if (block == Blocks.BEETROOTS) return Items.BEETROOT_SEEDS;
        return null;
    }

    private static List<BlockPos> scanPositions(BlockPos center, int radius) {
        List<BlockPos> positions = new ArrayList<>();
        for (int y = -2; y <= 2; y++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    positions.add(center.offset(x, y, z));
                }
            }
        }
        return positions;
    }
}
