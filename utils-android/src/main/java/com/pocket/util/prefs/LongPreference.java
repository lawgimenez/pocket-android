package com.pocket.util.prefs;

import io.reactivex.Observable;

public interface LongPreference extends Preference<Long> {
	/** The current value. If {@link #set(long)} has never been called, it returns the default value. Use {@link #isSet()} if you need to known. */
	long get();
	void set(long value);
	
	LongPreference NO_OP = new LongPreference() {
		@Override public boolean isSet() { return false;}
		@Override public Observable<Long> changes() { return Observable.never();}
		@Override public Observable<Long> getWithChanges() { return changes().startWith(get());}
		@Override public long get() { return 0;}
		@Override public void set(long value) {}
	};
}