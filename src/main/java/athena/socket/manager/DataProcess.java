package athena.socket.manager;

import athena.socket.control.BasicSocketChannelManager;
import athena.socket.core.MessageContext;
import athena.socket.core.MessageProcessor;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class DataProcess implements MessageProcessor {

    private static final long SENDER_GAP = 50L;

    private static final BlockingQueue<MessageContext> RECEIVER = new LinkedBlockingQueue<>(1024 * 1024);
    private static final BlockingQueue<MessageContext> SENDER = new LinkedBlockingQueue<>(1024 * 1024);
    private BasicSocketChannelManager socket = null;

    public void setSocketSender(BasicSocketChannelManager socket) {
        this.socket = socket;
        new Thread(new Send()).start();
    }

    public BasicSocketChannelManager getSocket() {
        return this.socket;
    }

    /**
     * loop this for incoming or you own handlers in "public String process(MessageContext context)"
     */
    public synchronized MessageContext receive() {
        if (RECEIVER.peek() != null) {
            return RECEIVER.poll();
        }
        return null;
    }

    public static synchronized void send(MessageContext messageContext) {
        if (messageContext != null) {
            SENDER.offer(messageContext);
        }
    }

    private class Send implements Runnable {

        @Override
        public void run() {
            while (true) {
                try {
                    MessageContext mc = SENDER.take();
                    if (mc.getContent() instanceof String) {
                        socket.send(mc.getAddress(), mc.getContent().toString().getBytes(StandardCharsets.UTF_8));
                    } else if (mc.getContent() instanceof byte[]) {
                        socket.send(mc.getAddress(), (byte[]) mc.getContent());
                    } else {
                        System.out.println("communication data unacceptable [DataProcess.Send]");
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    try {
                        Thread.sleep(SENDER_GAP);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private boolean messageFilter(String cypher) {
        return !cypher.startsWith(MessageProcessor.SERVICE_INIT_MESSAGE) &&
                !cypher.startsWith(MessageProcessor.DEFAULT_ACCEPTANCE_MESSAGE);
    }

	@Override
	public String process(MessageContext context) {
        try {
            String cypher = context.getContent().toString();
            if (messageFilter(cypher)) {
                RECEIVER.offer(context);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        if (context.getControlCode() == 10) {
            return String.join("-",
                    new String[] {MessageProcessor.BASIC_COMMON_RESP_CONFIRM,
                            String.valueOf(System.currentTimeMillis())});
        }
		return "COMMON_TIME-" + System.currentTimeMillis();
	}
}
