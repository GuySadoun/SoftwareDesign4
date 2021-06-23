package il.ac.technion.cs.softwaredesign

/**
 * Exception relating to permission errors when accessing the TechWM API.
 */
class PermissionException: RuntimeException()
class AdminUserAlreadyExistsException : RuntimeException()
class TokenWasDeletedAndCantBeReusedException : RuntimeException()
class TokenDoesNotExistException : RuntimeException()
class UserNameDoesNotExistException : RuntimeException()
class CanNotChangePermissionException : RuntimeException()
class CanNotChangeUserAccountTypeException : RuntimeException()
class IdAlreadyExistException : RuntimeException()
class UserIsAlreadyRevokedException : RuntimeException()
class ResourceDoesNotExistsException : RuntimeException()
class ResourceRequestIsIllegalException : RuntimeException()
class JobAlreadyExistInDbException : RuntimeException()
