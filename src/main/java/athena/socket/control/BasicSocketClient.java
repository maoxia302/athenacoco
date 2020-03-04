package athena.socket.control;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentLinkedQueue;

public abstract class BasicSocketClient implements SocketSession, Runnable {

    private Socket socket;
    private SocketChannelManager manager;
    private ByteBuffer readBuf;
    private InetSocketAddress socketAddress;
    private boolean active = false;
    private int bufSize;
    private ConcurrentLinkedQueue<byte[]> sendList = new ConcurrentLinkedQueue<>();

    public BasicSocketClient(Socket socket, SocketChannelManager manager, int bufSize) {
        this.socket = socket;
        this.manager = manager;
        this.bufSize = bufSize;
        this.readBuf = ByteBuffer.allocate(bufSize * 2);
        this.socketAddress = new InetSocketAddress(socket.getInetAddress(), socket.getPort());
    }

    public String getAddress() {
        return manager.getAddress(this);
    }

    public int getPort() {
        return manager.getPort(this);
    }

    public InetSocketAddress getSocketAddress() {
        return this.socketAddress;
    }

    public int getLocalPort() {
        if (socket != null)
            return socket.getLocalPort();
        return -1;
    }

    protected abstract void doOpen() throws IOException;

    protected abstract void doClose() throws IOException;

    protected abstract boolean doCanAccept() throws IOException;

    public final void open() throws IOException {
        active = true;
        if (manager.opened(this))
            doOpen();
    }

    protected final boolean canAccept() throws IOException {
        return doCanAccept();
    }

    protected abstract void doRead(ByteBuffer buf) throws IOException;

    private void initConnection() {
        try {
            if (canAccept()) {
                manager.register(this); // SingleSocketChannelManager/BasicSocketServerManager
                open();
            } else {
                close();
            }
        } catch (IOException e) {
            close();
            e.printStackTrace();
        }
    }

    public void run() {
        initConnection();
        try {
            InputStream socketIn = socket.getInputStream();
            byte[] data = new byte[bufSize];
            int size;
            System.out.println("*** CLIENT SOCKET WRITE DATA *** === " + openByteArr(data));
            while (active && ((size = socketIn.read(data)) >= 0)) {
                if (size > 0) {
                    readBuf.put(data, 0, size);
                    doRead(readBuf);
                }
            }
        } catch (Exception ignored) { }
        close();
    }

    public final boolean write(byte[] outBuf) throws IOException {
        boolean isActive = active;
        if (isActive)
            sendList.add(outBuf);
        write();
        return isActive;
    }

    private Boolean sendData = false;
    private Object writeLock = new Object();

    private void write() throws IOException {
        if (sendData) return;
        synchronized (writeLock) {
            if (sendData) return;
            sendData = true;
        }
        try {
            byte[] data;
            while (true) {
                data = sendList.poll();
                if (data == null) break;

                if (active) {
                    socket.getOutputStream().write(data);
                }
            }
            if (active) {
                socket.getOutputStream().flush();
            }
        } finally {
            sendData = false;
        }
    }

    public void close() {
        boolean isActive = active;
        active = false;
        sendList.clear();
        try {
            manager.unregister(this);
        } catch (Exception ignored) {
        } finally {
            try {
                if (isActive) {
                    if (manager.canClose(this))
                        doClose();
                }
            } catch (Throwable ignored) {
            }
        }
    }

    private static synchronized String openByteArr(byte[] bArr) {
        String b = "";
        if (bArr != null && bArr.length != 0) {
            b = new String(bArr, StandardCharsets.UTF_8);
        }
        return b;
    }
}
