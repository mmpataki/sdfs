package com.mmp.sdfs.conf;

import lombok.Getter;
import lombok.Setter;
import com.mmp.sdfs.rpc.RpcSerde;

@Getter
@Setter
public class SdfsConfig extends Configuration {

    @Argument(keys = {"-ip", "--infoport"}, required = true, help = "info port", defValue = "5002")
    int infoPort;

    @Argument(keys = {"--nnhost"}, help = "Namenode host", defValue = "localhost")
    String nnHost;

    @Argument(keys = {"--nnport"}, help = "Namenode port", defValue = "5001")
    int nnPort;

    @Argument(keys = {"--rpcSerde"}, help = "The RPC serde class used to serialize/deserialize RPC messages", parser = "classInstatiator", defValue = "com.mmp.sdfs.rpc.javaserde.JavaSerde")
    RpcSerde rpcSerde;

    @Argument(keys = {"--replFactor"}, help = "Replication factor", defValue = "3")
    int replicationFactor;

    @Argument(keys = {"--blockSize"}, help = "Block size", defValue = "268435456")
    int blockSize;

    @Argument(keys = {"--heartBeatInterval"}, help = "Heart beat interval", defValue = "10000")
    int heartBeatInterval;

    public SdfsConfig(String[] args) throws Exception {
        super(args);
    }

}
