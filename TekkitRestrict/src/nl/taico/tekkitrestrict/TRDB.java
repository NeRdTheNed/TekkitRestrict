package nl.taico.tekkitrestrict;

import static nl.taico.tekkitrestrict.config.SettingsStorage.databaseConfig;
import static nl.taico.tekkitrestrict.config.SettingsStorage.reloadConfigs;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import nl.taico.tekkitrestrict.Log.Warning;
import nl.taico.tekkitrestrict.database.DBException;
import nl.taico.tekkitrestrict.database.MySQL;
import nl.taico.tekkitrestrict.database.SQLite;
import nl.taico.tekkitrestrict.objects.TRDBSS;
import nl.taico.tekkitrestrict.objects.TREnums.DBType;

public class TRDB {
	private static boolean newdb = false;
	public static String antisqlinject(String ins) {
		return ins.replace("--", "")
				.replace("`", "")
				.replace("'", "")
				.replace("\"", "");
	}

	private static void dbFailMsg(final int fail){
		final String prefix = (TekkitRestrict.dbtype == DBType.MySQL ? "[MySQL] " : "[SQLite] ");
		if ((fail == 1) || (fail == 3) || (fail == 5))
			Warning.dbAndLoad(prefix+"The database will RESET upon next server startup because the version table couldn't be created!", true);
		if ((fail == 2) || (fail == 3) || (fail == 6))
			Warning.dbAndLoad(prefix+"Safezones will NOT work properly because the safezones table couldn't be created!", true);
		if ((fail == 4) || (fail == 5) || (fail == 6))
			Warning.dbAndLoad(prefix+"The limiter will NOT work properly because the limiter table couldn't be created!", true);
		else if (fail == 7) 
			Warning.dbAndLoad(prefix+"All database actions failed! Safezones and the limiter will NOT be stored!", true);
	}
	private static List<LinkedList<String>> getTableVals(final String table) throws SQLException {
		final ResultSet rs = TekkitRestrict.db.query("SELECT * FROM `"+table+"`");
		final List<LinkedList<String>> values = new LinkedList<LinkedList<String>>();
		if (rs == null) return values;
		while(rs.next()) {
			final LinkedList<String> row = new LinkedList<String>();
			for (int i=1;i<=10;i++){
				try {
					row.add(rs.getString(i));
				} catch (Exception ex){
					break;
				}
			}
			values.add(row);
		}
		rs.close();
		return values;
	}

