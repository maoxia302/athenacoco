package athena.socket.manager;

import athena.socket.control.BasicSocketChannelClient;
import athena.socket.control.SocketChannelManager;
import athena.socket.core.MessageContext;
import athena.socket.core.MessageProcessor;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class SocketChannelClient extends BasicSocketChannelClient {

    private MessageProcessor processor;
    private boolean valid;
    private static final String BASIC_COMMON_RESP_CONFIRM = "A8FFACEE";

    public SocketChannelClient(
            SocketChannel channel,
            SocketChannelManager manager,
            int bufSize,
            boolean remote,
            MessageProcessor processor) {
        super(channel, manager, bufSize, remote);
        this.processor = processor;
    }

    SocketChannelClient(
            SocketChannel channel,
            SocketChannelManager manager,
            int bufSize,
            InetSocketAddress socketAddress,
            MessageProcessor processor) {
        super(channel, manager, bufSize, socketAddress);
        this.processor = processor;
    }

    @Override
    protected void doRead(ByteBuffer buf) {
        if (processor == null) return;
        try {
            String respSign = processor.process(new MessageContext(getAddress()).makeDefault(buf));
            System.out.println("AUTO_RESP_SIGN::" + respSign);
            if (respSign.startsWith(BASIC_COMMON_RESP_CONFIRM)) { //own signature (for quick feed)
                this.write(BASIC_COMMON_RESP_CONFIRM.getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    @Override
    protected boolean doCanAccept() {
        if (processor == null) return false;
        try {
            MessageContext context = MessageContext.makeInitContext(getAddress());
            context.setContent(MessageProcessor.DEFAULT_ACCEPTANCE_MESSAGE); //to skip responding
            processor.process(context);
            return processor != null;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    protected void doClose() {
        valid = false;
        if (processor == null) return;
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
        if (!valid) {
            valid = true;
        }
        if (isRemote()) {
            if (processor == null) return;
            MessageContext context = new MessageContext(getAddress());
            context.setContent(MessageProcessor.SERVICE_INIT_MESSAGE);
            try {
                processor.process(context);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}