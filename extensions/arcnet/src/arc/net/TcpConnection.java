

package arc.net;

import java.io.IOException;
import java.net.Socket;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;

/**
 * @author Nathan Sweet <misc@n4te.com>
 */
public class TcpConnection{
    SocketChannel socketChannel;
    int keepAliveMillis = 8000;
    final ByteBuffer readBuffer, writeBuffer;
    int timeoutMillis = 12000;
    float idleThreshold = 0.1f;

    final NetSerializer serialization;
    private SelectionKey selectionKey;
    private volatile long lastWriteTime, lastReadTime;
    private int currentObjectLength;
    private final Object writeLock = new Object();

    public TcpConnection(NetSerializer serialization, int writeBufferSize, int objectBufferSize){
        this.serialization = serialization;
        writeBuffer = ByteBuffer.allocate(writeBufferSize);
        readBuffer = ByteBuffer.allocate(objectBufferSize);
        readBuffer.flip();
    }

    public SelectionKey accept(Selector selector, SocketChannel socketChannel) throws IOException{
        return accept(selector, socketChannel, null);
    }

    /**
     * Accepts an externally established channel. {@code preRead} seeds the object read buffer with bytes already
     * consumed by a demultiplexer (e.g. Shared Campaign single-entry handshake sniffing) so the injected connection
     * begins mid-stream without losing the first frame.
     */
    public SelectionKey accept(Selector selector, SocketChannel socketChannel, ByteBuffer preRead) throws IOException{
        synchronized(writeLock){
            writeBuffer.clear();
            readBuffer.clear();
            currentObjectLength = 0;
            if(preRead != null && preRead.hasRemaining()){
                if(preRead.remaining() > readBuffer.capacity())
                    throw new ArcNetException("Unable to seed read buffer: pre-read bytes exceed object buffer capacity");
                readBuffer.put(preRead);
            }
            readBuffer.flip();
            try{
                this.socketChannel = socketChannel;
                socketChannel.configureBlocking(false);
                Socket socket = socketChannel.socket();
                socket.setTcpNoDelay(true);

                // A hot-switch may re-register the very same SocketChannel on this selector immediately after its
                // previous key was cancelled by detachSocketChannel(). NIO only removes cancelled keys during a
                // select cycle; attempting register() before that cycle throws CancelledKeyException. Process the
                // cancellation synchronously because the old client update loop is deliberately stopped while the
                // channel is detached.
                SelectionKey previousKey = socketChannel.keyFor(selector);
                if(previousKey != null){
                    previousKey.cancel();
                    selector.selectNow();
                }
                selectionKey = socketChannel.register(selector,
                SelectionKey.OP_READ);

                lastReadTime = lastWriteTime = System.currentTimeMillis();

                return selectionKey;
            }catch(IOException ex){
                close();
                throw ex;
            }
        }
    }

    public void connect(Selector selector, SocketAddress remoteAddress, int timeout) throws IOException{
        connect(selector, remoteAddress, timeout, null);
    }

    /**
     * Opens a TCP connection and, when supplied, writes an unframed protocol preface before ArcNet begins reading
     * framed objects. This is intentionally transport-generic: callers own the preface format and the remote endpoint
     * must consume it before speaking ArcNet. The write happens while the channel is still blocking, so the complete
     * preface is on the wire before the selector can observe server registration traffic.
     */
    public void connect(Selector selector, SocketAddress remoteAddress, int timeout, byte[] preConnectBytes) throws IOException{
        close();
        writeBuffer.clear();
        readBuffer.clear();
        readBuffer.flip();
        currentObjectLength = 0;
        try{
            SocketChannel socketChannel = selector.provider().openSocketChannel();
            Socket socket = socketChannel.socket();
            socket.setTcpNoDelay(true);
            // socket.setTrafficClass(IPTOS_LOWDELAY);
            socket.connect(remoteAddress, timeout); // Connect using blocking mode for simplicity.
            if(preConnectBytes != null && preConnectBytes.length > 0){
                ByteBuffer preface = ByteBuffer.wrap(preConnectBytes);
                while(preface.hasRemaining()) socketChannel.write(preface);
            }
            socketChannel.configureBlocking(false);
            this.socketChannel = socketChannel;

            selectionKey = socketChannel.register(selector, SelectionKey.OP_READ);
            selectionKey.attach(this);

            lastReadTime = lastWriteTime = System.currentTimeMillis();
        }catch(IOException ex){
            close();
            throw new IOException("Unable to connect to: " + remoteAddress, ex);
        }
    }

    /**
     * Detaches the live TCP channel without closing it. Used by Shared Campaign hot-switch so the same
     * client→entry TCP can be re-bound to another Action world after Arc-level state is reset.
     * @return the still-open channel, or null when no channel is attached.
     */
    public SocketChannel detachSocketChannel(){
        synchronized(writeLock){
            SelectionKey key = selectionKey;
            if(key != null){
                key.cancel();
                selectionKey = null;
            }
            SocketChannel channel = socketChannel;
            socketChannel = null;
            writeBuffer.clear();
            readBuffer.clear();
            readBuffer.flip();
            currentObjectLength = 0;
            return channel;
        }
    }

