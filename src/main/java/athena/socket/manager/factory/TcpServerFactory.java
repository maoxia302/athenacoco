package athena.socket.manager.factory;

import athena.socket.control.BasicSocketChannelClient;
import athena.socket.control.BasicSocketChannelManager;
import athena.socket.control.BasicSocketChannelServerManager;
import athena.socket.control.SocketChannelManager;
import athena.socket.core.MessageProcessor;
import athena.socket.core.ProcessorFactory;
import athena.socket.manager.DataProcess;
import athena.socket.manager.SocketChannelClient;
import athena.socket.manager.processor.SocketChannelProcessor;

import java.nio.channels.SocketChannel;

public class TcpServerFactory implements ProcessorFactory {

    private static final int PACK_SIZE = 1024 * 8;
    private static final int PORT_SIZE = 1;
    private static final int THREAD_COUNT = 1;

    private int port;
	private int connectionLinger;
	private int connectionTimeout;
	private String poolName;
	private boolean multi;

    public TcpServerFactory setPort(int port) {
	    this.port = port;
	    return this;
    }

    public TcpServerFactory(int connectionLinger, int connectionTimeout, String poolName, boolean multi) {
	    this.connectionLinger = connectionLinger;
	    this.connectionTimeout = connectionTimeout;
	    this.poolName = poolName;
	    this.multi = multi;
    }

	private static class ChannelManager extends BasicSocketChannelServerManager {

	    private MessageProcessor processor;

	    public void setProcessor(MessageProcessor processor) {
	        this.processor = processor;
        }

        public MessageProcessor getProcessor() {
	        return processor;
        }

	    ChannelManager(int port,
                       int connectionLinger,
                       int connectionTimeout,
                       String poolName,
                       boolean multi) {
            super(port,
                    PORT_SIZE,
                    THREAD_COUNT,
                    connectionLinger,
                    connectionTimeout,
                    poolName,
                    multi);
        }

        @Override
        protected BasicSocketChannelClient doGetClient(SocketChannel channel, SocketChannelManager manager) {
	        System.out.println("BasicSocketChannelClient initiatives::" + channel.toString());
            return new SocketChannelClient(channel, manager, PACK_SIZE, true, this.processor);
        }
    }

    public static class ChannelProcess extends SocketChannelProcessor {

	    private BasicSocketChannelManager manager;

	    public MessageProcessor getProcessor() {
	        if (manager instanceof ChannelManager) {
                return ((ChannelManager) manager).getProcessor();
            }
	        return null;
        }

        ChannelProcess(BasicSocketChannelManager manager) {
            this.manager = manager;
        }

        @Override
        protected BasicSocketChannelManager getManager() {
            return this.manager;
        }
    }

	@Override
	public SocketChannelProcessor getProcessor() {
        ChannelManager manager = new ChannelManager(
                port,
                connectionLinger,
                connectionTimeout,
                poolName,
                multi);
        DataProcess dataProcess = new DataProcess();
        manager.setProcessor(dataProcess);
        ChannelProcess m = new ChannelProcess(manager);
        dataProcess.setSocketSender(m.getManager());
		return m;
	}

}
