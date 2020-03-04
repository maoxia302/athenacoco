package athena.socket.util;

class WorkerThread extends Thread {

	private PoolWorker worker;
	private ThreadPool pool;
	private int index;
	private boolean active;

	WorkerThread(ThreadPool pool, int index, PoolWorker worker) {
		super(pool.getPoolName() + " Worker#" + index);
		this.worker = worker;
		this.pool = pool;
		this.index = index;
	}

	public void run() {
		while (this.pool.isWorked(this)) {
			runWorker(this.pool.getJob());
		}
	}

	private void runWorker(Object processData) {
		try {
			this.active = true;
			if (processData != null) {
				this.worker.run(processData);
			}
		} catch (Throwable e) {
			e.printStackTrace();
		} finally {
			this.active = false;
		}
	}

	public boolean isActive() {
		return this.active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public int getIndex() {
		return this.index;
	}

	public void setIndex(int index) {
		this.index = index;
	}
}
