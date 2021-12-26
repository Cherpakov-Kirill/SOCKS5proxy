package nsu.networks.socks5.channel;

public enum State {
    TUNNEL,     //Tunnel-connection client-server
    CONN_READ,  //Server have read canonical-message
    DNS_READ,   //Server have read dnsResolving-message
    DNS_WRITE,
    AUTH_READ,  //Server have read authentication-message
    AUTH_WRITE
}
