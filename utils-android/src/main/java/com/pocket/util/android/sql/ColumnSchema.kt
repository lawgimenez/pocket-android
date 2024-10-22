package com.pocket.util.android.sql

/**
 * The schema of an SQLite column.
 */
data class ColumnSchema(val name: String, val type: String, val notnull: Boolean, val primary: Boolean)