	public static boolean initMySQL() {
		if (!TekkitRestrict.db.isOpen()) {
			if (!TekkitRestrict.db.open()){
				Warning.dbAndLoad("[MySQL] Cannot open the database connection!", false);
				TekkitRestrict.dbworking = 20;
				return false;
			}
		}

		ResultSet prev = null;

		try {
			final double verX;
			final boolean purged;
			prev = TekkitRestrict.db.query("SELECT version FROM tr_dbversion");
			if (prev == null) return false;
			if(prev.next()) verX = prev.getDouble("version");
			else verX = -1d;

			if(prev.next()) purged = false;
			else purged = true;

			prev.close();

			//Change version to 1.3 if it is lower
			if((verX != -1d) && (verX < TekkitRestrict.dbversion)){
				TekkitRestrict.db.query("DELETE FROM tr_dbversion;");//clear table
				TekkitRestrict.db.query("INSERT INTO tr_dbversion (version) VALUES(" + TekkitRestrict.dbversion + ");");//Insert new version
			} else if (!purged) {
				TekkitRestrict.db.query("DELETE FROM tr_dbversion;");//clear table
				TekkitRestrict.db.query("INSERT INTO tr_dbversion (version) VALUES(" + TekkitRestrict.dbversion + ");");//Insert new version
			}

		} catch(final Exception ex1){
			if(prev != null)
				try {prev.close();} catch (SQLException ex2) {}

			initNewMySQLDB();
		}
		if (TekkitRestrict.dbworking == 0) return true;
		return false;
	}
	private static void initNewMySQLDB(){
		TekkitRestrict.dbworking = 0;
		Log.info("[MySQL] Creating new database...");
		try {
			TekkitRestrict.db.query("CREATE TABLE IF NOT EXISTS tr_dbversion (version NUMERIC(3,2));");
			TekkitRestrict.db.query("REPLACE INTO tr_dbversion VALUES ("+TekkitRestrict.dbversion+");");
		} catch (Exception ex) {
			Warning.dbAndLoad("[MySQL] Unable to write version to database!", false);
			for (final StackTraceElement cur : ex.getStackTrace()){
				Warning.dbAndLoad("[MySQL] " + cur.toString(), false);
			}
			TekkitRestrict.dbworking += 1;
		}

		try {
			TekkitRestrict.db.query("CREATE TABLE IF NOT EXISTS tr_saferegion ( "
					+ "id INTEGER PRIMARY KEY AUTO_INCREMENT,"
					+ "name TINYTEXT,"
					+ "mode TINYINT UNSIGNED,"
					+ "data TINYTEXT,"
					+ "world TINYTEXT) CHARACTER SET latin1 COLLATE latin1_swedish_ci;");
		} catch (final Exception ex) {
			Warning.dbAndLoad("[MySQL] Unable to create safezones table!", true);
			for (final StackTraceElement cur : ex.getStackTrace()){
				Warning.dbAndLoad("[MySQL] " + cur.toString(), true);
			}

			TekkitRestrict.dbworking += 2;
		}

		try {
			TekkitRestrict.db.query("CREATE TABLE IF NOT EXISTS tr_limiter ( "
					+ "player VARCHAR(32) UNIQUE,"
					+ "blockdata TEXT) CHARACTER SET latin1 COLLATE latin1_swedish_ci;");
		} catch (final Exception ex) {
			Warning.dbAndLoad("[MySQL] Unable to create limiter table!", true);
			for (final StackTraceElement cur : ex.getStackTrace()){
				Warning.dbAndLoad("[MySQL] " + cur.toString(), true);
			}
			TekkitRestrict.dbworking += 4;
		}

		dbFailMsg(TekkitRestrict.dbworking);
		if (TekkitRestrict.dbworking != 0)
			Warning.dbAndLoad("[MySQL] Not all tables could be created!", true);
		else 
			Log.info("[MySQL] Database created successfully!");
	}

