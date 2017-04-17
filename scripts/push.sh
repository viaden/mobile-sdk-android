#!/usr/bin/env bash

if [ $# != 2 ]; then
  echo "Use: $0 <authorization key> <token>"
else
  curl --header "Authorization: key=$1" --header "Content-Type: application/json" https://android.googleapis.com/gcm/send -d "{\"to\":\"$2\"}"
fi
