package athena.socket.manager.processor.inter;

import athena.socket.core.MessageProcessor;

public interface TcpServerParaAwareProcessor extends MessageProcessor {

	String TCP_SERVER_NAME = "TCP_SERVER";
	void setPort(int port);
	void setPortSize(int portSize);
	void setThreadCount(int threadCount);
	void setConnectionLinger(int connectionLinger);
	void setConnectionTimeout(int connectionTimeout);
	void setBufferSize(int bufferSize);
	void setRemote(boolean remote);
	void setMessageProcessor(MessageProcessor messageProcessor);
	void setPoolName(String poolName);
	void setMulti(boolean multi);
	void setSelector(boolean selector);

}
