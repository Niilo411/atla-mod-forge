package io.github.niilo411.elementalpaths.platform;

import io.github.niilo411.elementalpaths.ElementalPaths;
import io.github.niilo411.elementalpaths.platform.net.CustomPacketPayload;
import io.github.niilo411.elementalpaths.platform.net.StreamCodec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * NeoForge's PayloadRegistrar, over Forge 1.20.1's SimpleChannel, so ModNetworking kept
 * its shape: {@code registrar.playToServer(TYPE, STREAM_CODEC, handler)}.
 *
 * ONE ENVELOPE PER DIRECTION, not one message class per packet. SimpleChannel keys its
 * messages by Java class, which the payload Types do not carry, so every packet travels
 * inside an {@link Envelope}: the payload's id, then the payload written by its own
 * StreamCodec. The registrar looks the id back up on arrival. The wire format of each
 * payload is therefore exactly what its codec says, the same as on NeoForge.
 *
 * An unknown id is dropped with a warning rather than thrown, so a client and server a
 * version apart do not disconnect over a packet only one of them knows.
 */
public final class PayloadRegistrar {

    /** Raised whenever any packet's format changes, so mismatched installs refuse to connect. */
    private static final String PROTOCOL = "2";

    static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(ElementalPaths.MODID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    private record Entry<T extends CustomPacketPayload>(
            StreamCodec<? super FriendlyByteBuf, T> codec, BiConsumer<T, IPayloadContext> handler) {
    }

    private static final Map<ResourceLocation, Entry<?>> TO_SERVER = new HashMap<>();
    private static final Map<ResourceLocation, Entry<?>> TO_CLIENT = new HashMap<>();

    /** What actually crosses the wire: one payload, tagged with its id. */
    record Envelope(CustomPacketPayload payload, boolean toServer) {
    }

    static {
        CHANNEL.messageBuilder(ServerBound.class, 0, NetworkDirection.PLAY_TO_SERVER)
                .encoder((msg, buf) -> write(msg.envelope(), buf))
                .decoder(buf -> new ServerBound(read(buf, TO_SERVER)))
                .consumerMainThread((msg, ctx) -> handle(msg.envelope(), ctx, TO_SERVER))
                .add();
        CHANNEL.messageBuilder(ClientBound.class, 1, NetworkDirection.PLAY_TO_CLIENT)
                .encoder((msg, buf) -> write(msg.envelope(), buf))
                .decoder(buf -> new ClientBound(read(buf, TO_CLIENT)))
                .consumerMainThread((msg, ctx) -> handle(msg.envelope(), ctx, TO_CLIENT))
                .add();
    }

    record ServerBound(Envelope envelope) {
    }

    record ClientBound(Envelope envelope) {
    }

    private PayloadRegistrar() {
    }

    /** Called once, from the mod constructor, before any packet can be sent. */
    public static PayloadRegistrar create() {
        return new PayloadRegistrar();
    }

    public <T extends CustomPacketPayload> PayloadRegistrar playToServer(
            CustomPacketPayload.Type<T> type, StreamCodec<? super FriendlyByteBuf, T> codec,
            BiConsumer<T, IPayloadContext> handler) {
        TO_SERVER.put(type.id(), new Entry<>(codec, handler));
        return this;
    }

    public <T extends CustomPacketPayload> PayloadRegistrar playToClient(
            CustomPacketPayload.Type<T> type, StreamCodec<? super FriendlyByteBuf, T> codec,
            BiConsumer<T, IPayloadContext> handler) {
        TO_CLIENT.put(type.id(), new Entry<>(codec, handler));
        return this;
    }

    @SuppressWarnings("unchecked")
    private static <T extends CustomPacketPayload> void write(Envelope envelope, FriendlyByteBuf buf) {
        T payload = (T) envelope.payload();
        Map<ResourceLocation, Entry<?>> table = envelope.toServer() ? TO_SERVER : TO_CLIENT;
        ResourceLocation id = payload.type().id();
        Entry<T> entry = (Entry<T>) table.get(id);
        if (entry == null) {
            throw new IllegalStateException("Packet " + id + " was sent but never registered");
        }
        buf.writeResourceLocation(id);
        entry.codec().encode(buf, payload);
    }

    private static Envelope read(FriendlyByteBuf buf, Map<ResourceLocation, Entry<?>> table) {
        ResourceLocation id = buf.readResourceLocation();
        Entry<?> entry = table.get(id);
        if (entry == null) {
            ElementalPaths.LOGGER.warn("Ignoring unknown packet {}", id);
            buf.skipBytes(buf.readableBytes());
            return new Envelope(null, table == TO_SERVER);
        }
        return new Envelope(entry.codec().decode(buf), table == TO_SERVER);
    }

    @SuppressWarnings("unchecked")
    private static <T extends CustomPacketPayload> void handle(
            Envelope envelope, Supplier<NetworkEvent.Context> ctx, Map<ResourceLocation, Entry<?>> table) {
        if (envelope.payload() == null) return;
        T payload = (T) envelope.payload();
        Entry<T> entry = (Entry<T>) table.get(payload.type().id());
        entry.handler().accept(payload, IPayloadContext.of(ctx.get()));
    }
}
