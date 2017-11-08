#!/bin/bash

# time ls ../regfixer-data/output/*.txt | xargs ./test.sh

echo "name,duration,templates,cost,solution,dot_rate,dotstar_rate,emptyset_rate"
for filename in "$@"
do
  printf "$filename,"
  gtimeout 10 java -jar target/regfixer.jar fix --quiet --csv --limit 5000 "$filename"
  if [ $? -eq 124 ]; then
    echo "timeout"
  fi
done
