package athena.socket.util;

public interface ThreadPool {
  	
  	int performWork(Object paramObject);

  	void close();

  	boolean isWorked(WorkerThread paramWorkerThread);

  	String getPoolName();

  	Object getJob();
}
