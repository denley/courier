package me.denley.courier;

import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.PutDataMapRequest;
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
        public DataMap pack(T target);
        public void pack(T target, DataMap map);
        public T unpack(DataMap map);
    }

    @SuppressWarnings("unchecked")
    public static PutDataRequest pack(String path, Object data) {
        try {
            final PutDataMapRequest request = PutDataMapRequest.create(path);
            final DataPackager packager = getDataPackager(data.getClass());
            packager.pack(data, request.getDataMap());
            return request.asPutDataRequest();
        } catch (Exception e) {
            final PutDataRequest request = PutDataRequest.create(path);
            request.setData(packSerializable((Serializable)data));
            return request;
        }
    }

    public static byte[] packBytes(Object deliverable) {
        try {
            return pack(deliverable).toByteArray();
        } catch (Exception e) {
            return packSerializable((Serializable) deliverable);
        }
    }

    @SuppressWarnings("unchecked")
    public static DataMap pack(Object deliverable) {
        if(deliverable==null) {
            return null;
        }

        final DataPackager packager = getDataPackager(deliverable.getClass());
        return packager.pack(deliverable);
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

    @SuppressWarnings("unused") // Used by generated classes
    public static <T> T unpack(DataItem data, Class<T> targetClass) {
        try {
            final DataMapItem dataMapItem = DataMapItem.fromDataItem(data);
            return unpack(dataMapItem.getDataMap(), targetClass);
        }catch (Exception e) {
            return unpackSerializable(data.getData());
        }
    }

    @SuppressWarnings("unused") // Used by generated classes
    public static <T> T unpack(byte[] data, Class<T> targetClass) {
        try {
            final DataMap dataMap = DataMap.fromByteArray(data);
            return unpack(dataMap, targetClass);
        }catch (Exception e) {
            return unpackSerializable(data);
        }
    }

    public static <T> T unpack(DataMap map, Class<T> targetClass) {
        if(map==null) {
            return null;
        }

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
            return null;
        }
    }


    private Packager(){}

}