	private static void initNewSQLiteDB(){
		TekkitRestrict.dbworking = 0;
		Log.info("[SQLite] Creating new database...");
		try {
			TekkitRestrict.db.query("CREATE TABLE IF NOT EXISTS tr_dbversion (version NUMERIC);");
			TekkitRestrict.db.query("INSERT OR REPLACE INTO tr_dbversion (version) VALUES("+TekkitRestrict.dbversion+");");
		} catch (Exception ex) {
			Warning.dbAndLoad("[SQLite] Unable to write version to database!", false);
			for (final StackTraceElement cur : ex.getStackTrace()){
				Warning.dbAndLoad("[SQLite] " + cur.toString(), false);
			}
			TekkitRestrict.dbworking += 1;
		}

		try {
			TekkitRestrict.db.query("CREATE TABLE IF NOT EXISTS tr_saferegion ( "
					+ "id INTEGER PRIMARY KEY AUTOINCREMENT,"
					+ "name TEXT,"
					+ "mode INT,"
					+ "data TEXT,"
					+ "world TEXT);");
		} catch (final Exception ex) {
			Warning.dbAndLoad("[SQLite] Unable to create safezones table!", true);
			for (final StackTraceElement cur : ex.getStackTrace()){
				Warning.dbAndLoad("[SQLite] " + cur.toString(), true);
			}

			TekkitRestrict.dbworking += 2;
		}

		try {
			TekkitRestrict.db.query("CREATE TABLE IF NOT EXISTS tr_limiter ( "
					+ "player TEXT UNIQUE,"
					+ "blockdata TEXT);");
		} catch (final Exception ex) {
			Warning.dbAndLoad("[SQLite] Unable to create limiter table!",true);
			for (final StackTraceElement cur : ex.getStackTrace()){
				Warning.dbAndLoad("[SQLite] " + cur.toString(),true);
			}
			TekkitRestrict.dbworking += 4;
		}

		dbFailMsg(TekkitRestrict.dbworking);
		if (TekkitRestrict.dbworking != 0)
			Warning.dbAndLoad("[SQLite] Not all tables could be created!",true);
		else 
			Log.info("[SQLite] Database created successfully!");
	}
	public static boolean initSQLite() {
		if (!TekkitRestrict.db.isOpen()) {
			if (!TekkitRestrict.db.open()){
				Warning.dbAndLoad("[SQLite] Cannot open the database!", false);
				TekkitRestrict.dbworking = 20;
				return false;
			}
		}

		ResultSet prev = null;

		try {
			final double verX;
			final boolean purged;
			prev = TekkitRestrict.db.query("SELECT version FROM tr_dbversion");
			if (prev == null) return false;
			if(prev.next()) verX = prev.getDouble("version");
			else verX = -1d;

			if(prev.next()) purged = false;
			else purged = true;

			prev.close();

			if (verX == TekkitRestrict.dbversion){
				TekkitRestrict.db.query("DROP TABLE IF EXISTS tr_limiter_old");
			}

			//Change version to 1.3 if it is lower
			if((verX != -1d) && (verX < TekkitRestrict.dbversion)){
				TekkitRestrict.db.query("DELETE FROM tr_dbversion");//clear table
				TekkitRestrict.db.query("INSERT INTO tr_dbversion (version) VALUES(" + TekkitRestrict.dbversion + ");");//Insert new version
				transferSQLite12To13();//Transfer to version 1.3
			} else if (!purged) {
				TekkitRestrict.db.query("DELETE FROM tr_dbversion");//clear table
				TekkitRestrict.db.query("INSERT INTO tr_dbversion (version) VALUES(" + TekkitRestrict.dbversion + ");");//Insert new version
			}

		} catch(Exception ex1){
			if(prev != null)
				try {prev.close();} catch (SQLException ex2) {}

			if (newdb) initNewSQLiteDB();
			else transferOldSQLite();
		}
		if (TekkitRestrict.dbworking == 0) return true;
		return false;
	}

