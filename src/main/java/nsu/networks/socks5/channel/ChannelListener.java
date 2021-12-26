package nsu.networks.socks5.channel;

import java.net.InetAddress;

public interface ChannelListener {
    void createDnsConnection(String hostname, int port, SOCKSChannel pairedChannel);

    SOCKSChannel createServerConnection(InetAddress ip, int port, SOCKSChannel pairedChannel);
}
