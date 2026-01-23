package edu.ifma.ifma_sdia.listeners.key_listeners;

import androidx.annotation.Nullable;

import dji.keysdk.callback.KeyListener;
import edu.ifma.ifma_sdia.controllers.BaseClient;
import edu.ifma.ifma_sdia.handlers.Builders;
import edu.ifma.ifma_sdia.handlers.Code;

public class GenericIntListener implements KeyListener {
    private final BaseClient client;
    private final Code code;
    public GenericIntListener(BaseClient client, Code code){
        this.client = client;
        this.code = code;
    }
    @Override
    public void onValueChange(@Nullable Object o, @Nullable Object o1) {
        int v;
        if(o==null && o1==null) return;
        if(o1!=null) v = (int) o1;
        else v = (int) o;
        byte[] data = Builders.genericIntData(this.code.value, v);
        this.client.addSenderData(data);
    }
}
