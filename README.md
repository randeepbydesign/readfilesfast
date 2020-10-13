# readfilesfast
Java libraries for working with file formats to load them in and read them quick

## What does it do?
You want to marshal files from excel, TSV, etc into Java objects with minimal fuss. You can specify
a datasource and a java.util.Function that transforms the various filetypes into the data you want
to work with.

Sacrifice efficiency for get-out-of-the-gatedness. These routines process the entire files in memory
so may not be the most memory efficient solution for large files.
 