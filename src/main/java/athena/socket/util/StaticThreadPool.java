package athena.socket.util;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class StaticThreadPool implements ThreadPool {

    private int maxThreadCount;
    private int minThreadCount;
    private int currentThreadCount;
    private Class workerClass;
    private final Object instance;
    private final BlockingQueue<Object> performWorkDataPool;
    private boolean poolRunning = true;
    private final ClassLoader contextClassLoader;
    private final String poolName;
    private final ArrayList<WorkerThread> threadList = new ArrayList<>();
    static ArrayList<StaticThreadPool> threadPoolList = new ArrayList<>();
    static boolean printTraceThread;
    private static Thread dumpThread = null;

    public StaticThreadPool(int threadCount, Class workerClass, Object instance) {
        this(workerClass.getName(), threadCount, workerClass, instance);
    }

    public StaticThreadPool(String poolName, int threadCount, Class workerClass, Object instance) {
        this.maxThreadCount = threadCount;
        this.minThreadCount = threadCount;
        this.performWorkDataPool = new LinkedBlockingQueue<>();
        this.contextClassLoader = Thread.currentThread().getContextClassLoader();
        this.workerClass = workerClass;
        this.instance = instance;
        this.poolName = poolName;
        synchronized (this.threadList) {
            threadPoolList.add(this);
        }
    }

    private void createWorker(int index) throws Exception {
        PoolWorker worker = (PoolWorker) this.workerClass.newInstance();
        worker.setInstance(this.instance);
        WorkerThread workThread = new WorkerThread(this, index, worker);
        workThread.setContextClassLoader(this.contextClassLoader);
        synchronized (this.threadList) {
            this.threadList.add(workThread);
        }
        this.currentThreadCount = (index + 1);
        System.out.println("PoolWorkerThread:::" + (index + 1));
        workThread.start();
    }

    public int performWork(Object data) {
        this.performWorkDataPool.add(data);
        int poolSize = this.performWorkDataPool.size();
        this.currentThreadCount = poolSize - 1;
        if (this.currentThreadCount < this.maxThreadCount) {
            try {
                System.out.println("PoolWorkerThreadObject:::" + data.toString());
                createWorker(this.currentThreadCount);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return poolSize;
    }

    public void close() {
        this.poolRunning = false;

        synchronized (this.threadList) {
            this.threadList.remove(this);
        }
        this.performWorkDataPool.clear();
    }

    public int getPerformWorkDataCount() {
        return this.performWorkDataPool.size();
    }

    public LinkedList getPerformWorkDataPool() {
        return new LinkedList() {
            public int size() {
                return StaticThreadPool.this.performWorkDataPool.size();
            }
        };
    }

    public Object getJob() {
        Object data = null;
        try {
            data = this.performWorkDataPool.poll(30L, TimeUnit.SECONDS);
        } catch (Exception ignored) { }
        return data;
    }

    public boolean isPoolRunning() {
        return this.poolRunning;
    }

    public int getMaxThreadCount() {
        return this.maxThreadCount;
    }

    public void setMaxThreadCount(int maxThreadCount) {
        if (this.currentThreadCount > maxThreadCount) {
            this.currentThreadCount = (maxThreadCount + 1);
        }
        this.maxThreadCount = maxThreadCount;
    }

    public int getMinThreadCount() {
        return this.minThreadCount;
    }

    public void setMinThreadCount(int minThreadCount) {
        this.minThreadCount = minThreadCount;
    }

    public int getCurrentThreadCount() {
        return this.currentThreadCount;
    }

    public Class getWorkerClass() {
        return this.workerClass;
    }

    public void setWorkerClass(Class workerClass) {
        this.workerClass = workerClass;
    }

    public String getPoolName() {
        return this.poolName;
    }

    public boolean isWorked(WorkerThread thread) {
        int index = thread.getIndex();
        boolean worked = (this.poolRunning) && (index < this.maxThreadCount);
        if (!worked) {
            synchronized (this.threadList) {
                this.threadList.remove(thread);
            }
        }
        return worked;
    }

    public int getIdleCount() {
        int idleCount = 0;
        for (WorkerThread w : this.threadList) {
            if (!w.isActive()) {
                idleCount++;
            }
        }
        return idleCount;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(1024);
        sb.append("{pool:\"");
        sb.append(this.poolName);
        sb.append("\", threadCount:");
        sb.append(this.threadList.size());
        sb.append(", poolSize:");
        sb.append(this.performWorkDataPool.size());
        sb.append(", activeThreads:[");
        for (WorkerThread w : this.threadList) {
            if (w.isActive()) {
                sb.append(w.getIndex());
                sb.append(" ,");
            }
        }

        sb.append("]}");
        return sb.toString();
    }

    static String dumpPoolStatus() {
        StringBuilder sb = new StringBuilder(4096);
        sb.append("PoolStat:{\ntime:\"");
        sb.append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()));
        sb.append("\"\n[\n");

        for (StaticThreadPool p : threadPoolList) {
            sb.append(p.toString());
            sb.append(",\n");
        }
        sb.append("]}\n");
        return sb.toString();
    }

    public static boolean isPrintTraceThread() {
        return printTraceThread;
    }

    public static void setPrintTraceThread(boolean printTraceThread) {
        printTraceThread = printTraceThread;
        if ((printTraceThread) && (dumpThread == null)) {
            dumpThread = new Thread() {
                public void run() {
                    setName("StaticThreadPool check dump");
                    while (true)
                        try {
                            Thread.sleep(60000L);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                }
            };
            dumpThread.start();
        }
    }
}
