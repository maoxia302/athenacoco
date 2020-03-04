package athena.socket.manager.factory;

import athena.socket.core.ProcessorFactory;
import athena.socket.manager.DataProcess;
import athena.socket.manager.processor.SocketChannelProcessor;
import athena.socket.manager.processor.TcpClientProcessor;

public class TcpClientFactory implements ProcessorFactory {

	private static final int THREAD_COUNT = 10;
	private static final int CONNECTION_LINGER = 0;
	private static final int CONNECTION_TIMEOUT = 30000;
	private static final int CONNECTION_SIZE = 100;
	private static final String DEFAULT_POOL_NAME = "ATHENA_TCP_CLIENT";

	DataProcess dataProcessor = new DataProcess();

	private static final int BUFFER = 32547;

	public SocketChannelProcessor getProcessor(String hostIp, int hostPort) {
		return new TcpClientProcessor(
				THREAD_COUNT,
				CONNECTION_LINGER,
				CONNECTION_TIMEOUT,
				DEFAULT_POOL_NAME,
				CONNECTION_SIZE,
				hostIp,
				hostPort,
				BUFFER,
				dataProcessor);
	}

	@Override
	public SocketChannelProcessor getProcessor() {
		return null;
	}
}
