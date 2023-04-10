# Differential Filesystem Scanner

This project contains a differential filesystem scanner implementation for Java. It allows to scan a filetree and to detect `unchanged`, `moved`, `deleted`, `modified` or `new` files. The implementation of this scanner uses the `inode` and [st_mtime](https://man7.org/linux/man-pages/man2/stat.2.html#:~:text=of%20file%20data.-,st_mtime,-This%20is%20the) information of a file and is thus Linux specific.

It is highly inspired by [snapraid filesystem scan implementation](https://github.com/amadvance/snapraid/blob/9bc570eeb3ce6d1d4d3e491b49a4c12488920cbe/cmdline/scan.c#L741) which uses randomized nanosecond modification timestamp to locate *potential* modified files.

## Maven

```xml
<dependency>
  <groupId>io.metaloom.utils</groupId>
  <artifactId>differential-filesystem-scanner</artifactId>
  <version>${project.version}</version>
</dependency>
```

## Example

```java
%{snippet|id=usage|file=src/test/java/io/metaloom/test/fs/ExampleUsageTest.java}
```

Alternatively a streaming API can be used to fetch the diff.

```java
%{snippet|id=stream|file=src/test/java/io/metaloom/test/fs/ExampleUsageTest.java}
```

## Limitations

Both filesystem scanner implementations however have limitations. It is **not possible** to precisely detected all modified files. A file can be modified without altering the modification timestamp and size. The scanner will thus not be able to detect the modification.

The current version of this library only contains a **Linux** specific implementation.

## Releasing

```bash
# Set release version
mvn versions:set -DgenerateBackupPoms=false

# Invoke the release
mvn clean deploy -Drelease
```