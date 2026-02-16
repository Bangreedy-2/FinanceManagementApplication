package com.bangreedy.splitsync.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bangreedy.splitsync.presentation.groups.GroupsScreen
import com.bangreedy.splitsync.presentation.groupdetails.GroupDetailsScreen

object Routes {
    const val GROUPS = "groups"
    const val GROUP_DETAILS = "group/{groupId}"
    fun groupDetails(groupId: String) = "group/$groupId"
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
            GroupDetailsScreen(groupId = groupId)
        }
    }
}
