package athena.socket.control;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class BasicSocketServerManager implements BasicSocketChannelManager{
	private boolean isActive;
	private SocketChannelManager manager;
	private int port;
	private int portSize;
	private int connectionLinger;
	private int connectionTimeout;
	private ServerSocket serverSocket;
	private ExecutorService executorService;

	public BasicSocketServerManager(int port, int portSize,
			int connectionLinger,int connectionTimeout) {
		this.manager = new SingleSocketChannelManager();
		this.port = port;
		this.portSize = portSize;
		this.connectionLinger = connectionLinger;
		this.connectionTimeout = connectionTimeout;
	}

	public SocketChannelManager getManager() {
		return manager;
	}

	private boolean send(SocketSession client, byte[] data) throws IOException{
		if (client != null) {
			try {
				client.write(data);
			}
			catch(IOException e) {
				e.printStackTrace();
				client.close();
				throw e;
			}
			return true;
		}
		else
			return false;
	}

	public boolean send(String socketAddress, byte[] data) throws IOException {
		SocketSession client = manager.get(socketAddress);
		return send(client, data);
	}

	public void close(String socketAddress) {
		manager.close(socketAddress);
	}

	private ServerSocket createSocket(int port,int portSize) {
		ServerSocket sSocket;
		for (int i = 0; i < portSize; i++) {
			try {
				sSocket = new ServerSocket(port+i);
				return sSocket;
			}
			catch (Exception ignored) { }
		}
		throw new RuntimeException("cannot open ports: " + port + " to " + (port+portSize - 1));
	}
	
	public void start() {
		try {
			serverSocket = createSocket(port, portSize);
			executorService = Executors.newCachedThreadPool();
			isActive = true;
			Thread thread = new Thread(this);
			thread.start();
		} catch (Exception e) {
			isActive = false;
			e.printStackTrace();
		}
	}

	public void stop() {
		try {
			if (isActive) {
				isActive = false;
				manager.close();
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void initiateConnection(SocketSession ss) {

	}

	public void addPendingChanges(SocketChannel socket, int type, int ops) {

	}
	
	protected abstract BasicSocketClient doGetClient(
			Socket socket,SocketChannelManager manager);

	public void run() {
		while (isActive) {
			Socket socket;
			try {
				socket = serverSocket.accept();
				socket.setTcpNoDelay(true);
//				/* Warning :
//				 *
//				 * To be able to close more quickly a connection, it's recommended
//				 * to set linger to a small value.
//				 *
//				 * AJP13 connection SHOULD be closed under web server responsibility and
//				 * in such case it's safe to close socket on Tomcat side without delay,
//				 * which may be also the case for HTTP connectors.
//				 *
//				 * I (henri) recommend to set Linger to 0, making socket closed immediately
//				 * so the OS will free faster the underlying io descriptor and resources.
//				 * It's very important under heavy load !
//				 */
				socket.setSoLinger(true, connectionLinger);
				socket.setKeepAlive(true);
//				/* Warning :
//				 *
//				 * AJP13 shouldn't use socket timeout on tomcat site since
//				 * when Tomcat close a connection after a timeout is reached
//				 * the socket stay in half-closed state until the web server
//				 * try to send a request to tomcat and detect the socket close
//				 * when it will try to read the reply.
//				 *
//				 * On many Unix platforms the write() call didn't told
//				 * web server that the socket is closed.
//				 */
				if (connectionTimeout >= 0) {
					socket.setSoTimeout(connectionTimeout);
				}
				BasicSocketClient client = doGetClient(socket,manager);
				executorService.execute(client);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
