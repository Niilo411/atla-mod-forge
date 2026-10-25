import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

/**
 * Writes the empty 8 x 8 x 8 structure the game tests stand in
 * (src/gametest/resources/data/atlamod/structures/empty.nbt).
 *
 * Forge 1.20.1's game tests need a structure template to place, and vanilla ships no empty
 * one. Written byte by byte in the NBT format rather than through Minecraft's own classes,
 * so it runs with nothing on the classpath:
 *
 *   java tools/GenEmptyStructure.java src/gametest/resources/data/atlamod/structures/empty.nbt
 *
 * DataVersion 3465 is Minecraft 1.20.1.
 */
public class GenEmptyStructure {
    private static final byte TAG_END = 0, TAG_INT = 3, TAG_LIST = 9, TAG_COMPOUND = 10, TAG_STRING = 8;

    public static void main(String[] args) throws IOException {
        try (DataOutputStream out = new DataOutputStream(new GZIPOutputStream(new FileOutputStream(args[0])))) {
            out.writeByte(TAG_COMPOUND);
            out.writeUTF("");

            out.writeByte(TAG_INT);
            out.writeUTF("DataVersion");
            out.writeInt(3465);

            out.writeByte(TAG_LIST);
            out.writeUTF("size");
            out.writeByte(TAG_INT);
            out.writeInt(3);
            out.writeInt(8);
            out.writeInt(8);
            out.writeInt(8);

            // One palette entry, air, which nothing uses: the template is empty.
            out.writeByte(TAG_LIST);
            out.writeUTF("palette");
            out.writeByte(TAG_COMPOUND);
            out.writeInt(1);
            out.writeByte(TAG_STRING);
            out.writeUTF("Name");
            out.writeUTF("minecraft:air");
            out.writeByte(TAG_END);

            emptyList(out, "blocks");
            emptyList(out, "entities");

            out.writeByte(TAG_END);
        }
    }

    private static void emptyList(DataOutputStream out, String name) throws IOException {
        out.writeByte(TAG_LIST);
        out.writeUTF(name);
        out.writeByte(TAG_END);
        out.writeInt(0);
    }
}
