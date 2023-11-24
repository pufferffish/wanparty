package octi.wanparty.http2.frame;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import octi.wanparty.http2.RawHttpFrame;
import octi.wanparty.http2.exception.HttpProtocolException;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class HttpSettingsFrame implements HttpFrame {
    public Map<Settings, Integer> settings;
    public boolean ack;

    @Override
    public void readFromRawFrame(ChannelHandlerContext ctx, RawHttpFrame rawHttpFrame) throws HttpProtocolException {
        rawHttpFrame.assertFrameType(HttpFrame.SETTINGS);
        rawHttpFrame.assertStreamID(0);
        Map<Settings, Integer> settingsMap = new HashMap<>();
        ByteBuf payload = rawHttpFrame.payload;
        if ((rawHttpFrame.flags & 1) == 1) {
            this.ack = true;
        }
        if (this.ack && payload.readableBytes() > 0) {
            throw new HttpProtocolException("Setting Frame has ACK flag but is not empty");
        }
        while (payload.readableBytes() > 0) {
            int id = payload.readShort() & 0xFFFF;
            int value = payload.readInt();
            Settings settings = Settings.getSettingById(id);
            if (settings != null)
                settingsMap.put(settings, value);
        }
        this.settings = Collections.unmodifiableMap(settingsMap);
    }

    @Override
    public RawHttpFrame writeToRawFrame(ChannelHandlerContext ctx) {
        ByteBuf payload = Unpooled.buffer(this.settings.size() * 6);
        this.settings.forEach((k, v) -> {
            payload.writeShort(k.id);
            payload.writeInt(v);
        });
        return new RawHttpFrame(HttpFrame.SETTINGS, this.ack ? (byte) 1 : (byte) 0, 0, payload);
    }

    @Override
    public String toString() {
        return "HttpSettingsFrame{" +
                "settings=" + settings +
                ", ack=" + ack +
                '}';
    }

    public enum Settings {
        SETTINGS_HEADER_TABLE_SIZE(0x01),
        SETTINGS_ENABLE_PUSH(0x02),
        SETTINGS_MAX_CONCURRENT_STREAMS(0x03),
        SETTINGS_INITIAL_WINDOW_SIZE(0x04),
        SETTINGS_MAX_FRAME_SIZE(0x05),
        SETTINGS_MAX_HEADER_LIST_SIZE(0x06),
        SETTINGS_MUXER_MAGIC(0x42db),
        SETTINGS_COMPRESSION(0xff20);
        private static final Map<Integer, Settings> SETTINGS = new HashMap<>();

        static {
            for (Settings setting : Settings.values()) {
                SETTINGS.put(setting.id, setting);
            }
        }

        public final int id;

        Settings(int id) {
            this.id = id;
        }

        public static Settings getSettingById(int id) {
            return SETTINGS.get(id);
        }
    }
}
