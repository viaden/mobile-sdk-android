#!/usr/bin/env bash

# Instance ID API
# https://developers.google.com/instance-id/reference/server

if [ $# != 2 ]; then
  echo "Use: $0 <authorization key> <token>"
else
  curl --header "Authorization: key=$1" https://iid.googleapis.com/iid/info/$2?details=true
fi
