package athena.socket.control;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class BasicSocketChannelClient implements SocketSession {

    private SocketChannel channel;
    private SocketChannelManager manager;
    private ByteBuffer readbuf;
    private InetSocketAddress socketAddress;
    private SelectionKey selectionKey;
    private boolean remote;
    private boolean active = false;
    private final AtomicBoolean locked = new AtomicBoolean(false);
    private int bufSize = 0;
    private final ByteBuffer readbufLength;
    private boolean isDynamicClient = false;
    private ConcurrentLinkedQueue<byte[]> sendList = new ConcurrentLinkedQueue<>();
    private boolean isRegistered = false;

    public BasicSocketChannelClient(SocketChannel channel, SocketChannelManager manager, int bufSize,
                                    InetSocketAddress socketAddress) {
        this.channel = channel;
        this.manager = manager;
        this.bufSize = bufSize;
        this.readbuf = ByteBuffer.allocate(bufSize);
        this.readbufLength = ByteBuffer.allocate(4);
        this.socketAddress = socketAddress;
        this.remote = true;
    }

    public BasicSocketChannelClient(SocketChannel channel, SocketChannelManager manager, int bufSize, boolean remote) {
        this.channel = channel;
        this.manager = manager;
        this.readbuf = ByteBuffer.allocate(bufSize);
        this.readbufLength = ByteBuffer.allocate(4);
        this.socketAddress = new InetSocketAddress(channel.socket().getInetAddress(), channel.socket().getPort());
        this.remote = remote;
    }

    void setDynamicClient() {
        this.isDynamicClient = true;
    }

    public String getAddress() {
        return manager.getAddress(this) + ":" + manager.getPort(this);
    }

    public int getPort() {
        return manager.getPort(this);
    }

    public InetSocketAddress getSocketAddress() {
        return this.socketAddress;
    }

    SocketChannel getSocketChannel() {
        return this.channel;
    }

    void setSocketChannel(SocketChannel channel) {
        this.channel = channel;
    }

    public int getLocalPort() {
        if (channel != null)
            return channel.socket().getLocalPort();
        return -1;
    }

    void setSelectionKey(SelectionKey selectionKey) {
        this.selectionKey = selectionKey;
    }

    protected boolean isRemote() {
        return this.remote;
    }

    protected abstract void doOpen() throws IOException;

    protected abstract void doClose() throws IOException;

    protected abstract boolean doCanAccept() throws IOException;

    protected abstract void doRead(ByteBuffer buf) throws IOException;

    final void open() throws IOException {
        active = true;
        locked.set(false);
        if (isDynamicClient || manager.opened(this)) {
            doOpen();
        }
    }

    final boolean isOpen() {
        if (isClosing)
            return true;
        if (channel == null)
            return false;
        return channel.isOpen();
    }

    final boolean canAccept() throws IOException {
        return doCanAccept();
    }

    private boolean isReadable = false;

    void setReadable(boolean isReadable) {
        this.isReadable = isReadable;
    }

    boolean isReadable() {
        return isReadable;
    }

    private boolean isWritable = false;

    void setWritable(boolean isWritable) {
        this.isWritable = isWritable;
    }

    boolean isWritable() {
        return isWritable;
    }

    private void resetLengthBuf() {
        this.readbufLength.clear();
        this.readbufLength.put(new byte[4]);
        this.readbufLength.position(0);
    }

    private void resetBuf() {
        readbuf.position(0);
        readbuf.clear();
    }

    private int toLength(byte[] lengthBytes) {
        return Integer.parseInt(new String(lengthBytes, StandardCharsets.UTF_8), 16);
    }

    final int read(BasicSocketChannelManager manager) {
        if (locked.get()) return 0;
        synchronized (locked) {
            if (locked.get()) return 0;
            locked.set(true);
        }
        try {
            if (channel == null) return -1;
            resetLengthBuf();
            int length;
            try {
                int r = channel.read(this.readbufLength);
                if (r > 0) {
                    length = toLength(this.readbufLength.array());
                    bufSize = length;
                    readbuf = ByteBuffer.allocate(bufSize);
                    int readSize;
                    try {
                        readSize = channel.read(readbuf);
                    } catch (Exception e) {
                        return -1;
                    }
                    if (readSize > 0) {
                        doRead(readbuf);
                    }
                    return readSize;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                return -1;
            }
            return -1;
        } finally {
            locked.set(false);
            resetBuf();
            manager.addPendingChanges(channel, ChangeRequest.CHANGE_OPS, SelectionKey.OP_READ);
        }
    }

    public final boolean write(byte[] outBuf) {
        boolean isActive = active;
        if (isActive) sendList.add(outBuf);
        return isActive;
    }

    private Boolean sendData = false;
    private final Object writeLock = new Object();

    private byte[] headLengthAppend(int length) {
        String h = Integer.toHexString(length);
        if (h.length() < 4) {
            h = "0000".substring(0, 4 - h.length()) + h;
        }
        return h.toUpperCase().getBytes(StandardCharsets.UTF_8);
    }

    void write(BasicSocketChannelManager manager) {
        if (sendData)
            return;
        synchronized (writeLock) {
            if (sendData)
                return;
            sendData = true;
        }
        try {
            byte[] data;
            manager.addPendingChanges(channel, ChangeRequest.CHANGE_OPS, SelectionKey.OP_WRITE);
            while (true) {
                data = sendList.poll();
                if (data == null)
                    break;
                if (active) {
                    byte[] lengthBytes = headLengthAppend(data.length);
                    byte[] finalData = new byte[lengthBytes.length + data.length];
                    System.arraycopy(lengthBytes, 0, finalData, 0, lengthBytes.length);
                    System.arraycopy(data, 0, finalData, lengthBytes.length, data.length);
                    ByteBuffer writebuf = ByteBuffer.wrap(finalData);
                    while (true) {
                        try {
                            channel.write(writebuf);
                        } catch (Exception e) {
                            e.printStackTrace();
                            System.out.println("SOCKET-CLOSED");
                            return;
                        }
                        if (!writebuf.hasRemaining()) {
                            break;
                        }
                        try {
                            Thread.sleep(5);
                        } catch (InterruptedException ignored) {
                        }
                    }
                    channel.socket().getOutputStream().flush();
                }
                Thread.sleep(30);
            }
            if (active) {
                if (channel != null &&
                        channel.socket() != null &&
                        channel.socket().getOutputStream() != null)
                    channel.socket().getOutputStream().flush();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            sendData = false;
        }
    }

    final void setRegistered() {
        this.isRegistered = true;
    }

    private boolean isClosing = false;

    public void close() {
        System.out.println("SOCKET CLOSING::" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        isClosing = true;
        boolean isActive = active;
        boolean isReg = isRegistered;
        active = false;
        isRegistered = false;
        sendList.clear();
        try {
            if (isReg) {
                manager.unregister(this);
            }
            try {
                if (selectionKey != null) {
                    selectionKey.cancel();
                    selectionKey = null;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                if (channel != null) {
                    channel.close();
                    channel = null;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (isActive && isReg) {
                    if (isDynamicClient || manager.canClose(this))
                        doClose();
                }
            } catch (Throwable ex) {
                ex.printStackTrace();
            }
            locked.set(false);
            isClosing = false;
        }
    }
}
