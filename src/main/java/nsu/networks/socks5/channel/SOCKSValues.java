package nsu.networks.socks5.channel;

public class SOCKSValues {
    //authentication
    public static final byte NO_AUTH = 0x00;
    //command
    public static final byte ESTABLISH = 0x01;
    //version of SOCKS protocol
    public static final byte SOCKS_VERSION = 0x05;
    //Address type of resource
    public static final byte IPV4 = 0x01;
    public static final byte DOMAIN = 0x03;
    //Request provided
    public static final byte STATUS_GRANTED = 0x00;
}
