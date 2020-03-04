package athena.socket.control;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import athena.socket.util.StaticThreadPool;
import athena.socket.util.ThreadPool;

public abstract class BasicSocketChannelServerManager implements BasicSocketChannelManager {

    private SocketChannelManager manager;
    private Selector selector = null;
    private boolean isActive;
    private ServerSocketChannel ssChannel;
    private ThreadPool pool;
    private int connectionLinger;
    private int connectionTimeout;
    private String poolName;
    private ConcurrentMap<SocketChannel, ChangeRequest> pendingChanges;

    private int port;
    private int portSize;
    private int threadCount;

    public BasicSocketChannelServerManager(int port, int portSize, int threadCount,
                                           int connectionLinger, int connectionTimeout) {
        this(port, portSize, threadCount, connectionLinger, connectionTimeout, null);
    }

    public BasicSocketChannelServerManager(int port, int portSize, int threadCount,
                                           int connectionLinger, int connectionTimeout, boolean multi) {
        this(port, portSize, threadCount, connectionLinger, connectionTimeout, null, multi);
    }

    public BasicSocketChannelServerManager(int port, int portSize, int threadCount,
                                           int connectionLinger, int connectionTimeout, String poolName) {
        this(port, portSize, threadCount, connectionLinger, connectionTimeout, poolName, false);
    }

    public BasicSocketChannelServerManager(int port, int portSize, int threadCount,
                                           int connectionLinger, int connectionTimeout, String poolName, boolean multi) {
        if (multi) {
            this.manager = new MultiSocketChannelManager(false);
        } else {
            this.manager = new SingleSocketChannelManager();
        }
        this.port = port;
        this.portSize = portSize;
        this.threadCount = threadCount;
        this.connectionLinger = connectionLinger;
        this.connectionTimeout = connectionTimeout;
        this.pendingChanges = new ConcurrentHashMap<>();
        this.poolName = poolName;
    }

    public SocketChannelManager getManager() {
        return manager;
    }

    private ServerSocketChannel createServerSocketChannel(int port, int portSize) throws
            Exception {
        ServerSocketChannel ssChannel = ServerSocketChannel.open();
        ssChannel.configureBlocking(false);
        for (int i = 0; i < portSize; i++) {
            try {
                System.out.println("server channel ports range:: " + port + " -- " + portSize);
                ssChannel.socket().bind(new InetSocketAddress(port + i));
                return ssChannel;
            } catch (Exception ignored) {
            }
        }
        throw new RuntimeException("unable to open tcp port:" + port + " to port:" + (port + portSize - 1));
    }

