#!/bin/sh
if [ ! "$3" ] ; then
    echo "usage: $0 path-to-dir src-root dst-root lnk-root"
    exit 1
fi

here=$(dirname $(readlink -f $0))

pth="$1"
echo "$pth"
fil="${pth%.*.xml}"
src_dir="$2"
dst_dir="$3"
lnk_url="$4/$fil/p-%03d.ptif/full/full/0/default.jpg"
lnk2_url="$4/$fil.ptif/full/full/0/default.jpg"
echo "$lnk_url"
src_pth="$src_dir/$pth"
dst_fil="$dst_dir/$fil.tei.xml"
echo "$dst_fil"
dir_nam="$(dirname $dst_fil)"

mkdir -p "$dir_nam"
../tikapdfxml-to-tei/bin/tikapdfxml-to-tei "$src_pth" "$lnk_url" 0 $here/teiHeader.properties $here/encodingDesc_fragment.xml "$lnk2_url" | xmllint --format - >"$dst_fil"
echo "=============="
