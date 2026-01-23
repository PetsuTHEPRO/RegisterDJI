package edu.ifma.ifma_sdia.listeners.key_listeners;

import androidx.annotation.Nullable;

import dji.keysdk.callback.KeyListener;
import edu.ifma.ifma_sdia.controllers.BaseClient;
import edu.ifma.ifma_sdia.handlers.Builders;
import edu.ifma.ifma_sdia.handlers.Code;

public class GenericFloatListener implements KeyListener {
    private final BaseClient client;
    private final Code code;
    public GenericFloatListener(BaseClient client, Code code){
        this.client = client;
        this.code = code;
    }
    @Override
    public void onValueChange(@Nullable Object o, @Nullable Object o1) {
        float v;
        if(o==null && o1==null) return;
        if(o1!=null) v = (float) o1;
        else v = (float) o;
        byte[] data = Builders.genericFloatData(this.code.value, v);
        this.client.addSenderData(data);
    }
}
