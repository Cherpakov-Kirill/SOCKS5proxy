package nsu.networks.socks5.channel;

public class RequestUtils {
    public static boolean sizeChecker(Buffers buffers, int normalSize) {
        boolean result = buffers.getInputSize() >= normalSize;
        if(result){
            return true;
        }else {
            System.err.println("Client send short message");
            return false;
        }
    }

    public static boolean versionChecker(Buffers buffers) {
        boolean result = buffers.getInputByte(0) == SOCKSValues.SOCKS_VERSION;
        if(result){
            return true;
        }else {
            System.err.println("Client needs other version of the SOCKS (supported 5 ver.)");
            return false;
        }
    }

    public static boolean noAuthChecker(Buffers buffers) {
        int methodNumber = buffers.getInputByte(1);
        if (methodNumber == 0) return false;
        for (int i = 0; i < methodNumber; i++) {
            if (buffers.getInputByte(i + 2) == SOCKSValues.NO_AUTH) {
                return true;
            }
        }
        System.err.println("Client have not got noAuth authentication method");
        return false;
    }

    public static boolean commandChecker(Buffers buffers){
        boolean result = buffers.getInputByte(1) == SOCKSValues.ESTABLISH;
        if(result){
            return true;
        }else {
            System.err.println("Client sent not supported command (supported 'establish a TCP/IP stream connection')");
            return false;
        }
    }

    public static boolean isIPV4Address(Buffers buffers){
        return buffers.getInputByte(3) == SOCKSValues.IPV4;
    }

    public static boolean isDomainAddress(Buffers buffers){
        return buffers.getInputByte(3) == SOCKSValues.DOMAIN;
    }

    public static boolean addressChecker(Buffers buffers){
        boolean isIPv4 = isIPV4Address(buffers);
        boolean isHostName = isDomainAddress(buffers);
        if(isIPv4 || isHostName){
            return true;
        }else {
            System.err.println("Client want's IPV6 resource (supported IPv4 and domain name)");
            return false;
        }
    }
}
