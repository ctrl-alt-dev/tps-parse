tps-parse
=========

Library for parsing Clarion TPS files. Also contains TPS to CSV converter.

(C) 2012-2013 E.Hooijmeijer, [Apache 2 licensed](https://www.apache.org/licenses/LICENSE-2.0.html)

WARNING : This software is based on Reverse Engineered TPS Files.
          As such, its probably incomplete and may mis-interpret data.
          It is no replacement for any existing Clarion tooling.
          Check the output files thoroughly before proceeding.

Typical use:
 java -jar tps-to-csv.jar -s [source file or folder] -t [target file or folder] -sort

Read the [blogpost](http://blog.42.nl/articles/liberating-data-from-clarion-tps-files).

V1.0.5 19 Feb 2013
------------------
- Support for BCD fields
- Support for Array fields

V1.0.4 21 Jan 2013
------------------
- Move to GITHUB 

V1.0.3 21 Jan 2013
------------------
- Unit tests.
- Java doc.

V1.0.2 13 Jan 2013
------------------

- Fixed bug in page scanning, where a page was missed when the previous ended at a page boundary
- Added character -encoding support to render csv in specific encoding.
- Added -raw support to have the csv without any applied encodings.
- Added -compare to topscan generated csv file.
- Added -sort to sort the records to their row nr before outputting.

V1.0.1  01 Jan 2013
-------------------
- Fixed bug in Block parsing, resulting in record duplication.
- Added support for parsing indexes.
- Added support for Table Name Records
- Added -layout option to display file layout.

V1.0.0  31 Dec 2012
-------------------
- First Release

