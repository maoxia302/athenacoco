package athena.socket.manager.processor;

import athena.socket.control.BasicSocketChannelManager;
import athena.socket.core.MessageContext;
import athena.socket.core.MessageProcessor;
import athena.socket.manager.PermanentSocketServerManager;
import athena.socket.manager.SocketServerManager;
import athena.socket.manager.processor.inter.TcpServerParaAwareProcessor;

public class TcpServerProcessor extends SocketChannelProcessor implements TcpServerParaAwareProcessor {

	private int port;
	private int portSize;
	private int threadCount;
	private int connectionLinger;
	private int connectionTimeout;
	private int bufferSize;
	private boolean remote;
	private MessageProcessor messageProcessor;
	private String poolName;
	private boolean multi;
	private boolean selector = true;

	@Override
	protected BasicSocketChannelManager getManager() {
		if (selector) {
			return new SocketServerManager(
				port, 
				portSize, 
				threadCount,
				connectionLinger,
				connectionTimeout,
				poolName, 
				multi,
				bufferSize,
				remote,
				messageProcessor);
		} else {
			return new PermanentSocketServerManager(
				port,
				portSize, 
				connectionLinger,
				connectionTimeout, 
				bufferSize, 
				messageProcessor);
		}
	}
	
	public void setBufferSize(int bufferSize) {
		this.bufferSize=bufferSize;
		
	}
	public void setConnectionLinger(int connectionLinger) {
		this.connectionLinger=connectionLinger;
		
	}
	public void setConnectionTimeout(int connectionTimeout) {
		this.connectionTimeout=connectionTimeout;
		
	}
	public void setMessageProcessor(MessageProcessor messageProcessor) {
		this.messageProcessor=messageProcessor;
		
	}
	public void setMulti(boolean multi) {
		this.multi=multi;
		
	}
	public void setPoolName(String poolName) {
		this.poolName=poolName;
		
	}
	public void setPort(int port) {
		this.port=port;
		
	}
	public void setPortSize(int portSize) {
		this.portSize=portSize;
		
	}
	public void setRemote(boolean remote) {
		this.remote=remote;
		
	}
	public void setThreadCount(int threadCount) {
		this.threadCount=threadCount;
		
	}

	public void setSelector(boolean selector){
		this.selector = selector;
	}

	@Override
	public String process(MessageContext context) {
		return null;
	}
}
