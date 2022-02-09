#!/bin/sh

here=$(dirname $(readlink -f $0))

srcdir=$(readlink -f ./)
echo "Extracts all images from: $srcdir"

dstdir=$(mktemp -d)
echo "Prepares them for IIPImage Server at: $dstdir"

if [ "$1" != "-f" ] ; then
    echo "usage: $0 -f"
    exit 0
fi

# mirror directory tree structure (without files)
mkdir -p $dstdir
rsync -av --include '*/' --exclude '*' $srcdir/ $dstdir



# extract all images from pdf files (to corresponding destination directory)
find $srcdir -type f    -name \*.pdf                    -printf "%P\0" | xargs -r -0 -n 1 -I {} $here/pdf.sh "{}" $srcdir $dstdir

# copy all non-pdf files (to corresponding destination directory)
find $srcdir -type f \! -name \*.pdf                    -printf "%P\0" | xargs -r -0 -n 1 -I {} cp -v "$srcdir/{}" "$dstdir/{}"

# convert jpg and png to ptif (all within destination)
find $dstdir -type f \( -name \*.jpg -o -name \*.png \) -printf "%P\0" | xargs -r -0 -n 1 -I {} $here/tif.sh "$dstdir/{}"

echo "$dstdir"
