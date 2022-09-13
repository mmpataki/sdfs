package com.mmp.sdfs.shell.commands;

import com.mmp.sdfs.client.SdfsClient;

public interface ShellCommand {
    void execute(SdfsClient client) throws Exception;
}
