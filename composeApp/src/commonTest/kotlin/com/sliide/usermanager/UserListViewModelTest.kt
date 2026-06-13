package com.sliide.usermanager

import app.cash.turbine.test
import com.sliide.usermanager.domain.model.Gender
import com.sliide.usermanager.domain.model.UserStatus
import com.sliide.usermanager.domain.repository.UserListError
import com.sliide.usermanager.domain.repository.ValidationError
import com.sliide.usermanager.domain.repository.ValidationException
import com.sliide.usermanager.domain.usecase.AddUserUseCaseImpl
import com.sliide.usermanager.domain.usecase.DeleteUserUseCaseImpl
import com.sliide.usermanager.domain.usecase.GetLastPageUsersUseCaseImpl
import com.sliide.usermanager.domain.usecase.ObserveUsersUseCaseImpl
import com.sliide.usermanager.fakes.FakeUserRepository
import com.sliide.usermanager.fakes.testUser
import com.sliide.usermanager.presentation.userlist.UserListEffect
import com.sliide.usermanager.presentation.userlist.UserListIntent
import com.sliide.usermanager.presentation.userlist.UserListViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class UserListViewModelTest {

    private lateinit var repo: FakeUserRepository
    private lateinit var vm: UserListViewModel

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        repo = FakeUserRepository()
        vm = UserListViewModel(
            observeUsers      = ObserveUsersUseCaseImpl(repo),
            getLastPageUsers  = GetLastPageUsersUseCaseImpl(repo),
            addUserUseCase    = AddUserUseCaseImpl(repo),
            deleteUserUseCase = DeleteUserUseCaseImpl(repo),
        )
    }

    @AfterTest
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `LoadUsers with empty DB and network success shows users`() = runTest {
        repo.refreshResult = Result.success(Unit)
        repo.emit(listOf(testUser))
        vm.process(UserListIntent.LoadUsers)
        val state = vm.state.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertTrue(state.users.isNotEmpty())
    }

    @Test
    fun `LoadUsers with empty DB and network failure shows NoInternet`() = runTest {
        repo.refreshResult = Result.failure(Exception("Unable to resolve host"))
        vm.process(UserListIntent.LoadUsers)
        val state = vm.state.value
        assertFalse(state.isLoading)
        assertEquals(UserListError.NoInternet, state.error)
    }

    @Test
    fun `LoadUsers with populated DB and network failure shows cached users with error`() = runTest {
        repo.emit(listOf(testUser))
        repo.refreshResult = Result.failure(Exception("Unable to resolve host"))
        vm.process(UserListIntent.LoadUsers)
        val state = vm.state.value
        assertTrue(state.users.isNotEmpty())
        assertNotNull(state.error)
    }

    @Test
    fun `RequestDelete sets showDeleteDialogFor`() = runTest {
        vm.process(UserListIntent.RequestDelete(testUser))
        assertEquals(testUser, vm.state.value.showDeleteDialogFor)
    }

    @Test
    fun `DismissDeleteDialog clears showDeleteDialogFor`() = runTest {
        vm.process(UserListIntent.RequestDelete(testUser))
        vm.process(UserListIntent.DismissDeleteDialog)
        assertNull(vm.state.value.showDeleteDialogFor)
    }

    @Test
    fun `ConfirmDelete sets pendingDeleteUser and clears dialog`() = runTest {
        vm.process(UserListIntent.ConfirmDelete(testUser))
        val state = vm.state.value
        assertNull(state.showDeleteDialogFor)
        assertEquals(testUser, state.pendingDeleteUser)
    }

    @Test
    fun `UndoDelete clears pendingDeleteUser without calling API`() = runTest {
        var deleteCalled = false
        repo.deleteResult = Result.success(Unit).also { deleteCalled = true }
        vm.process(UserListIntent.ConfirmDelete(testUser))
        deleteCalled = false
        vm.process(UserListIntent.UndoDelete(testUser))
        assertNull(vm.state.value.pendingDeleteUser)
        assertFalse(deleteCalled)
    }

    @Test
    fun `FinalizeDelete calls delete use case`() = runTest {
        repo.deleteResult = Result.success(Unit)
        vm.process(UserListIntent.ConfirmDelete(testUser))
        vm.process(UserListIntent.FinalizeDelete)
        assertNull(vm.state.value.pendingDeleteUser)
    }

    @Test
    fun `FinalizeDelete with no pendingDeleteUser is a no-op`() = runTest {
        vm.process(UserListIntent.FinalizeDelete)
        assertNull(vm.state.value.pendingDeleteUser)
    }

    @Test
    fun `FinalizeDelete with API failure restores user and emits ShowError`() = runTest {
        repo.deleteResult = Result.failure(Exception("Network error"))
        vm.process(UserListIntent.ConfirmDelete(testUser))
        vm.effects.test {
            vm.process(UserListIntent.FinalizeDelete)
            val snackbarEffect = awaitItem()
            assertTrue(snackbarEffect is UserListEffect.ShowDeleteSnackbar)
            val errorEffect = awaitItem()
            assertTrue(errorEffect is UserListEffect.ShowError)
        }
    }

    @Test
    fun `SubmitAddUser with valid form emits UserAddedSuccess`() = runTest {
        vm.process(UserListIntent.UpdateFormName("Alice Smith"))
        vm.process(UserListIntent.UpdateFormEmail("alice@example.com"))
        vm.effects.test {
            vm.process(UserListIntent.SubmitAddUser)
            val effect = awaitItem()
            assertEquals(UserListEffect.UserAddedSuccess, effect)
        }
    }

    @Test
    fun `SubmitAddUser when already submitting is idempotent`() = runTest {
        vm.process(UserListIntent.UpdateFormName("Alice Smith"))
        vm.process(UserListIntent.UpdateFormEmail("alice@example.com"))
        vm.process(UserListIntent.SubmitAddUser)
        val isSubmittingBefore = vm.state.value.formState.isSubmitting
        vm.process(UserListIntent.SubmitAddUser)
    }

    @Test
    fun `SubmitAddUser network failure sets submitError`() = runTest {
        repo.addResult = Result.failure(Exception("Unable to resolve host"))
        vm.process(UserListIntent.UpdateFormName("Alice Smith"))
        vm.process(UserListIntent.UpdateFormEmail("alice@example.com"))
        vm.process(UserListIntent.SubmitAddUser)
        val formState = vm.state.value.formState
        assertNotNull(formState.submitError)
        assertFalse(formState.isSubmitting)
    }

    @Test
    fun `SubmitAddUser 422 with email error sets emailError`() = runTest {
        repo.addResult = Result.failure(
            ValidationException(listOf(ValidationError("email", "has already been taken")))
        )
        vm.process(UserListIntent.UpdateFormName("Alice Smith"))
        vm.process(UserListIntent.UpdateFormEmail("alice@example.com"))
        vm.process(UserListIntent.SubmitAddUser)
        val formState = vm.state.value.formState
        assertNotNull(formState.emailError)
        assertNull(formState.nameError)
        assertNull(formState.submitError)
    }

    @Test
    fun `SubmitAddUser 422 with name error sets nameError`() = runTest {
        repo.addResult = Result.failure(
            ValidationException(listOf(ValidationError("name", "can't be blank")))
        )
        vm.process(UserListIntent.UpdateFormName("Alice Smith"))
        vm.process(UserListIntent.UpdateFormEmail("alice@example.com"))
        vm.process(UserListIntent.SubmitAddUser)
        val formState = vm.state.value.formState
        assertNotNull(formState.nameError)
        assertNull(formState.emailError)
    }

    @Test
    fun `SubmitAddUser 422 with both errors sets both`() = runTest {
        repo.addResult = Result.failure(
            ValidationException(listOf(
                ValidationError("name", "can't be blank"),
                ValidationError("email", "is invalid"),
            ))
        )
        vm.process(UserListIntent.UpdateFormName("Alice Smith"))
        vm.process(UserListIntent.UpdateFormEmail("alice@example.com"))
        vm.process(UserListIntent.SubmitAddUser)
        val formState = vm.state.value.formState
        assertNotNull(formState.nameError)
        assertNotNull(formState.emailError)
    }

    @Test
    fun `DismissAddUserSheet resets formState`() = runTest {
        vm.process(UserListIntent.UpdateFormName("Alice"))
        vm.process(UserListIntent.UpdateFormEmail("alice@example.com"))
        vm.process(UserListIntent.DismissAddUserSheet)
        val formState = vm.state.value.formState
        assertEquals("", formState.name)
        assertEquals("", formState.email)
        assertNull(formState.nameError)
        assertNull(formState.emailError)
        assertNull(formState.submitError)
    }

    @Test
    fun `RefreshUsers sets isRefreshing while in flight then clears it`() = runTest {
        repo.refreshResult = Result.success(Unit)
        vm.process(UserListIntent.RefreshUsers)
        val state = vm.state.value
        assertFalse(state.isRefreshing)
    }
}
