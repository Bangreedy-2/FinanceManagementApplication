package com.bangreedy.splitsync.di

import androidx.room.Room
import com.bangreedy.splitsync.data.local.db.AppDatabase
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
    single { get<AppDatabase>().memberDao() }
    single { get<AppDatabase>().expenseDao() }
    single { get<AppDatabase>().paymentDao() }

    // -------------------------
    // REPOSITORIES
    // -------------------------

    single<GroupRepository> { GroupRepositoryImpl(get()) }
    single<MemberRepository> { MemberRepositoryImpl(get()) }
    single<ExpenseRepository> { ExpenseRepositoryImpl(get()) }
    single<PaymentRepository> { PaymentRepositoryImpl(get()) }

    single<AuthRepository> {
        AuthRepositoryImpl(
            com.bangreedy.splitsync.data.remote.firestore.auth.FirebaseAuthDataSource(get())
        )
    }

    // -------------------------
    // FIREBASE
    // -------------------------

    single { FirebaseAuth.getInstance() }
    single { FirebaseFirestore.getInstance() }

    single { com.bangreedy.splitsync.data.remote.firestore.FirestoreGroupDataSource(get()) }
    single { com.bangreedy.splitsync.data.remote.firestore.FirestoreMemberDataSource(get()) }
    single { com.bangreedy.splitsync.data.remote.firestore.FirestoreExpenseDataSource(get()) }
    single { com.bangreedy.splitsync.data.remote.firestore.FirestorePaymentDataSource(get()) }

    // -------------------------
    // USE CASES
    // -------------------------

    factory { ObserveGroupsUseCase(get()) }
    factory { CreateGroupUseCase(get()) }

    factory { ObserveMembersUseCase(get()) }
    factory { AddMemberUseCase(get()) }
    factory { ObserveGroupUseCase(get()) }

    factory { CreateExpenseEqualSplitUseCase(get()) }
    factory { ObserveExpensesUseCase(get()) }

    factory { ComputeGroupBalancesUseCase() }
    factory { SuggestSettlementsUseCase() }

    factory { ObservePaymentsUseCase(get()) }
    factory { CreatePaymentUseCase(get()) }

    factory { ObserveAuthStateUseCase(get()) }
    factory { SignInUseCase(get()) }
    factory { SignUpUseCase(get()) }
    factory { SignOutUseCase(get()) }

    // -------------------------
    // SYNC LAYER
    // -------------------------

    single {
        GroupSyncManager(
            remote = get(),
            groupDao = get()
        )
    }

    single {
        MemberSyncManager(
            remote = get(),
            memberDao = get()
        )
    }
    single {ExpenseSyncManager(
        groupDataSource = get(),
        expenseDataSource = get(),
        expenseDao = get()
    ) }

    single {
        PaymentSyncManager(
            groupDataSource = get(),
            paymentDataSource = get(),
            paymentDao = get()
        )
    }
    single {
        SyncCoordinator(
            groupSyncManager = get(),
            memberSyncManager = get(),
            expenseSyncManager = get(),
            paymentSyncManager = get()
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
            syncCoordinator = get()
        )
    }

    viewModel { (groupId: String) ->
        GroupDetailsViewModel(
            groupId = groupId,
            observeGroup = get(),
            observeMembers = get(),
            addMember = get(),
            syncCoordinator = get()
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
            syncCoordinator = get()
        )
    }
}
