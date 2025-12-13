package edu.ifma.ifma_sdia.controllers;

import android.provider.ContactsContract;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedDeque;

import edu.ifma.ifma_sdia.listeners.DataReceiverListener;

public class BaseClient {
    private String host;
    private int port;
    private Socket skt;
    private OutputStream sender;

    private InputStream receiver;
    private volatile boolean senderRunning;
    private volatile boolean receiverRunning;
    private volatile boolean connected;
    private ConcurrentLinkedDeque<DataStruct> senderDataQueue;
    private ConcurrentLinkedDeque<DataStruct> receiverDataQueue;
    @Nullable
    private DataReceiverListener receiverListener;
    public BaseClient(String h, int p, @Nullable DataReceiverListener r){
        host = h;
        port = p;
        senderRunning = false;
        receiverRunning = false;
        receiverListener = r;
        senderDataQueue = new ConcurrentLinkedDeque<>();
        if(r!=null){
            receiverDataQueue = new ConcurrentLinkedDeque<>();
            receiverListener.setRecvDataQueue(receiverDataQueue);
        }
    }

    public boolean connect(boolean isSender, boolean isReceiver){
        if(!(isSender||isReceiver)) return false;
        if(connected){
            Log.d("BaseClient", "Ja conectado!");
            return true;
        }
        skt = null;
        sender = null;
        receiver = null;
        try {
            skt = new Socket(host, port);
            //writer = skt.getOutputStream();
            Log.i("BaseClient", "Conectado!");
            connected = true;
            if(isSender)
                this.startSender();
            if(isReceiver)
                this.startReceiver();
            return true;
        } catch (Exception e){
            Log.e("BaseClient", "Erro ao criar Socket: "+e.getMessage());
            connected = false;
            return false;
        }
    }
    public void stopSender(){
        senderRunning = false;
        sender = null;
    }
    public void stopReceiver(){
        receiverRunning = false;
        receiver = null;
    }
    public void startSender() throws IOException {
        senderRunning = true;
        sender = skt.getOutputStream();
        new Thread(new Runnable() {
            @Override
            public void run() {
                BaseClient.this.runSender();
            }
        }).start();
    }
    public void startReceiver() throws IOException {
        receiverRunning = true;
        receiver = skt.getInputStream();
        new Thread(new Runnable() {
            @Override
            public void run() {
                BaseClient.this.runReceiver();
            }
        }).start();
    }
    public void addSenderData(byte[] data, int size){
        DataStruct newData = new DataStruct(data, size);
        senderDataQueue.add(newData);
    }
    public void addSenderData(byte[] data){
        int size = data.length;
        addSenderData(data, size);
    }
    public void runSender(){
        Log.i("BaseClient", "Starting running Sender");
        while(connected && senderRunning){
            if(senderDataQueue.isEmpty()) continue;
            DataStruct data = senderDataQueue.pop();
            try {
                this.send(data);
            } catch (IOException e) {
                Log.e("BaseClient", "(runReceiver) closing connection due to error while recv!");
                stopAll();
            }

        }
    }
    private void send(DataStruct data) throws IOException {
        ByteBuffer pkt = ByteBuffer.allocate(data.size+4);
        pkt.putInt(data.size);
        //this.sender.write(data.size);
        pkt.put(data.data, 0, data.size);
        this.sender.write(pkt.array(), 0, pkt.capacity());
    }
    private int parse32Int(byte[] data){
        return ((data[0] & 0xFF) << 24) |
                ((data[1] & 0xFF) << 16) |
                ((data[2] & 0xFF) << 8 ) |
                ((data[3] & 0xFF));
    }
    private DataStruct recv() throws IOException {
        byte[] bSize = new byte[4];
        int r = receiver.read(bSize, 0, 4);
        if(r!=4) throw new IOException();
        int size = parse32Int(bSize);
        // Log.i("BaseClient", "(recv) size:"+size);
        if(size > 0xFFFFF) {
            // Log.e("BaseClient", "(recv) data size is too big!");
            throw new IOException();
        }
        byte[] data = new byte[size];
        r = receiver.read(data, 0, size);
        Log.i("BaseClient", "(recv) data:"+ Arrays.toString(data));
        if(r != size) {
            Log.e("BaseClient", "(recv) data received is different than size!");
            throw new IOException();
        }
        return new DataStruct(data, size);
    }
    public void runReceiver() {
        Log.i("BaseClient", "Starting running Receiver");
        if(receiverListener!=null){
            receiverListener.start();
        }
        while(connected && receiverRunning){
            try {
                DataStruct data = recv();
                this.receiverDataQueue.add(data);
            } catch (IOException e) {
                Log.e("BaseClient", "(runReceiver) closing connection due to error while recv!");
                stopAll();
            }
        }
    }
    public void stopAll(){
        connected = false;
        receiverRunning = false;
        senderRunning = false;
        try {
            skt.close();
        } catch (IOException e) {
            Log.e("BaseClient", "(stopAll) erro closing Socket: "+e.getMessage());
        }
    }
}
