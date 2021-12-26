package nsu.networks.socks5.channel;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class Buffers {
    private final static int BUF_SIZE = 10240;
    private ByteBuffer input; ///from
    private ByteBuffer output;///to

    public Buffers() {
        input = ByteBuffer.allocate(BUF_SIZE);
        output = null;
    }

    public void attachOutBuffer(ByteBuffer buffer) {
        output = buffer;
    }

    private void initOutBuffer() {
        if (output == null) output = ByteBuffer.allocate(BUF_SIZE);
    }


    public void fillOutput(byte[] arr) {
        initOutBuffer();
        clearOutput();
        output.put(arr);
    }

    public void fillInput(byte[] arr) {
        clearInput();
        input.put(arr);
    }


    public void clearInput() {
        input.clear();
    }

    public void clearOutput() {
        output.clear();
    }


    public ByteBuffer getInput() {
        return input;
    }

    public ByteBuffer getOutput() {
        return output;
    }


    public int getInputSize() {
        return input.position();
    }


    public byte getInputByte(int number) {
        return input.array()[number];
    }

    public byte[] getInputSubArray(int start, int finish) {
        return Arrays.copyOfRange(input.array(), start, finish);
    }


    public void printInput() {
        //It is debugging method
        System.out.println("input buffer:");
        System.out.println(Arrays.toString(input.array()));
    }

    public void printOutput() {
        //It is debugging method
        System.out.println("output buffer:");
        System.out.println(Arrays.toString(output.array()));
    }
}
