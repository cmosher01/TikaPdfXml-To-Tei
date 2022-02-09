# TikaPdfXml-To-Tei
Take a Tika-generated XML file from a PDF, and format it as a TEI file,
with the extracted text, and links to extracted images.

After building java program, extract into the `convert` directory.
Put PDF, JPG, and/or PNG files into target directory
structure under `convert/src_pdf_jpg_png/`

```shell
$ ./gradlew build
$ cd convert
$ mkdir -p tikapdfxml-to-tei
$ cd tikapdfxml-to-tei
$ tar -x -f ../../build/distributions/tikapdfxml-to-tei-*.tar --strip-components=1
$ cd ..
$ ./convert.sh
```

Final output will be in two `/tmp` directories, one for TEI files,
one for PTIF files.
