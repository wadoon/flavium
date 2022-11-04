#!/usr/bin/env python3

import sys
import subprocess
import re

regex = r"-> ([a-zA-Z0-9.]+)"

def filter_prefix(seq, prefixes):
    for item in seq:
        for p in prefixes:
            if item.startswith(p):
                yield item
                continue


def run(classfile):
    output = subprocess.getoutput(f"jdeps -v {classfile}")
    matches = re.finditer(regex, output, re.MULTILINE)
    used_classes = sorted(map(lambda m: m.group(1), matches))
    used_classes.remove("java.base")
    used_classes.remove("not")

    # if found error!
    hard_forbidden_prefixes = {
        "java.io.File",
        "java.nio",
        "java.net",
    }

    hard_forbidden = list(filter_prefix(used_classes, hard_forbidden_prefixes))
    if hard_forbidden:
        print(f"Found forbidden classes: {hard_forbidden}")
        sys.exit(1)

    # mark for removal
    allowed_prefixes = [
        "edu.kit.iti.formal.kuromasu",
        "java.lang",
        "java.util",
        "org.sat4j",
        "java.io.PrintStream"
    ]

    allowed_classes = set(filter_prefix(used_classes, allowed_prefixes))
    used_classes = [x for x in used_classes if x not in allowed_classes]

    if used_classes:
        print(f"Following classes are suspicious: {used_classes}")
        sys.exit(2)


if __name__== '__main__':
    run(str(sys.argv[1]))
