package de.rogallab.mobile.domain.utilities

import android.util.Log
import de.rogallab.mobile.Globals.isComp
import de.rogallab.mobile.Globals.isDebug
import de.rogallab.mobile.Globals.isInfo
import de.rogallab.mobile.Globals.isVerbose

// Logger as function type
typealias LogFunction = (tag: String, message: String) -> Unit

// internal logger as Android logger
internal var errorLogger: LogFunction = { tag, msg -> Log.e(tag, formatMessage(msg)) }
internal var warningLogger: LogFunction = { tag, msg -> Log.w(tag, formatMessage(msg)) }
internal var infoLogger: LogFunction = { tag, msg -> if(isInfo) Log.i(tag, formatMessage(msg)) }
internal var debugLogger: LogFunction = { tag, msg -> if(isDebug) Log.d(tag, formatMessage(msg)) }
internal var verboseLogger: LogFunction = { tag, msg -> if(isVerbose) Log.v(tag, msg) }
internal var compLogger: LogFunction = { tag, msg -> if(isComp) Log.d(tag, msg) }

// public functions
fun logError(tag: String, message: String) = errorLogger(tag, message)
fun logWarning(tag: String, message: String) = warningLogger(tag, message)
fun logInfo(tag: String, message: String) = infoLogger(tag, message)
fun logDebug(tag: String, message: String) = debugLogger(tag, message)
fun logVerbose(tag: String, message: String) = verboseLogger(tag, message)
fun logComp(tag: String, message: String) = compLogger(tag, message)

internal fun formatMessage(message: String) =
   String.format("%-110s %s", message, Thread.currentThread().toString())
