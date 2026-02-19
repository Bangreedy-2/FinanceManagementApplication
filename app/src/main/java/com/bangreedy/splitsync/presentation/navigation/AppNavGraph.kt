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
import com.bangreedy.splitsync.presentation.settleup.SettleUpScreen

object Routes {
    const val GROUPS = "groups"
    const val GROUP_DETAILS = "group/{groupId}"
    fun groupDetails(groupId: String) = "group/$groupId"
    const val ADD_EXPENSE = "group/{groupId}/add-expense"
    fun addExpense(groupId: String) = "group/$groupId/add-expense"
    const val SETTLE_UP = "group/{groupId}/settle?fromId={fromId}&toId={toId}&amountMinor={amountMinor}"

    fun settleUp(groupId: String) = "group/$groupId/settle"

    fun settleUpPrefill(groupId: String, fromId: String, toId: String, amountMinor: Long) =
        "group/$groupId/settle?fromId=$fromId&toId=$toId&amountMinor=$amountMinor"
    const val INVITES = "invites"
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
                },
                onInvitesClick = {
                    navController.navigate(Routes.INVITES)
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
                onAddExpense = { navController.navigate(Routes.addExpense(groupId)) },
                onOpenSettleUp = { navController.navigate(Routes.settleUp(groupId)) },
                onSettleSuggestion = { fromId, toId, amountMinor ->
                    navController.navigate(Routes.settleUpPrefill(groupId, fromId, toId, amountMinor))
                }
            )

        }

        composable(
            route = Routes.ADD_EXPENSE,
            arguments = listOf(navArgument("groupId") { type = NavType.StringType })
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: return@composable
            AddExpenseScreen(groupId = groupId, onBack = { navController.popBackStack() })
        }


        composable(
            route = Routes.SETTLE_UP,
            arguments = listOf(
                navArgument("groupId") { type = NavType.StringType },
                navArgument("fromId") { type = NavType.StringType; defaultValue = "" },
                navArgument("toId") { type = NavType.StringType; defaultValue = "" },
                navArgument("amountMinor") { type = NavType.LongType; defaultValue = 0L }
            )
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: return@composable
            val fromId = backStackEntry.arguments?.getString("fromId").orEmpty()
            val toId = backStackEntry.arguments?.getString("toId").orEmpty()
            val amountMinor = backStackEntry.arguments?.getLong("amountMinor") ?: 0L

            SettleUpScreen(
                groupId = groupId,
                initialFromId = fromId,
                initialToId = toId,
                initialAmountMinor = amountMinor,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.INVITES) {
            val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            if (uid != null) {
                com.bangreedy.splitsync.presentation.invites.InvitesScreen(myUid = uid)
            }
        }

    }
}
