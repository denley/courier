package me.denley.courier;

import android.util.Log;

import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.PutDataRequest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

import me.denley.courier.compiler.DataMapProcessor;

public final class Packager {

    private static final Map<Class, DataPackager> PACKAGERS = new LinkedHashMap<>();

    public interface DataPackager<T> {
        public com.google.android.gms.wearable.DataMap pack(T target);
        public T unpack(com.google.android.gms.wearable.DataMap map);
    }

    public static PutDataRequest pack(String path, Object data) {
        final PutDataRequest request = PutDataRequest.create(path);
        request.setData(pack(data));
        return request;
    }

    public static byte[] packSerializable(Serializable object) {
        if(object==null) {
            return new byte[0];
        }

        try {
            final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            final ObjectOutputStream out = new ObjectOutputStream(bytes);
            out.writeObject(object);

            return bytes.toByteArray();
        }catch (IOException e){
            throw new IllegalArgumentException("Unable to serialize object", e);
        }
    }

    @SuppressWarnings("unchecked")
    public static byte[] pack(Object deliverable) {
        try {
            final DataPackager packager = getDataPackager(deliverable.getClass());
            return packager.pack(deliverable).toByteArray();
        } catch (Exception e) {
            return packSerializable((Serializable)deliverable);
        }
    }


    @SuppressWarnings("unchecked")
    public static <T> T unpackSerializable(byte[] data) {
        if(data==null || data.length==0) {
            return null;
        }

        try {
            final ByteArrayInputStream bytes = new ByteArrayInputStream(data);
            final ObjectInputStream in = new ObjectInputStream(bytes);

            return (T)in.readObject();
        }catch (Exception e){
            throw new IllegalArgumentException("Unable to deserialize object", e);
        }
    }

    public static <T> T unpack(DataItem data, Class<T> targetClass) {
        try {
            final DataMapItem dataMapItem = DataMapItem.fromDataItem(data);
            return unpack(dataMapItem.getDataMap(), targetClass);
        }catch (Exception e) {
            return unpackSerializable(data.getData());
        }
    }

    public static <T> T unpack(byte[] data, Class<T> targetClass) {
        try {
            final DataMap dataMap = DataMap.fromByteArray(data);
            return unpack(dataMap, targetClass);
        }catch (Exception e) {
            return unpackSerializable(data);
        }
    }

    public static <T> T unpack(DataMap map, Class<T> targetClass) {
        final DataPackager<T> packager = getDataPackager(targetClass);
        return packager.unpack(map);
    }

    @SuppressWarnings("unchecked")
    private static <T> DataPackager<T> getDataPackager(Class<T> targetClass) {
        DataPackager<T> packager = PACKAGERS.get(targetClass);
        if(packager!=null) {
            return packager;
        }

        try {
            final String packagerClassName = targetClass.getName() + DataMapProcessor.CLASS_SUFFIX;
            final Class packagerClass = Class.forName(packagerClassName);
            packager = (DataPackager<T>)packagerClass.newInstance();
            PACKAGERS.put(targetClass, packager);
            return packager;
        }catch (Exception e) {
            Log.e("Packager", "Couldn't find packager for target class. Missing @Deliverable annotation?");
            return null;
        }
    }


    private Packager(){}

}
