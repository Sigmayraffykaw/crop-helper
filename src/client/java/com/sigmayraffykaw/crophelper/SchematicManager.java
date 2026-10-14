package com.sigmayraffykaw.crophelper;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class SchematicManager {
    private static final int CONTAINER_RADIUS = 4;
    private static final int MAX_BLOCKS = 512;
    private static final List<BuildBlock> queue = new ArrayList<>();
    private static String loadedName;
    private static boolean building;
    private static BlockPos anchor;
    private static int tickDelay;

    private SchematicManager() {}

    static void preview(Minecraft client, String name) {
        if (client.player == null || client.level == null) {
            message(client, "Join a world before using schematics.");
            return;
        }
        try {
            JsonObject root = load(name, client);
            List<BuildBlock> blocks = parseBlocks(root);
            loadedName = name;
            Map<String, Integer> required = countMaterials(blocks);
            Map<String, Integer> carried = inventoryCounts(client);
            Map<String, Integer> chest = nearbyContainerCounts(client);
            message(client, "Schematic '" + name + "': " + blocks.size() + " block positions.");
            message(client, "Build anchor: 3 blocks in front of you. Use a clear, flat area.");
            int shown = 0;
            for (Map.Entry<String, Integer> entry : required.entrySet()) {
                if (shown++ >= 10) {
                    message(client, "...more materials omitted from chat.");
                    break;
                }
                String id = entry.getKey();
                int need = entry.getValue();
                int inInventory = carried.getOrDefault(id, 0);
                int inChests = chest.getOrDefault(id, 0);
                message(client, id + " x" + need + " | inventory " + inInventory + " | nearby chests " + inChests);
            }
            message(client, "Preview only: no blocks placed. Take needed items from chests into your inventory, then run /farm build confirm.");
        } catch (Exception e) {
            message(client, "Couldn't load schematic '" + name + "': " + e.getMessage());
            message(client, "Custom schematics go in .minecraft/config/crop-helper/schematics/<name>.json");
        }
    }

    static void confirm(Minecraft client) {
        if (client.player == null || client.level == null || client.gameMode == null) {
            message(client, "Join a world before building.");
            return;
        }
        if (loadedName == null) {
            message(client, "Preview a schematic first: /farm build <name>");
            return;
        }
        try {
            JsonObject root = load(loadedName, client);
            List<BuildBlock> blocks = parseBlocks(root);
            Map<String, Integer> required = countMaterials(blocks);
            Map<String, Integer> carried = inventoryCounts(client);
            List<String> missing = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : required.entrySet()) {
                int have = carried.getOrDefault(entry.getKey(), 0);
                if (have < entry.getValue()) missing.add(entry.getKey() + " needs " + entry.getValue() + ", have " + have);
            }
            if (!missing.isEmpty()) {
                message(client, "Build cancelled: put the required blocks in your inventory first.");
                for (int i = 0; i < Math.min(8, missing.size()); i++) message(client, missing.get(i));
                return;
            }
            queue.clear();
            queue.addAll(blocks);
            queue.sort(Comparator.comparingInt((BuildBlock b) -> b.y).thenComparingInt(b -> b.x).thenComparingInt(b -> b.z));
            Direction facing = client.player.getDirection();
            anchor = client.player.blockPosition().relative(facing, 3);
            building = true;
            tickDelay = 0;
            message(client, "Building '" + loadedName + "' with " + queue.size() + " planned blocks. /farm stop cancels it.");
        } catch (Exception e) {
            message(client, "Build could not start: " + e.getMessage());
        }
    }

    static void stop(Minecraft client) {
        building = false;
        queue.clear();
        message(client, "Crop Helper: schematic build stopped.");
    }

    static boolean isBuilding() {
        return building;
    }

    static void tick(Minecraft client) {
        if (!building || client.player == null || client.level == null || client.gameMode == null) return;
        if (++tickDelay < 8) return;
        tickDelay = 0;
        if (queue.isEmpty()) {
            building = false;
            message(client, "Schematic build finished. Check the area for skipped or unsupported placements.");
            return;
        }

        BuildBlock next = queue.get(0);
        BlockPos target = anchor.offset(next.x, next.y, next.z);
        BlockState current = client.level.getBlockState(target);
        if (current.is(next.block)) {
            queue.remove(0);
            return;
        }
        if (!current.isAir() && !current.canBeReplaced()) {
            queue.remove(0);
            message(client, "Skipped occupied position " + target.toShortString() + " to avoid replacing a block.");
            return;
        }

        Item item = BuiltInRegistries.BLOCK.getKey(next.block) == null ? null : next.block.asItem();
        int slot = findHotbarItem(client, item);
        if (item == null || item == net.minecraft.world.item.Items.AIR || slot < 0) {
            building = false;
            message(client, "Build paused: missing hotbar block for " + next.id + ". Put remaining blocks in your hotbar and run /farm build confirm again.");
            return;
        }

        BlockPos support = null;
        Direction face = null;
        for (Direction direction : Direction.values()) {
            BlockPos neighbor = target.relative(direction);
            if (!client.level.getBlockState(neighbor).isAir()) {
                support = neighbor;
                face = direction.getOpposite();
                break;
            }
        }
        if (support == null) {
            building = false;
            message(client, "Build paused: no solid block to place against at " + target.toShortString() + ".");
            return;
        }

        client.player.getInventory().setSelectedSlot(slot);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(support), face, support, false);
        client.gameMode.useItemOn(client.player, net.minecraft.world.InteractionHand.MAIN_HAND, hit);
        queue.remove(0);
    }

    private static JsonObject load(String name, Minecraft client) throws Exception {
        if (!name.matches("[A-Za-z0-9_-]{1,48}")) throw new IllegalArgumentException("Use letters, numbers, _ or - in the name.");
        Path file = client.gameDirectory.toPath().resolve("config").resolve("crop-helper").resolve("schematics").resolve(name + ".json");
        if (Files.isRegularFile(file)) {
            try (var reader = Files.newBufferedReader(file)) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        }
        try (InputStream in = SchematicManager.class.getClassLoader().getResourceAsStream("schematics/" + name + ".json")) {
            if (in == null) throw new IllegalArgumentException("File not found.");
            return JsonParser.parseReader(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    private static List<BuildBlock> parseBlocks(JsonObject root) {
        if (!root.has("blocks") || !root.get("blocks").isJsonArray()) throw new IllegalArgumentException("JSON needs a 'blocks' array.");
        JsonArray array = root.getAsJsonArray("blocks");
        if (array.size() == 0 || array.size() > MAX_BLOCKS) throw new IllegalArgumentException("A schematic must contain 1-" + MAX_BLOCKS + " blocks.");
        List<BuildBlock> result = new ArrayList<>();
        for (JsonElement element : array) {
            JsonObject obj = element.getAsJsonObject();
            int x = obj.get("x").getAsInt();
            int y = obj.get("y").getAsInt();
            int z = obj.get("z").getAsInt();
            String id = obj.get("block").getAsString();
            Identifier key = Identifier.parse(id);
            Block block = BuiltInRegistries.BLOCK.get(key);
            if (block == Blocks.AIR || BuiltInRegistries.BLOCK.getKey(block) == null) throw new IllegalArgumentException("Unknown or air block: " + id);
            if (Math.abs(x) > 32 || Math.abs(y) > 32 || Math.abs(z) > 32) throw new IllegalArgumentException("Offsets must be within 32 blocks.");
            result.add(new BuildBlock(x, y, z, id, block));
        }
        return result;
    }

    private static Map<String, Integer> countMaterials(List<BuildBlock> blocks) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (BuildBlock block : blocks) counts.merge(block.id, 1, Integer::sum);
        return counts;
    }

    private static Map<String, Integer> inventoryCounts(Minecraft client) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        if (client.player == null) return counts;
        for (int i = 0; i < client.player.getInventory().getContainerSize(); i++) {
            ItemStack stack = client.player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;
            Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (id != null) counts.merge(id.toString(), stack.getCount(), Integer::sum);
        }
        return counts;
    }

    private static Map<String, Integer> nearbyContainerCounts(Minecraft client) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        if (client.player == null || client.level == null) return counts;
        BlockPos center = client.player.blockPosition();
        for (int x = -CONTAINER_RADIUS; x <= CONTAINER_RADIUS; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -CONTAINER_RADIUS; z <= CONTAINER_RADIUS; z++) {
                    var entity = client.level.getBlockEntity(center.offset(x, y, z));
                    if (!(entity instanceof Container container)) continue;
                    for (int i = 0; i < container.getContainerSize(); i++) {
                        ItemStack stack = container.getItem(i);
                        if (!stack.isEmpty()) {
                            Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
                            if (id != null) counts.merge(id.toString(), stack.getCount(), Integer::sum);
                        }
                    }
                }
            }
        }
        return counts;
    }

    private static int findHotbarItem(Minecraft client, Item item) {
        if (client.player == null || item == null) return -1;
        for (int i = 0; i < 9; i++) if (client.player.getInventory().getItem(i).is(item)) return i;
        return -1;
    }

    private static void message(Minecraft client, String text) {
        if (client.player != null) client.gui.getChat().addMessage(Component.literal("[Crop Helper] " + text));
    }

    private record BuildBlock(int x, int y, int z, String id, Block block) {}
}
