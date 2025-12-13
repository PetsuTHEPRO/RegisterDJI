package edu.ifma.ifma_sdia.listeners;

import org.bouncycastle.jcajce.provider.symmetric.ARC4;

import java.util.concurrent.ConcurrentLinkedDeque;

import edu.ifma.ifma_sdia.controllers.BaseClient;
import edu.ifma.ifma_sdia.controllers.DataStruct;
import edu.ifma.ifma_sdia.controllers.MainClientController;
import edu.ifma.ifma_sdia.handlers.Parsers;

public class DataReceiverListener {
    private ConcurrentLinkedDeque<DataStruct> receiverDataQueue;
    private volatile  boolean running;
    private MainClientController controller;
    public DataReceiverListener(MainClientController c){controller=c;}
    public void setRecvDataQueue(ConcurrentLinkedDeque<DataStruct> queue){
        receiverDataQueue = queue;
    }
    public void start(){
        running = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                DataReceiverListener.this.run();
            }
        }).start();
    }

    public void stop(){
        running = false;
    }
    private void run(){
        while(running){
            if(receiverDataQueue.isEmpty()) continue;
            DataStruct data = receiverDataQueue.pop();
            controller.handleData(data);
        }
    }
}
