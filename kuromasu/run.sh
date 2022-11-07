#!/usr/bin/env bash

cd $1

export CLASSPATH=$(pwd)/kuromasu-1.4-all.jar
export CLASSNAME=MyKuromasuSolver

export TIMEOUT=-1
export TO=120
export NO_CHECK=false
export RIDDLES_FOLDER=$(pwd)/riddles

#JAVAARGS="-Xmx12g -Xms8g -cp $CLASSPATH"
JAVAARGS="-Xmx4g -Xms1g -cp $CLASSPATH:."

time \
     timeout $TO \
     java $JAVAARGS edu.kit.iti.formal.kuromasu.SafeRunner
