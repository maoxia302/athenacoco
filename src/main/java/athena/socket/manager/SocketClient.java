package athena.socket.manager;

import athena.socket.control.BasicSocketClient;
import athena.socket.control.SocketChannelManager;
import athena.socket.core.MessageContext;
import athena.socket.core.MessageProcessor;

import java.net.Socket;
import java.nio.ByteBuffer;

public class SocketClient extends BasicSocketClient {

	private MessageProcessor processor;
	
	SocketClient(Socket socket, SocketChannelManager manager, int bufSize, MessageProcessor processor) {
		super(socket, manager, bufSize);
		this.processor = processor;
	}

	@Override
	protected void doRead(ByteBuffer buf) {
		if(processor == null) return;
		MessageContext context = new MessageContext(getAddress());
		context.setContent(buf);
		try {
			processor.process(context);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	protected boolean doCanAccept() {
		if(processor == null) return true;
		MessageContext context = new MessageContext(getAddress());
		context.setContent(false);
		try {
			processor.process(context);
    		return (Boolean) context.getContent();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	protected void doClose() {
		if(processor == null) return;
		MessageContext context = new MessageContext(getAddress());
		context.setContent(true);
		try {
			processor.process(context);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void doOpen() {

	}
}