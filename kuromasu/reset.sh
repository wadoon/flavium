#!/usr/bin/env bash

WORKFOLDER=$1
TEMPLATE=template

rm -rf $WORKFOLDER/!(MyKuromasuSolver.java)
cp -R $TEMPLATE/* $WORKFOLDER/