	/** @return If opening the database was successful. */
	public static boolean loadDB() {
		if (databaseConfig.getBoolean("TransferDBFromSQLiteToMySQL", false)){
			{
				String msg = "[DB] Transferring SQLite database to MySQL...";
				Warning.dbWarnings.add(msg);
				Warning.loadWarnings.add(msg);
				Log.info(msg);
			}

			TekkitRestrict.dbtype = DBType.SQLite;
			final File dbfile = new File(TekkitRestrict.getInstance().getDataFolder().getPath() + File.separator + "Data.db");
			if (!dbfile.exists()){
				TekkitRestrict.dbtype = DBType.Unknown;
				final String msg = "[DB] Unable to find SQLite database to transfer data to MySQL!";
				Warning.dbWarnings.add("[SEVERE] "+msg);
				Warning.loadWarnings.add("[SEVERE] "+msg);
				Log.severe(msg);
				return false;
			}

			TekkitRestrict.db = new SQLite("Data", TekkitRestrict.getInstance().getDataFolder().getPath());
			if (!TekkitRestrict.db.open()){
				TekkitRestrict.dbtype = DBType.Unknown;
				final String msg = "[DB] Unable to open SQLite database to transfer data to MySQL!";
				Warning.dbWarnings.add("[SEVERE] "+msg);
				Warning.loadWarnings.add("[SEVERE] "+msg);
				Log.severe(msg);
				return false;
			}

			if (transferSQLiteToMySQL()){
				final String msg = "[DB] Transferred SQLite database to MySQL successfully!";
				Warning.dbWarnings.add(msg);
				Warning.loadWarnings.add(msg);
				Log.info(msg);
			} else {
				final String msg = "[DB] Transferring SQLite database to MySQL failed!";
				Warning.dbWarnings.add("[SEVERE] "+ msg);
				Warning.loadWarnings.add("[SEVERE] "+ msg);
				Log.severe(msg);
				return false;
			}

			databaseConfig.set("TransferDBFromSQLiteToMySQL", true, false);
			reloadConfigs();

			final String type = databaseConfig.getString("DatabaseType", "sqlite").toLowerCase();
			if (!type.equals("mysql")){
				final String msg = "[DB] You have transferred to MySQL but you still have SQLite set as your preferred database type! TekkitRestrict will continue to use SQLite until you change it in the config!";
				Warning.dbWarnings.add("[SEVERE] "+ msg);
				Warning.loadWarnings.add("[SEVERE] "+ msg);
				Log.severe(msg);
			}
		} else if (databaseConfig.getBoolean("TransferDBFromMySQLToSQLite", false)){
			{
				final String msg = "[DB] Transferring MySQL database to SQLite...";
				Warning.dbWarnings.add(msg);
				Warning.loadWarnings.add(msg);
				Log.info(msg);
			}

			TekkitRestrict.dbtype = DBType.MySQL;
			final String host = databaseConfig.getString("MySQL.Hostname", "localhost");
			final String port = databaseConfig.getString("MySQL.Port", "3306");
			final String database = databaseConfig.getString("MySQL.Database", "minecraft");
			final String user = databaseConfig.getString("MySQL.Username", "root");
			final String password = databaseConfig.getString("MySQL.Password", "");
			try {
				TekkitRestrict.db = new MySQL(host, port, database, user, password);
			} catch (DBException ex){
				TekkitRestrict.dbtype = DBType.Unknown;
				final String msg = "[DB] Unable to connect to MySQL database to transfer data to SQLite!";
				Warning.dbWarnings.add("[SEVERE] "+msg);
				Warning.loadWarnings.add("[SEVERE] "+msg);
				Log.severe(msg);

				return false;
			}

			if (!TekkitRestrict.db.open()){
				TekkitRestrict.dbtype = DBType.Unknown;
				final String msg = "[DB] Unable to connect to MySQL database to transfer data to SQLite!";
				Warning.dbWarnings.add("[SEVERE] "+msg);
				Warning.loadWarnings.add("[SEVERE] "+msg);
				Log.severe(msg);
				return false;
			}

			if (transferMySQLToSQLite()){
				final String msg = "[DB] Transferred MySQL database to SQLite successfully!";
				Warning.dbWarnings.add(msg);
				Warning.loadWarnings.add(msg);
				Log.info(msg);
			} else {
				final String msg = "[DB] Transferring MySQL database to SQLite failed!";
				Warning.dbWarnings.add("[SEVERE] "+ msg);
				Warning.loadWarnings.add("[SEVERE] "+msg);
				Log.severe(msg);
				return false;
			}

			databaseConfig.set("TransferDBFromMySQLToSQLite", true, false);
			reloadConfigs();

			final String type = databaseConfig.getString("DatabaseType", "sqlite").toLowerCase();
			if (!type.equals("sqlite")){
				final String msg = "[DB] You have transferred to SQLite but you still have MySQL set as your preferred database type! TekkitRestrict will continue to use MySQL until you change it in the config!";
				Warning.dbWarnings.add("[SEVERE] "+ msg);
				Warning.loadWarnings.add("[SEVERE] "+msg);
				Log.severe(msg);
			}
		}

		final String type = databaseConfig.getString("DatabaseType", "sqlite").toLowerCase();

		if (type.equals("sqlite")){
			TekkitRestrict.dbtype = DBType.SQLite;
			final File dbfile = new File(TekkitRestrict.getInstance().getDataFolder().getPath() + File.separator + "Data.db");
			if (!dbfile.exists()){
				newdb = true;
				Log.info("[DB] Creating database file...");
			}
			TekkitRestrict.db = new SQLite("Data", TekkitRestrict.getInstance().getDataFolder().getPath());

			return TekkitRestrict.db.open();
		} else if (type.equals("mysql")){
			TekkitRestrict.dbtype = DBType.MySQL;
			final String host = databaseConfig.getString("MySQL.Hostname", "localhost");
			final String port = databaseConfig.getString("MySQL.Port", "3306");
			final String database = databaseConfig.getString("MySQL.Database", "minecraft");
			final String user = databaseConfig.getString("MySQL.Username", "root");
			final String password = databaseConfig.getString("MySQL.Password", "");
			try {
				TekkitRestrict.db = new MySQL(host, port, database, user, password);
			} catch (DBException ex){
				final String msg = "[MySQL] Error: " + ex.toString();
				Warning.dbWarnings.add("[SEVERE] "+msg);
				Warning.loadWarnings.add("[SEVERE] "+msg);
				Log.severe(msg);

				return false;
			}

			return TekkitRestrict.db.open();
		} else {
			TekkitRestrict.dbtype = DBType.Unknown;
			final String msg = "[DB] You set an unknown/unsupported database type! Supported: SQLite and MySQL.";
			Warning.dbWarnings.add("[SEVERE] "+msg);
			Warning.loadWarnings.add("[SEVERE] "+msg);
			Log.severe(msg);
			return false;
		}

	}

