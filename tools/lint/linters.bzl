"""Linter aspects for Calque."""

load("@aspect_rules_lint//lint:buildifier.bzl", "lint_buildifier_aspect")
load("@aspect_rules_lint//lint:lint_test.bzl", "lint_test")
load("//tools/lint:detekt.bzl", "lint_detekt_aspect")

detekt = lint_detekt_aspect(
    binary = Label("//tools/lint:detekt"),
    config = Label("//:detekt.yml"),
    args = ["--build-upon-default-config"],
)

detekt_test = lint_test(aspect = detekt)

buildifier = lint_buildifier_aspect(
    binary = Label("@buildifier_prebuilt//:buildifier"),
)

buildifier_test = lint_test(aspect = buildifier)
