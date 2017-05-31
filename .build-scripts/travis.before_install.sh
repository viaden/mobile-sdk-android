#!/bin/sh

# Check dokumentation for more info about Infer installation on Linux
# https://github.com/facebook/infer/blob/master/INSTALL.md#pre-compiled-versions

#echo -e "\e[33mInstalling Infer dependencies\e[0m"
#apt-get update
#apt-get -o Dpkg::Options::="--force-confnew" upgrade -y
#apt-get -o Dpkg::Options::="--force-confnew" install -y  \
#  autoconf \
#  automake \
#  build-essential \
#  libffi-dev \
#  libgmp-dev \
#  libmpc-dev \
#  libmpfr-dev \
#  m4 \
#  pkg-config \
#  python-software-properties \
#  unzip \
#  zlib1g-dev

# Opam is broken on some Ubuntu versions
# Install Opam from official repository (http://opam.ocaml.org/doc/Install.html#Binarydistribution)
#echo -e "\e[33mInstalling Opam\e[0m"
#yes '' | add-apt-repository ppa:avsm/ppa
#apt-get update
#apt-get install -y ocaml ocaml-native-compilers camlp4-extra opam

# Checkout Infer
echo -e "\e[33mCloning Infer from Github\e[0m"
git clone https://github.com/facebook/infer.git infer-sources
cd infer-sources
# Compile Infer
echo -e "\e[33mCompiling Infer\e[0m"
./build-infer.sh java
# Install Infer into your PATH
echo -e "\e[33mAdding Infer to PATH\e[0m"
export PATH=`pwd`/infer/bin:$PATH
