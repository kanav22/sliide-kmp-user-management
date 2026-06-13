package com.sliide.usermanager.presentation.userlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sliide.usermanager.config.goRestToken
import com.sliide.usermanager.domain.repository.UserListError
import com.sliide.usermanager.domain.repository.ValidationException
import com.sliide.usermanager.domain.usecase.AddUserUseCase
import com.sliide.usermanager.domain.usecase.DeleteUserUseCase
import com.sliide.usermanager.domain.usecase.GetLastPageUsersUseCase
import com.sliide.usermanager.domain.usecase.ObserveUsersUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class UserListViewModel(
    private val observeUsers: ObserveUsersUseCase,
    private val getLastPageUsers: GetLastPageUsersUseCase,
    private val addUserUseCase: AddUserUseCase,
    private val deleteUserUseCase: DeleteUserUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(UserListState())
    val state: StateFlow<UserListState> = _state.asStateFlow()

    private val _effects = Channel<UserListEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            observeUsers().collect { users ->
                _state.update { it.copy(users = users) }
            }
        }

        if (goRestToken.isBlank()) {
            _state.update { it.copy(error = UserListError.MissingToken) }
        } else {
            loadUsers()
        }
    }

    fun process(intent: UserListIntent) {
        when (intent) {
            UserListIntent.LoadUsers        -> loadUsers()
            UserListIntent.RefreshUsers     -> refreshUsers()
            is UserListIntent.RequestDelete -> _state.update { it.copy(showDeleteDialogFor = intent.user) }
            UserListIntent.DismissDeleteDialog -> _state.update { it.copy(showDeleteDialogFor = null) }
            is UserListIntent.ConfirmDelete -> confirmDelete(intent.user)
            is UserListIntent.UndoDelete    -> undoDelete(intent.user)
            UserListIntent.FinalizeDelete   -> finalizeDelete()
            is UserListIntent.SelectUser    -> _state.update { it.copy(selectedUser = intent.user) }
            UserListIntent.ClearSelectedUser -> _state.update { it.copy(selectedUser = null) }
            is UserListIntent.UpdateFormName  -> updateFormName(intent.value)
            is UserListIntent.UpdateFormEmail -> updateFormEmail(intent.value)
            is UserListIntent.UpdateFormGender -> _state.update {
                it.copy(formState = it.formState.copy(gender = intent.value))
            }
            is UserListIntent.UpdateFormStatus -> _state.update {
                it.copy(formState = it.formState.copy(status = intent.value))
            }
            UserListIntent.SubmitAddUser    -> submitAddUser()
            UserListIntent.DismissAddUserSheet -> _state.update { it.copy(formState = AddUserFormState()) }
        }
    }

    private fun loadUsers() {
        viewModelScope.launch {
            val cacheIsEmpty = _state.value.users.isEmpty()
            if (cacheIsEmpty) _state.update { it.copy(isLoading = true) }
            val result = getLastPageUsers()
            _state.update { it.copy(isLoading = false, error = if (result.isFailure) classifyError(result.exceptionOrNull()) else null) }
        }
    }

    private fun refreshUsers() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }
            val result = getLastPageUsers()
            _state.update {
                it.copy(
                    isRefreshing = false,
                    error = if (result.isFailure) classifyError(result.exceptionOrNull()) else null,
                )
            }
        }
    }

    private fun confirmDelete(user: com.sliide.usermanager.domain.model.User) {
        _state.update {
            it.copy(
                showDeleteDialogFor = null,
                pendingDeleteUser = user,
            )
        }
        viewModelScope.launch { _effects.send(UserListEffect.ShowDeleteSnackbar(user)) }
    }

    private fun undoDelete(user: com.sliide.usermanager.domain.model.User) {
        _state.update { it.copy(pendingDeleteUser = null) }
    }

    private fun finalizeDelete() {
        val user = _state.value.pendingDeleteUser ?: return
        viewModelScope.launch {
            val result = deleteUserUseCase(user.id)
            if (result.isSuccess) {
                _state.update { it.copy(pendingDeleteUser = null) }
            } else {
                _state.update { it.copy(pendingDeleteUser = null) }
                _effects.send(UserListEffect.ShowError("Could not delete user. Please try again."))
            }
        }
    }

    private fun updateFormName(value: String) {
        val error = if (value.length >= 2 && value.matches(Regex("^[\\p{L}\\s'\\-.]{2,100}$"))) null
                    else if (value.isEmpty()) null
                    else "Name must be at least 2 characters"
        _state.update { it.copy(formState = it.formState.copy(name = value, nameError = error)) }
    }

    private fun updateFormEmail(value: String) {
        val error = if (value.isEmpty()) null
                    else if (value.matches(Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$"))) null
                    else "Enter a valid email address"
        _state.update { it.copy(formState = it.formState.copy(email = value, emailError = error)) }
    }

    private fun submitAddUser() {
        val form = _state.value.formState
        if (form.isSubmitting) return

        val nameError = when {
            form.name.isBlank() -> "Name is required"
            !form.name.matches(Regex("^[\\p{L}\\s'\\-.]{2,100}$")) -> "Enter a valid name"
            else -> null
        }
        val emailError = when {
            form.email.isBlank() -> "Email is required"
            !form.email.matches(Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$")) -> "Enter a valid email address"
            else -> null
        }

        if (nameError != null || emailError != null) {
            _state.update {
                it.copy(formState = it.formState.copy(nameError = nameError, emailError = emailError))
            }
            return
        }

        _state.update { it.copy(formState = it.formState.copy(isSubmitting = true, submitError = null)) }

        viewModelScope.launch {
            val result = addUserUseCase(form.name, form.email, form.gender, form.status)
            result.fold(
                onSuccess = { user ->
                    _state.update { it.copy(formState = AddUserFormState()) }
                    _effects.send(UserListEffect.UserAddedSuccess)
                },
                onFailure = { throwable ->
                    when (throwable) {
                        is ValidationException -> {
                            val nameErr  = throwable.errors.firstOrNull { it.field == "name" }?.message
                            val emailErr = throwable.errors.firstOrNull { it.field == "email" }?.message
                            _state.update {
                                it.copy(formState = it.formState.copy(
                                    nameError    = nameErr,
                                    emailError   = emailErr,
                                    isSubmitting = false,
                                ))
                            }
                        }
                        else -> {
                            val msg = if (throwable.message?.contains("Unable to resolve host") == true ||
                                         throwable.message?.contains("Network") == true)
                                "No internet. Try again when online."
                            else "Server error. Please try again."
                            _state.update {
                                it.copy(formState = it.formState.copy(
                                    submitError  = msg,
                                    isSubmitting = false,
                                ))
                            }
                        }
                    }
                }
            )
        }
    }

    private fun classifyError(e: Throwable?): UserListError {
        val msg = e?.message ?: ""
        return if (msg.contains("Unable to resolve host") || msg.contains("Network") || msg.contains("timeout"))
            UserListError.NoInternet
        else UserListError.Generic
    }
}
