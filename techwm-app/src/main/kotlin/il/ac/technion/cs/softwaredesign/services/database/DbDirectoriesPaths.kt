package il.ac.technion.cs.softwaredesign.services.database

/***
 * configuration of directories names in our database.
 */

class DbDirectoriesPaths {
    companion object {
        const val UsernameToPassword = "UsernameToPassword_"
        const val UsernameToUser = "UsernameToUser_"
        const val UsernameToToken = "UsernameToToken_"
        const val TokenToUsername = "TokenToUsername_"
        const val DeletedTokens = "DeletedTokens_"
        const val SerialNumberToId = "SerialNumberToId_"
        const val IdToResourceName = "IdToResourceName_"
        const val UsernameIsRevoked = "IsUserNameRevoked_"
        const val JobIdToJobInfo = "JobIdToJobInfo_"
        const val AccessRequests = "AccessRequest_"
    }
}