package edu.ifma.ifma_sdia.listeners.key_listeners;

import androidx.annotation.Nullable;

import dji.common.flightcontroller.LocationCoordinate3D;
import dji.keysdk.callback.KeyListener;
import edu.ifma.ifma_sdia.controllers.BaseClient;
import edu.ifma.ifma_sdia.handlers.Builders;

public class AircraftLocationListener implements KeyListener {
    private final BaseClient client;
    public AircraftLocationListener(BaseClient client){
        this.client = client;
    }
    @Override
    public void onValueChange(@Nullable Object o, @Nullable Object o1) {
        LocationCoordinate3D loc;
        if(o==null && o1==null) return;
        if(o1!=null) loc = (LocationCoordinate3D) o1;
        else loc = (LocationCoordinate3D) o;
        byte[] data = Builders.aircraftLocation(loc);
        client.addSenderData(data);
    }
}