    public void initiateConnection(SocketSession ss) {
        BasicSocketChannelClient client = (BasicSocketChannelClient) ss;
        try {
            if (client.canAccept()) {
                getManager().register(client);
                client.setRegistered();
                client.setReadable(true);
                client.setWritable(true);
                client.open();
            } else {
                System.out.println("client not acceptable");
                client.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
            client.close();
        }
    }

    private boolean send(BasicSocketChannelClient client, byte[] data) {
        if (client != null) {
            if (client.write(data)) {
                pendingChanges.put(client.getSocketChannel(),
                        new ChangeRequest(client.getSocketChannel(),
                                ChangeRequest.CHANGE_OPS, SelectionKey.OP_WRITE | SelectionKey.OP_READ));
                this.selector.wakeup();
            }
            return true;
        } else
            return false;
    }

    public boolean send(String socketAddress, byte[] data) {
        BasicSocketChannelClient client = (BasicSocketChannelClient) manager.get(socketAddress);
        return send(client, data);
    }

    public void close(String socketAddress) {
        manager.close(socketAddress);
    }

    public void start() {
        try {
            // Create the selector
            selector = Selector.open();
            // Create two non-blocking sockets. This method is implemented in
            ssChannel = createServerSocketChannel(port, portSize);
            // Register the channel with selector, listening for all events
            ssChannel.register(selector, SelectionKey.OP_ACCEPT);
            if (poolName == null)
                pool = new StaticThreadPool(threadCount, SocketProcessWorker.class, this);
            else
                pool = new StaticThreadPool(poolName, threadCount, SocketProcessWorker.class, this);
            isActive = true;
            //启动线程
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
                ssChannel.socket().close();
                manager.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void run() {
        Thread.currentThread().setName("BasicSocketChannelServerManager: " + port);
        System.out.println("BasicSocketChannelServerManager: " + port);
        while (isActive) {
            try {
                for (SocketChannel mapKey : pendingChanges.keySet()) {
                    ChangeRequest change = pendingChanges.remove(mapKey);
                    if (change != null) {
                        if ((change.socket != null) && (change.socket.isOpen())) {
                            switch (change.type) {
                                case ChangeRequest.CHANGE_OPS:
                                    SelectionKey key = change.socket.keyFor(this.selector);
                                    key.interestOps(change.ops);
                                    break;
                                case ChangeRequest.REGISTER:
                                    change.socket.register(this.selector, change.ops);
                                    break;
                            }
                        }
                    }
                }
                // Wait for an event
                selector.select(3000);
                // Get list of selection keys with pending events
                Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                // Process each key at a time
                while (it.hasNext()) {
                    // Get the selection key
                    SelectionKey selKey = it.next();
                    // Remove it from the list to indicate that it is being
                    // processed
                    it.remove();
                    processSelectionKey(selKey);
                }
                Thread.sleep(20);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    public void addPendingChanges(SocketChannel socket, int type, int ops) {
        if (socket != null) {
            pendingChanges.put(socket, new ChangeRequest(socket, type, ops));
        }
    }

    protected abstract BasicSocketChannelClient doGetClient(SocketChannel channel, SocketChannelManager manager);

    private void processSelectionKey(SelectionKey selKey) {
        if (!selKey.isValid()) {
            BasicSocketChannelClient client = (BasicSocketChannelClient) selKey.attachment();
            if (client != null) {
                System.out.println("closing on selection key:::" + client.toString());
                client.close();
            }
            selKey.cancel();
            return;
        }
        if (selKey.isAcceptable()) {
            // Get channel with connection request
            ServerSocketChannel ssChannel = (ServerSocketChannel) selKey.channel();
            // Accept the connection request.
            // If serverSocketChannel is blocking, this method blocks.
            // The returned channel is in blocking mode.
            SocketChannel channel = null;
            try {
                channel = ssChannel.accept();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            // If serverSocketChannel is non-blocking, sChannel may be null
            if (channel == null) {
                // There were no pending connection requests; try again
                // later.
                // To be notified of connection requests,
                // see e179 Using a Selector to Manage Non-Blocking Server
                // Sockets.
                selKey.cancel();
                return;
            } else {
                // Use the socket channel to communicate with the client
                // See e176 Using a Selector to Manage Non-Blocking Sockets.
                SelectionKey clientKey = null;
                try {
                    channel.configureBlocking(false);
                    channel.socket().setTcpNoDelay(true);
                    /* Warning :
                     *
                     * To be able to close more quickly a connection, it's recommended
                     * to set linger to a small value.
                     *
                     * AJP13 connection SHOULD be closed under web server responsibility and
                     * in such case it's safe to close socket on Tomcat side without delay,
                     * which may be also the case for HTTP connectors.
                     *
                     * I (henri) recommend to set Linger to 0, making socket closed immediately
                     * so the OS will free faster the underlying io descriptor and resources.
                     * It's very important under heavy load !
                     */
                    channel.socket().setKeepAlive(true);
                    if (connectionLinger < 0) {
                        channel.socket().setSoLinger(false, 0);
                    } else {
                        channel.socket().setSoLinger(true, connectionLinger);
                    }
                    /* Warning :
                     *
                     * AJP13 shouldn't use socket timeout on tomcat site since
                     * when Tomcat close a connection after a timeout is reached
                     * the socket stay in half-closed state until the web server
                     * try to send a request to tomcat and detect the socket close
                     * when it will try to read the reply.
                     *
                     * On many Unix platforms the write() call didn't told
                     * web server that the socket is closed.
                     */
                    if (connectionTimeout >= 0) {
                        channel.socket().setSoTimeout(connectionTimeout);
                    }
                    clientKey = channel.register(selector, SelectionKey.OP_READ);
                } catch (Exception e) {
                    try {
                        channel.close();
                    } catch (IOException ignored) {
                    }
                    e.printStackTrace();
                }
                if (clientKey != null) {
                    BasicSocketChannelClient client = doGetClient(channel, manager);
                    client.setSelectionKey(clientKey);
                    System.out.println("SELECT_KEY:::" + client.getSocketChannel().toString());
                    pool.performWork(client);
                }
            }
        }

        if (selKey.isReadable() || selKey.isWritable()) {
            SocketChannel channel = (SocketChannel) selKey.channel();
            BasicSocketChannelClient client = (BasicSocketChannelClient) selKey.attachment();
            if (client == null) {
                client = (BasicSocketChannelClient) manager.get(channel);
                if (client == null) {
                    return;
                }
                selKey.attach(client);
            }
            client.setReadable(selKey.isReadable());
            client.setWritable(selKey.isWritable());
            addPendingChanges(channel, ChangeRequest.CHANGE_OPS, 0);
            pool.performWork(selKey);
        }
    }
}
