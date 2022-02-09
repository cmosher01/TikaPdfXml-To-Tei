#\!/bin/sh

here=$(dirname $(readlink -f $0))

cd $here
cd src_pdf_jpg_png
../bin/copy_and_extract_all_images_from_pdfs.sh -f
../bin/generate_tei_files2.sh -f
