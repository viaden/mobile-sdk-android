#!/usr/bin/env bash

curl --header "Authorization: key=" --header "Content-Type: application/json" https://android.googleapis.com/gcm/send \
-d "{\"to\":\"\"}"
