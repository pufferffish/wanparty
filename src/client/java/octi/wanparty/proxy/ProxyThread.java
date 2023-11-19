package octi.wanparty.proxy;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ProxyThread implements Runnable {

    private final ServerSocket server;

    public ProxyThread(ServerSocket server) {
        this.server = server;
    }

    @Override
    public void run() {
        try {
            while (true) {
                Socket socket = server.accept();
                System.out.println("Accepted connection from " + socket.getInetAddress());
                new Thread(new ProxyClientThread(socket), "WAN Party Connection").start();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
