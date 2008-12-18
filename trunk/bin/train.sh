#!/bin/bash

TESTEL='java -Djava.ext.dirs=lib -jar TestEl.jar'
PRETRAINED=corpus/html/pretrained/
MYDIR=../data/trainingsdateien
NOTAGDIR=../data/ohnetags

./cleancorups.sh

cp -v $MYDIR/*  $PRETRAINED
$TESTEL --train
