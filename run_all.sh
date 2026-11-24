#!/usr/bin/env bash
# Runs every task and saves console output to TaskN/output/*.txt and screenshots to TaskN/screenshots/.
# GUI programs need a display: on Linux without one, this script uses xvfb-run automatically.
# Tasks 2 and 6 need MySQL/MariaDB (database 'campus', user 'student'/'student123'; see Task6/sql/schema.sql).
unset JAVA_TOOL_OPTIONS _JAVA_OPTIONS
ROOT="$(cd "$(dirname "$0")" && pwd)"
JDBC="$ROOT/Task6/lib/mariadb-java-client-2.7.6.jar"
X=""; if [ -z "$DISPLAY" ] && command -v xvfb-run >/dev/null; then X="xvfb-run -a -s \"-screen 0 1280x900x24\""; fi
gui() { local t="$1" out="$2"; shift 2; mkdir -p "$ROOT/$t/output"; echo "== $t : $*"
  ( cd "$ROOT/$t" && eval $X java "$@" ) 2>&1 | tee "$ROOT/$t/output/$out.txt"; echo; }
mkdir -p "$ROOT/out"

# Task 1
mkdir -p "$ROOT/Task1/output"
( cd "$ROOT/Task1" && echo '$ javac original/FeeWindow.java' && javac -d /tmp/fw original/FeeWindow.java ) 2>&1 | tee "$ROOT/Task1/output/original_compile_errors.txt"
javac -d "$ROOT/out/t1" "$ROOT"/Task1/*.java && gui Task1 FeeDemo -cp "$ROOT/out/t1" FeeDemo

# Task 2
javac -d "$ROOT/out/t2" "$ROOT"/Task2/*.java && gui Task2 AppointmentDemo -cp "$ROOT/out/t2:$JDBC" AppointmentDemo

# Task 3
javac -d "$ROOT/out/t3" "$ROOT"/Task3/*.java && gui Task3 LibraryDemo -cp "$ROOT/out/t3" LibraryDemo

# Task 4: misspelled handler with and without @Override, then the demo
mkdir -p "$ROOT/Task4/output"
( cd "$ROOT/Task4/misspelled"
  echo '--- mouseclicked WITHOUT @Override:'; javac -d /tmp/m1 DrawPad.java && echo 'compiles with NO error or warning (the bug is silent)'
  sed 's#/\*OVERRIDE\*/#@Override#' DrawPad.java > /tmp/DrawPad.java
  echo '--- mouseclicked WITH @Override:'; javac -d /tmp/m2 /tmp/DrawPad.java ) 2>&1 | sed 's#/tmp/DrawPad.java#misspelled/DrawPad.java (with @Override)#' | tee "$ROOT/Task4/output/override_compile_check.txt"
javac -d "$ROOT/out/t4" "$ROOT"/Task4/*.java "$ROOT"/Task4/broken/DrawPadBroken.java && gui Task4 DrawPadDemo -cp "$ROOT/out/t4" DrawPadDemo

# Task 5: change effort v1 -> v2, then the demo
mkdir -p "$ROOT/Task5/output"
( cd "$ROOT/Task5/styles"; for s in A B C D; do printf "Style %s: %s lines changed v1 -> v2\n" $s "$(diff <(sed 1,13d v1/Converter$s.java) <(sed 1,13d v2/Converter$s.java) | grep -c '^[<>]')"; done
  for s in A B C D; do echo "--- diff style $s"; diff <(sed 1,13d v1/Converter$s.java) <(sed 1,13d v2/Converter$s.java); done ) | tee "$ROOT/Task5/output/change_effort.txt"
javac -d "$ROOT/out/t5v1" "$ROOT"/Task5/styles/v1/*.java && javac -d "$ROOT/out/t5v2" "$ROOT"/Task5/styles/v2/*.java
javac -d "$ROOT/out/t5" "$ROOT"/Task5/*.java "$ROOT"/Task5/styles/v2/ConverterD.java && gui Task5 ConverterDemo -cp "$ROOT/out/t5" ConverterDemo

# Task 6
javac -cp "$JDBC" -d "$ROOT/out/t6" $(find "$ROOT/Task6/src" -name '*.java') && gui Task6 RegistrationDemo -Xmx2g -cp "$ROOT/out/t6:$JDBC" registration.ui.RegistrationDemo
