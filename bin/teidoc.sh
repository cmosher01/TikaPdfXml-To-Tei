#!/bin/bash

optcvt="-verbose -alpha flatten -density 300 -compress jpeg -quality 35 -sampling-factor 4:2:0 -type TrueColor -interlace plane -define jpeg:dct-method=float -strip"
optvip="tiffsave --vips-progress --tile --pyramid --compression jpeg --tile-width 256 --tile-height 256"

if [ $# -lt 2 ]; then
    echo "usage:"
    echo "    $0 vpath/to/title_of_doc image-file [...]"
    echo "    $0 vpath/to/title_of_doc -s"
    exit 1
fi

path_to_title="$1"
shift 1
title="$(basename "$path_to_title")"

here="$(dirname "$(readlink -f "$0")")"

# top-level working directory
# the directory structure is as follows:
#   workdir/
#      scn/ (optional dir for scanned images)
#      jpg/ (jpeg images, converted from sources)
#      txt/ (OCR text, by tesseract, from images)
#      pdf/ (searchable PDFs, with jpegs and OCR text)
#      tif/ (ptif images from src)
#      tei/ (tei format document, referencing images)
workdir=$(mktemp -d)

for d in jpg pdf txt tif tei; do
    mkdir "$workdir/$d"
done

if [ $# = 1 ] && [ "$1" = "-s" ]; then
    mkdir "$workdir/scn"
    cd "$workdir/scn" || exit 1
    scanimage \
        --device-name='fujitsu:ScanSnap iX500:1637946' \
        --verbose \
        --progress \
        --batch='p%04d.tif' \
        --format=tiff \
        --mode=color \
        --source='ADF Duplex' \
        --swskip='9.1' \
        --swdeskew=yes
    set -- $workdir/scn/*
    cd - || exit 1
fi

echo "==========================="
# convert source images into jpg and tiff
# name files with page numbers: p####.TYPE
((i = 0))
for f in "$@"; do
    ((i = i + 1))
    pnnnn=$(printf "p%04d" $i)
    convert $optcvt "$f" "$workdir/jpg/$pnnnn.jpg"
    vips $optvip "$f" "$workdir/tif/$pnnnn.ptif"
done

cd $workdir || exit 1

echo "==========================="
# OCR to create pdf with jpg and text
find jpg -type f | tesseract - $title pdf txt
mv -nv $title.pdf pdf/
mv -nv $title.txt txt/

echo "==========================="
# build TEI files from xml
# shellcheck disable=SC2211
$here/../tikapdfxml-to-tei-*/bin/tikapdfxml-to-tei "$here/teiHeader.properties" "$path_to_title" <"txt/$title.txt" >"tei/$title.tei.xml"

echo "==========================="
tree $workdir

echo "Output here:"
echo "tree $workdir"
