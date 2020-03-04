package athena.socket.control;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public abstract class BasicDatagramSocket implements Runnable {

    private DatagramSocket socket;
    private int port;
    private int portSize;
    private boolean isActive;
    private byte[] buf;

    public BasicDatagramSocket(int port,int portSize,int bufSize) {
        this.port = port;
        this.portSize = portSize;
        this.buf = new byte[bufSize];
    }

    protected abstract boolean doCanAccept(String address) throws IOException;

    protected abstract void doRead(String address, byte[] buf) throws IOException;

    public synchronized boolean send(String address, byte[] buf) throws IOException {
        String[] s = address.split(":");
        DatagramPacket packet;
        switch(s.length) {
            case 1 :
                packet = new DatagramPacket(buf,buf.length,
                InetAddress.getLocalHost(),Integer.parseInt(s[0]));
                socket.send(packet);
                return true;
            case 2 :
                packet = new DatagramPacket(buf,buf.length,
                InetAddress.getByName(s[0]),Integer.parseInt(s[1]));
                socket.send(packet);
                return true;
            default :
                return false;
        }
    }

    public void start() {
        try {
            if (port == 0) {
                socket = new DatagramSocket();
            } else {
                boolean start = false;
                for (int i = 0; i < portSize; i++) {
                    try {
                        socket = new DatagramSocket(port + i);
                        start = true;
                        break;
                    } catch (Exception ignored) { }
                }
                if (!start) {
                    throw new RuntimeException("cannot open ports: " + port + " to " + (port + portSize - 1));
                }
                isActive = true;
            }
        } catch (Exception e) {
            isActive = false;
            e.printStackTrace();
        }
    }

    public void stop() {
        if (isActive) {
            isActive = false;
            socket.close();
        }
    }

    public void run() {
        while (true) {
            if (!isActive) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            } else {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                try {
                    socket.receive(packet);
                    String address = packet.getAddress().getHostAddress() + ":" + packet.getPort();
                    if (doCanAccept(address)) doRead(address, packet.getData());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
