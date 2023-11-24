package octi.wanparty.util;

import java.io.*;
import java.net.InetSocketAddress;

public class InetAddressCreator {

    public static InetSocketAddress localAddressWithHostname(String hostname, int port) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); ObjectOutputStream oos = new FakeObjectOutputStream(baos, hostname)) {
            oos.writeObject(new InetSocketAddress("127.0.0.1", port));
            oos.flush();
            return (InetSocketAddress) new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray())).readObject();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private static class FakeObjectOutputStream extends ObjectOutputStream {
        public final String hostname;
        public FakeObjectOutputStream(OutputStream out, String hostname) throws IOException {
            super(out);
            this.hostname = hostname;
        }

        @Override
        public PutField putFields() throws IOException {
            final PutField fields = super.putFields();
            return new PutField() {
                @Override
                public void put(String s, boolean b) {
                    fields.put(s, b);
                }

                @Override
                public void put(String s, byte b) {
                    fields.put(s, b);
                }

                @Override
                public void put(String s, char c) {
                    fields.put(s, c);
                }

                @Override
                public void put(String s, short i) {
                    fields.put(s, i);
                }

                @Override
                public void put(String s, int i) {
                    fields.put(s, i);
                }

                @Override
                public void put(String s, long l) {
                    fields.put(s, l);
                }

                @Override
                public void put(String s, float v) {
                    fields.put(s, v);
                }

                @Override
                public void put(String s, double v) {
                    fields.put(s, v);
                }

                @Override
                public void put(String s, Object o) {
                    if (s.equals("hostname")) {
                        fields.put(s, hostname);
                    } else {
                        fields.put(s, o);
                    }
                }

                @Override
                public void write(ObjectOutput objectOutput) throws IOException {
                    fields.write(objectOutput);
                }
            };
        }
    }


}
