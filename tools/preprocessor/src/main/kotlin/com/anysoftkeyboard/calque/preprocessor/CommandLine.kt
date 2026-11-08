package com.anysoftkeyboard.calque.preprocessor

import java.io.File

private const val OPTION_PREFIX = "--"

/** Parses `--name value` pairs; flags without a value and stray arguments are ignored. */
internal fun parseCommandLineArgs(args: List<String>): Map<String, String> {
    val options = mutableMapOf<String, String>()
    var i = 0
    while (i < args.size) {
        val arg = args[i]
        val hasValue = i + 1 < args.size && !args[i + 1].startsWith(OPTION_PREFIX)
        if (arg.startsWith(OPTION_PREFIX) && hasValue) {
            options[arg] = args[i + 1]
            i += 2
        } else {
            i++
        }
    }
    return options
}

/**
 * Resolves [path] against `BUILD_WORKING_DIRECTORY` (set by `bazel run`) when it is relative, so
 * that relative paths behave as the user typed them in their shell.
 */
internal fun resolveFile(path: String?): File? {
    if (path == null) return null
    val file = File(path)
    if (file.isAbsolute) return file
    val workingDir = System.getenv("BUILD_WORKING_DIRECTORY")
    return if (workingDir != null) File(workingDir, path) else file
}

/** Thrown when a command line is missing a required option or has an invalid value. */
internal class UsageException(message: String) : IllegalArgumentException(message)

/** Returns the file for a required path option, or throws a [UsageException]. */
internal fun Map<String, String>.requireFile(name: String): File =
    resolveFile(this[name]) ?: throw UsageException("Missing required option $name")

/** Returns the positive integer value of [name], or [default] when it is absent. */
internal fun Map<String, String>.positiveInt(name: String, default: Int): Int {
    val raw = this[name] ?: return default
    val value = raw.toIntOrNull()
    if (value == null || value <= 0) throw UsageException("Option $name must be a positive integer")
    return value
}
