package athena.socket.control;

import java.io.IOException;
import java.net.InetSocketAddress;

public interface SocketSession {

	void close();
	InetSocketAddress getSocketAddress();
	int getLocalPort();
	String getAddress();
	int getPort();
	boolean write(byte[] outBuf) throws IOException;
}
