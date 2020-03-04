package athena.socket.manager.processor;

import athena.socket.control.BasicSocketChannelClientManager;
import athena.socket.control.BasicSocketChannelManager;
import athena.socket.core.MessageContext;
import athena.socket.core.MessageProcessor;
import athena.socket.manager.SocketClientManager;
import athena.socket.manager.processor.inter.TcpClientParaAwareProcessor;

import java.io.IOException;

public class TcpClientProcessor extends SocketChannelProcessor implements TcpClientParaAwareProcessor {

    private int threadCount;
    private int connectionLinger;
    private int connectionTimeout;
    private String poolName;
    private int connectionSize;
    private String address;
    private int port;
    private int bufferSize;
    private MessageProcessor messageProcessor;

    public BasicSocketChannelManager getManager() {
        BasicSocketChannelClientManager manager;
        manager = new SocketClientManager(
                threadCount,
                connectionLinger,
                connectionTimeout,
                poolName,
                connectionSize,
                bufferSize,
                messageProcessor);
        try {
            manager.addConnection(address, port);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return manager;
    }

    public TcpClientProcessor(
            int threadCount,
            int connectionLinger,
            int connectionTimeout,
            String poolName,
            int connectionSize,
            String address,
            int port,
            int bufferSize,
            MessageProcessor messageProcessor) {
        super();
        this.threadCount = threadCount;
        this.connectionLinger = connectionLinger;
        this.connectionTimeout = connectionTimeout;
        this.poolName = poolName;
        this.connectionSize = connectionSize;
        this.address = address;
        this.port = port;
        this.bufferSize = bufferSize;
        this.messageProcessor = messageProcessor;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setConnectionLinger(int connectionLinger) {
        this.connectionLinger = connectionLinger;
    }

    public void setConnectionSize(int connectionSize) {
        this.connectionSize = connectionSize;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public void setPoolName(String poolName) {
        this.poolName = poolName;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setThreadCount(int threadCount) {
        this.threadCount = threadCount;
    }

    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    @Override
    public void setTimeAfterSend(int timeAfterSend) {
        System.out.println("time after send:: " + timeAfterSend);
    }

    public void setMessageProcessor(MessageProcessor messageProcessor) {
        this.messageProcessor = messageProcessor;
    }

    public void setProcessReadException(boolean processReadException) {

    }

    @Override
    public String process(MessageContext context) {
        return null;
    }

}
