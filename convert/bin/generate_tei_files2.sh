#!/bin/sh

here=$(dirname $(readlink -f $0))

srcdir=$(readlink -f ./)
echo "Based on .pdf,.jpg,.png files in this tree: $srcdir"

imgurl=https://mosher.mine.nu/images

dstdir=$(mktemp -d)
echo "Generates TEI files at: $dstdir"

if [ "$1" != "-f" ] ; then
    echo "usage: $0 -f"
    exit 0
fi

tikdir=$(mktemp -d)
java -jar /usr/share/java/tika-app-1.18.jar -x -i $srcdir -o  $tikdir

find $tikdir -type f -printf '%P\0' | xargs -0 -n 1 -I {} $here/gentei2.sh '{}' $tikdir $dstdir $imgurl

echo "intermediate: $tikdir"
echo "final: $dstdir"
