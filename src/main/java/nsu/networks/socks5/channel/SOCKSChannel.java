package nsu.networks.socks5.channel;

import org.xbill.DNS.*;
import org.xbill.DNS.Record;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.channels.*;
import java.util.Optional;

import static nsu.networks.socks5.channel.State.*;

public class SOCKSChannel {
    private final ChannelListener listener;
    private SelectionKey channelKey;
    private SelectableChannel channel;
    private Buffers buffers;
    private State state;
    private final String name;

    private SOCKSChannel pairedChannel;

    private int portOfDomainNameServer;


    public SOCKSChannel(ChannelListener listener, SelectionKey channelKey) {
        this.listener = listener;
        this.channelKey = channelKey;
        this.channel = channelKey.channel();
        this.name = "'Client'";
    }

    public SOCKSChannel(ChannelListener listener, String name, SelectionKey channelKey, SOCKSChannel pairedChannel, int port, State state) {
        this.listener = listener;
        this.channelKey = channelKey;
        this.pairedChannel = pairedChannel;
        this.channel = channelKey.channel();
        this.buffers = new Buffers();
        this.state = state;
        this.portOfDomainNameServer = port;
        this.name = name;
        System.out.println("\tCreating new obj. SOCKSChannel. State = " + state + "; Name = " + name);
    }

    public void setPairedChannel(SOCKSChannel pairedChannel) {
        this.pairedChannel = pairedChannel;
    }

    public void accept() throws IOException {
        System.out.println("\tAccepting new connection");
        SocketChannel socketChannel = ((ServerSocketChannel) channel).accept();
        socketChannel.configureBlocking(false);
        this.channel = socketChannel;
        this.channelKey = socketChannel.register(channelKey.selector(), SelectionKey.OP_READ);
        channelKey.attach(this);
        this.buffers = new Buffers();
        this.state = AUTH_READ;
    }

    public void connect() throws IOException {
        ((SocketChannel) channel).finishConnect();
        System.out.println("\tConnection to server established!");
        buffers.attachOutBuffer(pairedChannel.buffers.getInput());
        //I fill input in server's Buffer
        //because I set logic-tunnel between server and client.
        //Server's input => client's output
        //Client's input => server's output
        //This response will send to client.
        buffers.fillInput(new byte[]{
                SOCKSValues.SOCKS_VERSION,
                SOCKSValues.STATUS_GRANTED,
                0x00,
                SOCKSValues.IPV4,
                0x00, 0x00, 0x00, 0x00,
                0x00, 0x00,
        });
        pairedChannel.buffers.attachOutBuffer(buffers.getInput());
        pairedChannel.channelKey.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
        channelKey.interestOps(0);
    }

    public void read() throws IOException {
        buffers.clearInput();
        int size = ((ByteChannel) channel).read(buffers.getInput());
        System.out.println("\tState = " + state + "; Name = " + name + "; Read " + size + " bytes.");
        //buffers.printInput();
        if (size <= 0) {
            close();
            return;
        }
        switch (state) {
            case AUTH_READ -> authReading();
            case CONN_READ -> canonicalReading();
            case DNS_READ -> dnsReading();
            default -> prepareWriteForPair();
        }
    }

    private void authReading() throws IOException {
        if (RequestUtils.sizeChecker(buffers, 2) && RequestUtils.versionChecker(buffers) && RequestUtils.noAuthChecker(buffers)) {
            System.out.println("\tAuthReading success!" + " Name = " + name);
            buffers.fillOutput(new byte[]{SOCKSValues.SOCKS_VERSION, SOCKSValues.NO_AUTH});
            state = State.AUTH_WRITE;
            channelKey.interestOps(SelectionKey.OP_WRITE);
        } else {
            close();
        }
    }

    private void canonicalReading() throws IOException {
        if (RequestUtils.sizeChecker(buffers, 10) && RequestUtils.versionChecker(buffers) && RequestUtils.commandChecker(buffers) && RequestUtils.addressChecker(buffers)) {
            if (RequestUtils.isIPV4Address(buffers)) ipv4Connecting();
            else domainResolving();
            state = TUNNEL;
            channelKey.interestOps(0);
        } else {
            close();
        }
    }

