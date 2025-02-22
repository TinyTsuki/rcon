package xin.vanilla.rcon;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.ByteChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class Rcon implements Closeable {

    final private ByteChannel channel;
    final private PacketReader reader;
    final private PacketWriter writer;

    private volatile int requestCounter;

    Rcon(final ByteChannel channel, final int readBufferCapacity,
         final int writeBufferCapacity, final PacketCodec codec) {
        this.channel = channel;

        reader = new PacketReader(channel::read, readBufferCapacity, codec);
        writer = new PacketWriter(channel::write, writeBufferCapacity, codec);
    }

    public static Rcon open(final SocketAddress remote) throws IOException {
        return open(remote, StandardCharsets.UTF_8);
    }

    public static Rcon open(final SocketAddress remote, final Charset charset) throws IOException {
        return new RconBuilder().withChannel(SocketChannel.open(remote)).withCharset(charset).build();
    }

    public static Rcon open(final String hostname, final int port) throws IOException {
        return open(hostname, port, StandardCharsets.UTF_8);
    }

    public static Rcon open(final String hostname, final int port, final Charset charset) throws IOException {
        return open(new InetSocketAddress(hostname, port), charset);
    }

    public static RconBuilder newBuilder() {
        return new RconBuilder();
    }

    public boolean authenticate(final String password) throws IOException {
        Packet response;

        synchronized (this) {
            response = writeAndRead(PacketType.SERVERDATA_AUTH, password);

            // This works around a quirk in CS:GO where an empty SERVERDATA_RESPONSE_VALUE is sent before the SERVERDATA_AUTH_RESPONSE.
            if (response.type == PacketType.SERVERDATA_RESPONSE_VALUE) {
                response = read(response.requestId);
            }
        }
        if (response.type != PacketType.SERVERDATA_AUTH_RESPONSE) {
            throw new IOException("Invalid auth response type: " + response.type);
        }
        return response.isValid();
    }

    public void tryAuthenticate(final String password) throws IOException {
        if (!authenticate(password)) {
            throw new IOException("Authentication failed");
        }
    }

    public String sendCommand(final String command) throws IOException {
        final Packet response = writeAndRead(PacketType.SERVERDATA_EXECCOMMAND, command);

        if (response.type != PacketType.SERVERDATA_RESPONSE_VALUE) {
            throw new IOException("Wrong command response type: " + response.type);
        }
        if (!response.isValid()) {
            throw new IOException("Invalid command response: " + response.payload);
        }
        return response.payload;
    }

    private synchronized Packet writeAndRead(final int packetType, final String payload) throws IOException {
        final int requestId = requestCounter++;

        writer.write(new Packet(requestId, packetType, payload));

        return read(requestId);
    }

    private synchronized Packet read(final int expectedRequestId) throws IOException {
        final Packet response = reader.read();

        if (response.isValid() && response.requestId != expectedRequestId) {
            throw new IOException(String.format("Unexpected response id (%d -> %d)", expectedRequestId, response.requestId));
        }
        return response;
    }

    @Override
    public void close() throws IOException {
        channel.close();
    }
}
