package com.pocket.usecase

import com.pocket.repository.ItemRepository
import com.pocket.repository.UserRepository
import kotlinx.coroutines.flow.first
import javax.annotation.CheckReturnValue
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Save
@Inject constructor(
    private val itemRepository: ItemRepository,
    private val userRepository: UserRepository,
) {
    @CheckReturnValue
    suspend operator fun invoke(url: String): Result {
        val isLoggedIn = userRepository.isLoggedIn().first()
        if (isLoggedIn) {
            itemRepository.save(url)
            return Result.Success
        } else {
            return Result.NotLoggedIn
        }
    }

    enum class Result {
        Success, NotLoggedIn
    }
}
