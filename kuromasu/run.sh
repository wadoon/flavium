#!/usr/bin/env bash

cd $1

export CLASSPATH=$(pwd)/kuromasu-1.3-deps.jar
export CLASSNAME=MyKuromasuSolver

javac -cp $CLASSPATH:. $CLASSNAME.java \
    || javac -encoding cp1252 -cp $CLASSPATH:. $CLASSNAME.java \
    || echo "Could not compile solution in $1"

if [ -f $CLASSNAME.class ]; then
    export TIMEOUT=-1
    export TO=120
    export NO_CHECK=false
    export RIDDLES_FOLDER=$(pwd)/riddles

    #JAVAARGS="-Xmx12g -Xms8g -cp $CLASSPATH"
    JAVAARGS="-Xmx4g -Xms1g -cp $CLASSPATH:."

    if ./classCheck.py MyKuromasuSolver.class; then
        /usr/bin/time \
            timeout $TO \
            java $JAVAARGS edu.kit.kastel.formal.kuromasu.KuromasuTest
    else
        echo "Class check did not succeed. Please avoid dangerous classes/methods like java.io.File or java.net.Socket."
        exit 111
    fi
fi
