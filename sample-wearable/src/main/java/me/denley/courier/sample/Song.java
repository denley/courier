package me.denley.courier.sample;

import com.google.android.gms.wearable.Asset;

import java.util.ArrayList;

import me.denley.courier.Deliverable;

@Deliverable
public class Song {

    public String title;
    public Artist artist;
    public long length;


    // Test variables (to test each type of mapping)
    public Asset assetTest;
    public boolean aBoolean;
    public Boolean anotherBoolean;
    public byte aByte;
    public Byte anotherByte;
    public byte[] bytes;
    public com.google.android.gms.wearable.DataMap map;
    public ArrayList<com.google.android.gms.wearable.DataMap> maps;
    public double aDouble;
    public Double anotherDouble;
    public float aFloat;
    public Float anotherFloat;
    public float[] floats;
    public int anInt;
    public Integer anInteger;
    public ArrayList<Integer> integers;
    public long aLong;
    public Long anotherLong;
    public long[] longs;
    public String[] strings;
    public ArrayList<String> moreStrings;

}
