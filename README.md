Cerberus
========

Cerberus is the post-2020 CIF lab access control system, superseding
GrimReaper. Cerberus features more graceful error handling and detailed
logging, simplifying the testing and auditing process.

## Usage

```
usage: cerberus [-c <arg>] [-d] [-h] [-V] [-v]
 -c,--config <arg>   use specified config path
 -d,--debug          enable debug mode
 -h,--help           print usage
 -V,--verbose        verbose logging
 -v,--version        print version info
```

The default configuration path is `/etc/cerberus/cerberus.properties`. A
sample config can be found in the `config` directory. Debug mode allows the
testing of the code base without an actual card reader attached to your
computer, using stdin as a fake card reader. Log files are placed in
`/var/log/cerberus`. You will need to set appropriate permissions for these
files and directories on first run

## Compiling

To compile the project, run `./gradlew build`. The generated binary should be
inside `build/libs`. Java 8 is required for this project as Ben Ackerman's
legacy serial communication code for the current card reader is broken on
Java 11.

## License

This project is licensed under the LGPLv3. If you have any questions, please
contact the original author Jack Yu \<yuydevel at protonmail dot com\>.

