package athena.socket.control;

import athena.socket.util.StaticThreadPool;
import athena.socket.util.ThreadPool;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@SuppressWarnings("ALL")
public abstract class BasicSocketChannelClientManager implements BasicSocketChannelManager {

    private Selector selector = null;
    private boolean isActive;
    private ThreadPool pool;
    private SocketChannelManager manager;
    private ConcurrentMap<SocketChannel, ChangeRequest> pendingChanges;
    private int threadCount;
    private int connectionLinger;
    private int connectionTimeout;
    private String poolName;
    private int connectionSize;
    List<BasicSocketChannelClient> connectList;

    public List<BasicSocketChannelClient> getConnectList() {
        return connectList;
    }

    public void setConnectList(List<BasicSocketChannelClient> connectList) {
        this.connectList = connectList;
    }

    public BasicSocketChannelClientManager(int threadCount,
                                           int connectionLinger, int connectionTimeout) {
        this(threadCount, connectionLinger, connectionTimeout, null);
    }

    public BasicSocketChannelClientManager(int threadCount,
                                           int connectionLinger, int connectionTimeout, int connectionSize) {
        this(threadCount, connectionLinger, connectionTimeout, null,
                connectionSize);
    }

    public BasicSocketChannelClientManager(int threadCount,
                                           int connectionLinger, int connectionTimeout, String poolName) {
        this(threadCount, connectionLinger, connectionTimeout, poolName, 1);
    }

    public BasicSocketChannelClientManager(int threadCount,
                                           int connectionLinger, int connectionTimeout, String poolName,
                                           int connectionSize) {
        this.connectionSize = connectionSize;
        if (this.connectionSize < 1) this.connectionSize = 1;
        this.manager = new MultiSocketChannelManager(true);
        this.pendingChanges = new ConcurrentHashMap<>();
        this.threadCount = threadCount;
        this.connectionLinger = connectionLinger;
        this.connectionTimeout = connectionTimeout;
        this.poolName = poolName;
        this.connectList = new ArrayList<>();
    }

    public SocketChannelManager getManager() {
        return manager;
    }

    public boolean send(String socketAddress, byte[] data) throws IOException {
        BasicSocketChannelClient client = (BasicSocketChannelClient) manager.get(socketAddress);
        return send(client, data);
    }

    public boolean send(BasicSocketChannelClient client, byte[] data) {
        if (client != null) {
            if (client.write(data)) {
                pendingChanges.put(client.getSocketChannel(),
                        new ChangeRequest(client.getSocketChannel(), ChangeRequest.CHANGE_OPS,
                                SelectionKey.OP_WRITE | SelectionKey.OP_READ));
                this.selector.wakeup();
            }
            return true;
        } else {
            return false;
        }
    }

    public void close(String socketAddress) {
        manager.close(socketAddress);
    }

    public void start() {
        try {
            // Create the selector
            selector = Selector.open();
            if (poolName == null)
                pool = new StaticThreadPool(threadCount,
                        SocketProcessWorker.class, this);
            else
                pool = new StaticThreadPool(poolName, threadCount,
                        SocketProcessWorker.class, this);
            isActive = true;
            Thread thread = new Thread(this);
            thread.start();
        } catch (Exception e) {
            e.printStackTrace();
            isActive = false;
        }
    }

