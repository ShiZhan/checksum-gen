checksum-gen
============
Generate a group of checksums (MD5) for a given directory or file, optionally as zip archive or chunks.

How to use:

1. deploy sbt
2. run `sbt assembly`
3. copy the **ChecksumGen-assembly-1.0.jar** anywhere and run

[download](http://goo.gl/nINYvb)

E.g.:

`java -jar ChecksumGen-assembly-1.0.jar`

Show program usage.

`java -jar ChecksumGen-assembly-1.0.jar -c 65536 e:\test`

Which means list all files in **e:\test**, with corresponding md5 checksums, if any file is larger than the chunk size (65536), also list the checksum of every chunk.

`java -jar ChecksumGen-assembly-1.0.jar ChecksumGen -a e:\test.zip`

Which means list all files in **e:\test.zip**, with corresponding md5 checksums.

