"""Build rule for the pre-packaged `seed.db` that ships in the app's assets."""

def seed_db(
        name,
        out,
        dictionary_seed,
        false_cognates,
        curated_lessons,
        cognate_rules,
        kaikki = None,
        sublex = None,
        tatoeba = None,
        reading_overrides = None,
        extra_args = [],
        **kwargs):
    """Generates `seed.db` with the preprocessor from checked-in inputs.

    The build never downloads anything: every input is a file in the repository. The curated seeds
    are required; the optional `kaikki`, `sublex` and `tatoeba` inputs are the subsets produced by
    the preprocessor's extract-* commands (see tools/preprocessor/README.md).

    Args:
      name: Name of the target.
      out: Path of the generated database, relative to the package, e.g.
        `src/main/assets/databases/seed.db` so that it lands where Android expects assets.
      dictionary_seed: Label of dictionary_seed.json.
      false_cognates: Label of false_cognates_seed.json.
      curated_lessons: Label of curated_lessons_seed.json.
      cognate_rules: Label of cognate_rules_seed.json.
      kaikki: Optional label of the Kaikki lemma subset (.jsonl.gz).
      sublex: Optional label of the SUBTLEX-ESP rank file (.tsv).
      tatoeba: Optional label of the Tatoeba candidate pool (.tsv.gz).
      reading_overrides: Optional label of reading_overrides.json (primary reading per spelling).
      extra_args: Extra command line arguments for the preprocessor.
      **kwargs: Passed to the underlying genrule (e.g. visibility).
    """
    inputs = [
        ("--dictionary-seed", dictionary_seed),
        ("--false-cognates", false_cognates),
        ("--curated-lessons", curated_lessons),
        ("--cognate-rules", cognate_rules),
        ("--kaikki", kaikki),
        ("--sublex", sublex),
        ("--tatoeba", tatoeba),
        ("--reading-overrides", reading_overrides),
    ]
    srcs = []
    args = []
    for flag, label in inputs:
        if label:
            srcs.append(label)
            args.append("%s $(location %s)" % (flag, label))

    native.genrule(
        name = name,
        srcs = srcs,
        outs = [out],
        cmd = " ".join(["$(location //tools/preprocessor:preprocessor)", "--db", "$@"] + args + extra_args),
        tools = ["//tools/preprocessor:preprocessor"],
        **kwargs
    )
