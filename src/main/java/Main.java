import nsu.networks.socks5.Server;

import java.io.IOException;

public class Main {
    public static void main( String[] args ){
        if(args.length < 1){
            System.err.println("Add port to arguments");
            System.exit(-1);
        }
        int port = Integer.parseInt(args[0]);
        if(port<1024 || port > 65535){
            System.err.println("Type port from interval [1024;65535]");
            System.exit(-2);
        }
        try {
            Server server = new Server(port);
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
