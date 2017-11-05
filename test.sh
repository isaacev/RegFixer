#!/bin/bash

for filename in tests/test_*.txt; do
  echo "$filename"
  printf ">> "
  head -n 1 $filename
  printf "   "
  java -jar target/regfixer.jar fix --quiet --limit 5000 "$filename"
  echo ""
done
