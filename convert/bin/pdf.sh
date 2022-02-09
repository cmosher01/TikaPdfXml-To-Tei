#!/bin/sh
if [ ! "$1" ] ; then
    echo "usage: $0 path-to-pdf src-root dst-root"
    exit 1
fi

pth_fil_ext="$1"
src_dir="$2"
dst_dir="$3"

pth=$(dirname $pth_fil_ext)
fil_ext=$(basename $pth_fil_ext)
fil="${fil_ext%.*}"

mkdir -p "$dst_dir/$pth/$fil"
pdfimages -all "$src_dir/$pth_fil_ext" "$dst_dir/$pth/$fil/p"
