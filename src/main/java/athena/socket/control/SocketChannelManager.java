package athena.socket.control;

import java.nio.channels.SocketChannel;

public interface SocketChannelManager {

	void close();
	
	void close(String socketAddress);

	SocketSession get(SocketChannel channel);

	SocketSession get(String address);

	void register(SocketSession client);

	void unregister(SocketSession client);

	String getAddress(SocketSession client);
	
	int getPort(SocketSession client);
	
	boolean opened(SocketSession client);
	
	boolean canClose(SocketSession client);

}