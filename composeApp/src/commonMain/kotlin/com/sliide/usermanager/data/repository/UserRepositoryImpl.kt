package com.sliide.usermanager.data.repository

import com.sliide.usermanager.data.local.UserLocalDataSource
import com.sliide.usermanager.data.remote.UserApiService
import com.sliide.usermanager.domain.model.Gender
import com.sliide.usermanager.domain.model.User
import com.sliide.usermanager.domain.model.UserStatus
import com.sliide.usermanager.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Clock

class UserRepositoryImpl(
    private val apiService: UserApiService,
    private val localDataSource: UserLocalDataSource,
) : UserRepository {

    override fun observeUsers(): Flow<List<User>> = localDataSource.observeUsers()

    override suspend fun refreshLastPage(): Result<Unit> = runCatching {
        val users = apiService.fetchLastPage()
        val now = Clock.System.now()
        // INSERT OR IGNORE preserves the original addedAt for rows already in the cache.
        // A refresh must not reset when the user was first seen locally.
        users.forEach { dto ->
            localDataSource.insertOrIgnore(
                id = dto.id,
                name = dto.name,
                email = dto.email,
                gender = dto.gender,
                status = dto.status,
                addedAt = now.toString(),
            )
        }
    }

    override suspend fun addUser(
        name: String,
        email: String,
        gender: Gender,
        status: UserStatus,
    ): Result<User> = runCatching {
        val user = apiService.createUser(name, email, gender.apiValue, status.apiValue)
        localDataSource.insertOrIgnore(
            id = user.id,
            name = user.name,
            email = user.email,
            gender = user.gender.apiValue,
            status = user.status.apiValue,
            addedAt = user.addedAt.toString(),
        )
        user
    }

    override suspend fun deleteUser(id: Long): Result<Unit> = runCatching {
        apiService.deleteUser(id)
        localDataSource.deleteById(id)
    }
}
