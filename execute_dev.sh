#!/bin/sh

# Experimental bot
echo "Loading environment variables."
. ./experimental_env.sh

make build

echo "Starting bot..."
./target/bin/main
