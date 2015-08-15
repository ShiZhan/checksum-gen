checksum-gen
============
Generate a group of checksums (MD5) for a given directory or file, optionally as zip archive or chunks.

How to use:

1. deploy sbt
2. run `sbt assembly`
3. copy the **ChecksumGen-assembly-0.1-SNAPSHOT.jar** anywhere and run

[download](http://share.weiyun.com/95fd4da504454566457b6f7efa1dd8d8)

E.g.:

`java -jar ChecksumGen-assembly-0.1-SNAPSHOT.jar`

Show program usage.

`java -jar ChecksumGen-assembly-0.1-SNAPSHOT.jar e:\test.zip`

print md5 checksum for **e:\test.zip**.

`java -jar ChecksumGen-assembly-0.1-SNAPSHOT.jar e:\test`

list all files in **e:\test**, with corresponding md5 checksums.

`java -jar ChecksumGen-assembly-0.1-SNAPSHOT.jar -c 65536 e:\test`

list all files in **e:\test**, with corresponding md5 checksums, if any file is larger than the chunk size (65536), also list the checksum of every chunk.

`java -jar ChecksumGen-assembly-0.1-SNAPSHOT.jar -a e:\test.zip`

list all files in **e:\test.zip**, with corresponding md5 checksums.

