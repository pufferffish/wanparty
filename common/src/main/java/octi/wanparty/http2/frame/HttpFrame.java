package octi.wanparty.http2.frame;

import io.netty.channel.ChannelHandlerContext;
import octi.wanparty.http2.RawHttpFrame;
import octi.wanparty.http2.exception.HttpProtocolException;

public interface HttpFrame {
    byte DATA = 0x00;
    byte HEADERS = 0x01;
    byte PRIORITY = 0x02;
    byte RST_STREAM = 0x03;
    byte SETTINGS = 0x04;
    byte PUSH_PROMISE = 0x05;
    byte PING = 0x06;
    byte GO_AWAY = 0x07;
    byte WINDOW_UPDATE = 0x08;
    byte CONTINUATION = 0x09;

    void readFromRawFrame(ChannelHandlerContext ctx, RawHttpFrame rawHttpFrame) throws HttpProtocolException;

    RawHttpFrame writeToRawFrame(ChannelHandlerContext ctx);
}
