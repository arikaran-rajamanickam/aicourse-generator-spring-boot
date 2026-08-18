#!/usr/bin/env bash
# Graded test runner for the CourseGen coding challenge.
#
#   ./challenge/run_graded.sh          # run every graded suite
#   ./challenge/run_graded.sh q1       # run only Q1's graded suite
#   ./challenge/run_graded.sh q1 q3    # run selected suites
#
# A suite that has no graded tests shipped with it is reported as MANUAL.
set -uo pipefail
cd "$(dirname "$0")/.." || exit 1

ALL=(q1 q2 q3 q4)
TARGETS=("${@:-${ALL[@]}}")
OUT_DIR="challenge/.report"
mkdir -p "$OUT_DIR"

printf '\n=======================================================\n'
printf '  CourseGen Challenge — graded run\n'
printf '=======================================================\n\n'

overall=0
for q in "${TARGETS[@]}"; do
  suite_dir="src/test/java/com/challenge/${q}"
  if [ ! -d "$suite_dir" ]; then
    printf '  %-4s  MANUAL   (no automated suite — reviewed by hand)\n' "$(echo "$q" | tr "[:lower:]" "[:upper:]")"
    continue
  fi
  log="${OUT_DIR}/${q}.log"
  ./mvnw test -B -DfailIfNoTests=false -Dsurefire.failIfNoSpecifiedTests=false \
      -Dtest="com.challenge.${q}.**" >"$log" 2>&1
  status=$?
  line=$(grep -E '^\[(INFO|ERROR)\] Tests run: [0-9]+, Failures' "$log" | tail -1 \
         | sed -E 's/^\[(INFO|ERROR)\] //')
  if [ $status -eq 0 ]; then
    printf '  %-4s  PASS     %s\n' "$(echo "$q" | tr "[:lower:]" "[:upper:]")" "${line:-no tests}"
  else
    overall=1
    printf '  %-4s  FAIL     %s\n' "$(echo "$q" | tr "[:lower:]" "[:upper:]")" "${line:-see $log}"
    grep -E '^\[ERROR\]   \S+\.\S+' "$log" | sed 's/^\[ERROR\]   /           ↳ /' | head -20
  fi
  printf '           full log: %s\n' "$log"
done

printf '\n-------------------------------------------------------\n'
[ $overall -eq 0 ] && printf '  RESULT: all selected suites green\n' \
                   || printf '  RESULT: failures present\n'
printf -- '-------------------------------------------------------\n\n'
exit $overall
