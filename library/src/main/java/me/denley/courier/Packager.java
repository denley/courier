package me.denley.courier;

import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.PutDataRequest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public final class Packager {

    public static PutDataRequest pack(String path, Serializable data) {
        final PutDataRequest request = PutDataRequest.create(path);
        request.setData(pack(data));
        return request;
    }

    public static <T> T unpack(DataItem data) {
        return unpack(data.getData());
    }

    public static byte[] pack(Serializable object) {
        try {
            final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            final ObjectOutputStream out = new ObjectOutputStream(bytes);
            out.writeObject(object);

            return bytes.toByteArray();
        }catch (IOException e){
            return new byte[0];
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T unpack(byte[] data) {
        try {
            final ByteArrayInputStream bytes = new ByteArrayInputStream(data);
            final ObjectInputStream in = new ObjectInputStream(bytes);

            return (T)in.readObject();
        }catch (Exception e){
            return null;
        }
    }





    private Packager(){}

}
