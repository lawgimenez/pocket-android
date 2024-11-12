package com.pocket.sync.value;

import android.content.Intent;
import android.os.Bundle;

import com.fasterxml.jackson.databind.node.TextNode;
import com.pocket.sync.source.JsonConfig;
import com.pocket.sync.spec.Syncable;
import com.pocket.sync.thing.Thing;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility for parcelling sync values like {@link Thing}, {@link EnumType} to and from a {@link Bundle}.
 */
public class Parceller {

    /** No special Json parsing configuration rules are required here */
    private static final JsonConfig JSON_CONFIG = Syncable.NO_ALIASES;

    // Put Things

    /**
     * Put this thing into the bundle at the provided key.  Read it back later with {@link #get(Bundle, String, SyncableParser)}
     */
    public static void put(Bundle into, String key, Thing thing) {
        into.putString(key, toString(thing));
    }

    /**
     * Put this list of things into the bundle at the provided key.  Read it back later with {@link #getList(Bundle, String, SyncableParser)}
     */
    public static void put(Bundle into, String key, List<? extends Thing> things) {
        into.putStringArray(key, toStringArray(things));
    }

    /**
     * Put this thing into the intent extras at the provided key.  Read it back later with {@link #get(Intent, String, SyncableParser)}
     */
    public static void put(Intent into, String key, Thing thing) {
        into.putExtra(key, toString(thing));
    }

    /**
     * Put this list of things into the intent extras at the provided key.  Read it back later with {@link #getList(Intent, String, SyncableParser)}
     */
    public static void put(Intent into, String key, List<? extends Thing> things) {
        into.putExtra(key, toStringArray(things));
    }


    // Put StringEnums

    /**
     * Put this enum into the bundle at the provided key.  Read it back later with {@link #getStringEnum(Bundle, String, TypeParser)}
     */
    public static void put(Bundle into, String key, StringEnum value) {
        into.putString(key, toString(value));
    }

    /**
     * Put this enum into the intent extras at the provided key.  Read it back later with {@link #getStringEnum(Intent, String, TypeParser)}
     */
    public static void put(Intent into, String key, StringEnum value) {
        into.putExtra(key, toString(value));
    }






    // Thing conversions

    private static String[] toStringArray(List<? extends Thing> things) {
        String[] array = things != null ? new String[things.size()] : null;
        if (array != null) {
            for (int i = 0, len = things.size(); i < len; i++) {
                array[i] = toString(things.get(i));
            }
        }
        return array;
    }

    private static String toString(Thing thing) {
        return thing != null ? thing.toJson(JSON_CONFIG).toString() : null;
    }

    private static <T extends Thing> ArrayList<T> fromStringArray(String[] strings, SyncableParser<T> creator) {
        ArrayList<T> things = strings != null ? new ArrayList<>(strings.length) : null;
        if (things != null) {
            for (String s : strings) {
                things.add(fromString(s, creator));
            }
        }
        return things;
    }

    private static <T extends Thing> T fromString(String string, SyncableParser<T> creator) {
        if (string == null) return null;
        return creator.create(BaseModeller.toObjectNode(string), JSON_CONFIG);
    }


    // StringEnum conversions

    private static String toString(StringEnum value) {
        return value != null ? value.value : null;
    }

    private static <E extends StringEnum> E enumFromString(String string, TypeParser<E> creator) {
        return string != null ? creator.create(new TextNode(string)) : null;
    }


    // Get Things

    /**
     * Reads a thing from a bundle, previously added with {@link #put(Bundle, String, Thing)}.
     * @param from The bundle to read from. If null, null will be returned
     * @param key The key that was used in {@link #put(Bundle, String, Thing)}
     * @param creator A json creator for the expected type. Generated things have this as a public static JSON_CREATOR field. For example Item.JSON_CREATOR
     * @return The thing or null if the bundle is null or the key is not set or contains a null value.
     */
    public static <T extends Thing> T get(Bundle from, String key, SyncableParser<T> creator) {
        if (from == null) return null;
        return fromString(from.getString(key), creator);
    }

    /** Same as {@link #get(Intent, String, SyncableParser)} but will read from the intent's extras. If the intent or its extras are null this returns null. */
    public static <T extends Thing> T get(Intent from, String key, SyncableParser<T> creator) {
        if (from == null || from.getExtras() == null) return null;
        return fromString(from.getStringExtra(key), creator);
    }

    public static <T extends Thing> ArrayList<T> getList(Bundle from, String key, SyncableParser<T> creator) {
        if (from == null) return null;
        return fromStringArray(from.getStringArray(key), creator);
    }

    public static <T extends Thing> ArrayList<T> getList(Intent from, String key, SyncableParser<T> creator) {
        if (from == null || from.getExtras() == null) return null;
        return fromStringArray(from.getStringArrayExtra(key), creator);
    }


    // Get String Enums

    public static <T extends StringEnum> T getStringEnum(Bundle from, String key, TypeParser<T> creator) {
        if (from == null) return null;
        return enumFromString(from.getString(key), creator);
    }

    public static <T extends StringEnum> T getStringEnum(Intent from, String key, TypeParser<T> creator) {
        if (from == null || from.getExtras() == null) return null;
        return enumFromString(from.getStringExtra(key), creator);
    }

}
