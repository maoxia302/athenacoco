package athena.socket.core;

import athena.socket.manager.processor.SocketChannelProcessor;

public interface ProcessorFactory {

	SocketChannelProcessor getProcessor();
}
