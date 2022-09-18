# sdfs
simple distributed file system which assumes ideal (no-failures/exceptions) environment

SDFS is simple, easy to install (copy a jar) and has less features. If you have few nodes where you want to distribute your file chunks and process them, this is a FS you are looking for.

### Install
- download the project / binary (sdfs.jar from releases page)
- copy it to node where you want it to run

### Architecture
- It has a single name node (which uses sqlite to store the filepaths)
- It can have many datenodes
- RPC is by a implementation
     - Java serde

### Running
- Namenode
  ```
  $ java -jar sdfs.jar namenode
  ```
- Datanode
  ```
  $ java.-jar sdfs.jar datanode --nnhost namenodehost.com
  ```
- Client cmdline
  ```
  $ java -jar sdfs.jar
  Usage: Shell <command> ...args
  Commands available:
     get
     rm
     list
     put

  $ java -jar sdfs.jar get --help
   switch                              reqd multiple help
   --src <srcPpath>                                  Source path (sdfs path) (default: )
   --dst <dstPpath>                                  Destination path (local path) (default: )
   --nnhost <nnHost>                                 Namenode host (default: localhost)
   --nnport <nnPort>                                 Namenode port (default: 5001)
   --rpcSerde <rpcSerde>                             The RPC serde class used to serialize/deserialize RPC messages (default: com.mmp.sdfs.rpc.javaserde.JavaSerde)
   --replFactor <replicationFactor>                  Replication factor (default: 3)
   --blockSize <blockSize>                           Block size (default: 268435456)
   -h, --help                                        Prints help (default: )
   --props <propsFile>                               Config props file (default: )
   --printProps                                      Prints sample props file (default: )
  ```

### Configuration
- Configuration can be done by command line arguments or a props file. To get available config, just run the above commands with `--help` option
  ```
  $ java -jar sdfs.jar namenode --help
   switch                              reqd multiple help
   -d, --namedir <namedir>               *           namenode directory (default: namenode)
   --storeclass <storeClass>             *           namenode store class (default: com.mmp.sdfs.headnode.SqliteNameStore)
   --nnhost <nnHost>                                 Namenode host (default: localhost)
   --nnport <nnPort>                                 Namenode port (default: 5001)
   --rpcSerde <rpcSerde>                             The RPC serde class used to serialize/deserialize RPC messages (default: com.mmp.sdfs.rpc.javaserde.JavaSerde)
   --replFactor <replicationFactor>                  Replication factor (default: 3)
   --blockSize <blockSize>                           Block size (default: 268435456)
   -h, --help                                        Prints help (default: )
   --props <propsFile>                               Config props file (default: )
   --printProps                                      Prints sample props file (default: )
  ```
