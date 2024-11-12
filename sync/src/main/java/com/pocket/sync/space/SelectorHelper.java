package com.pocket.sync.space;

import com.pocket.sync.space.mutable.MutableSpace;

public class SelectorHelper<T> {
			
	private final Space.Selector selector;
	private final T defaultValue;
	private Select<T, MutableSpace.Selector> mutable;
	
	public SelectorHelper(Space.Selector selector, T defaultValue) {
		this.selector = selector;
		this.defaultValue = defaultValue;
	}
	
	public SelectorHelper<T> mutable(Select<T, MutableSpace.Selector> select) {
		this.mutable = select;
		return this;
	}
	
	public T query() {
		if (selector == null) {
			return defaultValue;
		} else if (selector instanceof MutableSpace.Selector && mutable != null) {
			return mutable.select((MutableSpace.Selector) selector);
		} else {
			throw new RuntimeException("unsupported selector " + selector);
		}
	}
	
	public interface Select<T, S extends Space.Selector> {
		T select(S selector);
	}
	
}