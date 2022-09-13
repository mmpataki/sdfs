package com.mmp.sdfs;

import com.mmp.sdfs.datanode.DatanodeApp;
import com.mmp.sdfs.namenode.NamenodeApp;
import com.mmp.sdfs.shell.Shell;

import java.util.Arrays;

public class Main {
    public static void main(String[] args) throws Exception {
        if (args.length != 0) {
            if(args[0].equals("namenode")) {
                NamenodeApp.main(Arrays.copyOfRange(args, 1, args.length));
            } else if(args[0].equals("datanode")) {
                DatanodeApp.main(Arrays.copyOfRange(args, 1, args.length));
            } else {
                Shell.main(args);
            }
        } else {
            Shell.main(args);
        }
    }
}
