package me.denley.courier;

import com.google.android.gms.wearable.PutDataRequest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public final class Packager {

    public static PutDataRequest packageSerializable(String path, Serializable data) {
        final PutDataRequest request = PutDataRequest.create(path);
        request.setData(packageSerializable(data));
        return request;
    }

    public static byte[] packageSerializable(Serializable object) {
        try {
            final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            final ObjectOutputStream out = new ObjectOutputStream(bytes);
            out.writeObject(object);

            return bytes.toByteArray();
        }catch (IOException e){
            return new byte[0];
        }
    }

    private Packager(){}

}
