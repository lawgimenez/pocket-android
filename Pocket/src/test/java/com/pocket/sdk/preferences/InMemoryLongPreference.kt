package com.pocket.sdk.preferences

import com.pocket.util.prefs.LongPref
import com.pocket.util.prefs.MemoryPrefStore

class InMemoryLongPreference : LongPref("key", 0L, MemoryPrefStore())
