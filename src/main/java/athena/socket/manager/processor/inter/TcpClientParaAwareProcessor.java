package athena.socket.manager.processor.inter;

import athena.socket.core.MessageProcessor;

public interface TcpClientParaAwareProcessor extends MessageProcessor {

	static String SEND_IP = "SEND_IP";
	static String SEND_PORT = "SEND_PORT";
	static String RETRY_COUNT = "RETRY_COUNT";
	static String MESSAGE_INFO = "MESSAGE_INFO";
	static String LONG_TCP_CLIENT_NAME = "LONG_TCP_CLIENT";
	static String SHORT_TCP_CLIENT_NAME = "SHORT_TCP_CLIENT";

	void setThreadCount(int threadCount);
	void setConnectionLinger(int connectionLinger);
	void setConnectionSize(int connectionSize);
	void setPoolName(String poolName);
	void setConnectionTimeout(int connectionTimeout);
	void setAddress(String address);
	void setPort(int port);
	void setBufferSize(int bufferSize);
	void setTimeAfterSend(int timeAfterSend);
	void setMessageProcessor(MessageProcessor messageProcessor);
	void setProcessReadException(boolean processReadException);
}
