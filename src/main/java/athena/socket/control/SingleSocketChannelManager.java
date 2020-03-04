package athena.socket.control;

import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;

public class SingleSocketChannelManager implements SocketChannelManager {

    private final Map<String, SocketSession> clientTable;

    SingleSocketChannelManager() {
        this.clientTable = new HashMap<>();
    }

    public void close() {
        for (SocketSession client : clientTable.values()) {
            client.close();
        }
    }

    public void close(String socketAddress) {
        SocketSession client = get(socketAddress);
        if (client != null) {
            client.close();
        }
    }

    public String getAddress(SocketSession client) {
        InetSocketAddress socketAddress = client.getSocketAddress();
        return socketAddress.getAddress().getHostAddress() + ":" + socketAddress.getPort();
    }

    public int getPort(SocketSession client) {
        InetSocketAddress socketAddress = client.getSocketAddress();
        return socketAddress.getPort();
    }

    public SocketSession get(SocketChannel channel) {
        return get(channel.socket().getInetAddress().getHostAddress()
                + ":" + channel.socket().getPort());
    }

    public SocketSession get(String address) {
        synchronized (clientTable) {
            return clientTable.get(address);
        }
    }

    public void register(SocketSession client) {
        synchronized (clientTable) {
            clientTable.put(client.getAddress(), client);
        }
    }

    public void unregister(SocketSession client) {
        String address = client.getAddress();
        if (address != null) {
            synchronized (clientTable) {
                clientTable.remove(address);
            }
        }
    }

    public boolean opened(SocketSession client) {
        return true;
    }

    public boolean canClose(SocketSession client) {
        return true;
    }
}