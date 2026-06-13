package com.sliide.usermanager.di

import com.sliide.usermanager.config.goRestToken
import com.sliide.usermanager.data.local.UserLocalDataSource
import com.sliide.usermanager.data.remote.UserApiService
import com.sliide.usermanager.data.repository.UserRepositoryImpl
import com.sliide.usermanager.domain.repository.UserRepository
import com.sliide.usermanager.domain.usecase.AddUserUseCase
import com.sliide.usermanager.domain.usecase.AddUserUseCaseImpl
import com.sliide.usermanager.domain.usecase.DeleteUserUseCase
import com.sliide.usermanager.domain.usecase.DeleteUserUseCaseImpl
import com.sliide.usermanager.domain.usecase.GetLastPageUsersUseCase
import com.sliide.usermanager.domain.usecase.GetLastPageUsersUseCaseImpl
import com.sliide.usermanager.domain.usecase.ObserveUsersUseCase
import com.sliide.usermanager.domain.usecase.ObserveUsersUseCaseImpl
import com.sliide.usermanager.data.local.db.SliideDatabase
import com.sliide.usermanager.presentation.userlist.UserListViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val networkModule = module {
    single {
        HttpClient(get<HttpClientEngine>()) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 10_000
                connectTimeoutMillis = 10_000
            }
            install(HttpRequestRetry) {
                retryOnServerErrors(maxRetries = 3)
                exponentialDelay()
            }
            defaultRequest {
                header(HttpHeaders.Authorization, "Bearer $goRestToken")
            }
        }
    }
    single { UserApiService(get()) }
}

val databaseModule = module {
    single { SliideDatabase(get()) }
    single { UserLocalDataSource(get()) }
}

val repositoryModule = module {
    single<UserRepository> { UserRepositoryImpl(get(), get()) }
}

val domainModule = module {
    single<ObserveUsersUseCase>     { ObserveUsersUseCaseImpl(get()) }
    single<GetLastPageUsersUseCase> { GetLastPageUsersUseCaseImpl(get()) }
    single<AddUserUseCase>          { AddUserUseCaseImpl(get()) }
    single<DeleteUserUseCase>       { DeleteUserUseCaseImpl(get()) }
}

val presentationModule = module {
    viewModel { UserListViewModel(get(), get(), get(), get()) }
}

fun initKoin(
    additionalModules: List<Module> = emptyList(),
    appDeclaration: org.koin.core.KoinApplication.() -> Unit = {},
) {
    org.koin.core.context.startKoin {
        appDeclaration()
        modules(networkModule, databaseModule, repositoryModule, domainModule, presentationModule)
        modules(additionalModules)
    }
}
