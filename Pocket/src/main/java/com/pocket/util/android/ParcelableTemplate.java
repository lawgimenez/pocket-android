package com.pocket.util.android;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Boilerplate for a parcelable class
 */
public class ParcelableTemplate implements Parcelable {

    // Parcelling

    public ParcelableTemplate(Parcel in) {

    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {

    }

    public static final Parcelable.Creator<ParcelableTemplate> CREATOR = new Parcelable.Creator<ParcelableTemplate>() {
        @Override
        public ParcelableTemplate createFromParcel(Parcel in) {
            return new ParcelableTemplate(in);
        }

        @Override
        public ParcelableTemplate[] newArray(int size) {
            return new ParcelableTemplate[size];
        }
    };

}
