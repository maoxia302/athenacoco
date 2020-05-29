package athena.socket.core;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class MessageContext implements Serializable {

	private static final long serialVersionUID = -2362828799318959815L;

	private transient Object origin;  //exchange data. normally not used.
	private Object content;
	private String protocolName; //partyName
	private int gateConnectId;  //track client (or server) default 9 as for no need to track.
	private String gateService; //keep track on the gateway if there is any.
	private int	priority;   //to use: 1 - 4. 4 being the highest and should be handled at higher level.
	private String address; //socket address to get endpoint and communicate.
	private int controlCode;  // if controlCode is 10, athena will forge a default response.
	
	private Map<String, Object>	props;

	public MessageContext() {
		props = new HashMap<>();
		this.priority = 1;
	}

	public MessageContext(String address) {
		this();
		this.address = address;
	}

	public MessageContext(MessageContext context) {
		this(context, false);
	}

	public MessageContext(MessageContext context, boolean original) {
		props = new HashMap<>();
		clone(context);
		if (original) this.origin = context.content;
	}
	
	public int getControlCode() {
		return controlCode;
	}

	public void setControlCode(int controlCode) {
		this.controlCode = controlCode;
	}

	public int getGateConnectId(){
		return this.gateConnectId;
	}
	
	public void setGateConnectId(int gateConnectId){
		this.gateConnectId = gateConnectId;
	}
	
	public String getGateService(){
		return this.gateService;
	}
	
	public void setGateService(String gateService){
		this.gateService = gateService;
	}
	
	public String getProtocolName(){
		return this.protocolName;
	}
	
	public void setProtocolName(String protocolName){
		this.protocolName = protocolName;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}

	public Object getOrigin() {
		return origin;
	}

	public void setOrigin(Object origin) {
		this.origin = origin;
	}

	public Object getContent() {
		return content;
	}

	public void setContent(Object content) {
		this.content = content;
	}

	public Object getProperty(String key) {
		if (props == null) return null;
		return props.get(key);
	}

	public void putProperty(String key, Object prop) {
		if (props == null){
			props = new HashMap<>();
		}
		props.put(key, prop);
	}

	public void removeProperty(String key) {
		if (props == null) return;
		props.remove(key);
	}

	private void clone(Object object) {
		if (object instanceof MessageContext) {
			MessageContext context = (MessageContext) object;
			this.content = context.content;
			this.priority = context.priority;
			this.address = context.address;
			this.protocolName = context.protocolName;
			this.gateConnectId = context.gateConnectId;
			this.gateService = context.gateService;
			this.controlCode=context.controlCode;
			if (context.props != null){
				if (this.props != null){
					this.props.clear();
				}
				this.props = new HashMap<>();
				this.props.putAll(context.props);
			}
		}
	}

	public MessageContext makeDefault(ByteBuffer buf) {
        this.setGateConnectId(MessageProcessor.DEFAULT_GATE_ID);
        this.setControlCode(0);
        this.setGateService(MessageProcessor.SERVICE_NAME);
        this.setPriority(1);
        this.setProtocolName(MessageProcessor.DEFAULT_PROTOCOL_NAME);
        this.setOrigin(MessageProcessor.DEFAULT_ORIGIN);
        String content = new String(buf.array(), StandardCharsets.UTF_8);
        System.out.println("#=incoming content::=>" + content);
        this.setContent(content);
        return this;
	}

    public static MessageContext makeInitContext(String remoteAddress) {
        MessageContext context = new MessageContext(remoteAddress);
        context.setOrigin(MessageProcessor.DEFAULT_ORIGIN);
        context.setContent(MessageProcessor.SERVICE_INIT_MESSAGE);
        context.setProtocolName(MessageProcessor.DEFAULT_PROTOCOL_NAME);
        context.setPriority(0);
        context.setGateService(MessageProcessor.SERVICE_NAME);
        context.setControlCode(0);
        context.setGateConnectId(MessageProcessor.DEFAULT_GATE_ID);
        return context;
    }
	
	public String toStringPlay() {
		String originClaName = "";
		if(origin != null) {
			originClaName = origin.getClass().getName();
		}
		return "MessageContext [" +
				"origin=" + originClaName + ", " +
				"content=" + content + ", " +
				"protocolName=" + protocolName + ", " +
				"gateConnectId=" + gateConnectId + ", " +
				"gateService=" + gateService + ", " +
				"priority=" + priority + ", " +
				"address=" + address + ", " +
				"controlCode=" + controlCode + ", " +
				"props=" + props + "]";
	}
	
}
