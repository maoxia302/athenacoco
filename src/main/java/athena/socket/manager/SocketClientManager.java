package athena.socket.manager;

import athena.socket.control.BasicSocketChannelClient;
import athena.socket.control.BasicSocketChannelClientManager;
import athena.socket.control.SocketChannelManager;
import athena.socket.core.MessageProcessor;

import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

public class SocketClientManager extends BasicSocketChannelClientManager {

    private int bufferSize;
    private MessageProcessor processor;

    public SocketClientManager(
            int threadCount,
            int connectionLinger,
            int connectionTimeout,
            String poolName,
            int connectionSize,
            int bufferSize,
            MessageProcessor processor) {
        super(threadCount, connectionLinger, connectionTimeout, poolName, connectionSize);
        this.bufferSize = bufferSize;
        this.processor = processor;
    }

    @Override
    protected BasicSocketChannelClient doGetClient(
            SocketChannel channel,
            SocketChannelManager manager,
            InetSocketAddress socketAddress) {
        return new SocketChannelClient(channel, manager, bufferSize, socketAddress, processor);
    }

}
