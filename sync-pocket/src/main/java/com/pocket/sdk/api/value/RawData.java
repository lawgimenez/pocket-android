package com.pocket.sdk.api.value;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * A chunk of serialized data that should not be parsed into anything automatically.
 */
public class RawData {

	public final JsonNode data;

	public RawData(JsonNode data) {
		this.data = data;
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		
		RawData rawData = (RawData) o;
		
		return data.equals(rawData.data);
		
	}
	
	@Override
	public int hashCode() {
		return data.hashCode();
	}
	
	@Override
	public String toString() {
		return data.toString();
	}
}
