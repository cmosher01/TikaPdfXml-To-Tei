#!/bin/sh

here=$(dirname "$(readlink -f $0)")

cd $here || exit 1
cd src_pdf_jpg_png || exit 1

if [ "$1" != "-f" ] ; then
    ../bin/copy_and_extract_all_images_from_pdfs.sh
    ../bin/generate_tei_files2.sh
    echo "usage: $0 -f"
    exit 1
fi

../bin/copy_and_extract_all_images_from_pdfs.sh -f
../bin/generate_tei_files2.sh -f