	public static boolean transferMySQLToSQLite(){
		ResultSet rs = null;
		try {
			rs = TekkitRestrict.db.query("SELECT * FROM tr_limiter;");
		} catch (final SQLException ex) {
			Warning.dbAndLoad("[MySQL] Unable to read limits from MySql Database! Error: "+ex.toString(), false);
			return false;
		}

		if (rs == null){
			Warning.dbAndLoad("[MySQL] Unable to read limits from MySql Database! Error: ResultSet is null", false);
			return false;
		}

		int i = 0;
		final HashMap<String, String> limits = new HashMap<String, String>();
		try {
			while (rs.next()){
				limits.put(rs.getString(1), rs.getString(2));
				i++;
			}
		} catch (final SQLException ex) {
			Warning.dbAndLoad("[MySQL] Unable to read limits from MySql Database! Error: "+ex.toString(), false);
			try {
				rs.close();
			} catch (SQLException ex2) {}
			return false;
		}

		Log.info("[DB] Found "+i+" limiters to transfer.");

		try {
			rs.close();
		} catch (final SQLException e) {}

		final HashMap<Integer, TRDBSS> safezones = new HashMap<Integer, TRDBSS>();
		try {
			rs = TekkitRestrict.db.query("SELECT * FROM tr_saferegion;");
		} catch (final SQLException ex) {
			Warning.dbAndLoad("[MySQL] Unable to read safezones from MySql Database! Error: "+ex.toString(), false);
			return false;
		}

		if (rs == null){
			Warning.dbAndLoad("[MySQL] Unable to read safezones from MySql Database! Error: ResultSet is null.", false);
			return false;
		}
		int j = 0;
		try {
			while (rs.next()){
				safezones.put(rs.getInt("id"), new TRDBSS(rs.getString("name"), rs.getInt("mode"), rs.getString("data"), rs.getString("world")));
				j++;
			}
		} catch (final SQLException ex) {
			Warning.dbAndLoad("[MySQL] Unable to read safezones from MySql Database! Error: "+ex.toString(), false);
			try {
				rs.close();
			} catch (SQLException ex2) {}
			return false;
		}

		try {
			rs.close();
		} catch (SQLException e) {}

		Log.info("[DB] Found "+j+" SafeZones to transfer.");

		TekkitRestrict.dbtype = DBType.SQLite;
		final String path = TekkitRestrict.getInstance().getDataFolder().getPath();
		File dbfile = new File(path + File.separator + "Data.db");
		final File backup = new File(path + File.separator + "Data_backup.db");
		if (dbfile.exists()){
			if (!dbfile.renameTo(backup)){
				backup.delete();
				dbfile = new File(path + File.separator + "Data.db");
				if (!dbfile.renameTo(backup)){
					Warning.dbAndLoad("[DB] Unable to delete/backup old database file!", false);
					return false;
				}
			}
			dbfile = new File(path + File.separator + "Data.db");
		}
		Log.info("[DB] Creating database file...");
		try {
			TekkitRestrict.db = new SQLite("Data", path);
		} catch (final DBException ex){
			Warning.dbAndLoad("[DB] Unable to create database file!", false);
			return false;
		}

		if (!TekkitRestrict.db.open()){
			Log.Warning.dbAndLoad("[SQLite] Unable to open Database!", false);
			return false;
		}

		initNewSQLiteDB();
		if (TekkitRestrict.dbworking != 0) return false;

		int k = i;
		final Iterator<Entry<String, String>> limitsit = limits.entrySet().iterator();
		while (limitsit.hasNext()){
			final Entry<String, String> entry = limitsit.next();
			try {
				TekkitRestrict.db.query("INSERT INTO tr_limiter (player, blockdata) VALUES (\""+entry.getKey()+"\", \""+entry.getValue()+"\");");
			} catch (final SQLException ex) {
				Warning.dbAndLoad("[SQLite] Unable to transfer limits of player " + entry.getKey() + "! Error: "+ex.toString(), true);
				k--;
				continue;
			}
		}

		Log.info("[DB] Transferred "+k+" out of "+i+" limiters successfully.");

		int l = j;
		final Iterator<Entry<Integer, TRDBSS>> ssit = safezones.entrySet().iterator();
		while (ssit.hasNext()){
			final Entry<Integer, TRDBSS> entry = ssit.next();
			final TRDBSS ss = entry.getValue();
			try {
				TekkitRestrict.db.query("INSERT INTO tr_saferegion (id, name, mode, data, world) VALUES ("+entry.getKey()+", \""+ss.name+"\", "+ss.mode+", \""+ss.data+"\", \""+ss.world+"\");");
			} catch (SQLException ex) {
				Warning.dbAndLoad("[SQLite] Unable to transfer safezone " + ss.name + "! Error: "+ex.toString(), true);
				l--;
				continue;
			}
		}

		Log.info("[DB] Transferred "+l+" out of "+j+" safezones successfully.");

		return true;
	}

