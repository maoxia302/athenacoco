package athena.socket.control;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public abstract class BasicSocket implements Runnable {

	private Socket socket;
	private boolean active;
	private boolean open;
	private boolean autoReceive;
	private ByteBuffer readBuf;
	private int interval;
	private int connectionLinger;
	private int connectionTimeout;
	private List<InetSocketAddress> addresses;
	private ConcurrentLinkedQueue<byte[]> sendList;

	private class SendThread implements Runnable {
		public void run() {
			while (true) {
				if (!active) {
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					continue;
				}
				try {
					while (true) {
						byte[] data = sendList.poll();
						if (data == null)
							break;
						if (open) {
							System.out.println("*** SOCKET WRITE DATA *** === " + openByteArr(data));
							socket.getOutputStream().write(data);
						}
					}
					if (open) {
						socket.getOutputStream().flush();
					}
					Thread.sleep(1);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

	}

	public BasicSocket(int interval, int bufSize, int connectionLinger,
			int connectionTimeout, boolean autoReceive) {
		this.addresses = new LinkedList<>();
		this.autoReceive = autoReceive;
		this.interval = interval;
		this.readBuf = ByteBuffer.allocate(bufSize);
		this.connectionLinger = connectionLinger;
		this.connectionTimeout = connectionTimeout;
		sendList = new ConcurrentLinkedQueue<>();
		Thread sendThread = new Thread(new SendThread());
		sendThread.start();
	}

	protected final String getAddress() {
		return socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
	}

	public final boolean isAutoReceive() {
		return autoReceive;
	}

	public void add(String address, int port) {
		addresses.add(new InetSocketAddress(address, port));
	}

	protected abstract void doOpen() throws IOException;

	protected abstract void doClose() throws IOException;

	protected abstract void doRead(ByteBuffer buf) throws IOException;

	public final void read() {
		try {
			int readSize = socket.getChannel().read(readBuf);
			if (readSize < 0) {
				restart();
			} else {
				doRead(readBuf);
			}
		} catch (IOException e) {
			restart();
		}
	}

	public boolean send(byte[] buf) {
		if (!open) {
			return false;
		}
		sendList.add(buf);
		return true;
	}

	/**
	 * modified
	 */
	private synchronized void internalOpen() {
		if (active) {
			if (open) {
				return;
			}
			for (InetSocketAddress currentAddress : addresses) {
				try {
					SocketChannel channel = SocketChannel.open(currentAddress);
					socket = channel.socket();
					if (connectionLinger < 0) {
						socket.setSoLinger(false, 0);
					} else {
						socket.setSoLinger(true, connectionLinger);
					}
					socket.setTcpNoDelay(true);
					socket.setKeepAlive(true);
					if (connectionTimeout >= 0) {
						socket.setSoTimeout(connectionTimeout);
					}
					open = true;
					doOpen();
					break;
				} catch (Exception e) {
					if (socket != null) {
						if (!socket.isClosed()) {
							try {
								socket.close();
								socket = null;
							} catch (Exception ignored) { }
						}
					}
					open = false;
				}
			}
			if (!open) {
				printFailAddresses();
			}
		}
	}

	private void printFailAddresses() {
		for (InetSocketAddress currentAddress : addresses) {
			System.out.println("Can not open connection for address:" + currentAddress.getHostName() + " port:" + currentAddress.getPort());
		}
	}

	private synchronized void internalClose() {
		if (socket == null)
			return;
		if (socket.isClosed()) {
			socket = null;
			open = false;
			return;
		}
		try {
			doClose();
		} catch (IOException ignored) {
		} finally {
			try {
				socket.close();
				socket = null;
			} catch (Exception e) {
				e.printStackTrace();
			}
			open = false;
		}
	}

	public void start() {
		active = true;
	}

	private boolean restarting = false;

	public void restart() {
		if (restarting) return;
		restarting = true;
		internalClose();
		internalOpen();
		restarting = false;
	}

	public void stop() {
		active = false;
	}

	public void close() {
		internalClose();
	}

	public void run() {
		while (true) {
			try {
				if (active) {
					if (!open) {
						internalOpen();
						if (!open) {
							Thread.sleep(interval);
						}
					} else if (autoReceive) {
						read();
					} else {
						Thread.sleep(1);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private static synchronized String openByteArr(byte[] bArr) {
		String b = "";
		if(bArr != null && bArr.length != 0) {
			b = new String(bArr, StandardCharsets.UTF_8);
		}
		return b;
	}
}
