#!/bin/sh
if [ ! "$1" ] ; then
    echo "usage: $0 path-to-image-file"
    echo "CAUTION: DELETES ORIGINAL FILE!"
    exit 1
fi

pth_fil_ext="$1"

pth=$(dirname $pth_fil_ext)
fil_ext=$(basename $pth_fil_ext)
fil="${fil_ext%.*}"

echo "converting $pth_fil_ext to $pth/$fil.ptif ..."
vips tiffsave "$pth_fil_ext" "$pth/$fil.ptif" --tile --pyramid --compression jpeg --tile-width 256 --tile-height 256
rm "$pth_fil_ext"
