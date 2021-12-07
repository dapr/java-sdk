#!/bin/bash

# Usage:
# ./entrypoint.sh \
#     --version 1.3.0 --package io.dapr.client --class DaprClientBuilder \
#     --methods "withObjectSerializer withStateSerializer builder" \
#     --keep-going=1

set -u

# Set default values
Version="1.3.1"
Package=""
Class=""
Methods=""
KeepGoingCount="1"

POSITIONAL=()
while [[ $# -gt 0 ]]; do
  key="$1"
  case $key in
    -v|--version)
      Version="$2"
      shift # past argument
      shift # past value
      ;;
    -p|--package)
      Package="$2"
      shift # past argument
      shift # past value
      ;;
    -c|--class)
      Class="$2"
      shift # past argument
      shift # past value
      ;;
    -m|--methods)
      Methods="$2"
      shift # past argument
      shift # past value
      ;;
    -kc|--keep-going)
      KeepGoingCount="$2"
      shift # past argument
      ;;
    *)    # unknown option
      POSITIONAL+=("$1") # save it in an array for later
      shift # past argument
      ;;
  esac
done

set -- "${POSITIONAL[@]}" # restore positional parameters

echo "Version   = ${Version}" 
echo "Package   = ${Package}" 
echo "Class     = ${Class}"

for Method in ${Methods}; do
  echo "Fuzzing Method = ${Method}"
  docker run --rm -v /"$(pwd)/fuzzing/$Version/$Package/$Class/$Method":/fuzzing cifuzz/jazzer-autofuzz \
   io.dapr:dapr-sdk:${Version} \
   ${Package}.${Class}::${Method} --keep_going=${KeepGoingCount}
done

# store exit status of grep
# if code 77 or 0 fuzzer ran successfully
# if not, something went wrong
status=$?
echo $status
if [ $status -eq 77 ] || [ $status -eq 0 ]
then
	echo "Jazzer completed successfully. Artifacts will be available as part of the workflow pipeline"
else
	echo "Something went wrong"
fi