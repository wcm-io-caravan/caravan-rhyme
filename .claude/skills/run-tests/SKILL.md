---
description: Run tests and analyse results. Handles Maven output safely by redirecting to log files.
argument-hint: "[module|class|class#method|all]"
---

Run tests and analyse the results for this project. All Maven output MUST be redirected to a timestamped log file — never consume it directly.

## Arguments

- `$ARGUMENTS` — What to test. Examples: "core", "all", "HttpHalResourceLoaderTest", "HttpHalResourceLoaderTest#should_parse_valid_URI"

## Log File Convention

- Directory: `/private/tmp/claude-501/`
- Naming: `mvn-test-<YYYYMMDD-HHMMSS>.log`
- Create via: `LOGFILE="/private/tmp/claude-501/mvn-test-$(date +%Y%m%d-%H%M%S).log"`

## Execution

All commands run from the repository root with `-s .maven-settings.xml`.

### Single test class or method (in a specific module)

```bash
./mvnw test -s .maven-settings.xml -pl <module> -Dtest=<TestClass> > "$LOGFILE" 2>&1
# or with method:
./mvnw test -s .maven-settings.xml -pl <module> -Dtest=<TestClass>#<method> > "$LOGFILE" 2>&1
```

### All tests in a single module

```bash
./mvnw test -s .maven-settings.xml -pl <module> > "$LOGFILE" 2>&1
```

### Full CI build with all tests

```bash
./mvnw clean install -s .maven-settings.xml -Pcontinuous-integration > "$LOGFILE" 2>&1
```

### OSGi integration tests

These launch a full Sling 14 OSGi container. Use `verify` (not `test`) to trigger failsafe:
```bash
./mvnw clean verify -s .maven-settings.xml -pl examples/osgi-jaxrs-example-launchpad > "$LOGFILE" 2>&1
```

## Analysing Results

After the build finishes, check the exit code first.

### On success (exit code 0)

```bash
tail -5 "$LOGFILE"
```

Report "All tests passed" with the build time.

### On failure (non-zero exit code)

Test failures need BOTH the stacktrace AND the console log context leading up to the failure. Extract them in this order:

1. **Find which tests failed:**
   ```bash
   grep -E "Tests run:.*Failures: [1-9]|Tests run:.*Errors: [1-9]" "$LOGFILE"
   ```

2. **Get the failure summary with stacktraces** from the surefire/failsafe reports (these are more readable than the Maven console):
   ```bash
   find . -path "*/surefire-reports/*.txt" -newer "$LOGFILE" -exec grep -l "FAILURE\|ERROR" {} \;
   # Then read the relevant report files with cat (these are small, just the failing test)
   ```

3. **Get the console log context** — look for logs leading up to the failure (e.g. server startup, request/response details):
   ```bash
   grep -B 50 "FAILURE\|ERROR.*Exception" "$LOGFILE" | head -80
   ```

4. **For OSGi integration tests**, check container startup:
   ```bash
   grep -E "Framework started|BundleException|ERROR" "$LOGFILE"
   ```

Report: which test(s) failed, the relevant stacktrace, and any console context that helps explain the failure.
