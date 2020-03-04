package athena.socket.util;

import java.util.concurrent.atomic.AtomicInteger;

public class DebugWatch extends Thread {
	
	private AtomicInteger count = new AtomicInteger(0);
	long interval;
	String message;

	public DebugWatch(String message, long interval) {
		this.interval = interval;
		this.message = message;
		start();
	}

	public int add(int inc) {
		return this.count.addAndGet(inc);
	}

	public int setCount(int nCount) {
		return this.count.getAndSet(nCount);
	}

	public void run() {
		int lastPrint = 0;
		while (true) {
			if (lastPrint != this.count.get()) {
				lastPrint = this.count.get();
			}
			try {
				Thread.sleep(this.interval);
			} catch (Exception ignored) { }
		}
	}
}
