package com.pocket.app.reader.queue

import com.pocket.util.android.NoObfuscation

/**
 * Used when first navigating to the ReaderFragment.  Tells us what type of queue manager we should
 * initially build
 */
enum class InitialQueueType : NoObfuscation {
    Empty,
    SavesList
}