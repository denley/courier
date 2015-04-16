package me.denley.courier;

import android.content.Context;

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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * This class contains various static methods used to serialize and deserialize objects into
 * DataMaps, DataItems, and byte arrays. This in turn, allows objects to be transferred
 * to other devices using the Wearable API.
 */
@SuppressWarnings("unused")
public final class Packager {

    private static final Map<Class, DataPackager> PACKAGERS = new LinkedHashMap<Class, DataPackager>();

    /** For use by generated code. Don't use this. */
    public interface DataPackager<T> {
        public DataMap pack(T target);
        public void pack(T target, DataMap map);
        public T unpack(Context context, DataMap map);
    }

    /**
     * In general, this method will only be used by generated code. However, it may be suitable
     * to use this method in some cases (such as in a WearableListenerService).
     *
     * Packages the given object into a PutDataRequest for the specified path.
     *
     * This method will attempt to convert the object to a DataMap using generated code from
     * the {@link Deliverable} annotation.
     *
     * If that fails (e.g. if the object's class was not annotated with {@link Deliverable}),
     * then the object will be converted using the {@link java.io.Serializable} system.
     *
     * If both of these methods are not possible, a {@link java.lang.ClassCastException} will be thrown.
     *
     * @param path  The Wearable API path that the data will be sent on.
     * @param data  The object to serialize into bytes.
     * @return      A PutDataRequest for the given path that encapsulates the given object.
     */
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

    /**
     * In general, this method will only be used by generated code. However, it may be suitable
     * to use this method in some cases (such as in a WearableListenerService).
     *
     * Packages the given array of objects into an array of DataMaps.
     *
     * This method will attempt to convert each object to a DataMap using generated code from
     * the {@link Deliverable} annotation.
     *
     * If this is not possible, a {@link java.lang.RuntimeException} will be thrown.
     *
     * @param deliverablse  The objectn to serialize into DataMaps.
     * @return An ArrayList of DataMaps representing the the object.
     * @throws java.lang.RuntimeException If the packager for the object's class could not be found
     */
    public static ArrayList<DataMap> pack(ArrayList<?> deliverables) {
        final ArrayList<DataMap> packed = new ArrayList<DataMap>();
        if(deliverables != null) {
            for(Object deliverable:deliverables) {
                packed.add(pack(deliverable));
            }
        }
        return packed;
    }

    /**
     * In general, this method will only be used by generated code. However, it may be suitable
     * to use this method in some cases (such as in a WearableListenerService).
     *
     * Packages the given object into a byte array.
     *
     * This method will attempt to convert the object to a DataMap using generated code from
     * the {@link Deliverable} annotation (and then converted to a byte array from the DataMap).
     *
     * If that fails (e.g. if the object's class was not annotated with {@link Deliverable}),
     * then the object will be converted using the {@link java.io.Serializable} system.
     *
     * If both of these methods are not possible, a {@link java.lang.ClassCastException} will be thrown.
     *
     * @param deliverable  The object to serialize into bytes.
     * @return A byte array representing the serialized form of the object.
     */
    public static byte[] packBytes(Object deliverable) {
        try {
            return pack(deliverable).toByteArray();
        } catch (Exception e) {
            return packSerializable((Serializable) deliverable);
        }
    }

    /**
     * In general, this method will only be used by generated code. However, it may be suitable
     * to use this method in some cases (such as in a WearableListenerService).
     *
     * Packages the given object into a DataMap.
     *
     * This method will attempt to convert the object to a DataMap using generated code from
     * the {@link Deliverable} annotation.
     *
     * If this is not possible, a {@link java.lang.ClassCastException} will be thrown.
     *
     * @param deliverable  The object to serialize into a DataMap.
     * @return A DataMap representing the the object.
     * @throws java.lang.RuntimeException If the packager for the object's class could not be found
     */
    @SuppressWarnings("unchecked")
    public static DataMap pack(Object deliverable) {
        if(deliverable==null) {
            return null;
        }

        final DataPackager packager = getDataPackager(deliverable.getClass());
        if(packager == null) {
            throw new RuntimeException("Unable to find packager for the given object. " +
                    "Please ensure that the object's class is annotated with @Deliverable, " +
                    "and that the annotation processor has run correctly");
        }
        return packager.pack(deliverable);
    }

