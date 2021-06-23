package il.ac.technion.cs.softwaredesign

enum class PermissionLevel {
    USER,
    OPERATOR,
    ADMINISTRATOR
}

/**
 * Account types associate users with types of resources they can request, in the following hierarchy:
 * * [DEFAULT] account types can only request up to two CPU cores.
 * * [RESEARCH] account types can request up to two GPU resources resources and two CPU cores.
 * * [ROOT] account types can request unlimited resources.
 */
enum class AccountType {
    DEFAULT,
    RESEARCH,
    ROOT
}

/**
 * A class holding a single user's information in the system.
 *
 * @property username A unique username identifying the user throughout the system
 * @property account An association of the user to an [AccountType]. Account types are used to restrict usage of
 * certain resources throughout the system
 * @property permissionLevel A permission level for the user. Permissions are used to restrict access to certain
 * actions in the system, for example to revoke or promote permissions of other users.
 */
data class User(val username: String, val account: AccountType, val permissionLevel: PermissionLevel)
