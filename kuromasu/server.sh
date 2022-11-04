#!/usr/bin/env bash

DATA=$(pwd)/data
java \
    -P SOLUTION_FILENAME=MyKuromasuSolver.java\
    -P RESET_SCRIPT="$(pwd)/reset.sh"\
    -P RUN_SCRIPT="$(pwd)/run.sh"\
    -P REGEX_SUCCESS_RATE='success (.*?) %' \
    -P WORK_FOLDER="$(pwd)/work/"\
    -P RESULT_FOLDER="$DATA/data/results/"\
    -P WORK_QUEUE="$DATA/workqueue.json"\
    -P LEADERBOARD_FILE="$DATA/data/leaderboard.json" \
    -P PORT=80
    -jar flavium-1.0-all.jar
