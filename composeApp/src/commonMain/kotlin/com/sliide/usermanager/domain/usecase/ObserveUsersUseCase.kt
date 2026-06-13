package com.sliide.usermanager.domain.usecase

import com.sliide.usermanager.domain.model.User
import com.sliide.usermanager.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow

fun interface ObserveUsersUseCase {
    operator fun invoke(): Flow<List<User>>
}

class ObserveUsersUseCaseImpl(
    private val repository: UserRepository,
) : ObserveUsersUseCase {
    override fun invoke(): Flow<List<User>> = repository.observeUsers()
}
