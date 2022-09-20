package com.mmp.sdfs.headnode;

import com.mmp.sdfs.common.DnAddress;
import com.mmp.sdfs.common.FileStat;
import com.mmp.sdfs.common.LocatedBlock;
import com.mmp.sdfs.conf.HeadNodeConfig;
import com.mmp.sdfs.utils.Pair;
import lombok.extern.slf4j.Slf4j;

import java.io.FileNotFoundException;
import java.sql.*;
import java.util.*;

@Slf4j
public class SqliteNameStore extends NameStore {

    private final Connection conn;

    public SqliteNameStore(HeadNodeConfig conf, Map<String, DnRef> dataNodes) throws Exception {
        super(conf, dataNodes);
        conn = DriverManager.getConnection(String.format("jdbc:sqlite:%s/store.db", conf.getNamedir()), "", "");
        conn.setAutoCommit(true);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Closing sqlite conn in shutdown hook");
            try {
                conn.close();
            } catch (SQLException e) {
                log.error("Error while closing connection in shutdown hook");
            }
        }));
        exec("create table if not exists files (path string, owner string, replicas integer, size integer)");
        exec("create table if not exists blocks (block_id string, pathid integer)");
        exec("create table if not exists block_locations (block_id string, dn_id string, deleted integer)");
    }

    private void exec(String s) throws SQLException {
        try (Statement statement = conn.createStatement()) {
            statement.execute(s);
        }
    }

    private FileStat getPathId(String path) throws Exception {
        try (PreparedStatement rps = conn.prepareStatement("select rowid, path, owner, replicas, size from files where path = ?")) {
            rps.setString(1, path);
            rps.execute();
            ResultSet rs = rps.getResultSet();
            if (rs.next()) {
                return new FileStat(path, rs.getString(3), rs.getLong(1), rs.getLong(4), rs.getLong(5));
            }
        }
        throw new FileNotFoundException(path);
    }

    @Override
    public FileStat create(String path, String owner, int replicationFactor) throws Exception {
        try {
            FileStat stat = getPathId(path);
            throw new Exception("File exists");
        } catch (FileNotFoundException ignore) {
            PreparedStatement ps = conn.prepareStatement("insert into files (path, owner, replicas) values (?, ?, ?)");
            ps.setString(1, path);
            ps.setString(2, owner);
            ps.setInt(3, replicationFactor);
            ps.execute();
            return getPathId(path);
        }
    }


    @Override
    public void delete(String path) throws Exception {
        FileStat stat = getPathId(path);
        List<LocatedBlock> blocks = getBlocks(path);
        try (PreparedStatement dlps = conn.prepareStatement("update block_locations set deleted = 1 where block_id = ?")) {
            for (LocatedBlock block : blocks) {
                dlps.setString(1, block.getId());
                dlps.addBatch();
            }
            dlps.execute();
        }
        try (PreparedStatement dbps = conn.prepareStatement("delete from blocks where pathid = ?")) {
            dbps.setLong(1, stat.getId());
            dbps.execute();
        }
        try (PreparedStatement fps = conn.prepareStatement("delete from files where path = ?")) {
            fps.setString(1, path);
            fps.execute();
        }
    }

    @Override
    public LocatedBlock addBlock(String path, String user) throws Exception {
        FileStat fs = getPathId(path);
        if (!user.equals(fs.getOwner())) {
            throw new Exception(String.format("Access denied, path=%s, user:%s, you: %s", path, fs.getOwner(), user));
        }
        String bId = UUID.randomUUID().toString();
        try (PreparedStatement wps = conn.prepareStatement("insert into blocks (pathid, block_id) values (?, ?)")) {
            wps.setLong(1, fs.getId());
            wps.setString(2, bId);
            wps.execute();
        }
        List<DnAddress> newLocations = getDnLocations((int) fs.getReplicas());

        try (PreparedStatement dnps = conn.prepareStatement("insert into block_locations (block_id, dn_id, deleted) values (?, ?, 0)")) {
            for (DnAddress newLocation : newLocations) {
                dnps.setString(1, bId);
                dnps.setString(2, newLocation.getId());
                dnps.addBatch();
            }
            dnps.execute();
        }
        return new LocatedBlock(bId, newLocations);
    }

    @Override
    public List<LocatedBlock> getBlocks(String path) throws Exception {
        FileStat stat = getPathId(path);
        List<LocatedBlock> ret = new LinkedList<>();
        try (PreparedStatement rps = conn.prepareStatement("select block_id from blocks where pathid = ?")) {
            rps.setLong(1, stat.getId());
            rps.execute();
            ResultSet rs = rps.getResultSet();
            while (rs.next()) {
                String bId = rs.getString(1);
                List<DnAddress> dnLocations = new LinkedList<>();
                try (PreparedStatement dps = conn.prepareStatement("select block_id, dn_id from block_locations where block_id = ?")) {
                    dps.setString(1, bId);
                    dps.execute();
                    ResultSet drs = dps.getResultSet();
                    while (drs.next()) {
                        if (dataNodes.containsKey(drs.getString(2)))
                            dnLocations.add(dataNodes.get(drs.getString(2)).getAddr());
                    }
                }
                ret.add(new LocatedBlock(bId, dnLocations));
            }
        }
        return ret;
    }

    @Override
    public List<FileStat> list(String basePath) throws Exception {
        List<FileStat> fstats = new LinkedList<>();
        try (PreparedStatement ps = conn.prepareStatement(String.format("select rowid, path, owner, replicas, size from files where path like '%s/%%'", basePath == null ? "" : basePath))) {
            ps.execute();
            ResultSet rs = ps.getResultSet();
            while (rs.next())
                fstats.add(new FileStat(rs.getString(2), rs.getString(3), rs.getLong(1), rs.getLong(4), rs.getLong(5)));
        }
        return fstats;
    }

    @Override
    public List<Pair<String, String>> getDeletedBlocks() throws Exception {
        List<Pair<String, String>> blks = new ArrayList<>();
        try (PreparedStatement dps = conn.prepareStatement("select block_id, dn_id from block_locations where deleted = 1")) {
            dps.execute();
            ResultSet rs = dps.getResultSet();
            while (rs.next()) {
                blks.add(new Pair<>(rs.getString(1), rs.getString(2)));
            }
        }
        return blks;
    }

    @Override
    public void blocksDeleted(List<Pair<String, String>> deleted) throws SQLException {
        try (PreparedStatement dps = conn.prepareStatement("delete from block_locations where block_id = ? and dn_id = ?")) {
            for (Pair<String, String> dp : deleted) {
                dps.setString(1, dp.getSecond());
                dps.setString(2, dp.getFirst());
                dps.addBatch();
            }
            dps.execute();
        }
    }

    @Override
    public void close(String path, long size) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("update files set size = ? where path = ?")) {
            ps.setLong(1, size);
            ps.setString(2, path);
            ps.execute();
        }
    }

    @Override
    public FileStat get(String path) throws Exception {
        return getPathId(path);
    }
}
