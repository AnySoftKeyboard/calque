"""Linter aspects for Calque."""

load("@aspect_rules_lint//lint:buildifier.bzl", "lint_buildifier_aspect")
load("@aspect_rules_lint//lint:ktlint.bzl", "lint_ktlint_aspect")
load("@aspect_rules_lint//lint:lint_test.bzl", "lint_test")

ktlint = lint_ktlint_aspect(
    binary = Label("@com_github_pinterest_ktlint//file"),
    editorconfig = Label("//:.editorconfig"),
    baseline_file = Label("//:ktlint-baseline.xml"),
)

ktlint_test = lint_test(aspect = ktlint)

buildifier = lint_buildifier_aspect(
    binary = Label("@buildifier_prebuilt//:buildifier"),
)

buildifier_test = lint_test(aspect = buildifier)
