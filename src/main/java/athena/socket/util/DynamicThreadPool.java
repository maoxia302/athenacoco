package athena.socket.util;

import java.util.WeakHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class DynamicThreadPool implements ThreadPool {
	
  	private final int maxThreadCount;
  	private final Class workerClass;
  	private final Object instance;
  	private boolean poolRunning = true;
  	private final String poolName;
  	private final LinkedBlockingQueue performWorkDataPool;
	private final ClassLoader contextClassLoader;
  	private final WeakHashMap threadList = new WeakHashMap();

  	public DynamicThreadPool(int max, Class workerClass, Object instance) throws Exception {
		this(workerClass.getName(), max, workerClass, instance);
  	}

  	private void createWorker(int index) throws Exception {
		PoolWorker worker = (PoolWorker) this.workerClass.newInstance();
 		worker.setInstance(this.instance);
		WorkerThread wThread = new WorkerThread(this, index, worker);
		wThread.setContextClassLoader(this.contextClassLoader);
		synchronized (this.threadList) {
      		this.threadList.put(wThread.getName(), wThread);
    	}
    	wThread.start();
  	}

  	public DynamicThreadPool(String pooName, int max, Class workerClass, Object instance) throws Exception {
     	this.maxThreadCount = max;
     	this.performWorkDataPool = new LinkedBlockingQueue();
     	this.contextClassLoader = Thread.currentThread().getContextClassLoader();
     	this.workerClass = workerClass;
     	this.instance = instance;
     	this.poolName = pooName;
     	for (int i = 0; i < max; i++) {
       		createWorker(i);
     	}
  	}

  	public int performWork(Object data) {
     	WorkerThread thread = null;
     	return -1;
  	}

  	public void close() {
     	this.poolRunning = false;
  	}

  	public boolean isPoolRunning() {
     	return this.poolRunning;
  	}

  	public String getPoolName() {
     	return this.poolName;
  	}

  	public Object getJob() {
     	Object data = null;
    	try {
       		data = this.performWorkDataPool.poll(1000L, TimeUnit.MILLISECONDS);
    	} catch (Exception e) {
       		e.printStackTrace();
    	}
     	return data;
  	}

  	public boolean isWorked(WorkerThread thread) {
     	int index = thread.getIndex();
     	return (this.poolRunning) && (index < this.maxThreadCount);
  	}
}
