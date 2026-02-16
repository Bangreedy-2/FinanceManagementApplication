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
        Room.databaseBuilder(
            get(),
            AppDatabase::class.java,
            "splitsync.db"
        ).build()
    }

    single { get<AppDatabase>().groupDao() }

    single<GroupRepository> { GroupRepositoryImpl(get()) }

    factory { ObserveGroupsUseCase(get()) }
    factory { CreateGroupUseCase(get()) }

    viewModel { GroupsViewModel(get(), get()) }
}