    /**
     * In general, this method will only be used by generated code. However, it may be suitable
     * to use this method in some cases (such as in a WearableListenerService).
     *
     * Packages the given object into a byte array.
     *
     * This method will attempt to convert the object to a byte array using the {@link java.io.Serializable} system.
     *
     * If this is not possible, an {@link java.lang.IllegalArgumentException} will be thrown.
     *
     * @param object  The object to serialize into bytes.
     * @return A byte array representing the serialized form of the object.
     */
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

    /**
     * In general, this method will only be used by generated code. However, it may be suitable
     * to use this method in some cases (such as in a WearableListenerService).
     *
     * Unpacks the given byte array into an object.
     *
     * This method will use the {@link java.io.Serializable} system to deserialize the data.
     *
     * @param data  The byte array to deserialize into an object.
     * @return An object deserialized from the byte array.
     */
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

    /**
     * In general, this method will only be used by generated code. However, it may be suitable
     * to use this method in some cases (such as in a WearableListenerService).
     *
     * Unpacks the given ArrayList of DataMaps into an ArrayList of objects of the given class.
     *
     * This method will attempt to use generated code from the {@link Deliverable}
     * annotation to convert each DataMap to an object of the given class.
     *
     * @param context The Context that may be used to load Assets from the data.
     * @param maps  The DataItems to load the object from.
     * @param targetClass The class of object to unpack.
     * @return An ArrayList of objects of the given class.
     * * @throws java.lang.RuntimeException If the packager for targetClass could not be found
     */
    public static <T> ArrayList<T> unpack(Context context, ArrayList<DataMap> maps, Class<T> targetClass) {
        if(maps==null) {
            return null;
        }

        final DataPackager<T> packager = getDataPackager(targetClass);
        if(packager == null) {
            throw new RuntimeException("Unable to find packager for " + targetClass.toString() +
                    "Please ensure that it is annotated with @Deliverable, " +
                    "and that the annotation processor has run correctly");
        }

        final ArrayList<T> unpacked = new ArrayList<T>();
        for(DataMap map:maps) {
            unpacked.add(packager.unpack(context, map));
        }
        return unpacked;
    }

    /**
     * In general, this method will only be used by generated code. However, it may be suitable
     * to use this method in some cases (such as in a WearableListenerService).
     *
     * Unpacks the given DataItem into an object of the given class.
     *
     * This method will attempt to load a DataMap from the DataItem and then use generated code from
     * the {@link Deliverable} annotation to convert it to an object of the given class.
     *
     * If this is not possible, this method will then attempt to deserialize the byte array contained
     * in the DataItem using the {@link java.io.Serializable} system.
     *
     * @param context The Context that may be used to load Assets from the data.
     * @param data  The DataItem to load the object from.
     * @param targetClass The class of object to unpack.
     * @return An object of the given class.
     */
    public static <T> T unpack(Context context, DataItem data, Class<T> targetClass) {
        try {
            final DataMapItem dataMapItem = DataMapItem.fromDataItem(data);
            return unpack(context, dataMapItem.getDataMap(), targetClass);
        }catch (Exception e) {
            return unpackSerializable(data.getData());
        }
    }

    /**
     * In general, this method will only be used by generated code. However, it may be suitable
     * to use this method in some cases (such as in a WearableListenerService).
     *
     * Unpacks the given byte array into an object of the given class.
     *
     * This method will attempt to convert the byte array into a DataMap and then use generated code from
     * the {@link Deliverable} annotation to convert it to an object of the given class.
     *
     * If this is not possible, this method will then attempt to deserialize the byte array
     * using the {@link java.io.Serializable} system.
     *
     * @param context The Context that may be used to load Assets from the data.
     * @param data  The byte array to load the object from.
     * @param targetClass The class of object to unpack.
     * @return An object of the given class.
     */
    public static <T> T unpack(Context context, byte[] data, Class<T> targetClass) {
        try {
            final DataMap dataMap = DataMap.fromByteArray(data);
            return unpack(context, dataMap, targetClass);
        }catch (Exception e) {
            return unpackSerializable(data);
        }
    }

