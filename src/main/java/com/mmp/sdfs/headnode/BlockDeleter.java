package com.mmp.sdfs.headnode;

import com.mmp.sdfs.client.DNClient;
import com.mmp.sdfs.conf.HeadNodeConfig;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.mmp.sdfs.utils.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@AllArgsConstructor
public class BlockDeleter {
    private final HeadNodeConfig conf;
    private final NameStore store;
    private final Map<String, DnRef> dns;

    public void start() {
        new Thread(() -> {
            while (true) {
                try {
                    List<Pair<String, String>> deletedBlocks = store.getDeletedBlocks();
                    log.trace("Deleting blocks : {}", deletedBlocks);
                    Map<String, List<String>> grouped = new HashMap<>();
                    for (Pair<String, String> db : deletedBlocks)
                        grouped.computeIfAbsent(db.getSecond(), k -> new ArrayList<>()).add(db.getFirst());
                    grouped.forEach((dnid, blocks) -> {
                        try {
                            if (dns.containsKey(dnid)) {
                                List<String> deleted = new DNClient(conf).deleteBlocks(dns.get(dnid).getAddr(), blocks);
                                store.blocksDeleted(deleted.stream().map(d -> new Pair<>(dnid, d)).collect(Collectors.toList()));
                            } else {
                                log.warn("Dn {} is not available, available dns = {}", dnid, dns);
                            }
                        } catch (Exception e) {
                            log.error("Error while deleting blocks from " + dnid + " " + blocks, e);
                        }
                    });
                    Thread.sleep(10 * 1000);
                } catch (Exception e) {
                    log.error("Error while deleted blocks, this will be retried", e);
                }
            }
        }, "Block-deleter").start();
    }
}
