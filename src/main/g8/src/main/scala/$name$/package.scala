package $name$

infix type Or[A, B] = Either[A, B]

enum AuthErr(val msg: String):
  case NoHeader              extends AuthErr("No Authorization header provided in request.")
  case InvalidHeader         extends AuthErr("Authorization header not valid.")
  case SessionNotFound       extends AuthErr("Session not found.")
  case SessionExpired        extends AuthErr("Session expired.")
  case IncorrectLoginDetails extends AuthErr("Incorrect login details.")

  // TODO: By default pg row level security won't throw an exception it will just
  //       return an empty result. I can change this by writing a pg function to
  //       raise an exception like this: https://stackoverflow.com/a/58524300.
  //       This type would then be useful.
  case InsufficientPermissions extends AuthErr("Not permitted to perfom this action.")
