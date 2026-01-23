package edu.ifma.ifma_sdia.listeners;

import dji.sdk.camera.VideoFeeder;
import edu.ifma.ifma_sdia.controllers.BaseClient;

public class VideoDataSenderListener implements VideoFeeder.VideoDataListener {
    private BaseClient client;
    public VideoDataSenderListener(BaseClient client){
        this.client = client;
    }
    @Override
    public void onReceive(byte[] bytes, int i) {
        client.addSenderData(bytes, i);
    }
}
