package com.bangreedy.splitsync.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bangreedy.splitsync.presentation.addexpense.AddExpenseScreen
import com.bangreedy.splitsync.presentation.groups.GroupsScreen
import com.bangreedy.splitsync.presentation.groupdetails.GroupDetailsScreen

object Routes {
    const val GROUPS = "groups"
    const val GROUP_DETAILS = "group/{groupId}"
    fun groupDetails(groupId: String) = "group/$groupId"
    const val ADD_EXPENSE = "group/{groupId}/add-expense"
    fun addExpense(groupId: String) = "group/$groupId/add-expense"
}

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.GROUPS
    ) {
        composable(Routes.GROUPS) {
            GroupsScreen(
                onGroupClick = { groupId ->
                    navController.navigate(Routes.groupDetails(groupId))
                }
            )
        }

        composable(
            route = Routes.GROUP_DETAILS,
            arguments = listOf(navArgument("groupId") { type = NavType.StringType })
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: return@composable
            GroupDetailsScreen(
                groupId = groupId,
                onAddExpense = { navController.navigate(Routes.addExpense(groupId)) }
            )

        }

        composable(
            route = Routes.ADD_EXPENSE,
            arguments = listOf(navArgument("groupId") { type = NavType.StringType })
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: return@composable
            AddExpenseScreen(groupId = groupId, onBack = { navController.popBackStack() })
        }

    }
}
