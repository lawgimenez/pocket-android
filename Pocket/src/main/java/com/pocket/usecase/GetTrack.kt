package com.pocket.usecase

import com.pocket.repository.ItemRepository
import com.pocket.sdk.tts.toTrack
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetTrack @Inject constructor(private val itemRepository: ItemRepository) {
    suspend operator fun invoke(url: String) = itemRepository.getItemOrThrow(url).toTrack()
}