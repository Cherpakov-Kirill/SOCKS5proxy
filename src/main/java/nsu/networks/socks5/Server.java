package nsu.networks.socks5;

import nsu.networks.socks5.channel.ChannelListener;
import nsu.networks.socks5.channel.SOCKSChannel;
import org.xbill.DNS.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;

public class Server extends Thread implements ChannelListener {

    private final int port;
    private final Selector selector;

    public Server(int port) throws IOException {
        this.port = port;
        this.selector = Selector.open();
    }

    @Override
    public void run() {
        try {
            ServerSocketChannel server = ServerSocketChannel.open();
            server.bind(new InetSocketAddress("localhost", port));
            server.configureBlocking(false);
            server.register(selector, SelectionKey.OP_ACCEPT);
            System.out.println("SOCKS-proxy server started on " + port + " port");
        } catch (IOException e) {
            e.printStackTrace();
        }
        while (true) {
            try {
                int count = selector.select();
                if (count == 0) {
                    continue;
                } //else System.out.println("Selected " + count + " keys");
            } catch (IOException e) {
                e.printStackTrace();
            }
            Set<SelectionKey> keys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = keys.iterator();
            while (iterator.hasNext()) {
                SelectionKey selectionKey = iterator.next();
                iterator.remove();
                try {
                    if (selectionKey.isAcceptable()) {
                        System.out.println("Server: _Accept_");
                        SOCKSChannel socksChannel = new SOCKSChannel(this, selectionKey);
                        socksChannel.accept();
                    } else if (selectionKey.isConnectable()) {
                        System.out.println("Server: _Connect_");
                        ((SOCKSChannel) selectionKey.attachment()).connect();
                    } else if (selectionKey.isReadable()) {
                        System.out.println("Server: _Read_");
                        ((SOCKSChannel) selectionKey.attachment()).read();
                    } else if (selectionKey.isWritable()) {
                        System.out.println("Server: _Write_");
                        ((SOCKSChannel) selectionKey.attachment()).write();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    if(selectionKey.attachment()!= null){
                        try {
                            ((SOCKSChannel) selectionKey.attachment()).close();
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }
        }

    }

    @Override
    public void createDnsConnection(String hostname, int port, SOCKSChannel pairedChannel) {
        try {
            DatagramChannel dnsChannel = DatagramChannel.open();
            dnsChannel.connect(ResolverConfig.getCurrentConfig().server());
            dnsChannel.configureBlocking(false);
            SelectionKey key = dnsChannel.register(selector, SelectionKey.OP_WRITE);
            SOCKSChannel socksChannel = new SOCKSChannel(this, "'DNS for " + hostname + "'", key, pairedChannel, port, nsu.networks.socks5.channel.State.DNS_WRITE);
            socksChannel.setDNSRequest(hostname);
            key.attach(socksChannel);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public SOCKSChannel createServerConnection(InetAddress ip, int port, SOCKSChannel pairedChannel) {
        try {
            SocketChannel coupleChannel = SocketChannel.open();
            coupleChannel.configureBlocking(false);
            coupleChannel.connect(new InetSocketAddress(ip, port));
            System.out.println("Connecting to server...");
            SelectionKey key = coupleChannel.register(selector, SelectionKey.OP_CONNECT);
            SOCKSChannel socksChannel = new SOCKSChannel(this, "'Server " + ip.getHostAddress() + "'", key, pairedChannel, port, nsu.networks.socks5.channel.State.TUNNEL);
            key.attach(socksChannel);
            return socksChannel;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
