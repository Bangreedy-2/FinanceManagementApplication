package com.bangreedy.splitsync.di

import androidx.room.Room
import com.bangreedy.splitsync.data.local.db.AppDatabase
import com.bangreedy.splitsync.data.remote.firestore.*
import com.bangreedy.splitsync.data.repository.*
import com.bangreedy.splitsync.data.sync.*
import com.bangreedy.splitsync.domain.repository.*
import com.bangreedy.splitsync.domain.usecase.*
import com.bangreedy.splitsync.presentation.addexpense.AddExpenseViewModel
import com.bangreedy.splitsync.presentation.app.AppStateViewModel
import com.bangreedy.splitsync.presentation.auth.AuthViewModel
import com.bangreedy.splitsync.presentation.groupdetails.GroupDetailsViewModel
import com.bangreedy.splitsync.presentation.groupdetails.LedgerViewModel
import com.bangreedy.splitsync.presentation.groups.GroupsViewModel
import com.bangreedy.splitsync.presentation.invites.InvitesViewModel
import com.bangreedy.splitsync.presentation.invites.SendInviteViewModel
import com.bangreedy.splitsync.presentation.notifications.NotificationsViewModel
import com.bangreedy.splitsync.presentation.profile.CreateProfileViewModel
import com.bangreedy.splitsync.presentation.profile.ProfileGateViewModel
import com.bangreedy.splitsync.presentation.settleup.SettleUpViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {

    // -------------------------
    // DATABASE
    // -------------------------

    single {
        Room.databaseBuilder(
            get(),
            AppDatabase::class.java,
            "splitsync.db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    single { get<AppDatabase>().groupDao() }
    single { get<AppDatabase>().expenseDao() }
    single { get<AppDatabase>().paymentDao() }
    single { get<AppDatabase>().groupMemberDao() }
    single { get<AppDatabase>().userProfileDao() }
    single { get<AppDatabase>().notificationDao() }
    // -------------------------
    // FIREBASE
    // -------------------------

    single { FirebaseAuth.getInstance() }
    single { FirebaseFirestore.getInstance() }

    // -------------------------
    // FIRESTORE DATA SOURCES
    // -------------------------

    single { FirestoreGroupDataSource(get()) }

    single { FirestoreGroupMemberDataSource(get()) }

    single { FirestoreExpenseDataSource(get()) }
    single { FirestorePaymentDataSource(get()) }

    // Profiles
    single { FirestoreUserDataSource(get()) }               // profile create/claim username (you already use it)
    single { FirestoreUserProfileDataSource(get()) }        // fetch profiles by uid for sync

    // Invites + lookup
    single { FirestoreUserLookupDataSource(get()) }
    single { FirestoreInviteDataSource(get()) }

    // Notifications
    single { FirestoreNotificationDataSource(get()) }

    // -------------------------
    // REPOSITORIES
    // -------------------------

    single<GroupRepository> { GroupRepositoryImpl(get()) }

    // ✅ MemberRepository now reads from group_members JOIN user_profiles (via GroupMemberDao)
    single<MemberRepository> { MemberRepositoryImpl(get()) }

    single<ExpenseRepository> { ExpenseRepositoryImpl(get()) }
    single<PaymentRepository> { PaymentRepositoryImpl(get()) }

    single<AuthRepository> {
        AuthRepositoryImpl(
            com.bangreedy.splitsync.data.remote.firestore.auth.FirebaseAuthDataSource(get())
        )
    }

    single<UserProfileRepository> {
        // keep your current ctor signature as in your project
        UserProfileRepositoryImpl(get(), get())
    }

    single<InviteRepository> {
        InviteRepositoryImpl(get(), get(), get())
    }

    single<NotificationRepository> {
        NotificationRepositoryImpl(get(), get())
    }

    // -------------------------
    // USE CASES
    // -------------------------

    factory { ObserveGroupsUseCase(get()) }
    factory { CreateGroupUseCase(get()) }
    factory { ObserveGroupUseCase(get()) }

    // ✅ ObserveMembersUseCase stays, AddMemberUseCase removed
    factory { ObserveMembersUseCase(get()) }

    factory { CreateExpenseEqualSplitUseCase(get()) }
    factory { ObserveExpensesUseCase(get()) }

    factory { ComputeGroupBalancesUseCase() }
    factory { SuggestSettlementsUseCase() }

    factory { ObservePaymentsUseCase(get()) }
    factory { CreatePaymentUseCase(get(), get()) }

    factory { ObserveAuthStateUseCase(get()) }
    factory { SignInUseCase(get()) }
    factory { SignUpUseCase(get()) }
    factory { SignOutUseCase(get()) }

    // Profiles
    factory { ObserveMyProfileUseCase(get()) }
    factory { ClaimUsernameUseCase(get()) }
    factory { CheckUsernameAvailabilityUseCase(get()) }

    // Invites
    factory { ObserveInvitesUseCase(get()) }
    factory { SendInviteUseCase(get()) }
    factory { AcceptInviteUseCase(get()) }
    factory { DeclineInviteUseCase(get()) }

    //Notifications
    factory { ObserveNotificationsUseCase(get()) }
    factory { MarkNotificationReadUseCase(get()) }
    factory { ObserveUnreadNotificationsCountUseCase(get()) }

    // -------------------------
    // SYNC LAYER (NEW PIPELINE)
    // -------------------------

    single {
        GroupSyncManager(
            remote = get(),
            groupDao = get()
        )
    }

    single {
        GroupMemberSyncManager(
            remote = get(),
            groupMemberDao = get(),
            userProfileSyncManager = get()
        )
    }

    single {
        UserProfileSyncManager(
            remote = get(),
            groupMemberDao = get(),
            userProfileDao = get()
        )
    }

    // ✅ Expense/Payment sync now only depend on their own DS + DAO
    single {
        ExpenseSyncManager(
            expenseDataSource = get(),
            expenseDao = get()
        )
    }

    single {
        PaymentSyncManager(
            paymentDataSource = get(),
            paymentDao = get()
        )
    }

    single {
        NotificationSyncManager(
            remote = get(),
            dao = get()
        )
    }

    single {
        SyncCoordinator(
            groupSyncManager = get(),
            groupMemberSyncManager = get(),
            userProfileSyncManager = get(),
            expenseSyncManager = get(),
            paymentSyncManager = get(),
            notificationSyncManager = get()
        )
    }

    // -------------------------
    // VIEW MODELS
    // -------------------------

    viewModel {
        GroupsViewModel(
            observeGroups = get(),
            createGroup = get(),
            authRepository = get(),
            syncCoordinator = get(),
            observeUnreadNotificationsCount = get()
        )
    }

    // ✅ GroupDetailsViewModel should no longer receive addMember
    viewModel { (groupId: String) ->
        GroupDetailsViewModel(
            groupId = groupId,
            observeGroup = get(),
            observeMembers = get()
        )

    }

    viewModel { (groupId: String) ->
        AddExpenseViewModel(
            groupId = groupId,
            observeMembers = get(),
            createExpenseEqualSplit = get(),
            syncCoordinator = get()
        )
    }

    viewModel { (groupId: String) ->
        LedgerViewModel(
            groupId = groupId,
            observeMembers = get(),
            observeExpenses = get(),
            computeBalances = get(),
            suggestSettlements = get(),
            observePayments = get()
        )
    }

    viewModel { (groupId: String, fromId: String, toId: String, amountMinor: Long) ->
        SettleUpViewModel(
            groupId = groupId,
            initialFromId = fromId,
            initialToId = toId,
            initialAmountMinor = amountMinor,
            observeMembers = get(),
            observeExpenses = get(),
            computeBalances = get(),
            suggestSettlements = get(),
            createPayment = get(),
            observePayments = get(),
            syncCoordinator = get()
        )
    }

    viewModel {
        AuthViewModel(
            signIn = get(),
            signUp = get()
        )
    }

    viewModel {
        AppStateViewModel(
            observeAuthState = get(),
            syncCoordinator = get(),
            expenseDao = get()
        )
    }

    // Profile gate
    viewModel { ProfileGateViewModel(get()) }
    viewModel { CreateProfileViewModel(get(), get()) }

    // Invites
    viewModel { SendInviteViewModel(get()) }
    viewModel { (myUid: String) ->
        InvitesViewModel(
            myUid = myUid,
            observeInvites = get(),
            acceptInvite = get(),
            declineInvite = get(),
            userProfileRemote = get()
        )
    }

    viewModel { (uid: String) ->
        NotificationsViewModel(
            uid,
            observeNotifications = get(),
            markRead = get(),
        )
    }


}
