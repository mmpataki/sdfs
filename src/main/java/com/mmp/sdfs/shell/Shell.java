package com.mmp.sdfs.shell;

import com.mmp.sdfs.client.SdfsClient;
import com.mmp.sdfs.client.SdfsInputStream;
import com.mmp.sdfs.client.SdfsOutputStream;
import com.mmp.sdfs.common.FileStat;
import com.mmp.sdfs.conf.Argument;
import com.mmp.sdfs.conf.SdfsClientConfig;
import com.mmp.sdfs.utils.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public abstract class Shell extends SdfsClientConfig {

    SdfsClient client;

    public Shell(String[] args) throws Exception {
        super(args);
        if (this.isHelp()) {
            System.out.println(getHelpString());
            System.exit(0);
        }
        client = new SdfsClient(this);
    }

    abstract void execute(SdfsClient client) throws Exception;

    public void run() throws Exception {
        execute(client);
    }

    static class ListCommand extends Shell {

        @Argument(keys = {"-p", "--path"}, help = "Path to list", defValue = "")
        String path;

        public ListCommand(String[] args) throws Exception {
            super(args);
        }

        @Override
        public void execute(SdfsClient client) throws Exception {
            String fmt = "%-5s %-8s %-10s %10s %s\n";
            System.out.printf(fmt, "Id", "Replicas", "Size", "Owner", "Path");
            for (FileStat fs : client.list(path)) {
                System.out.printf(fmt, fs.getId(), fs.getReplicas(), fs.getSize(), fs.getOwner(), fs.getPath());
            }
        }
    }

    static class DeleteCommand extends Shell {

        @Argument(keys = {"-p", "--path"}, help = "Path to list", defValue = "")
        String path;

        public DeleteCommand(String[] args) throws Exception {
            super(args);
        }

        @Override
        public void execute(SdfsClient client) throws Exception {
            client.delete(path);
        }
    }

    static class UploadCommand extends Shell {

        @Argument(keys = {"--src"}, help = "Source path (local path)", defValue = "")
        String srcPpath;

        @Argument(keys = {"--dst"}, help = "Destination path (sdfs path)", defValue = "")
        String dstPpath;

        public UploadCommand(String[] args) throws Exception {
            super(args);
        }

        @Override
        public void execute(SdfsClient client) throws Exception {
            File file = new File(srcPpath);
            try (SdfsOutputStream dos = client.create(dstPpath)) {
                try (FileInputStream fis = new FileInputStream(file)) {
                    IOUtils.copy(fis, dos, (int) file.length());
                }
            }
        }
    }

    static class DownloadCommand extends Shell {

        @Argument(keys = {"--src"}, help = "Source path (sdfs path)", defValue = "")
        String srcPpath;

        @Argument(keys = {"--dst"}, help = "Destination path (local path)", defValue = "")
        String dstPpath;

        public DownloadCommand(String[] args) throws Exception {
            super(args);
        }

        @Override
        public void execute(SdfsClient client) throws Exception {
            File file = new File(srcPpath);
            try (SdfsInputStream is = client.open(srcPpath)) {
                try (FileOutputStream fos = new FileOutputStream(dstPpath)) {
                    IOUtils.copy(is, fos, (int) file.length());
                }
            }
        }
    }


    static Map<String, Class<?>> commands = new HashMap<String, Class<?>>() {{
        put("list", ListCommand.class);
        put("put", UploadCommand.class);
        put("get", DownloadCommand.class);
        put("rm", DeleteCommand.class);
    }};

    static void printValidCommands() {
        System.out.println("Commands available: ");
        commands.keySet().forEach(k -> System.out.println("   " + k));
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.out.println("Usage: Shell <command> ...args");
            printValidCommands();
        } else {
            if (!commands.containsKey(args[0])) {
                System.out.println(args[0] + " is not a valid command");
                printValidCommands();
            } else {
                Shell shell = (Shell) commands
                        .get(args[0])
                        .getConstructor(String[].class)
                        .newInstance(new Object[]{Arrays.copyOfRange(args, 1, args.length)});
                shell.run();
                System.exit(0);
            }
        }
    }
}