    public void stop() {
        try {
            if (isActive) {
                isActive = false;
                selector.close();
                manager.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void run() {
        while (isActive) {
            try {
                initiateConnection();
                // Process any pending changes
                Iterator<SocketChannel> iter = pendingChanges.keySet()
                        .iterator();
                while (iter.hasNext()) {
                    SocketChannel mapKey = iter.next();
                    ChangeRequest change = pendingChanges.remove(mapKey);
                    if (change != null) {
                        if ((change.socket != null) && (change.socket.isOpen())) {
                            switch (change.type) {
                                case ChangeRequest.CHANGE_OPS:
                                    SelectionKey key = change.socket
                                            .keyFor(this.selector);
                                    key.interestOps(change.ops);
                                    break;
                                case ChangeRequest.REGISTER:
                                    change.socket.register(this.selector,
                                            change.ops);
                                    break;
                            }
                        }
                    }
                }

                // Wait for an event one of the registered channels
                this.selector.select(100);

                // Iterate over the set of keys for which events are available
                Iterator<SelectionKey> it = this.selector.selectedKeys()
                        .iterator();
                while (it.hasNext()) {
                    SelectionKey key = it.next();
                    it.remove();

                    if (key.isValid()) {
                        // Check what event is available and deal with it
                        if (key.isConnectable()) {
                            this.finishConnection(key);
                        } else if (key.isReadable() || key.isWritable()) {
                            this.read(key);
                        }
                    }
                }
            } catch (CancelledKeyException e) {

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void read(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        BasicSocketChannelClient client = (BasicSocketChannelClient) key.attachment();
        if (client == null) {
            client = (BasicSocketChannelClient) manager.get(channel);
            if (client == null) {
                return;
            }
            key.attach(client);
        }
        client.setReadable(key.isReadable());
        client.setWritable(key.isWritable());
        addPendingChanges(channel, ChangeRequest.CHANGE_OPS, 0);

        pool.performWork(key);
    }

    protected abstract BasicSocketChannelClient doGetClient(
            SocketChannel channel, SocketChannelManager manager,
            InetSocketAddress socketAddress);

    public void addConnection(String address, int port) throws IOException {
        for (int i = 0; i < connectionSize; i++) {
            // Create a non-blocking socket channel
            InetSocketAddress socketAddress = new InetSocketAddress(address, port);
            synchronized (connectList) {
                connectList.add(doGetClient(null, manager, socketAddress));
            }
        }
    }

    public void addConnection(BasicSocketChannelClient client) {
        client.setDynamicClient();
        synchronized (connectList) {
            connectList.add(client);
        }
    }

    public void removeConnection(BasicSocketChannelClient client) {
        synchronized (connectList) {
            connectList.remove(client);
        }
    }

    private BasicSocketChannelClient getClient(SocketChannel channel) {
        for (BasicSocketChannelClient client : connectList) {
            if (channel == client.getSocketChannel()) {
                return client;
            }
        }
        return null;
    }

    private void initiateConnection() {
        synchronized (connectList) {
            for (BasicSocketChannelClient client : connectList) {
                if (!client.isOpen()) initiateConnection(client);
            }
        }
    }

    public void addPendingChanges(SocketChannel socket, int type, int ops) {
        if (socket != null) {
            pendingChanges.put(socket, new ChangeRequest(socket, type, ops));
        }
    }

    public void initiateConnection(SocketSession ss) {
        BasicSocketChannelClient client = (BasicSocketChannelClient) ss;
        SocketChannel socketChannel = null;
        try {
            socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(false);
            socketChannel.connect(client.getSocketAddress());
            client.setSocketChannel(socketChannel);
            addPendingChanges(socketChannel, ChangeRequest.REGISTER, SelectionKey.OP_CONNECT);
        } catch (IOException e) {
            if (socketChannel != null) {
                try {
                    client.setSocketChannel(null);
                    socketChannel.close();
                } catch (IOException ignored) { }
            }
            e.printStackTrace();
        }
    }

    private void finishConnection(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();

        // Finish the connection. If the connection operation failed
        // this will raise an IOException.
        try {
            socketChannel.finishConnect();
        } catch (IOException e) {
            // Cancel the channel's registration with our selector
            key.cancel();
            try {
                socketChannel.close();
            } catch (IOException ignored) { }
            return;
        }
        socketChannel.socket().setTcpNoDelay(true);
        /*
         * Warning :
         *
         * To be able to close more quickly a connection, it's recommanded to
         * set linger to a small value.
         *
         * AJP13 connection SHOULD be closed under webserver responsability and
         * in such case it's safe to close socket on Tomcat side without delay,
         * which may be also the case for HTTP connectors.
         *
         * I (henri) recommand to set Linger to 0, making socket closed
         * immediatly so the OS will free faster the underlying io descriptor
         * and resources. It's very important under heavy load !
         */
        if (connectionLinger < 0)
            socketChannel.socket().setSoLinger(false, 0);
        else
            socketChannel.socket().setSoLinger(true, connectionLinger);

        socketChannel.socket().setKeepAlive(true);

        /*
         * Warning :
         *
         * AJP13 shouldn't use socket timeout on tomcat site since when Tomcat
         * close a connection after a timeout is reached the socket stay in
         * half-closed state until the webserver try to send a request to tomcat
         * and detect the socket close when it will try to read the reply.
         *
         * On many Unix platforms the write() call didn't told webserver that
         * the socket is closed.
         */
        if (connectionTimeout >= 0) {
            socketChannel.socket().setSoTimeout(connectionTimeout);
        }

        addPendingChanges(socketChannel, ChangeRequest.CHANGE_OPS, SelectionKey.OP_READ);
        BasicSocketChannelClient client = getClient(socketChannel);
        if (client != null) {
            client.setRegistered();
            manager.register(client);
            client.setSelectionKey(key);
            client.open();
        }
    }
}