	/** Transfer the database from PRE-1.00 version format to the new format. */
	private static void transferOldSQLite() {
		TekkitRestrict.dbworking = 0;
		Log.info("[SQLite] Transfering old database into the new database format...");

		List<LinkedList<String>> srvals = null, limvals=null;

		//tr_saferegion =	id name mode data world
		//tr_limiter = 		id player blockdata
		try {
			srvals = getTableVals("tr_saferegion");
		} catch(final SQLException ex){
			Warning.dbAndLoad("[SQLite] Unable to transfer safezones from the old format to the new one!", true);
		}
		try {
			limvals = getTableVals("tr_limiter");
		} catch(final SQLException ex){
			Warning.dbAndLoad("[SQLite] Unable to transfer limits from the old format to the new one!", true);
		}

		//Delete old tables
		try{TekkitRestrict.db.query("DROP TABLE tr_saferegion;");} catch(Exception ex){}
		try{TekkitRestrict.db.query("DROP TABLE tr_limiter;");} catch(Exception ex){}

		//################################### VERSION ###################################
		try {
			TekkitRestrict.db.query("CREATE TABLE IF NOT EXISTS tr_dbversion (version NUMERIC);");
			TekkitRestrict.db.query("INSERT OR REPLACE INTO tr_dbversion (version) VALUES("+TekkitRestrict.dbversion+");");
		} catch (Exception ex) {
			Warning.dbAndLoad("[SQLite] Unable to write version to database!", false);
			for (StackTraceElement cur : ex.getStackTrace()){
				Warning.dbAndLoad("[SQLite] " + cur.toString(), false);
			}
			TekkitRestrict.dbworking += 1;
		}
		//###############################################################################

		//################################## SAFEZONES ##################################
		try {
			TekkitRestrict.db.query("CREATE TABLE IF NOT EXISTS tr_saferegion ( "
					+ "id INTEGER PRIMARY KEY AUTOINCREMENT,"
					+ "name TEXT,"
					+ "mode INT,"
					+ "data TEXT,"
					+ "world TEXT); ");
		} catch (final Exception ex) {
			Warning.dbAndLoad("[SQLite] Unable to create safezones table!", true);
			for (StackTraceElement cur : ex.getStackTrace()){
				Warning.dbAndLoad("[SQLite] " + cur.toString(), true);
			}

			TekkitRestrict.dbworking += 2;
		}

		try {
			//Import safezones
			if(srvals != null){
				for(final LinkedList<String> vals : srvals){
					String toadd = "";
					for(final String str : vals) toadd+=","+str;
					//toadd = toadd.replace("null", "''");
					if(toadd.startsWith(",")) toadd=toadd.substring(1, toadd.length());
					TekkitRestrict.db.query("INSERT INTO tr_saferegion VALUES("+toadd+");");
				}

				Log.info("[SQLite] Transferred " + srvals.size() + " safezones.");
			}
		} catch (final Exception ex) {
			Warning.dbAndLoad("[SQLite] Unable to write safezones to database!", true);
			for (final StackTraceElement cur : ex.getStackTrace()){
				Warning.dbAndLoad("[SQLite] " + cur.toString(), true);
			}
		}
		//###############################################################################

		//################################### LIMITER ###################################
		try {
			TekkitRestrict.db.query("CREATE TABLE IF NOT EXISTS tr_limiter ( "
					+ "player TEXT UNIQUE,"
					+ "blockdata TEXT);");
		} catch (final Exception ex) {
			Warning.dbAndLoad("[SQLite] Unable to create limiter table!", true);
			for (final StackTraceElement cur : ex.getStackTrace()){
				Warning.dbAndLoad("[SQLite] " + cur.toString(), true);
			}
			TekkitRestrict.dbworking += 4;
		}

		try {
			if(limvals != null){
				for(final LinkedList<String> vals:limvals){
					String toadd = "";
					for(final String str:vals) toadd+=","+str;
					if(toadd.startsWith(",")) toadd=toadd.substring(1, toadd.length());
					TekkitRestrict.db.query("INSERT INTO tr_limiter VALUES("+toadd+");");
				}

				Log.info("[SQLite] Transferred "+ limvals.size() + " limits.");
			}
		} catch (final Exception ex) {
			Warning.dbAndLoad("[SQLite] Unable to write limits to database!", true);
			for (final StackTraceElement cur : ex.getStackTrace()){
				Warning.dbAndLoad("[SQLite] " + cur.toString(), true);
			}
		}
		if (TekkitRestrict.dbworking == 0) {
			Log.info("[SQLite] Transfering into the new database format succeeded!");
		} else {
			dbFailMsg(TekkitRestrict.dbworking);
			Warning.dbAndLoad("[SQLite] Transfering into the new database format failed!", true);
		}

	}

