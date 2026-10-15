"""API for declaring a Detekt lint aspect that visits Kotlin rules."""

load(
    "@aspect_rules_lint//lint/private:lint_aspect.bzl",
    "LintOptionsInfo",
    "filter_srcs",
    "noop_lint_action",
    "output_files",
    "should_visit",
)

_MNEMONIC = "AspectRulesLintDetekt"

def detekt_action(
        ctx,
        executable,
        srcs,
        config,
        baseline,
        human_out,
        machine_out,
        human_exit_code = None,
        machine_exit_code = None,
        args = []):
    """Run Detekt as an action under Bazel."""
    inputs = list(srcs)
    outputs = [human_out, machine_out]

    action_args = ctx.actions.args()
    action_args.add("--input")
    action_args.add_joined(srcs, join_with = ",")

    if config:
        inputs.append(config)
        action_args.add("--config")
        action_args.add(config.path)

    if baseline:
        inputs.append(baseline)
        action_args.add("--baseline")
        action_args.add(baseline.path)

    action_args.add("--report")
    action_args.add("sarif:{}".format(machine_out.path))
    action_args.add_all(args)

    command = """
{detekt} "$@" >"{human_out}" 2>&1
EC=$?
if [ ! -f "{machine_out}" ]; then
  touch "{machine_out}"
fi
"""

    if human_exit_code:
        outputs.extend([human_exit_code, machine_exit_code])
        command += """
echo $EC > "{human_exit_code}"
echo $EC > "{machine_exit_code}"
"""
    else:
        command += """
if [ $EC -ne 0 ]; then
  cat "{human_out}"
  exit $EC
fi
"""

    ctx.actions.run_shell(
        inputs = inputs,
        outputs = outputs,
        tools = [executable],
        command = command.format(
            detekt = executable.path,
            human_out = human_out.path,
            machine_out = machine_out.path,
            human_exit_code = human_exit_code.path if human_exit_code else "",
            machine_exit_code = machine_exit_code.path if machine_exit_code else "",
        ),
        arguments = [action_args],
        mnemonic = _MNEMONIC,
        progress_message = "Linting %{label} with Detekt",
    )

def _detekt_aspect_impl(target, ctx):
    if not should_visit(ctx.rule, ctx.attr._rule_kinds):
        return []

    files_to_lint = [
        f
        for f in filter_srcs(ctx.rule)
        if f.extension in ["kt", "kts"]
    ]

    outputs, info = output_files(_MNEMONIC, target, ctx)

    if len(files_to_lint) == 0:
        noop_lint_action(ctx, outputs)
        return [info]

    baseline = getattr(ctx.file, "_baseline", None)

    detekt_action(
        ctx = ctx,
        executable = ctx.executable._detekt,
        srcs = files_to_lint,
        config = ctx.file._config,
        baseline = baseline,
        human_out = outputs.human.out,
        machine_out = outputs.machine.out,
        human_exit_code = outputs.human.exit_code,
        machine_exit_code = outputs.machine.exit_code,
        args = ctx.attr._args,
    )

    return [info]

def lint_detekt_aspect(
        binary,
        config,
        baseline = None,
        rule_kinds = ["kt_jvm_library", "kt_jvm_binary", "kt_jvm_test"],
        args = []):
    """A factory function to create a Detekt linter aspect."""
    attrs = {
        "_options": attr.label(
            default = Label("@aspect_rules_lint//lint:options"),
            providers = [LintOptionsInfo],
        ),
        "_detekt": attr.label(
            default = binary,
            executable = True,
            cfg = "exec",
        ),
        "_config": attr.label(
            allow_single_file = True,
            mandatory = True,
            doc = "Detekt YAML configuration file",
            default = config,
        ),
        "_rule_kinds": attr.string_list(
            default = rule_kinds,
        ),
        "_args": attr.string_list(
            default = args,
        ),
    }

    if baseline:
        attrs["_baseline"] = attr.label(
            allow_single_file = True,
            default = baseline,
        )

    return aspect(
        implementation = _detekt_aspect_impl,
        attrs = attrs,
    )
