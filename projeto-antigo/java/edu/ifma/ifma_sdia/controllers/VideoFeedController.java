package edu.ifma.ifma_sdia.controllers;

import android.util.Log;

import dji.sdk.camera.VideoFeeder;
import edu.ifma.ifma_sdia.listeners.VideoDataSenderListener;

public class VideoFeedController {
    private VideoFeeder.VideoFeed videoFeed;
    private VideoFeeder.VideoActiveStatusListener videoStatusListener;
    private boolean isConnected;
    private VideoDataSenderListener listener;
    private BaseClient client;
    private String host;
    private int port;
    public VideoFeedController(String host, int port){
        this.host = host;
        this.port = port;

        videoStatusListener = new VideoFeeder.VideoActiveStatusListener() {
            @Override
            public void onUpdate(boolean b) {
                Log.i("VideoFeedController.", "(statusListener) Video status ativo mudado para: "+b);
            }
        };
        connect();
    }
    public void setAddress(String host, int port){
        if(isConnected){
            Log.i("VideoFeedController.", "(setAddress) Não é possível mudar host:port enquanto cliente está conectado.");
            return;
        }
        this.host = host;
        this.port = port;

    }
    public void connect(){
        if(isConnected){
            Log.i("VideoFeedController", "(connect) Já está conectado.");
            return;
        }
        client = new BaseClient(host, port, null);
        if(client.connect(true, false)){
            Log.i("VideoFeedController", "(connect) conectado.");
            addListeners();
            isConnected = true;
        }
    }
    public void disconnect(){
        if(!isConnected){
            Log.i("VideoFeedController", "(disconnect) Já está desconectado.");
            return;
        }
        client.stopAll();
        removeListeners();
        isConnected = false;
    }
    public void addListeners(){
        Log.d("VideoFeedController", "(startVideoFeedListener) Iniciando listeners.");
        videoFeed = VideoFeeder.getInstance().getPrimaryVideoFeed();
        listener = new VideoDataSenderListener(client);
        videoFeed.addVideoDataListener(listener);
        videoFeed.addVideoActiveStatusListener(videoStatusListener);
    }
    public void removeListeners(){
        Log.d("VideoFeedController", "(removeListener) Removendo listeners.");
        videoFeed = VideoFeeder.getInstance().getPrimaryVideoFeed();
        videoFeed.removeVideoDataListener(listener);
        videoFeed.removeVideoActiveStatusListener(videoStatusListener);
    }
}
