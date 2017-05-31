#!/bin/sh
set -ex
wget https://github.com/facebook/infer/releases/download/0.12.0/infer-linux64-0.12.0.tar.xz
tar xf infer-linux64-0.12.0.tar.xz
cd infer-linux64-0.12.0 && ./build-infer.sh java && make install
