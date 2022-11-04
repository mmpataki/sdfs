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

### Java API

##### Initialize the sdfs client config (SdfsClientConfig)
Config can be initialized with command line arguments syntax

```java
SdfsClientConfig conf = new SdfsClientConfig(new String[]{"--nnhost", "host.com"});
```

##### Reading a file
```java
// create a SdfsClient
SdfsClient client = new SdfsClient(conf);

// open the file
try (InputStream is = client.open(srcPpath)) {
    // use the `is` as a normal java.io input stream
}
```

##### Creating a file
```java
// create a SdfsClient
SdfsClient client = new SdfsClient(conf);

// create a file (fails if file exists)
try (OutputStream os = client.create(path)) {
    // use `os` as a normal java.io output stream
}
```

##### Submitting a job
This one runs a task for every block of a sdfs file.

```java
// create a SdfsClient
SdfsClient client = new SdfsClient(conf);

// create a Job
Job job = new Job();
job.setJobLabel("my job name");

// add artifacts needed for job execution
job.setArtifacts(Arrays.asList(
        new Pair<>("/path/to/local/file", "name_of_file_when_job_starts")
        new Pair<>("/path/to/local/job.jar", "bin/job.jar")
));

// create tasks
List<TaskDef> tasks = new ArrayList<>();
job.setTasks(tasks);

String remotePath = "/path/to/a/file/you/are/processing";
List<LocatedBlock> blocks = sdfsClient.getBlocks(remotePath);
for (int i = 0, blocksSize = blocks.size(); i < blocksSize; i++) {
    LocatedBlock block = blocks.get(i);
    long start = (long) i * sdfsConf.getBlockSize();
    TaskDef task = TaskDef.builder()
            .memNeeded(512 * 1024 * 1024)
            .cpuPercentNeeded(10)
            .taskLabel("chunk-" + start)
            .command(Arrays.asList("java", "-jar", "bin/job.jar", remotePath, String.valueOf(start)))
            .preferredNodes(block.getLocations().stream().map(DnAddress::getId).collect(Collectors.toList()))
            .build();
    tasks.add(task);
}

// submit
sdfsClient.submit(job, new SdfsClient.JobUpdateCallBack() {

    @Override
    public void jobUpdated(JobState js) {
        // this is a callback on job update, do something with it
        log.info("Job state updated {} ({}) : {}", js.getJobId(), js.getJobLabel(), js.getState());
    }

    @Override
    public void taskUpdated(TaskState taskState) {
        // this is a callback on task update, do something with it
        log.info("Task state updated {} ({}) {}", taskState.getTaskId(), taskState.getTaskLabel(), taskState.getState());
    }
});
```

### Available APIs
All the APIs can be found in

##### SdfsClient
/src/main/java/com/mmp/sdfs/client/SdfsClient.java

##### Headnode interface
/src/main/java/com/mmp/sdfs/hnwnrpc/HeadNode.java
