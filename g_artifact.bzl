"""Helper macro for resolving Google/AndroidX Maven dependencies."""

load("@mabel//rules:mabel.bzl", "artifact")

_MAVEN_AND_GOOGLE = [
    "https://repo1.maven.org/maven2/",
    "https://maven.google.com/",
]

_COMMON_EXCLUDES = [
    "org.jetbrains.kotlinx:kotlinx-coroutines-core",
    "org.jetbrains.kotlinx:atomicfu",
]

def g_artifact(
        coordinate,
        maven_exclude_deps = _COMMON_EXCLUDES,
        repositories = _MAVEN_AND_GOOGLE,
        **kwargs):
    """Wraps mabel artifact with Google Maven repo and standard coroutine excludes."""
    return artifact(
        coordinate = coordinate,
        maven_exclude_deps = maven_exclude_deps,
        repositories = repositories,
        **kwargs
    )