    public Object readObject() throws IOException{
        SocketChannel socketChannel = this.socketChannel;
        if(socketChannel == null)
            throw new SocketException("Connection is closed.");

        if(currentObjectLength == 0){
            // Read the length of the next object from the socket.
            int lengthLength = serialization.getLengthLength();
            if(readBuffer.remaining() < lengthLength){
                readBuffer.compact();
                int bytesRead = socketChannel.read(readBuffer);
                readBuffer.flip();
                if(bytesRead == -1)
                    throw new SocketException("Connection is closed.");
                lastReadTime = System.currentTimeMillis();

                if(readBuffer.remaining() < lengthLength)
                    return null;
            }
            currentObjectLength = serialization.readLength(readBuffer);

            if(currentObjectLength <= 0)
                throw new ArcNetException(
                "Invalid object length: " + currentObjectLength);
            if(currentObjectLength > readBuffer.capacity())
                throw new ArcNetException(
                "Unable to read object larger than read buffer: "
                + currentObjectLength);
        }

        int length = currentObjectLength;
        if(readBuffer.remaining() < length){
            // Fill the tcpInputStream.
            readBuffer.compact();
            int bytesRead = socketChannel.read(readBuffer);
            readBuffer.flip();
            if(bytesRead == -1)
                throw new SocketException("Connection is closed.");
            lastReadTime = System.currentTimeMillis();

            if(readBuffer.remaining() < length)
                return null;
        }
        currentObjectLength = 0;

        int startPosition = readBuffer.position();
        int oldLimit = readBuffer.limit();
        readBuffer.limit(startPosition + length);
        Object object;
        try{
            object = serialization.read(readBuffer);
        }catch(Exception ex){
            throw new ArcNetException("Error during deserialization.", ex);
        }

        readBuffer.limit(oldLimit);
        if(readBuffer.position() - startPosition != length)
            throw new ArcNetException("Incorrect number of bytes ("
            + (startPosition + length - readBuffer.position())
            + " remaining) used to deserialize object: " + object);

        return object;
    }

    public void writeOperation() throws IOException{
        synchronized(writeLock){
            if(writeToSocket()){
                // Write successful, clear OP_WRITE.
                selectionKey.interestOps(SelectionKey.OP_READ);
            }
            lastWriteTime = System.currentTimeMillis();
        }
    }

    private boolean writeToSocket() throws IOException{
        SocketChannel socketChannel = this.socketChannel;
        if(socketChannel == null)
            throw new SocketException("Connection is closed.");

        ByteBuffer buffer = writeBuffer;
        buffer.flip();
        while(buffer.hasRemaining()){
            if(socketChannel.write(buffer) == 0)
                break;
        }
        buffer.compact();

        return buffer.position() == 0;
    }

    /**
     * This method is thread safe.
     */
    public int send(Object object) throws IOException{
        SocketChannel socketChannel = this.socketChannel;
        if(socketChannel == null) throw new SocketException("Connection is closed.");
        synchronized(writeLock){

            int start = writeBuffer.position();
            int lengthLength = serialization.getLengthLength();

            try{
                // Leave room for length.
                writeBuffer.position(writeBuffer.position() + lengthLength);

                // Write data.
                serialization.write(writeBuffer, object);
            }catch(Throwable ex){
                throw new ArcNetException("Error serializing object of type: " + object.getClass().getName(), ex);
            }
            int end = writeBuffer.position();

            // Write data length.
            writeBuffer.position(start);
            serialization.writeLength(writeBuffer, end - lengthLength - start);
            writeBuffer.position(end);

            // Write to socket if no data was queued.
            if(start == 0 && !writeToSocket()){
                // A partial write, set OP_WRITE to be notified when more
                // writing can occur.
                selectionKey.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
            }else{
                // Full write, wake up selector so idle event will be fired.
                selectionKey.selector().wakeup();
            }

            lastWriteTime = System.currentTimeMillis();
            return end - start;
        }
    }

    /**
     * Writes a pre-serialized, length-prefixed buffer directly into this connection's write buffer.
     * Note that this cannot use a raw buffer like UDP, it must contain length!
     */
    public int sendBuffer(ByteBuffer buffer) throws IOException{
        SocketChannel socketChannel = this.socketChannel;
        if(socketChannel == null) throw new SocketException("Connection is closed.");
        synchronized(writeLock){
            buffer.rewind();
            int length = buffer.remaining();

            int start = writeBuffer.position();
            if(length > writeBuffer.remaining()){
                throw new ArcNetException("Bulk buffer too large for write buffer: " + length + " > " + writeBuffer.remaining());
            }
            writeBuffer.put(buffer);
            int end = writeBuffer.position();

            if(start == 0 && !writeToSocket()){
                selectionKey.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
            }else{
                selectionKey.selector().wakeup();
            }

            lastWriteTime = System.currentTimeMillis();
            return end - start;
        }
    }

    public void close(){
        try{
            if(socketChannel != null){
                socketChannel.close();
                socketChannel = null;
                if(selectionKey != null)
                    selectionKey.selector().wakeup();
            }
        }catch(IOException ignored){
        }
    }

    public boolean needsKeepAlive(long time){
        return socketChannel != null && keepAliveMillis > 0 && time - lastWriteTime > keepAliveMillis;
    }

    public boolean isTimedOut(long time){
        return socketChannel != null && timeoutMillis > 0 && time - lastReadTime > timeoutMillis;
    }
}
