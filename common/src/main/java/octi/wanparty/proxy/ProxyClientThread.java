package octi.wanparty.proxy;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class ProxyClientThread implements Runnable {
    private final Socket socket;
    private static final int SEGMENT_BITS = 0x7F;
    private static final int CONTINUE_BIT = 0x80;

    public ProxyClientThread(Socket socket) {
        this.socket = socket;
    }

    public static int readVarInt(InputStream is) throws IOException {
        int value = 0;
        int position = 0;
        byte currentByte;

        while (true) {
            int read = is.read();
            if (read == -1)
                throw new IOException("Premature end of stream");

            currentByte = (byte) read;
            value |= (currentByte & SEGMENT_BITS) << position;

            if ((currentByte & CONTINUE_BIT) == 0) break;

            position += 7;

            if (position >= 32) throw new RuntimeException("VarInt is too big");
        }

        return value;
    }

    public static void writeVarInt(OutputStream os, int value) throws IOException {
        while (true) {
            if ((value & ~SEGMENT_BITS) == 0) {
                os.write(value);
                return;
            }

            os.write((value & SEGMENT_BITS) | CONTINUE_BIT);

            // Note: >>> means that the sign bit is shifted with the rest of the number rather than being left alone
            value >>>= 7;
        }
    }

    private static void writeLine(OutputStream os, String line) throws IOException {
        os.write(line.getBytes(StandardCharsets.UTF_8));
        os.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }

    private static Map<String, String> handshake(Socket socket, String hostname) throws IOException {
        OutputStream os = socket.getOutputStream();
        InputStream is = socket.getInputStream();
        writeLine(os, "GET /connect HTTP/1.1");
        writeLine(os, "Connection: Upgrade");
        writeLine(os, "Upgrade: websocket");
        writeLine(os, "Sec-WebSocket-Key: x3JJHMbDL1EzLkh9GBhXDw==");
        writeLine(os, "Sec-WebSocket-Version: 13");
        writeLine(os, "Host: " + hostname);
        writeLine(os, "");

        Map<String, String> headers = new HashMap<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String line = reader.readLine();
        if (!"HTTP/1.1 101 Switching Protocols".equals(line)) {
            throw new IOException("Protocol error, expected 101, got: " + line);
        }

        while (true) {
            line = reader.readLine();
            if (line == null) {
                throw new IOException("Protocol error");
            }
            if (line.isEmpty()) {
                break;
            }
            String[] segments = line.split(": ", 2);
            if (segments.length != 2) {
                throw new IOException("Invalid header line: " + line);
            }
            headers.put(segments[0].toLowerCase(), segments[1]);
        }

        if (!"upgrade".equals(headers.get("connection"))) {
            throw new IOException("Connection header not found");
        }

        if (!"websocket".equals(headers.get("upgrade"))) {
            throw new IOException("Upgrade header not found");
        }

        return headers;
    }

    @Override
    public void run() {
        try {
            int packetLength = readVarInt(socket.getInputStream());
            byte[] buf = new byte[packetLength];
            socket.getInputStream().read(buf);
            String hostname = checkHandshake(buf);
            if (hostname == null) {
                socket.close();
                return;
            } else if (hostname.startsWith("#")) {
                hostname = hostname.substring(1);
            }
            hostname += ".trycloudflare.com";
            System.out.println("Proxying to " + hostname);

            Socket edge = new Socket();
            edge.setTcpNoDelay(true);
            edge.connect(new InetSocketAddress(hostname, 80));
            handshake(edge, hostname);
            OutputStream os = edge.getOutputStream();
            writeVarInt(os, packetLength);
            os.write(buf);
            new Thread(() -> proxy(socket, edge), "WAN Party Proxy Connection 1").start();
            new Thread(() -> proxy(edge, socket), "WAN Party Proxy Connection 2").start();
        } catch (IOException ex) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            ex.printStackTrace();
        }
    }

    private static void proxy(Socket from, Socket to) {
        byte[] buf = new byte[4096];
        int bytesRead;
        try {
            InputStream is = from.getInputStream();
            OutputStream os = to.getOutputStream();
            while (-1 != (bytesRead = is.read(buf))) {
                os.write(buf, 0, bytesRead);
            }
        } catch (IOException ex) {
            try {
                from.close();
                to.close();
            } catch (IOException ignored) {
            }
            ex.printStackTrace();
        }
    }

    private String checkHandshake(byte[] buf) throws IOException {
        try (ByteArrayInputStream baos = new ByteArrayInputStream(buf)) {
            int packetID = readVarInt(baos);
            if (packetID != 0x00) {
                return null;
            }
            readVarInt(baos); // protocol version
            int serverAddressLength = readVarInt(baos);
            byte[] serverAddressBytes = new byte[serverAddressLength];
            baos.read(serverAddressBytes);

            int i;
            for (i = 0; i < serverAddressBytes.length && serverAddressBytes[i] != 0; i++) { }

            return new String(serverAddressBytes, 0, i, StandardCharsets.UTF_8);
        }
    }

}
