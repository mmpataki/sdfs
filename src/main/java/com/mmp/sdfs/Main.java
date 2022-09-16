package com.mmp.sdfs;

import com.mmp.sdfs.datanode.WorkerNodeApp;
import com.mmp.sdfs.namenode.HeadNodeApp;
import com.mmp.sdfs.shell.Shell;

import java.util.Arrays;

public class Main {
    public static void main(String[] args) throws Exception {
        if (args.length != 0) {
            if(args[0].equals("namenode")) {
                HeadNodeApp.main(Arrays.copyOfRange(args, 1, args.length));
            } else if(args[0].equals("datanode")) {
                WorkerNodeApp.main(Arrays.copyOfRange(args, 1, args.length));
            } else {
                Shell.main(args);
            }
        } else {
            Shell.main(args);
        }
    }
}
