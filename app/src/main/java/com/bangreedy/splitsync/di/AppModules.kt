package com.bangreedy.splitsync.di

import androidx.room.Room
import com.bangreedy.splitsync.data.local.db.AppDatabase
import com.bangreedy.splitsync.data.repository.GroupRepositoryImpl
import com.bangreedy.splitsync.domain.repository.GroupRepository
import com.bangreedy.splitsync.domain.usecase.CreateGroupUseCase
import com.bangreedy.splitsync.domain.usecase.ObserveGroupsUseCase
import com.bangreedy.splitsync.presentation.groups.GroupsViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {

    single {
        Room.databaseBuilder(get(), AppDatabase::class.java, "splitsync.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    single { get<AppDatabase>().groupDao() }

    single<GroupRepository> { GroupRepositoryImpl(get()) }

    factory { ObserveGroupsUseCase(get()) }
    factory { CreateGroupUseCase(get()) }

    viewModel { GroupsViewModel(get(), get()) }

    single { get<AppDatabase>().memberDao() }

    single<com.bangreedy.splitsync.domain.repository.MemberRepository> {
        com.bangreedy.splitsync.data.repository.MemberRepositoryImpl(get())
    }

    factory { com.bangreedy.splitsync.domain.usecase.ObserveMembersUseCase(get()) }
    factory { com.bangreedy.splitsync.domain.usecase.AddMemberUseCase(get()) }
    factory { com.bangreedy.splitsync.domain.usecase.ObserveGroupUseCase(get()) }

    viewModel { (groupId: String) ->
        com.bangreedy.splitsync.presentation.groupdetails.GroupDetailsViewModel(
            groupId = groupId,
            observeGroup = get(),
            observeMembers = get(),
            addMember = get()
        )
    }

    single { get<AppDatabase>().expenseDao() }

    single<com.bangreedy.splitsync.domain.repository.ExpenseRepository> {
        com.bangreedy.splitsync.data.repository.ExpenseRepositoryImpl(get())
    }
    factory { com.bangreedy.splitsync.domain.usecase.CreateExpenseEqualSplitUseCase(get()) }
    factory { com.bangreedy.splitsync.domain.usecase.ObserveExpensesUseCase(get()) }
    factory { com.bangreedy.splitsync.domain.usecase.ComputeGroupBalancesUseCase() }

    viewModel { (groupId: String) ->
        com.bangreedy.splitsync.presentation.addexpense.AddExpenseViewModel(
            groupId = groupId,
            observeMembers = get(),
            createExpenseEqualSplit = get()
        )
    }

    viewModel { (groupId: String) ->
        com.bangreedy.splitsync.presentation.groupdetails.LedgerViewModel(
            groupId = groupId,
            observeMembers = get(),
            observeExpenses = get(),
            computeBalances = get(),
            suggestSettlements = get()
        )
    }

    factory { com.bangreedy.splitsync.domain.usecase.SuggestSettlementsUseCase() }


}
