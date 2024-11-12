package com.pocket.util.android.sql

/**
 * The schema of an SQLite table.
 * @property name Table name
 * @property columns List of columns in order
 * @property columnsMap Map of columns for lookup by name
 */
data class TableSchema(val name: String, val columns: List<ColumnSchema>, val columnsMap: Map<String, ColumnSchema>)