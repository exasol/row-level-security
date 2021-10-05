#!/bin/bash

function append {
    echo "$1" >> "$assembly"
}

if [[ $# -lt 2 ]]; then
    echo "usage: $0 <assembled-file> <file-to-be-added> [...]"
    echo
    echo "This script assembles SQL statements from files into one SQL batch file."
    echo "It removes coverage markers and adds statement terminators."
    echo "SQL statements appear in the order of the files in the parameter list."
else
    readonly assembly="$1"
    readonly scripts="${@:2}"
    
    echo "Creating SQL bundle '$assembly'"
    
    append "-- Row Level Security administration script bundle"
    append "--"

    for script in $scripts ; do
        if [[ -r "$script" ]]; then
            echo "Adding '$script'"
            append "-- Script source '$(basename "$script")'"
            grep --invert-match --perl-regexp '^--(/?\s*\[impl|\[\[|\]\])' "$script" >> "$assembly"
            append ";"
            append ""
        else
            echo "SQL script to be added to bundle not found: $script"
            exit -1; 
        fi
    done
fi