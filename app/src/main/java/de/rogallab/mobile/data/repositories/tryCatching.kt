package de.rogallab.mobile.data.repositories

fun <R> tryCatching(
      repositoryAction: () -> R,
   ): Result<R> =
      try {
         Result.success(repositoryAction())
      }
      catch (e: Exception) {
         Result.failure(e)
      }