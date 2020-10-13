# readfilesfast
Java libraries for working with file formats to load them in and read them quick into Java objects
for processing

## What does it do?
You want to marshal files from excel, TSV, etc into Java objects with minimal fuss. You can specify
a datasource and a java.util.Function that transforms the various filetypes into the data you want
to work with.

Sacrifice efficiency for get-out-of-the-gatedness. These routines process the entire files in memory
so may not be the most memory efficient solution for large files.

An example of reading in a Comma-separated value, and turning it into a list of Pair

```
new DelimitedTextDataSource<>("/users/randeepbydesign/filedata.csv",
	DelimiterType.COMMA, 
	strArr -> Pair.of(strArr[0], strArr[1]), 
	true);
```

