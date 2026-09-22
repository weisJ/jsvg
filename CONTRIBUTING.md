# Contributing

Before starting a feature or substantial fix, please open an issue or comment on an existing one so
we can coordinate the work.

## Code style

Run `./gradlew spotlessApply` before submitting a change.

Some general conventions:

- Keep implementation details private. Providing general-purpose DOM introspection is not a goal of
  the project.
- Prefer immutable classes where practical.
- Mark fields and method parameters with `@Nullable` or `@NotNull` where possible.
- Name accessors after the value they return (`thing()` rather than `getThing()`).

## Testing

Tests should cover meaningful, observable behavior. Rendering changes are usually best tested with a
small SVG fixture in `jsvg/src/test/resources/com/github/weisj/jsvg` and an `ImageComparison` test.
Create reference images independently or with a renderer (e.g. Batik) that already supports the feature.
If Batik does not support the feature being tested or does not follow the relevant spec create an alternative
simpler reference SVG where the required appearance is replicated manually and test that JSVG's output for
both SVG files is the same.

Keep each test focused, use the existing loading and comparison helpers, and avoid brittle assertions
about implementation details. Use the smallest practical image-comparison tolerance.

Run a single test class with:

```sh
./gradlew :jsvg:test --tests com.github.weisj.jsvg.filter.FilterTest
```

Run the complete module test suite with:

```sh
./gradlew :jsvg:test
```

## External rendering suites

The resvg, W3C SVG 1.1, and Web Platform Test suites are optional Git submodules. Their tests are
skipped when the corresponding submodule is not available. To initialize all three, run:

```sh
git submodule update --init resvg-test-suite w3c-svg-11-test-suite wpt-test-suite
```

Each suite can be run on its own:

```sh
./gradlew :jsvg:test --tests com.github.weisj.jsvg.ReSvgTestSuite
./gradlew :jsvg:test --tests com.github.weisj.jsvg.W3cSvg11TestSuite
./gradlew :jsvg:test --tests com.github.weisj.jsvg.WptSvgTestSuite
```

Gradle configures the suite paths automatically.

Known unsupported or mismatching cases are excluded in the suite runners.

There are audit tasks to help find excluded cases that now pass:

```sh
./gradlew :jsvg:resvgTestAudit
./gradlew :jsvg:w3cSvg11TestAudit
./gradlew :jsvg:wptSvgTestAudit
```

Audit reports are written below `jsvg/build/reports`. Pass a relative path prefix with `--args` to
limit an audit, for example `./gradlew :jsvg:wptSvgTestAudit --args=text`.
