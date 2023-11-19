package octi.wanparty;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.UUID;

public class TunnelAddress extends SocketAddress {
    public final UUID id;
    public final String name;
    public final String hostname;
    public final String accountTag;
    public final byte[] secret;

    String regionName;

    public TunnelAddress(UUID id, String name, String hostname, String accountTag, byte[] secret) {
        this.id = id;
        this.name = name;
        this.hostname = hostname;
        this.accountTag = accountTag;
        this.secret = secret;
    }

    public byte[] tunnelID() {
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(id.getMostSignificantBits());
        bb.putLong(id.getLeastSignificantBits());
        return bb.array();
    }

    public String getRegionName() {
        return regionName;
    }

    @Override
    public String toString() {
        return "TunnelRegistry{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", hostname='" + hostname + '\'' +
                ", accountTag='" + accountTag + '\'' +
                ", secret=" + Base64.getEncoder().encodeToString(secret) +
                '}';
    }
}
