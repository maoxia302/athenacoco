package athena.socket.control;

import java.io.IOException;
import java.nio.channels.SocketChannel;

public interface BasicSocketChannelManager extends Runnable {
    boolean send(String socketAddress, byte[] data) throws IOException;
    void start();
    void stop();
    void close(String socketAddress) throws IOException;
    SocketChannelManager getManager();
    void initiateConnection(SocketSession client);
    void addPendingChanges(SocketChannel socket, int type, int ops);
}
