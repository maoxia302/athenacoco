package athena.socket.core;


import athena.socket.manager.DataProcess;
import athena.socket.manager.factory.TcpServerFactory;
import athena.socket.manager.processor.SocketChannelProcessor;

public class Initiation {

    private static final int CONNECTION_LINGER = 0;
    private static final int CONNECTION_TIMEOUT = 30000;
    private static final String DEFAULT_SOCKET_POOL_NAME = "ATHENA_SOCKET_POOL";
    private static final boolean DEFAULT_MULTIPLE_SERVER_SIGN = false;
    public static final int DEFAULT_PORT = 9999;

    private static Initiation initiation = null;

    private TcpServerFactory factory;

    private Initiation(int port) {
        factory = new TcpServerFactory(CONNECTION_LINGER,
                CONNECTION_TIMEOUT,
                DEFAULT_SOCKET_POOL_NAME,
                DEFAULT_MULTIPLE_SERVER_SIGN);
        factory.setPort(port);
    }

    public static Initiation init(int port) {
        if (initiation == null) {
            initiation = new Initiation(port);
        }
        return initiation;
    }

    public DataProcess start() throws Exception {
        SocketChannelProcessor process = factory.getProcessor();
        process.start();
        return (DataProcess) ((TcpServerFactory.ChannelProcess) process).getProcessor();
    }

}
