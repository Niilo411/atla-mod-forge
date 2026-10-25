package io.github.niilo411.elementalpaths.platform.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

/**
 * The vanilla 1.20.5 ByteBufCodecs the mod's packets use, rebuilt on 1.20.1's
 * FriendlyByteBuf. Every one writes exactly what the vanilla codec of the same name
 * writes, so nothing about the wire format is invented here.
 */
public final class ByteBufCodecs {
    private ByteBufCodecs() {
    }

    private static FriendlyByteBuf friendly(ByteBuf buf) {
        return buf instanceof FriendlyByteBuf f ? f : new FriendlyByteBuf(buf);
    }

    public static final StreamCodec<ByteBuf, Boolean> BOOL =
            StreamCodec.of((buf, v) -> buf.writeBoolean(v), ByteBuf::readBoolean);

    public static final StreamCodec<ByteBuf, Integer> INT =
            StreamCodec.of((buf, v) -> buf.writeInt(v), ByteBuf::readInt);

    public static final StreamCodec<ByteBuf, Integer> VAR_INT =
            StreamCodec.of((buf, v) -> friendly(buf).writeVarInt(v), buf -> friendly(buf).readVarInt());

    public static final StreamCodec<ByteBuf, String> STRING_UTF8 =
            StreamCodec.of((buf, v) -> friendly(buf).writeUtf(v), buf -> friendly(buf).readUtf());

    public static final StreamCodec<ByteBuf, byte[]> BYTE_ARRAY =
            StreamCodec.of((buf, v) -> friendly(buf).writeByteArray(v), buf -> friendly(buf).readByteArray());

    public static final StreamCodec<ByteBuf, CompoundTag> COMPOUND_TAG =
            StreamCodec.of((buf, v) -> friendly(buf).writeNbt(v), buf -> friendly(buf).readNbt());

    /** A length-prefixed list of whatever the codec it is applied to reads. */
    public static <B extends ByteBuf, V> StreamCodec.CodecOperation<B, V, List<V>> list() {
        return element -> StreamCodec.of(
                (buf, values) -> {
                    friendly(buf).writeVarInt(values.size());
                    for (V value : values) element.encode(buf, value);
                },
                buf -> {
                    int size = friendly(buf).readVarInt();
                    List<V> out = new ArrayList<>(size);
                    for (int i = 0; i < size; i++) out.add(element.decode(buf));
                    return out;
                });
    }
}