    /**
     * In general, this method will only be used by generated code. However, it may be suitable
     * to use this method in some cases (such as in a WearableListenerService).
     *
     * Unpacks the given DataMap into an object of the given class.
     *
     * This method will attempt to use generated code from the {@link Deliverable}
     * annotation to convert the DataMap to an object of the given class.
     *
     * @param context The Context that may be used to load Assets from the data.
     * @param map  The DataItem to load the object from.
     * @param targetClass The class of object to unpack.
     * @return An object of the given class.
     * * @throws java.lang.RuntimeException If the packager for targetClass could not be found
     */
    public static <T> T unpack(Context context, DataMap map, Class<T> targetClass) {
        if(map==null) {
            return null;
        }

        final DataPackager<T> packager = getDataPackager(targetClass);
        if(packager == null) {
            throw new RuntimeException("Unable to find packager for " + targetClass.toString() +
                    "Please ensure that it is annotated with @Deliverable, " +
                    "and that the annotation processor has run correctly");
        }
        return packager.unpack(context, map);
    }

    /**
     * In general, this method will only be used by generated code. However, it may be suitable
     * to use this method in some cases (such as in a WearableListenerService).
     *
     * Unpacks the given DataItem into an object of the given class.
     *
     * This method will attempt to load a DataMap from the DataItem and then use generated code from
     * the {@link Deliverable} annotation to convert it to an object of the given class.
     *
     * If this is not possible, this method will then attempt to deserialize the byte array contained
     * in the DataItem using the {@link java.io.Serializable} system.
     *
     * @param data  The DataItem to load the object from.
     * @param targetClass The class of object to unpack.
     * @return An object of the given class.
     * @deprecated Use {@link #unpack(android.content.Context, com.google.android.gms.wearable.DataItem, Class)} instead to allow automatic Asset loading.
     */
    @Deprecated
    public static <T> T unpack(DataItem data, Class<T> targetClass) {
        return unpack(null, data, targetClass);
    }

    /**
     * In general, this method will only be used by generated code. However, it may be suitable
     * to use this method in some cases (such as in a WearableListenerService).
     *
     * Unpacks the given byte array into an object of the given class.
     *
     * This method will attempt to convert the byte array into a DataMap and then use generated code from
     * the {@link Deliverable} annotation to convert it to an object of the given class.
     *
     * If this is not possible, this method will then attempt to deserialize the byte array
     * using the {@link java.io.Serializable} system.
     *
     * @param data  The byte array to load the object from.
     * @param targetClass The class of object to unpack.
     * @return An object of the given class.
     * @deprecated Use {@link #unpack(android.content.Context, byte[], Class)} instead to allow automatic Asset loading.
     */
    @Deprecated
    public static <T> T unpack(byte[] data, Class<T> targetClass) {
        return unpack(null, data, targetClass);
    }

    /**
     * In general, this method will only be used by generated code. However, it may be suitable
     * to use this method in some cases (such as in a WearableListenerService).
     *
     * Unpacks the given DataMap into an object of the given class.
     *
     * This method will attempt to use generated code from the {@link Deliverable}
     * annotation to convert the DataMap to an object of the given class.
     *
     * @param map  The DataItem to load the object from.
     * @param targetClass The class of object to unpack.
     * @return An object of the given class.
     * @deprecated Use {@link #unpack(android.content.Context, com.google.android.gms.wearable.DataMap, Class)} instead to allow automatic Asset loading.
     */
    @Deprecated
    public static <T> T unpack(DataMap map, Class<T> targetClass) {
        return unpack(null, map, targetClass);
    }

    @SuppressWarnings("unchecked")
    private static <T> DataPackager<T> getDataPackager(Class<T> targetClass) {
        DataPackager<T> packager = PACKAGERS.get(targetClass);
        if(packager!=null) {
            return packager;
        }

        try {
            final String packagerClassName = targetClass.getName() + "$$DataMapPackager";
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
