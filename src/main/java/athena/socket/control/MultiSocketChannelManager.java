package athena.socket.control;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.util.Collection;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class MultiSocketChannelManager implements SocketChannelManager {

	private final ConcurrentMap<String, ConcurrentMap<Integer, SocketSession>> clientTables;
	private boolean isClient;
	private Random random = new Random();

	MultiSocketChannelManager(boolean client) {
		this.clientTables = new ConcurrentHashMap<>();
		this.isClient = client;
	}

	public void close() {
		for (Map<Integer, SocketSession> clientTable : clientTables.values()) {
			for (SocketSession client : clientTable.values()) {
				client.close();
			}
		}
	}

	public void close(String socketAddress) {
		ConcurrentMap<Integer, SocketSession> clientTable = clientTables
				.get(socketAddress);
		if (clientTable != null) {
			Collection<SocketSession> values = clientTable.values();
			for (SocketSession client : values) {
				if (client != null) {
					client.close();
				}
			}
		}
	}

	public String getAddress(SocketSession client) {
		InetSocketAddress socketAddress = client.getSocketAddress();
		System.out.println("MultiSocket-client-address::" + socketAddress.getAddress().getHostAddress() + ":" + socketAddress.getPort());
		return socketAddress.getAddress().getHostAddress();
	}

	public int getPort(SocketSession client) {
		if (isClient) {
			return client.getLocalPort();
		} else {
			InetSocketAddress socketAddress = client.getSocketAddress();
			return socketAddress.getPort();
		}
	}

	public SocketSession get(SocketChannel channel) {
		if (channel == null)
			return null;
		Socket socket = channel.socket();
		if (socket == null)
			return null;
		InetAddress netAddr = socket.getInetAddress();
		if (netAddr == null)
			return null;
		String addressKey = netAddr.getHostAddress();
		ConcurrentMap<Integer, SocketSession> clientTable = clientTables
				.get(addressKey);

		if (clientTable == null)
			return null;
		int portKey;
		if (isClient) {
			portKey = channel.socket().getLocalPort();
		} else {
			portKey = channel.socket().getPort();
		}
		return clientTable.get(portKey);
	}

	public SocketSession get(String address) {
		ConcurrentMap<Integer, SocketSession> clientTable = clientTables
				.get(address);
		if (clientTable == null)
			return null;
		Collection<SocketSession> values = clientTable.values();
		int size = values.size();
		if (size == 0)
			return null;

		SocketSession[] bscl = new SocketSession[size];
		bscl = values.toArray(bscl);

		int index = random.nextInt() % size;
		if (index < 0) {
			index = index + size;
		}
		return bscl[index];
	}

	public void register(SocketSession client) {
		String firstKey = client.getAddress();  //firstKey: address of client
		ConcurrentMap<Integer, SocketSession> clientTable = clientTables.get(firstKey);
		if (clientTable == null) {
			synchronized (clientTables) {
				clientTable = clientTables.get(firstKey);
				if (clientTable == null)
					clientTable = new ConcurrentHashMap<>();
				clientTables.put(firstKey, clientTable);
			}
		}
		clientTable.put(client.getPort(), client);
	}

	public void unregister(SocketSession client) {
		String address = client.getAddress();
		if (address != null) {
			ConcurrentMap<Integer, SocketSession> clientTable = clientTables
					.get(address);
			if (clientTable != null) {
				clientTable.remove(client.getPort());
			}
		}
	}

	public boolean opened(SocketSession client) {
		ConcurrentMap<Integer, SocketSession> clientTable = clientTables
				.get(client.getAddress());
		if (clientTable == null)
			return true;
		return (clientTable.size() <= 1);
	}

	public boolean canClose(SocketSession client) {
		ConcurrentMap<Integer, SocketSession> clientTable = clientTables
				.get(client.getAddress());
		if (clientTable == null)
			return true;
		return (clientTable.size() < 1);
	}
}