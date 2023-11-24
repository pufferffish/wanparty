package octi.wanparty.cloudflare;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import octi.wanparty.TunnelAddress;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

public enum Cloudflare {
    ;

    public static final String TUNNEL_REGISTRY = "https://api.trycloudflare.com/tunnel";
    public static final String EDGE_NODE_RESOLVER = "_v2-origintunneld._tcp.argotunnel.com";
    public static final String HTTP2_SNI = "h2.cftunnel.com";
    public static final String[] CLOUDFLARE_DNS = {"1.1.1.1", "1.0.0.1"};
    private static final Gson GSON = new Gson();

    public static InetSocketAddress[] resolveEdgeNodes() throws IOException {
        IOException exception = null;
        for (String dnsAddress : CLOUDFLARE_DNS) {
            try {
                URL url = new URL("https", dnsAddress, String.format("/dns-query?name=%s&type=SRV", EDGE_NODE_RESOLVER));
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestProperty("Accept", "application/dns-json");
                conn.setDoInput(true);
                conn.setRequestMethod("GET");
                conn.connect();
                conn.disconnect();
                JsonArray response = GSON.fromJson(new InputStreamReader(conn.getInputStream()), JsonObject.class).getAsJsonArray("Answer");
                InetSocketAddress[] records = new InetSocketAddress[response.size()];
                int i = 0;
                for (JsonElement element : response) {
                    String[] nodeRecord = element.getAsJsonObject().get("data").getAsString().split("\\s", 4);
                    InetSocketAddress nodeAddress = new InetSocketAddress(nodeRecord[3], Integer.parseInt(nodeRecord[2]));
                    records[i++] = nodeAddress;
                }
                return records;
            } catch (MalformedURLException ex) {
                throw new RuntimeException(ex);
            } catch (IOException ex) {
                exception = ex;
            } catch (Exception ex) {
                exception = new IOException(ex);
            }
        }
        throw exception;
    }

    public static TunnelAddress createTunnel() throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(TUNNEL_REGISTRY).openConnection();
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("User-Agent", "cloudflared/DEV");
        conn.setDoInput(true);
        conn.setRequestMethod("POST");
        conn.connect();
        conn.disconnect();
        JsonObject response = GSON.fromJson(new InputStreamReader(conn.getInputStream()), JsonObject.class).getAsJsonObject("result");
        UUID id = UUID.fromString(response.get("id").getAsString());
        String name = response.get("name").getAsString();
        String hostname = response.get("hostname").getAsString();
        String accountTag = response.get("account_tag").getAsString();
        byte[] secret = Base64.getDecoder().decode(response.get("secret").getAsString());
        return new TunnelAddress(id, name, hostname, accountTag, secret);
    }

    public static String serializeHeaders(Map<String, String> headers) {
        Base64.Encoder encoder = Base64.getEncoder().withoutPadding();
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (sb.length() != 0)
                sb.append(';');
            sb.append(encoder.encodeToString(entry.getKey().getBytes(StandardCharsets.UTF_8)));
            sb.append(':');
            sb.append(encoder.encodeToString(entry.getValue().getBytes(StandardCharsets.UTF_8)));
        }
        return sb.toString();
    }

    public static String calculateAcceptKey(String key) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA1");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        digest.update(key.getBytes(StandardCharsets.UTF_8));
        digest.update("258EAFA5-E914-47DA-95CA-C5AB0DC85B11".getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(digest.digest());
    }

}
