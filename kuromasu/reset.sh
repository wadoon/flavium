#!/usr/bin/env bash

WORKFOLDER=$1
path=$(dirname $(readlink -f "${BASH_SOURCE:-$0}"))
TEMPLATE="$path/template"

rm -rf $WORKFOLDER/!(MyKuromasuSolver.java)
cp -R $TEMPLATE/* $WORKFOLDER/

cd $WORKFOLDER

export CLASSPATH=$(pwd)/kuromasu-1.4-all.jar
export CLASSNAME=MyKuromasuSolver

javac -cp $CLASSPATH:. $CLASSNAME.java \
    || javac -encoding cp1252 -cp $CLASSPATH:. $CLASSNAME.java \
    || (echo "Could not compile solution in $1"; exit 110)



if $path/classCheck.py $CLASSNAME.class; then
  # everything is fine
else
    echo "Class check did not succeed. Please avoid dangerous classes/methods like java.io.File or java.net.Socket."
    exit 111
fi