    private void ipv4Connecting() throws UnknownHostException {
        System.out.println("\tipv4 reading");
        byte[] addr = buffers.getInputSubArray(4, 8);
        InetAddress ip = Inet4Address.getByAddress(addr);
        int port = ((buffers.getInputByte(8) & 0xFF) << 8) + (buffers.getInputByte(9) & 0xFF);
        System.out.println("\tRequest for: ip:port = " + ip + ":" + port);
        this.pairedChannel = listener.createServerConnection(ip, port, this);
        this.pairedChannel.setPairedChannel(this);
    }

    private void domainResolving() throws IOException {
        int domainLen = buffers.getInputByte(4) & 0xff;
        if (5 + domainLen + 2 > buffers.getInputSize()) {
            System.err.println("\tDomain name length are bigger then message size");
            close();
            return;
        }
        String hostname = new String(buffers.getInputSubArray(5, 5 + domainLen));
        int port = ((buffers.getInputByte(5 + domainLen) & 0xFF) << 8) + (buffers.getInputByte(5 + domainLen + 1) & 0xFF);
        System.out.println("\tRequest for: domain:port = " + hostname + ":" + port);
        listener.createDnsConnection(hostname, port, this);
    }

    private void dnsReading() throws IOException {
        Message message = new Message(buffers.getInput().array());
        Optional<Record> maybeRecord = message.getSection(Section.ANSWER).stream().findAny();
        if (maybeRecord.isPresent()) {
            InetAddress ip = InetAddress.getByName(maybeRecord.get().rdataToString());
            System.out.println("DNS response: ip:port = " + ip.getHostAddress() + ":" + portOfDomainNameServer);
            SOCKSChannel socksChannel = listener.createServerConnection(ip, portOfDomainNameServer, pairedChannel);
            this.pairedChannel.setPairedChannel(socksChannel);
            channelKey.interestOps(0);
            close();
        } else {
            close();
            throw new RuntimeException("Host cannot be resolved");
        }
    }

    private void prepareWriteForPair() throws IOException {
        if (pairedChannel == null) {
            close();
            return;
        }
        pairedChannel.channelKey.interestOpsOr(SelectionKey.OP_WRITE);
        System.out.println("\t" + pairedChannel.name + " ++OP_WRITE");
        channelKey.interestOps(channelKey.interestOps() ^ SelectionKey.OP_READ);
        System.out.println("\t" + name + " --OP_READ");
        System.out.println("\tPrepared to write" + " Name = " + name);
    }

    public void write() throws IOException {
        int size = ((ByteChannel) channel).write(buffers.getOutput().flip());
        System.out.println("\tState = " + state + "; Name = " + name + "; Write " + size + " bytes.");
        //buffers.printOutput();
        if (size <= 0) {
            close();
            return;
        }
        if (buffers.getOutput().remaining() == 0) {
            switch (state) {
                case AUTH_WRITE -> authWriting();
                case DNS_WRITE -> dnsWriting();
                default -> prepareReadForPair();
            }
        }
        buffers.clearOutput();
    }

    private void authWriting() {
        channelKey.interestOps(SelectionKey.OP_READ);
        state = CONN_READ;
    }

    private void dnsWriting() {
        channelKey.interestOpsOr(SelectionKey.OP_READ);
        state = DNS_READ;
    }

    private void prepareReadForPair() throws IOException {
        if (pairedChannel == null) {
            close();
            return;
        }
        pairedChannel.channelKey.interestOpsOr(SelectionKey.OP_READ);
        System.out.println("\t" + pairedChannel.name + " ++OP_READ");
        channelKey.interestOps(channelKey.interestOps() ^ SelectionKey.OP_WRITE);
        System.out.println("\t" + name + " --OP_WRITE");
        System.out.println("\tPrepared to read" + " Name = " + name);
    }

    public void close() throws IOException {
        System.out.println("\tClosing channel. State = " + state + " Name = " + name);
        if (pairedChannel != null && pairedChannel.channelKey.isValid()) {
            if (state != DNS_READ) pairedChannel.channelKey.interestOps(SelectionKey.OP_WRITE);
        }
        pairedChannel = null;
        channelKey.cancel();
        channelKey.channel().close();
    }

    public void setDNSRequest(String hostname) throws TextParseException {
        Message message = new Message();
        Record record = Record.newRecord(Name.fromString(hostname + '.').canonicalize(), org.xbill.DNS.Type.A, DClass.IN);
        message.addRecord(record, Section.QUESTION);

        Header header = message.getHeader();
        header.setFlag(Flags.AD);
        header.setFlag(Flags.RD);
        buffers.fillOutput(message.toWire());
    }
}
