package me.denley.courier.compiler;

import javax.lang.model.element.Element;

public enum DataMapElementType {
    ASSET(new String[]{"com.google.android.gms.wearable.Asset"}, "putAsset", "getAsset"),
    BOOLEAN(new String[]{"boolean", "java.lang.Boolean"}, "putBoolean", "getBoolean"),
    BYTE(new String[]{"byte", "java.lang.Byte"}, "putByte", "getByte"),
    BYTE_ARRAY(new String[]{"byte[]"}, "putByteArray", "getByteArray"),
    DATA_MAP(new String[]{"com.google.android.gms.wearable.DataMap"}, "putDataMap", "getDataMap"),
    DATA_MAP_ARRAY_LIST(new String[]{"java.util.ArrayList<com.google.android.gms.wearable.DataMap>"}, "putDataMapArrayList", "getDataMapArrayList"),
    DOUBLE(new String[]{"double", "java.lang.Double"}, "putDouble", "getDouble"),
    FLOAT(new String[]{"float", "java.lang.Float"}, "putFloat", "getFloat"),
    FLOAT_ARRAY(new String[]{"float[]"}, "putFloatArray", "getFloatArray"),
    INTEGER(new String[]{"int", "java.lang.Integer"}, "putInt", "getInt"),
    INTEGER_ARRAY_LIST(new String[]{"java.util.ArrayList<java.lang.Integer>"}, "putIntegerArrayList", "getIntegerArrayList"),
    LONG(new String[]{"long", "java.lang.Long"}, "putLong", "getLong"),
    LONG_ARRAY(new String[]{"long[]"}, "putLongArray", "getLongArray"),
    STRING(new String[]{"java.lang.String"}, "putString", "getString"),
    STRING_ARRAY(new String[]{"java.lang.String[]"}, "putStringArray", "getStringArray"),
    STRING_ARRAY_LIST(new String[]{"java.util.ArrayList<java.lang.String>"}, "putStringArrayList", "getStringArrayList"),
    ;

    public final String[] classTypes;
    public final String putMethod;
    public final String getMethod;

    private DataMapElementType(String[] classTypes, String putMethod, String getMethod) {
        this.classTypes = classTypes;
        this.putMethod = putMethod;
        this.getMethod = getMethod;
    }


    public static DataMapElementType getElementType(Element element) {
        final String elementType = element.asType().toString();

        for(DataMapElementType type:values()) {
            for(String classType:type.classTypes) {
                if(classType.equals(elementType)) {
                    return type;
                }
            }
        }

        return null;
    }

}
