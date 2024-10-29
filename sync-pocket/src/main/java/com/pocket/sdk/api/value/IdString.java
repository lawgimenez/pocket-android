package com.pocket.sdk.api.value;

import org.apache.commons.lang3.ObjectUtils;

/**
 * A String that represents some kind of id or guid
 */
public class IdString implements Comparable<IdString> {

	public final String id;

	public IdString(String id) {
		this.id = id;
	}
	
	@Override public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		IdString that = (IdString) o;
		if (id != null ? !id.equals(that.id) : that.id != null) return false;
		return true;
	}
	
	@Override public int hashCode() {
		return id != null ? id.hashCode() : 0;
	}
	
	@Override
	public String toString() {
		return id;
	}
	
	@Override
	public int compareTo(IdString o) {
		return ObjectUtils.compare(id, o.id);
	}
}
