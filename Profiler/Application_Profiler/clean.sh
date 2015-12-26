#!/bin/bash

python print_stats.py>statistics.txt
sed '1,6d' statistics.txt>stats.txt
awk '{ gsub(/{/,"\"{"); print }' stats.txt>stats1.txt
awk '{ gsub(/}/,"}\""); print }' stats1.txt>stats2.txt

rm statistics.txt
rm stats.txt
rm stats1.txt
mv stats2.txt statistics.txt
