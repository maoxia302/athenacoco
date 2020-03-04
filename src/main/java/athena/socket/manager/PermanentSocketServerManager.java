package athena.socket.manager;

import java.net.Socket;

import athena.socket.control.BasicSocketClient;
import athena.socket.control.BasicSocketServerManager;
import athena.socket.control.SocketChannelManager;
import athena.socket.core.MessageProcessor;

public class PermanentSocketServerManager extends BasicSocketServerManager {

	private int bufferSize;
	private MessageProcessor processor;
	
	public PermanentSocketServerManager(int port,
										int portSize,
										int connectionLinger,
										int connectionTimeout,
										int bufferSize,
										MessageProcessor processor) {
		super(port, portSize, connectionLinger, connectionTimeout);
		this.bufferSize = bufferSize;
		this.processor = processor;
	}
	
	@Override
	protected BasicSocketClient doGetClient(Socket socket, SocketChannelManager manager) {
		return new SocketClient(socket, manager, bufferSize, processor);
	}

}