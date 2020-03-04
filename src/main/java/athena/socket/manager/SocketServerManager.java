package athena.socket.manager;

import athena.socket.control.BasicSocketChannelClient;
import athena.socket.control.BasicSocketChannelServerManager;
import athena.socket.control.SocketChannelManager;
import athena.socket.core.MessageProcessor;

import java.nio.channels.SocketChannel;

public class SocketServerManager extends BasicSocketChannelServerManager {

	private int bufferSize ;
	private boolean remote;
	private MessageProcessor processor;
	
	public SocketServerManager(int port,
							   int portSize,
							   int threadCount,
							   int connectionLinger,
							   int connectionTimeout,
							   String poolName,
							   boolean multi,
							   int bufferSize,
							   boolean remote,
							   MessageProcessor processor) {
		super(port, portSize, threadCount, connectionLinger, connectionTimeout, poolName, multi);
		this.bufferSize = bufferSize;
		this.remote = remote;
		this.processor = processor;
	}
	
	@Override
	protected BasicSocketChannelClient doGetClient(SocketChannel channel, SocketChannelManager manager) {
		return new SocketChannelClient(channel, manager, bufferSize, remote, processor);
	}

	public boolean isRemote() {
		return remote;
	}

	public void setRemote(boolean remote) {
		this.remote = remote;
	}

	public int getBlock() {
		return bufferSize;
	}

	public void setBlock(int block) {
		this.bufferSize = block;
	}
}
