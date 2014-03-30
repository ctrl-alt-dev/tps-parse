tps-parse
=========

Library for parsing Clarion TPS files. Also contains a TPS to CSV converter.

(C) 2012-2013 E.Hooijmeijer, [Apache 2 licensed](https://www.apache.org/licenses/LICENSE-2.0.html)

WARNING : This software is based on Reverse Engineered TPS Files.
          As such, its probably incomplete and may mis-interpret data.
          It is no replacement for any existing Clarion tooling.
          Check the output files thoroughly before proceeding.

Typical use:
 java -jar tps-to-csv.jar -s [source file or folder] -t [target file or folder]

Read the [blogpost](http://blog.42.nl/articles/liberating-data-from-clarion-tps-files) or this one about [the encryption of TPS files](http://blog.42.nl/articles/liberating-data-from-encrypted-tps-files/).

Download the [binary](http://www.ctrl-alt-dev.nl/Projects/TPS-to-CSV/TPS-to-CSV.html).

Sample code
-----------

```java
    //
    // Read the TPS file
    //
    TpsFile tpsFile = new TpsFile(new File("datafile.tps"));
    //
    // TPS files can contain multiple tables (commonly only one is used).
    //
    Map<Integer, TableDefinitionRecord> tables = tpsFile.getTableDefinitions(false);
    for (Map.Entry<Integer, TableDefinitionRecord> entry : tables.entrySet()) {
        TableDefinitionRecord table = entry.getValue();
        //
        // For each table get the field definition (columns).
        //
        for (FieldDefinitionRecord field : table.getFields()) {
            // Do something with the field definition.
        }
        //
        // And data records (rows).
        //
        for (DataRecord rec : tpsFile.getDataRecords(entry.getKey(), entry.getValue(), false)) {
            // Do something with the data record.
        }
    }
```

Example for an encrypted TPS file:

```java
    TpsFile tpsFile = new TpsFile(new File("datafile.tps"), "password");
```

V1.0.12 30 Mar 2014
------------------
- TpsPage flushing, less memory is used at the expense of CPU.
- Added memory reporting when running with -verbose.
- Byte buffers are now shared instead of copied where possible.
- For TPS files with multiple tables, the name is now exposed in the CSV file name.    

V1.0.11 03 Jan 2014
------------------
- Fixed a bug in array handling. Correct offset is now used.    

V1.0.10 21 Dec 2013
------------------
- Added support for custom TPS string encodings such as CP850. 

V1.0.9 22 Aug 2013
------------------
- Leading zero's of BCD values are now trimmed.
- Fixed a bug in BCD parsing. 
  For some TPS files the 'bcdLengthOfElement' value exceeds the number of available (remaining) digits.
  The value is now ignored and the actual length of available digits is taken.   

V1.0.8 15 May 2013
------------------
- Streaming Support for large files.
- Refactoring of TPS to CSV utility. 
- removed sort option as its now implicit (use -direct to not sort).
- added verbose option to have some sense of progress on large files.

V1.0.7 11 May 2013
------------------
- Support for encrypted files.

V1.0.6 04 May 2013
------------------
- Support for Binary Memo's (aka BLOBs)

V1.0.5 26 Feb 2013
------------------
- Support for BCD fields
- Support for Array fields
- Expand Array fields into multiple CSV columns

V1.0.4 21 Jan 2013
------------------
- Move to Github 

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

