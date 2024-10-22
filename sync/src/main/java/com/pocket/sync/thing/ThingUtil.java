package com.pocket.sync.thing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

/**
 * TODO Documentation
 */

public class ThingUtil {
	
	public static <T extends Thing> T merge(T base, T override) {
		if (override == null) {
			return base;
		} else if (base == null) {
			return override;
		}
		return (T) base.builder().set(override).build();
	}
	
	/**
	 * Compare two maps of {@link Thing}s, using a {@link Thing.Equality} flag.
	 */
	public static <K> boolean mapEquals(Thing.Equality equality, Map<K, ? extends Thing> c1, Map<K, ? extends Thing> c2) {
		if (equality == Thing.Equality.FLAT_SIZED) {
			return (c1 != null ? c1.size() : 0) == (c2 != null ? c2.size() : 0);
		} else if (c1 == null) {
			return c2 == null;
		} else if (c2 == null) {
			return false;
		} else if (c1 == c2) {
			return true;
		} else if (equality == Thing.Equality.IDENTITY) {
			return c1.equals(c2);
		} else if (equality == Thing.Equality.STATE || equality == Thing.Equality.STATE_DECLARED || equality == Thing.Equality.FLAT) {
			// Algorithm follows AbstractMap.equals
			if (c1.size() != c2.size()) {
				return false;
			}
			try {
				Iterator<? extends Map.Entry<K, ? extends Thing>> i = c1.entrySet().iterator();
				while (i.hasNext()) {
					Map.Entry<K,? extends Thing> e = i.next();
					K key = e.getKey();
					Thing value = e.getValue();
					if (value == null) {
						if (!(c2.get(key)==null && c2.containsKey(key))) {
							return false;
						}
					} else {
						if (!value.equals(equality == Thing.Equality.FLAT && value.isIdentifiable() ? Thing.Equality.IDENTITY : equality, c2.get(key))) {
							return false;
						}
					}
				}
			} catch (ClassCastException unused) {
				return false;
			} catch (NullPointerException unused) {
				return false;
			}
			return true;
		} else {
			throw new UnsupportedOperationException("unknown equality " + equality);
		}
	}
	
	/**
	 * Compare two lists of {@link Thing}s, using a {@link Thing.Equality} flag.
	 */
	public static boolean listEquals(Thing.Equality equality, List<? extends Thing> c1, List<? extends Thing> c2) {
		if (equality == Thing.Equality.FLAT_SIZED) {
			return (c1 != null ? c1.size() : 0) == (c2 != null ? c2.size() : 0);
		} else if (c1 == null) {
			return c2 == null;
		} else if (c2 == null) {
			return false;
		}
		if (equality == Thing.Equality.IDENTITY) {
			return c1.equals(c2);
		} else if (equality == Thing.Equality.STATE || equality == Thing.Equality.STATE_DECLARED || equality == Thing.Equality.FLAT) {
			// Follows AbstractList.equals
			ListIterator<? extends Thing> e1 = c1.listIterator();
			ListIterator<? extends Thing> e2 = c2.listIterator();
			while (e1.hasNext() && e2.hasNext()) {
				Thing o1 = e1.next();
				Object o2 = e2.next();
				if (!(o1==null ? o2==null : o1.equals(equality == Thing.Equality.FLAT && o1.isIdentifiable() ? Thing.Equality.IDENTITY : equality, o2)))
					return false;
			}
			return !(e1.hasNext() || e2.hasNext());
		} else {
			throw new UnsupportedOperationException("unknown equality " + equality);
		}
	}
	
	/**
	 * See {@link Thing#hashCode(Thing.Equality)} for more info and some usage rules.
	 */
	public static <K> int mapHashCode(Thing.Equality equality, Map<K, ? extends Thing> c) {
		if (equality == Thing.Equality.IDENTITY) {
			return c.hashCode();
		} else if (equality == Thing.Equality.STATE || equality == Thing.Equality.STATE_DECLARED || equality == Thing.Equality.FLAT) {
			// This follows AbstractMap.hashCode
			int h = 0;
			Iterator<? extends Map.Entry<K, ? extends Thing>> i = c.entrySet().iterator();
			while (i.hasNext()) {
				// Follows AbstractMap.Entry.hashCode
				Map.Entry<K, ? extends Thing> entry = i.next();
				K key = entry.getKey();
				Thing value = entry.getValue();
				h += (key == null ? 0 :   key.hashCode()) ^
						(value == null ? 0 : value.hashCode(equality == Thing.Equality.FLAT && value.isIdentifiable() ? Thing.Equality.IDENTITY : equality));
			}
			return h;
		} else {
			throw new UnsupportedOperationException("unknown equality " + equality);
		}
	}
	
	/**
	 * See {@link Thing#hashCode(Thing.Equality)} for more info and some usage rules.
	 */
	public static int collectionHashCode(Thing.Equality equality, Collection<? extends Thing> c) {
		if (equality == Thing.Equality.IDENTITY) {
			return c.hashCode();
		} else if (equality == Thing.Equality.STATE || equality == Thing.Equality.STATE_DECLARED || equality == Thing.Equality.FLAT) {
			// This follows the spec defined in List.hashCode
			int hashCode = 1;
			for (Thing e : c) {
				hashCode = 31*hashCode + (e==null ? 0 : e.hashCode(equality == Thing.Equality.FLAT && e.isIdentifiable() ? Thing.Equality.IDENTITY : equality));
			}
			return hashCode;
		} else {
			throw new UnsupportedOperationException("unknown equality " + equality);
		}
	}
	
	public static boolean fieldEquals(Thing.Equality e, Thing t1, Thing t2) {
		if (t1 == null) return t2 == null;
		if ((e == Thing.Equality.FLAT || e == Thing.Equality.FLAT_SIZED) && t1.isIdentifiable()) {
			return t1.equals(Thing.Equality.IDENTITY, t2);
		} else {
			return t1.equals(e, t2);
		}
	}
	
	/**
	 * See {@link Thing#hashCode(Thing.Equality)} for more info and some usage rules.
	 */
	public static int fieldHashCode(Thing.Equality e, Thing t) {
		if (t == null) return 0;
		if ((e == Thing.Equality.FLAT || e == Thing.Equality.FLAT_SIZED) && t.isIdentifiable()) {
			return t.hashCode(Thing.Equality.IDENTITY);
		} else {
			return t.hashCode(e);
		}
	}
	
	public static <T extends Thing> List<T> castList(List<Thing> list, Class<T> type) {
		if (list == null) return null;
		List<T> cast = new ArrayList<>(list.size());
		for (Thing t: list) {
			if (!type.isAssignableFrom(t.getClass())) throw new RuntimeException("element is wrong type");
			cast.add((T) t);
		}
		return cast;
	}
	
	public static <T extends Thing> Set<T> castSet(Set<Thing> set, Class<T> type) {
		if (set == null) return null;
		Set<T> cast = new HashSet<>(set.size());
		for (Thing t: set) {
			if (!type.isAssignableFrom(t.getClass())) throw new RuntimeException("element is wrong type");
			cast.add((T) t);
		}
		return cast;
	}
	
}
