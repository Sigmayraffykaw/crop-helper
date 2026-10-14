package com.sigmayraffykaw.crophelper;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * Small Sponge .schem reader for palette-based schematics (v2/v3-style root or Schematic compound).
 * Block states are reduced to their block identifier; state properties are not applied yet.
 */
final class SpongeSchematicReader {
    private SpongeSchematicReader() {}

    static JsonObject read(Path path, int maxBlocks) throws IOException {
        try (var raw = new BufferedInputStream(Files.newInputStream(path))) {
            raw.mark(2);
            int a = raw.read();
            int b = raw.read();
            raw.reset();
            var decoded = (a == 0x1f && b == 0x8b) ? new GZIPInputStream(raw) : raw;
            try (var in = new DataInputStream(decoded)) {
                int rootType = in.readUnsignedByte();
                if (rootType != 10) throw new IOException("Schematic file is not an NBT compound.");
                in.readUTF();
                Object rootValue = readPayload(in, 10);
                if (!(rootValue instanceof Map<?, ?> root)) throw new IOException("Invalid NBT root.");
                Object nested = root.get("Schematic");
                Map<?, ?> schematic = nested instanceof Map<?, ?> map ? map : root;

                int width = number(schematic.get("Width"));
                int height = number(schematic.get("Height"));
                int length = number(schematic.get("Length"));
                long volume = (long) width * height * length;
                if (width < 1 || height < 1 || length < 1 || volume > maxBlocks) {
                    throw new IOException("Schematic volume must be between 1 and " + maxBlocks + " blocks (found " + volume + ").");
                }
                Object blocksValue = schematic.get("Blocks");
                Map<?, ?> blockSection = blocksValue instanceof Map<?, ?> map ? map : schematic;
                Object paletteValue = blockSection.get("Palette");
                Object dataValue = blockSection.containsKey("Data") ? blockSection.get("Data") : blockSection.get("BlockData");
                if (!(paletteValue instanceof Map<?, ?> palette) || !(dataValue instanceof byte[] data)) {
                    throw new IOException("Expected Sponge Palette compound and Data/BlockData byte array.");
                }

                Map<Integer, String> names = new LinkedHashMap<>();
                for (Map.Entry<?, ?> entry : palette.entrySet()) {
                    if (entry.getKey() instanceof String id && entry.getValue() instanceof Number index) {
                        names.put(index.intValue(), id.split("\\[", 2)[0]);
                    }
                }
                int[] indices = decodeVarInts(data, (int) volume);
                JsonArray blocks = new JsonArray();
                for (int index = 0; index < indices.length; index++) {
                    String id = names.get(indices[index]);
                    if (id == null) throw new IOException("Palette index " + indices[index] + " is missing.");
                    if (id.equals("minecraft:air") || id.equals("minecraft:cave_air") || id.equals("minecraft:void_air")) continue;
                    int x = index % width;
                    int z = (index / width) % length;
                    int y = index / (width * length);
                    JsonObject block = new JsonObject();
                    block.addProperty("x", x - width / 2);
                    block.addProperty("y", y);
                    block.addProperty("z", z - length / 2);
                    block.addProperty("block", id);
                    blocks.add(block);
                }
                if (blocks.size() == 0) throw new IOException("Schematic contains no non-air blocks.");
                if (blocks.size() > maxBlocks) throw new IOException("Schematic contains too many non-air blocks.");
                JsonObject result = new JsonObject();
                result.addProperty("name", path.getFileName().toString());
                result.add("blocks", blocks);
                return result;
            }
        }
    }

    private static int[] decodeVarInts(byte[] data, int expected) throws IOException {
        int[] result = new int[expected];
        int byteIndex = 0;
        for (int i = 0; i < expected; i++) {
            int value = 0;
            int position = 0;
            boolean finished = false;
            while (!finished) {
                if (byteIndex >= data.length || position > 28) throw new IOException("Invalid Sponge BlockData VarInt stream.");
                int next = data[byteIndex++] & 0xff;
                value |= (next & 0x7f) << position;
                finished = (next & 0x80) == 0;
                position += 7;
            }
            result[i] = value;
        }
        return result;
    }

    private static int number(Object value) throws IOException {
        if (value instanceof Number number) return number.intValue();
        throw new IOException("Missing or invalid schematic dimensions.");
    }

    private static Object readPayload(DataInputStream in, int type) throws IOException {
        return switch (type) {
            case 1 -> in.readByte();
            case 2 -> in.readShort();
            case 3 -> in.readInt();
            case 4 -> in.readLong();
            case 5 -> in.readFloat();
            case 6 -> in.readDouble();
            case 7 -> {
                int length = checkedLength(in.readInt());
                byte[] values = new byte[length];
                in.readFully(values);
                yield values;
            }
            case 8 -> in.readUTF();
            case 9 -> {
                int childType = in.readUnsignedByte();
                int length = checkedLength(in.readInt());
                List<Object> values = new ArrayList<>(length);
                for (int i = 0; i < length; i++) values.add(readPayload(in, childType));
                yield values;
            }
            case 10 -> {
                Map<String, Object> values = new LinkedHashMap<>();
                while (true) {
                    int childType = in.readUnsignedByte();
                    if (childType == 0) break;
                    String name = in.readUTF();
                    values.put(name, readPayload(in, childType));
                }
                yield values;
            }
            case 11 -> {
                int length = checkedLength(in.readInt());
                int[] values = new int[length];
                for (int i = 0; i < length; i++) values[i] = in.readInt();
                yield values;
            }
            case 12 -> {
                int length = checkedLength(in.readInt());
                long[] values = new long[length];
                for (int i = 0; i < length; i++) values[i] = in.readLong();
                yield values;
            }
            default -> throw new IOException("Unsupported NBT tag type: " + type);
        };
    }

    private static int checkedLength(int length) throws IOException {
        if (length < 0 || length > 2_000_000) throw new IOException("NBT collection length is out of bounds.");
        return length;
    }
}