	private static void transferSQLite12To13(){
		Log.info("[SQLite] Updating Database to new format...");
		try {
			TekkitRestrict.db.query("ALTER TABLE tr_limiter RENAME TO tr_limiter_old");
			TekkitRestrict.db.query("CREATE TABLE tr_limiter ("
					+ "player TEXT UNIQUE,"
					+ "blockdata TEXT);");
			TekkitRestrict.db.query("INSERT INTO tr_limiter (player, blockdata) SELECT player, blockdata FROM tr_limiter_old ORDER BY player ASC");
			TekkitRestrict.db.query("DROP TABLE IF EXISTS tr_limiter_old");
		} catch (final SQLException ex) {
			Warning.dbAndLoad("[SQLite] Error while updating db!", false);
			for (final StackTraceElement st : ex.getStackTrace()){
				Warning.dbAndLoad("[SQLite] " + st.toString(), false);
			}
		}
	}

	public static boolean transferSQLiteToMySQL(){
		ResultSet rs = null;
		try {
			rs = TekkitRestrict.db.query("SELECT * FROM tr_limiter;");
		} catch (final SQLException ex) {
			Warning.dbAndLoad("[SQLite] Unable to read limits from SQLite Database! Error: "+ex.toString(), false);
			return false;
		}

		if (rs == null){
			Warning.dbAndLoad("[SQLite] Unable to read limits from SQLite Database! Error: ResultSet is null", false);
			return false;
		}

		int i = 0;
		final HashMap<String, String> limits = new HashMap<String, String>();
		try {
			while (rs.next()){
				limits.put(rs.getString(1), rs.getString(2));
				i++;
			}
		} catch (final SQLException ex) {
			Warning.dbAndLoad("[SQLite] Unable to read limits from SQLite Database! Error: "+ex.toString(), false);
			try {
				rs.close();
			} catch (SQLException ex2) {}
			return false;
		}

		Log.info("[DB] Found "+i+" limiters to transfer.");

		try {
			rs.close();
		} catch (SQLException e) {}

		final HashMap<Integer, TRDBSS> safezones = new HashMap<Integer, TRDBSS>();
		try {
			rs = TekkitRestrict.db.query("SELECT * FROM tr_saferegion;");
		} catch (SQLException ex) {
			Warning.dbAndLoad("[SQLite] Unable to read safezones from SQLite Database! Error: "+ex.toString(), false);
			return false;
		}

		if (rs == null){
			Warning.dbAndLoad("[SQLite] Unable to read safezones from SQLite Database! Error: ResultSet is null.", false);
			return false;
		}
		int j = 0;
		try {
			while (rs.next()){
				safezones.put(rs.getInt("id"), new TRDBSS(rs.getString("name"), rs.getInt("mode"), rs.getString("data"), rs.getString("world")));
				j++;
			}
		} catch (final SQLException ex) {
			Warning.dbAndLoad("[SQLite] Unable to read safezones from SQLite Database! Error: "+ex.toString(), false);
			try {
				rs.close();
			} catch (SQLException ex2) {}
			return false;
		}

		try {
			rs.close();
		} catch (SQLException e) {}

		Log.info("[DB] Found "+j+" SafeZones to transfer.");

		TekkitRestrict.dbtype = DBType.MySQL;
		final String host = databaseConfig.getString("MySQL.Hostname", "localhost");
		final String port = databaseConfig.getString("MySQL.Port", "3306");
		final String database = databaseConfig.getString("MySQL.Database", "minecraft");
		final String user = databaseConfig.getString("MySQL.Username", "root");
		final String password = databaseConfig.getString("MySQL.Password", "");
		try {
			TekkitRestrict.db = new MySQL(host, port, database, user, password);
		} catch (DBException ex){
			final String msg = "[MySQL] Unable to connect to MySQL database! Error: " + ex.toString();
			Warning.dbWarnings.add("[SEVERE] "+msg);
			Warning.loadWarnings.add("[SEVERE] "+msg);
			Log.severe(msg);

			return false;
		}

		if (!TekkitRestrict.db.open()){
			Log.Warning.dbAndLoad("[MySQL] Unable to connect to Database!", false);
			return false;
		}

		initNewMySQLDB();
		if (TekkitRestrict.dbworking != 0) return false;

		int k = i;
		final Iterator<Entry<String, String>> limitsit = limits.entrySet().iterator();
		while (limitsit.hasNext()){
			final Entry<String, String> entry = limitsit.next();
			try {
				TekkitRestrict.db.query("INSERT INTO tr_limiter (player, blockdata) VALUES (\""+entry.getKey()+"\", \""+entry.getValue()+"\");");
			} catch (SQLException ex) {
				Warning.dbAndLoad("[MySQL] Unable to transfer limits of player " + entry.getKey() + "! Error: "+ex.toString(), true);
				k--;
				continue;
			}
		}

		Log.info("[DB] Transferred "+k+" out of "+i+" limiters successfully.");

		int l = j;
		final Iterator<Entry<Integer, TRDBSS>> ssit = safezones.entrySet().iterator();
		while (ssit.hasNext()){
			final Entry<Integer, TRDBSS> entry = ssit.next();
			final TRDBSS ss = entry.getValue();
			try {
				TekkitRestrict.db.query("INSERT INTO tr_saferegion (id, name, mode, data, world) VALUES ("+entry.getKey()+", \""+ss.name+"\", "+ss.mode+", \""+ss.data+"\", \""+ss.world+"\");");
			} catch (SQLException ex) {
				Warning.dbAndLoad("[MySQL] Unable to transfer safezone " + ss.name + "! Error: "+ex.toString(), true);
				l--;
				continue;
			}
		}

		Log.info("[DB] Transferred "+l+" out of "+j+" safezones successfully.");

		return true;
	}
}
