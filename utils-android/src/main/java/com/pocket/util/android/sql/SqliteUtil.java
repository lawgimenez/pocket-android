package com.pocket.util.android.sql;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tools for working with SQLite, specifically Android's implementation.
 */

public class SqliteUtil {
	
	/**
	 * SQLite's SQLITE_MAX_VARIABLE_NUMBER defaults to 999.
	 * https://www.sqlite.org/limits.html
	 */
	public static final int SQLITE_MAX_VARIABLE_NUMBER = 999;
	/**
	 * SQLite's SQLITE_MAX_COLUMN defaults to 2000
	 * https://www.sqlite.org/limits.html
	 */
	public static final int SQLITE_MAX_COLUMN = 2000;
	/**
	 * SQLite's SQLITE_MAX_SQL_LENGTH defaults to 1000000
	 * https://www.sqlite.org/limits.html
	 */
	public static final int SQLITE_MAX_SQL_LENGTH = 1000000;
	
	/** @return true if this table exists. */
	public static boolean tableExists(SQLiteDatabase db, String name) {
		Cursor cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name=?", new String[]{name});
		boolean exists = cursor.moveToNext();
		cursor.close();
		return exists;
	}
	
	/** @return The schema of this table or null if it doesn't exist. */
	public static TableSchema tableSchema(SQLiteDatabase db, String name) {
		List<ColumnSchema> columns = new ArrayList<>();
		Map<String, ColumnSchema> columnsMap = new HashMap<>();
		Cursor cursor = db.rawQuery("PRAGMA table_info(" + name + ")", null); // TODO REVIEW seems like it doesn't allow use of ? params here, investigate how to prevent sqlite injection if we can't use ? here
		int iName = cursor.getColumnIndex("name");
		int iType = cursor.getColumnIndex("type");
		int iNotNull = cursor.getColumnIndex("notnull");
		int iPk = cursor.getColumnIndex("pk");
		boolean exists = false;
		while (cursor.moveToNext()) {
			exists = true;
			boolean notnull = false;
			try {
				notnull = cursor.getInt(iNotNull) == 1;
			} catch (Throwable ignored) {} // Not expecting this, but not sure if there is a case where it would be null, seems like it should only be 0 or 1?
			boolean pk = false;
			try {
				pk = cursor.getInt(iPk) == 1;
			} catch (Throwable ignored) {} // Not expecting this, but not sure if there is a case where it would be null, seems like it should only be 0 or 1?
			
			ColumnSchema c = new ColumnSchema(cursor.getString(iName), cursor.getString(iType), notnull, pk);
			columns.add(c);
			columnsMap.put(c.getName(), c);
		}
		cursor.close();
		return exists ? new TableSchema(name, columns, columnsMap) : null;
	}
	
	/** @return the schema of this column within a table or null if the table or the column does not exist. */
	public static ColumnSchema columnSchema(SQLiteDatabase db, String tableName, String columnName) {
		TableSchema table = tableSchema(db, tableName);
		if (table != null) {
			for (ColumnSchema c : table.getColumns()) {
				if (columnName.equalsIgnoreCase(c.getName())) {
					return c;
				}
			}
		}
		return null;
	}
	
	/** @return true if the column exists within the table. (returns false if the table does not exist). */
	public static boolean columnExists(SQLiteDatabase db, String tableName, String columnName) {
		return columnSchema(db, tableName, columnName) != null;
	}
	
	public static StringBuilder placeholderList(int elements, StringBuilder sb) {
		sb = sb != null ? sb :new StringBuilder(elements*2);
		for (int i = 0; i < elements; i++) {
			if (i > 0) {
				sb.append(",");
			}
			sb.append("?");
		}
		return sb;
	}
	
	/**
	 * Converts a collection of Strings to a String[] suitable for use in {@link SQLiteDatabase#rawQuery(String, String[])}
	 * Use this for collections of strings.
	 * @see #toSelectArgsFromObjects(Collection) for objects that are not strings yet
	 */
	public static String[] toSelectArgsFromStrings(Collection<? extends String> args) {
		return args.toArray(new String[0]); // Note: I didn't test this, but I've seen  on the internets that a [0] is faster than [args.size()] :shrug: https://shipilev.net/blog/2016/arrays-wisdom-ancients/#_conclusion
	}
	
	/**
	 * Same as {@link #toSelectArgsFromObjects(Collection)} but internally can avoid allocating an iterator since List can use an indexed for loop.
	 */
	public static String[] toSelectArgsFromObjects(List args) {
		String[] strings = new String[args.size()];
		for (int i = 0, len = args.size(); i < len; i++) {
			Object o = args.get(i);
			strings[i] = o != null ? o.toString() : null;
		}
		return strings;
	}
	
	/**
	 * Converts a collection of Objects to a String[] suitable for use in {@link SQLiteDatabase#rawQuery(String, String[])}
	 * For each element it will invoke {@link Object#toString()} to get the string value (or leave as null if null)
	 * @see #toSelectArgsFromStrings(Collection) if your collection is already of strings for a quicker implementation
	 */
	public static String[] toSelectArgsFromObjects(Collection args) {
		String[] strings = new String[args.size()];
		int i = 0;
		for (Object arg : args) {
			strings[i++] = arg != null ? arg.toString() : null;
		}
		return strings;
	}

	public static Integer getInteger(SQLiteDatabase db, String query, String... args) {
		Integer value;
		Cursor cursor = db.rawQuery(query, args.length > 0 ? args : null);
		if (cursor.moveToNext() && !cursor.isNull(0)) {
			value = cursor.getInt(0);
		} else {
			value = null;
		}
		cursor.close();
		return value;
	}
}
