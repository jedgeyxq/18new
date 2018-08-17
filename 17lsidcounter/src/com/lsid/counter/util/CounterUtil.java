package com.lsid.counter.util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Vector;

import com.lsid.autoconfig.client.AutoConfig;

public class CounterUtil {
	public static final Path incr = Paths.get(AutoConfig.config(null, "lsidcountercachefolder")).resolve("incr");
	public static final Path decr = Paths.get(AutoConfig.config(null, "lsidcountercachefolder")).resolve("decr");
	public static final Path tosort = Paths.get(AutoConfig.config(null, "lsidcountercachefolder")).resolve("sort")
			.resolve("tosort");
	public static final Path position = Paths.get(AutoConfig.config(null, "lsidcountercachefolder")).resolve("sort")
			.resolve("sortedall");

	private static Vector<String> synchronizedfiles = new Vector<String>(1000000);

	private synchronized static void initsynchronizedfiles(String namespace) {
		Integer filehash = Integer.parseInt(AutoConfig.config(namespace, "lsid.interface.cache.filehash"));
		if (synchronizedfiles.size() < filehash) {
			int start = synchronizedfiles.size();
			for (int i = start; i < filehash; i++) {
				synchronizedfiles.add(String.valueOf(i));
			}
		}
	}

	public static long incrementcache(String namespace, String tablename, String column, String hash, String row,
			long toincrement) throws Exception {
		Path to = incrfile(namespace, tablename, column, hash, row);
		if (toincrement < 0) {
			to = decrfile(namespace, tablename, column, hash, row);
		}
		if (!Files.exists(to.getParent())) {
			Files.createDirectories(to.getParent());
		}
		initsynchronizedfiles(namespace);
		synchronized (synchronizedfiles.get(Integer.parseInt(to.getParent().getFileName().toString()))) {
			Files.write(to, new byte[0], StandardOpenOption.CREATE, StandardOpenOption.APPEND, StandardOpenOption.SYNC);
			if (toincrement != 0) {
				long total = Math.abs(toincrement);
				int steps = 10000;
				while (total - steps > 0) {
					Files.write(to, new byte[steps], StandardOpenOption.CREATE, StandardOpenOption.APPEND,
							StandardOpenOption.SYNC);
					total -= steps;
				}
				Files.write(to, new byte[(int) total], StandardOpenOption.CREATE, StandardOpenOption.APPEND,
						StandardOpenOption.SYNC);
			}
			return incrementedcache(namespace, tablename, column, hash, row);
		}
	}

	public static long incrementedcache(String namespace, String tablename, String column, String hash, String row)
			throws Exception {
		Path incrfile = incrfile(namespace, tablename, column, hash, row);
		Path decrfile = decrfile(namespace, tablename, column, hash, row);
		long increment = 0;
		long decrease = 0;
		if (Files.exists(incrfile)) {
			increment = Files.size(incrfile);
		}
		if (Files.exists(decrfile)) {
			decrease = Files.size(decrfile);
		}
		return increment - decrease;
	}

	public static Path incrfile(String namespace, String tablename, String column, String hash, String row) {
		return incr.resolve(namespace).resolve(tablename).resolve(column).resolve(hash(namespace, hash)).resolve(row);
	}

	public static Path positionfile(String namespace, String tablename, String column, String hash, String row) {
		return position.resolve(namespace).resolve(tablename).resolve(column).resolve(hash(namespace, hash))
				.resolve(row);
	}

	public static Path decrfile(String namespace, String tablename, String column, String hash, String row) {
		return decr.resolve(namespace).resolve(tablename).resolve(column).resolve(hash(namespace, hash)).resolve(row);
	}

	public static boolean needcache(String namespace, String tablename, String column, String hash, String row) {
		if (!Files.exists(incrfile(namespace, tablename, column, hash, row))
				&& !Files.exists(decrfile(namespace, tablename, column, hash, row))) {
			return true;
		}
		return false;
	}

	public static synchronized void cache(String namespace, String tablename, String column, String hash, String row)
			throws Exception {
		Path incrfile = incrfile(namespace, tablename, column, hash, row);
		Path decrfile = decrfile(namespace, tablename, column, hash, row);
		if (!Files.exists(incrfile) && !Files.exists(decrfile)) {
			String current = AutoConfig.innerpost(AutoConfig.rotation(null, "lsid.interface.hbase.read"),
					Integer.parseInt(AutoConfig.config(null, "lsid.interface.hbase.connectimeoutinsec")),
					Integer.parseInt(AutoConfig.config(null, "lsid.interface.hbase.socketimeoutinsec")), "eid",
					namespace, "hash", hash, "table", tablename, "row", row, "col", column, "long", "");
			long amount = 0;
			try {
				amount = Long.parseLong(current);
			} catch (Exception ex) {
				// do nothing
			}
			incrementcache(namespace, tablename, column, hash, row, amount);
		}
	}

	private static String hash(String eid, String hash) {
		return String.valueOf(
				Math.abs(hash.hashCode()) % Integer.parseInt(AutoConfig.config(eid, "lsid.interface.cache.filehash")));
	}

}
