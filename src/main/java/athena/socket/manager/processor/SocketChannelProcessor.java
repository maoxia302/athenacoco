package athena.socket.manager.processor;

import athena.socket.control.BasicSocketChannelManager;

public abstract class SocketChannelProcessor {

	private BasicSocketChannelManager manager;

	protected abstract BasicSocketChannelManager getManager();
	
	public void start() {
		manager = getManager();
		System.out.println("ATHENA-SOCKET-MANAGER-INITIATION:::: [" + manager.getClass().getName() + "]");
		manager.start();
	}

	public void stop() {
		if (manager != null) {
			manager.stop();
		}
	}
}