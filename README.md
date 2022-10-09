# TikaPdfXml-To-Tei

[This doesn't use Tika anymore, so the name doesn't make sense.]

Uses a series of images of documents (optionally scanning
them in) to generate two digital artifacts using OCR:

1. a searchable PDF document; and,
1. a TEI document, with references to ptif files.

## running

Requirements: bash, java 17, scanimage, vips, imagemagick, tesseract.

Edit the `bin/teiHeader.properties` file with your own custom
settings (author, copyright, etc.).
Build the java program and extract the distribution.
Turn on your scanner and insert the physical documents.
Run the `bin/teidoc.sh` shell script.

```shell
$ ./gradlew build
$ unzip build/distributions/tikapdfxml-to-tei-VERSION.zip
$ ./bin/teidoc.sh title_of_output_document -s
```

Final output will be in a `/tmp` directory.
