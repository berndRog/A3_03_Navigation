package de.rogallab.mobile.test.domain.utilities

import de.rogallab.mobile.Globals.isComp
import de.rogallab.mobile.Globals.isDebug
import de.rogallab.mobile.Globals.isInfo
import de.rogallab.mobile.Globals.isVerbose
import de.rogallab.mobile.domain.utilities.compLogger
import de.rogallab.mobile.domain.utilities.debugLogger
import de.rogallab.mobile.domain.utilities.errorLogger
import de.rogallab.mobile.domain.utilities.formatMessage
import de.rogallab.mobile.domain.utilities.infoLogger
import de.rogallab.mobile.domain.utilities.verboseLogger
import de.rogallab.mobile.domain.utilities.warningLogger

fun setupConsoleLogger() {
   errorLogger = { tag, msg -> println("E/$tag: ${formatMessage(msg)}") }
   warningLogger = { tag, msg -> println("W/$tag: ${formatMessage(msg)}") }
   infoLogger = { tag, msg -> if(isInfo) println("I/$tag: ${formatMessage(msg)}") }
   debugLogger = { tag, msg -> if(isDebug) println("D/$tag: ${formatMessage(msg)}") }
   verboseLogger = { tag, msg -> if(isVerbose) println("V/$tag: $msg") }
   compLogger = { tag, msg -> if(isComp) println("D/$tag: $msg") }
}