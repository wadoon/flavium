#!/usr/bin/env bash

shopt -s extglob

if [ -z "$1" ]
then
      echo "No workfolder, exiting..."
      exit "111";
fi

WORKFOLDER=$1
path=$(dirname $(readlink -f "${BASH_SOURCE:-$0}"))
TEMPLATE="$path/template"
cd $WORKFOLDER

rm -rf ./!(*.java)
cp -R $TEMPLATE/* ./

export CLASSPATH=$(pwd)/kuromasu-1.4-all.jar
export CLASSNAME=MyKuromasuSolver

javac -cp $CLASSPATH:. $CLASSNAME.java \
    || javac -encoding cp1252 -cp $CLASSPATH:. $CLASSNAME.java

if [ $? -ne 0 ]; then
	echo "Could not compile solution. See previous error messages."
	exit "110";
fi

if javap -v *.class | grep -q "java/lang/Class.forName"; then
    echo "Though shall not use Class.forName"
    exit 111
fi

if $path/classCheck.py "*.class"; then
	exit 0
else
    echo "Class check did not succeed. Please avoid dangerous classes/methods like java.io.File or java.net.Socket."
    exit 111
fi
