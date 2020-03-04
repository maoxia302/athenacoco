package athena.socket.control;

import athena.socket.util.PoolWorker;

import java.nio.channels.SelectionKey;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SocketProcessWorker implements PoolWorker {

    private static final long STARTER_GAP = 1000L;
    private static final long READ_WRITE_TIME_GAP = 40L;

    private ExecutorService readWritePool = Executors.newFixedThreadPool(65535 * 2); //max connection 65535

    private BasicSocketChannelManager manager;

	public void setInstance(Object instance) {
		manager = (BasicSocketChannelManager) instance;
	}

	public void run(Object data) {
	    // short circuit connection mode with SelectionKey class.
        // to use this, change the calling object type and client handling methods.
		if (data instanceof SelectionKey) {
			SelectionKey selKey = (SelectionKey) data;
			BasicSocketChannelClient client = (BasicSocketChannelClient) selKey.attachment();
			if (client.isWritable()) {
				try {
				    System.out.println("socket selector write:::" + selKey.toString());
					client.write(manager);
				} catch (Exception e) {
				    e.printStackTrace();
					client.close();
					return;
				}
			}
			if (client.isReadable()) {
				try {
					int readSize = client.read(manager);
                    System.out.println("socket selector read:::" + selKey.toString() + "  " + readSize);
                    if (readSize == -1) {
						client.close();
					}
				} catch (Exception ex) {
					ex.printStackTrace();
					client.close();
				}
			}
		} else if (data instanceof BasicSocketChannelClient) {
		    // long term socket connection mode with BasicSocketChannelClient.
            // preference. but not resource conservative.
		    System.out.println("socket connection basic::" + data.toString());
			BasicSocketChannelClient client = (BasicSocketChannelClient) data;
			manager.initiateConnection(client);
			try {
			    Thread writerProcessor = new Thread(new WriterProcessor(client));
			    this.readWritePool.submit(writerProcessor);
                Thread.sleep(STARTER_GAP);
                Thread readerProcessor = new Thread(new ReaderProcessor(client));
                this.readWritePool.submit(readerProcessor);
            } catch (Exception ex) {
			    ex.printStackTrace();
            }
		}
	}

	private class WriterProcessor implements Runnable {

	    private BasicSocketChannelClient client;

	    WriterProcessor(BasicSocketChannelClient client) {
	        this.client = client;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    client.write(manager);
                    Thread.sleep(READ_WRITE_TIME_GAP);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    client.close();
                    return;
                }
            }
        }
    }

    private class ReaderProcessor implements Runnable {

	    private BasicSocketChannelClient client;

	    ReaderProcessor(BasicSocketChannelClient client) {
	        this.client = client;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    if (client.read(manager) == -1) {
                        Thread.sleep(READ_WRITE_TIME_GAP);
                        continue;
                    }
                    Thread.sleep(READ_WRITE_TIME_GAP);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    client.close();
                    return;
                }
            }
        }
    }

}
