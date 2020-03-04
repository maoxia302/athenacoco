package athena.socket.core;

public interface MessageProcessor {

    int DEFAULT_GATE_ID = 9; //for no track back
    String DEFAULT_ORIGIN = "<MASTER>";
	String SERVICE_INIT_MESSAGE = "<INIT>";
	String DEFAULT_ACCEPTANCE_MESSAGE = "<OK-ACCEPTED>";
	String SERVICE_NAME = "AthenaGateServe";
	String DEFAULT_PROTOCOL_NAME = "private-compose";
    String BASIC_COMMON_RESP_CONFIRM = "A8FFACEE";
	String process(MessageContext context) throws Exception;
}