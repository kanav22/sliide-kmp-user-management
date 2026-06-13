package com.sliide.usermanager.data.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.sliide.usermanager.UserEntity
import com.sliide.usermanager.data.local.db.SliideDatabase
import com.sliide.usermanager.domain.model.Gender
import com.sliide.usermanager.domain.model.User
import com.sliide.usermanager.domain.model.UserStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant

class UserLocalDataSource(private val db: SliideDatabase) {

    fun observeUsers(): Flow<List<User>> =
        db.userQueries.selectAllOrderedByAddedAt()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { entities -> entities.map { it.toDomain() } }

    // Dispatchers.IO is JVM-only; Dispatchers.Default is the correct off-main dispatcher
    // for background work in KMP common code.
    suspend fun insertOrIgnore(
        id: Long,
        name: String,
        email: String,
        gender: String,
        status: String,
        addedAt: String,
    ) = withContext(Dispatchers.Default) {
        db.userQueries.insertOrIgnore(id, name, email, gender, status, addedAt)
    }

    suspend fun deleteById(id: Long) = withContext(Dispatchers.Default) {
        db.userQueries.deleteById(id)
    }

    private fun UserEntity.toDomain(): User = User(
        id = id,
        name = name,
        email = email,
        gender = if (gender.lowercase() == "female") Gender.FEMALE else Gender.MALE,
        status = if (status.lowercase() == "inactive") UserStatus.INACTIVE else UserStatus.ACTIVE,
        addedAt = Instant.parse(addedAt),
    )
}
