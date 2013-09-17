package com.github.dreadslicer.tekkitrestrict;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import com.github.dreadslicer.tekkitrestrict.Log.Warning;
import com.github.dreadslicer.tekkitrestrict.annotations.Safe;
import com.github.dreadslicer.tekkitrestrict.objects.TREnums.ConfigFile;

public class TRCacheItem {
	//Note:
	//Block ID's Forge: 0-4095
	//Item ID's: 4096-32000
	//ID's: 5 chars
	//Block Dmg values: 0-15 (4bits)
	//Item Dmg values: 0-65536 (2 bytes)
	//Data: 5 chars
	
	// Pre-caches items into a range.
	private static ConcurrentHashMap<String, TRCacheItem> cache = new ConcurrentHashMap<String, TRCacheItem>();
	private static ConcurrentHashMap<String, List<TRCacheItem>> cacheMods = new ConcurrentHashMap<String, List<TRCacheItem>>();
	
	/**
	 * There are 3 cache types: "l" (limiter), "n" (noitem) and "c" (creative)<br>
	 * CachePermTypes contains a HashSet&lt;String&gt; for each of these values.
	 */
	private static ConcurrentHashMap<String, Set<String>> cachePermTypes = new ConcurrentHashMap<String, Set<String>>();
	
	private static String[] modItems = new String[] {
		"ee=27520-27599;126-130",
		"buildcraft=153-174;4056-4066;4298-4324",
		"additionalpipes=4299-4305;179",
		"industrialcraft=219-223;225-250;30171-30256",
		"nuclearcontrol=192;31256-31260", "powerconverters=190",
		"compactsolars=183", "chargingbench=187",
		"advancedmachines=253-254;188-191", "redpowercore=136",
		"redpowerlogic=138;1258-1328", "redpowercontrol=133-134;148",
		"redpowermachine=137;150-151", "redpowerlighting=147",
		"wirelessredstone=177;6358-6363;6406;6408-6412",
		"mffs=253-254;11366-11374", "railcraft=206-215;7256-7316",
		"tubestuffs=194", "ironchests=19727-19762;181",
		"balkonweaponmod=26483-26530", "enderchest=178;7493",
		"chunkloaders=4095;214;7303;179"
	};

	protected TRCacheItem() {}

	public int id = -1;
	public int data;
	private String cacher = "";
	private int Data1 = -1;
	private Object Data2 = -1;

	public int getIntData() {
		return Data1;
	}
	public void setIntData(int x) {
		Data1 = x;
	}
	public Object getObjectData() {
		return Data2;
	}
	public void setObjectData(Object x) {
		Data2 = x;
	}

	/**
	 * Compare this CacheObject with the given id and data
	 * @return True if:<br>
	 * <ul>
	 * <li>this.id == -11</li>
	 * <li>this.id == id AND this.data == data</li>
	 * <li>this.id == id AND this.data == -10 AND data == 0</li>
	 * </ul>
	 */
	@Safe
	public boolean compare(int id, int data) {
		//boolean isAllDataTypes = (this.data == -10 && data == 0);
		//boolean isALL = (this.id == -11);
		return (this.id == -11) || ((this.id == id && (this.data == data || (this.data == -10 && data == 0))));
	}

	/** @return A string representation of this Cache Item: "id:data" */
	@Override
	public String toString() {
		return new StringBuilder(12).append(id).append(":").append(data).toString();
	}
	
	@Override
	public Object clone(){
		TRCacheItem tci = new TRCacheItem();
		tci.id = this.id;
		tci.data = this.data;
		tci.cacher = this.cacher;
		tci.setIntData(this.Data1);
		tci.setObjectData(this.Data2);
		return tci;
	}

	public static void reload() {
		clearCache();
		for (String s : modItems) {
			if (s.contains("=")) {
				String[] gg = s.split("=");
				String mod = gg[0];
				TRNoItem.aasdf(mod, addCacheList(mod, processModString("", mod, gg[1])));
			}
		}

		// pre-load variables
		ConfigurationSection groups = tekkitrestrict.config.getConfigurationSection(ConfigFile.GroupPermissions, "PermissionGroups");
		if (groups != null) {
			pstring("", groups);
		}
		/*
		ConfigurationSection micro = tekkitrestrict.config.getConfigurationSection(ConfigFile.MicroPermissions, "MicroPermissions");
		if (micro != null) {
			ConfigurationSection cNoItem = micro.getConfigurationSection("NoItem");
			//ConfigurationSection cBlockLimiter = micro.getConfigurationSection("BlockLimiter");
			ConfigurationSection cLimitedCreative = micro.getConfigurationSection("LimitedCreative");
			if (cNoItem != null) 
				pstring("n", cNoItem);
			
			//if (cBlockLimiter != null) 
			//	pstring("l", cBlockLimiter);

			if (cLimitedCreative != null) 
				pstring("c", cLimitedCreative);
		}
		*/
	}

	private static void pstring(String permType, ConfigurationSection cs) {
		Set<String> keys = cs.getKeys(true);
		Iterator<String> keyIterator = keys.iterator();
		while (keyIterator.hasNext()) {
			try {
				String key = keyIterator.next().toLowerCase();
				String value = cs.getString(key);

				if (value == null || value.equals("")) continue;
				
				int i = -1;
				if (value.contains(" ")) {
					Log.Warning.config("Invalid value in PermissionGroups: Invalid value \""+value+"\"!");
					continue;
					/*
					try {
						String[] c = value.split(" ");
						i = Integer.parseInt(c[1]);
						value = c[0];
					} catch (NumberFormatException e) {
						
					}
					*/
				}
				List<TRCacheItem> cacheItems = TRCacheItem.processMultiString(permType, key, value, i);
				TRNoItem.aasdf(key, cacheItems);

			} catch (Exception ex) {
				tekkitrestrict.log.warning("Error in PermissionGroups: " + ex.getMessage());
				Log.Exception(ex, false);
			}
		}
	}

	/*
	 * public static boolean hasItem(String permType,String type,int id, int data){
	 * 		return hasItem(permType+";"+type,id,data);
	 * }
	 * 
	 * public static boolean hasItem(String type,int id, int data){
	 * 		return hasItem(type+"="+id+":"+data);
	 * }
	 * public static boolean hasItem(String key){
	 * 		return cache.containsKey(key);
	 * }
	 */
	
	/** @see #getCacheList(String) Uses getCacheList(permType + ";" + type) */
	@SuppressWarnings("unused")
	private static List<TRCacheItem> getCacheList(String permType, String type) {
		return getCacheList(permType + ";" + type);
	}

	/** @return cacheMods.get(type) */
	private static List<TRCacheItem> getCacheList(String type) {
		return cacheMods.get(type.toLowerCase());
	}

	@SuppressWarnings("unused")
	private static List<TRCacheItem> setCacheList(String permType, String type, int id, int data) {
		List<TRCacheItem> l = new LinkedList<TRCacheItem>();
		l.add(cacheItem(permType, type, id, data));
		return addCacheList(permType, type, l);
	}

	/** @see #addCacheList(String, List) Uses addCacheList(permType + ";" + type, list) */
	private static List<TRCacheItem> addCacheList(String permType, String type, List<TRCacheItem> list) {
		String key = permType + ";" + type;
		return addCacheList(key, list);
	}

	private static List<TRCacheItem> addCacheList(String key, List<TRCacheItem> list) {
		key = key.toLowerCase();
		List<TRCacheItem> m = cacheMods.get(key);
		if (m != null) {
			if (m.contains(key)) {
				return m;
			} else {
				m.addAll(list);
				//synchronized (cacheMods) {
					cacheMods.put(key, m);
					return m;
				//}
			}
		} else {
			m = new LinkedList<TRCacheItem>();
			m.addAll(list);
			//synchronized (cacheMods) {
				cacheMods.put(key, m);
				return m;
			//}
		}
	}

	/**
	 * Warning: input permType in LowerCase!
	 * @return Null if:
	 * <ul>
	 * <li>id == 0</li>
	 * <li>Player has bypass permission.</li>
	 * <li></li>
	 *  </ul>
	 */
	public static TRCacheItem getPermCacheItem(Player player, String permType, String perm, int id, int data, boolean doBypassCheck) {
		if (id == 0) return null;
		if (doBypassCheck && player.hasPermission("tekkitrestrict.bypass."+perm)) return null;
		
		Set<String> l = cachePermTypes.get(permType);//n
		if (l == null) l = new HashSet<String>();
		
		Iterator<String> itt = l.iterator();
		while (itt.hasNext()) {
			String type = itt.next();
			
			boolean hasALL = TRPermHandler.hasPermission(player, perm, "*");
			
			if (hasALL || TRPermHandler.hasPermission(player, perm, type) || type.equals("afsd90ujpj")) {
				boolean C = hasCacheItem(permType, type, 99999, 99999);
				if(C || hasALL) return new TRCacheItem(); // has "*"
				
				boolean A = hasCacheItem(permType, type, id, data);
				boolean B = hasCacheItem(permType, "afsd90ujpj", id, data);
				
				/*if(type.equals("afsd90ujpj"))
					tekkitrestrict.log.info("tekkitrestrict."+permType+"."+type+" -> "+id+":"+data);*/
				if (A || B) {
					//if(type.equals("afsd90ujpj"))
						//tekkitrestrict.log.info("===tekkitrestrict."+permType+"."+type+" -> "+id+":"+data);

					if (A) {
						return getCacheItem(permType, type, id, data);
					} else {
						return getCacheItem(permType, "afsd90ujpj", id, data);
					}
				}
			}
		}
		return null;
	}
	
	/**
	 * Warning: input permType in LowerCase!
	 * @return Null if:
	 * <ul>
	 * <li>id = 0</li>
	 * <li>Player has bypass permission.</li>
	 * <li></li>
	 *  </ul>
	 */
	/**/@Deprecated
	public static TRCacheItem getPermCacheItem(Player player, String permType, int id, int data) {
		if (id == 0) return null;
		String perm;
		if (permType.equals("l")){
			if (player.hasPermission("tekkitrestrict.bypass.limiter")) return null;
			perm = "limiter";
		} else if (permType.equals("c")){
			if (player.hasPermission("tekkitrestrict.bypass.creative")) return null;
			perm = "creative";
		} else if (permType.equals("n")){
			if (player.hasPermission("tekkitrestrict.bypass.noitem")) return null;
			perm = "noitem";
		} else {
			perm = permType;
			tekkitrestrict.log.warning("Misssed implementation! " + permType);
		}
		
		Set<String> l = cachePermTypes.get(permType);
		if (l == null) l = new HashSet<String>();
		
		Iterator<String> itt = l.iterator();
		while (itt.hasNext()) {
			String type = itt.next();
			
			boolean hasALL = TRPermHandler.hasPermission(player, perm, "*");
			
			if (hasALL || TRPermHandler.hasPermission(player, perm, type) || type.equals("afsd90ujpj")) {
				boolean C = hasCacheItem(permType, type, 99999, 99999);
				if(C || hasALL) return new TRCacheItem(); // has "*"
				
				boolean A = hasCacheItem(permType, type, id, data);
				boolean B = hasCacheItem(permType, "afsd90ujpj", id, data);
				
				/*if(type.equals("afsd90ujpj"))
					tekkitrestrict.log.info("tekkitrestrict."+permType+"."+type+" -> "+id+":"+data);*/
				if (A || B) {
					//if(type.equals("afsd90ujpj"))
						//tekkitrestrict.log.info("===tekkitrestrict."+permType+"."+type+" -> "+id+":"+data);

					if (A) {
						return getCacheItem(permType, type, id, data);
					} else {
						return getCacheItem(permType, "afsd90ujpj", id, data);
					}
				}
			}
		}
		return null;
	}
	
	/**
	 * Safer than {@link #getPermCacheItem(Player, String, int, int)} because it uses .toLowerCase() on
	 * permType. Only use this if the permType is not coded in lowercase.
	 * @return Null if:
	 * <ul>
	 * <li>id = 0</li>
	 * <li>Player has bypass permission.</li>
	 * <li></li>
	 *  </ul>
	 */
	/**/@Deprecated
	public static TRCacheItem getPermCacheItem_Safe(Player player, String permType, int id, int data) {
		if (id == 0) return null;
		permType = permType.toLowerCase();
		String perm;
		if (permType.equals("l")){
			if (player.hasPermission("tekkitrestrict.bypass.limiter")) return null;
			perm = "limiter";
		} else if (permType.equals("c")){
			if (player.hasPermission("tekkitrestrict.bypass.creative")) return null;
			perm = "creative";
		} else if (permType.equals("n")){
			if (player.hasPermission("tekkitrestrict.bypass.noitem")) return null;
			perm = "noitem";
		} else {
			perm = permType;
			tekkitrestrict.log.warning("Misssed implementation! " + permType);
		}
		
		Set<String> l = cachePermTypes.get(permType);
		if (l == null) l = new HashSet<String>();
		
		Iterator<String> itt = l.iterator();
		while (itt.hasNext()) {
			String type = itt.next();
			
			boolean hasALL = TRPermHandler.hasPermission(player, perm, "*");
			
			if (hasALL || TRPermHandler.hasPermission(player, perm, type) || type.equals("afsd90ujpj")) {
				boolean C = hasCacheItem(permType, type, 99999, 99999);
				if(C || hasALL) return new TRCacheItem(); // has "*"
				
				boolean A = hasCacheItem(permType, type, id, data);
				boolean B = hasCacheItem(permType, "afsd90ujpj", id, data);
				
				/*if(type.equals("afsd90ujpj"))
					tekkitrestrict.log.info("tekkitrestrict."+permType+"."+type+" -> "+id+":"+data);*/
				if (A || B) {
					//if(type.equals("afsd90ujpj"))
						//tekkitrestrict.log.info("===tekkitrestrict."+permType+"."+type+" -> "+id+":"+data);

					if (A) {
						return getCacheItem(permType, type, id, data);
					} else {
						return getCacheItem(permType, "afsd90ujpj", id, data);
					}
				}
			}
		}
		return null;
	}

	/**
	 * Has same effect as using hasCacheItem(permType+";"+type, id, data),<br>
	 * (but using this one is more efficient than doing it without this function).
	 */
	private static boolean hasCacheItem(String permType, String type, int id, int data) {
		//return hasCacheItem(permType+";"+type,id,data);
		String keybase = new StringBuilder(permType.length()+type.length()+8)
						.append(permType.toLowerCase())
						.append(";")
						.append(type.toLowerCase())
						.append("=")
						.append(id)
						.append(":").toString();
		//l;CachePermTypes.get(l)=49:
		//l;afsd90ujpj=49:
		
		//l;afsd90ujpj=49:-10
		if (data == 0 && cache.contains(new StringBuilder(keybase.length()+3).append(keybase).append("-10").toString()))
			return true;
		
		//l;afsd90ujpj=49:0
		if (cache.contains(new StringBuilder(keybase.length()+5).append(keybase).append(data).toString())) 
			return true;
		
		//l;afsd90ujpj=49:0
		if (cache.contains(new StringBuilder(keybase.length()+1).append(keybase).append("0").toString()))
			return true;

		return false;
	}
	
	/*
	private static boolean hasCacheItem(String type, int id, int data) {
		//noitem;type=99999:99999
		String keybase = new StringBuilder(type.length()+7).append(type.toLowerCase()).append("=").append(id).append(":").toString();
		//String keybase = type=id:data
		//noitem=10:
		
		if (data == 0 && cache.contains(new StringBuilder(keybase.length()+3).append(keybase).append("-10").toString())) { //If data = 0 and the cache contains noitem=10:-10
			return true;
		}
		//Data can be max 5 chars long
		if (cache.contains(new StringBuilder(keybase.length()+5).append(keybase).append(data).toString())) { //If the cache contains noitem=10:1
			return true;
		}
		//
		if (cache.contains(new StringBuilder(keybase.length()+1).append(keybase).append("0").toString())) { //If the cache contains noitem=10:0
			return true;
		}

		return false;
	}*/

	private static TRCacheItem getCacheItem(String permType, String type, int id, int data) {
		String keybase = new StringBuilder(permType.length()+type.length()+8)
						.append(permType.toLowerCase())
						.append(";")
						.append(type.toLowerCase())
						.append("=")
						.append(id)
						.append(":").toString();
		//String key = keybase + data;
		//String key0 = keybase + "0";
		//String key10 = keybase + "-10";
		TRCacheItem cii = null;
		if (data == 0 && (cii = cache.get(new StringBuilder(keybase.length()+3).append(keybase).append("-10").toString())) != null) {
			return cii;
		}
		if ((cii = cache.get(new StringBuilder(keybase.length()+5).append(keybase).append(data).toString())) != null) {
			return cii;
		}
		if ((cii = cache.get(new StringBuilder(keybase.length()+1).append(keybase).append("0").toString())) != null) {
			return cii;
		}

		return null;
	}

	/** @param data2 Use -1 to apply to all data values. */
	public static List<TRCacheItem> processItemString(String permType, String type, String item, int data2) {
		String key = permType + ";" + type;			//l;afsd90ujpj		"n;"		"c;afsd90ujpj"
		cpermtype(permType, type);					//l, afsd90ujpj		"n",""		"c","afsd90ujpj"
		return processItemString(key, item, data2);
	}

	// used for reloads...
	public static List<TRCacheItem> processItemString(String type, String item, int data2) {
		//type = noitem;key
		//item = value
		//data2 = -1 || c[1]
		String itemx = item.replace(":-", ":=");
		// converts a variable string into a list of data.
		LinkedList<TRCacheItem> tci = new LinkedList<TRCacheItem>();

		//############################## RANGE OF ITEMS ###########################
		if (itemx.contains("-")) {
			// loop through this range and add each to the return stack.
			int data1 = -1;
			if (itemx.contains(":")) {
				String dataString = itemx.split(":")[1];
				if (dataString.equals("*")) data1 = -1;
				else {
					try {
						data1 = Integer.parseInt(dataString);
						if (data1 == 0){
							data1 = -10;
						}
					} catch (NumberFormatException ex){
						Warning.config("Invalid data value: \"" + dataString + "\" in \"" + itemx + "\"!");
					}
				}
				
				itemx = itemx.split(":")[0];
			}
			String[] t = itemx.split("-");
			int fromId = 0, toId = 0;
			try {
				fromId = Integer.parseInt(t[0]);
				toId = Integer.parseInt(t[1]);
			} catch (NumberFormatException ex){
				Warning.config("Invalid range: \"" + t[0]+"-"+t[1] + "\" of " +type);
				return tci;
			}

			for (int i = fromId; i <= toId; i++) {
				try {
					tci.add(cacheItem(type, i, data1, data2));
				} catch (Exception ex) {
					Log.Exception(ex, true);
					return tci;
				}
			}
			
			addCacheList(type, tci);
			return tci;
		}
		
		//############################## SINGLE ID WITH DATA ###########################
		else if (itemx.contains(":")) {
			String[] t = itemx.split(":");
			int id = 0, data = 0;
			try {
				id = Integer.parseInt(t[0]);
				data = Integer.parseInt(t[1].replace('=', '-'));
			} catch (NumberFormatException | ArrayIndexOutOfBoundsException ex){
				Warning.config("Invalid entry: \"" + itemx + "\"!");
				return tci;
			}
			
			if (data == 0) { //If :0, then :-10
				data = -10;
			}
			
			try {
				tci.add(cacheItem(type, id, data, data2));
				addCacheList(type, tci);
				return tci;
			} catch (Exception ex) {
				Log.Exception(ex, true);
				return tci;
			}
		}
		
		//############################## ALL ITEMS ###########################
		else if(itemx.equals("*")) {
			tci.add(cacheItem(type, 99999, 99999, data2)); //Every single freaking item.
			addCacheList(type, tci);
			return tci;
		}
		
		//############################## SINGLE ID ###########################
		else {
			int id = 0;
			try {
				id = Integer.parseInt(itemx);
			} catch (NumberFormatException ex){
				if (cacheMods.containsKey(item.toLowerCase())) {
					// modtypes
					List<TRCacheItem> cc = cacheMods.get(item.toLowerCase());
					for (TRCacheItem ci : cc) {
						//tekkitrestrict.log.info(type+" <- "+ci.toString());
						try{
							TRCacheItem ci2 = (TRCacheItem) ci.clone();
							ci2.setIntData(data2);
							ci2.cacher = type+"="+ci2.id+":"+ci2.data;
							cache.put(type+"="+ci2.id+":"+ci2.data, ci2);
						}
						catch(Exception ex2){}
					}
					//tekkitrestrict.log.info("place mod - "+item+" size: "+cc.size());
					if(type.contains(";")){
						String[] uu = type.split(";");
						if (uu.length == 2) cpermtype(uu[0], uu[1]);//Crashes here FIXME
					}
					addCacheList(type, cc);
					return cc;
				}
				addCacheList(type, tci);
				return tci;
			}

			try {
				tci.add(cacheItem(type, id, -1, data2));
				addCacheList(type, tci);
				return tci;
			} catch (Exception ex) {
				Warning.config("Invalid entry: \"" + itemx + "\"!");
				return tci;
			}
		}
	}

	/** To be used exclusively for types that require a permission type. */
	private static TRCacheItem cacheItem(String permType, String type, int id, int data) {
		cpermtype(permType, type);
		return cacheItem(permType + ";" + type, id, data);
	}

	// 
	/**
	 * Warning: do not use for permission types. use the permType version instead.
	 */
	private static TRCacheItem cacheItem(String type, int id, int data) {
		String key = type.toLowerCase() + "=" + id + ":" + data;
		TRCacheItem m = cache.get(key);
		if (m == null) {
			TRCacheItem c = new TRCacheItem();
			c.id = id;
			c.data = data;
			c.cacher = key;
			cache.put(key, m = c);
		}
		return m;
	}

	/**
	 * Sdd an item to the cache with cacher: "type=id:data"
	 */
	private static TRCacheItem cacheItem(String type, int id, int data, int numdata) {
		String key = type.toLowerCase() + "=" + id + ":" + data;//n=12:0
		TRCacheItem m = cache.get(key);
		if (m == null) {
			TRCacheItem c = new TRCacheItem();
			c.id = id;
			c.data = data;
			c.cacher = key;
			c.setIntData(numdata);

			cache.put(key, m = c);
		}
		return m;
	}

	public static TRCacheItem cacheItem(String type, int id, int data, Object objdata) {
		String key = type.toLowerCase() + "=" + id + ":" + data;
		TRCacheItem m = cache.get(key);
		if (m == null) {
			TRCacheItem c = new TRCacheItem();
			c.id = id;
			c.data = data;
			c.cacher = key;
			c.Data2 = objdata;

			cache.put(key, m = c);
		}
		return m;
	}

	// 
	/**
	 * Used for any item call that is not directly using a type or permission type
	 * @see #cacheItem(String, int, int) cacheItem("", id, data)
	 */
	public static TRCacheItem cacheItem(int id, int data) {
		return cacheItem("", id, data);
	}

	/**
	 * Adds the given type to the Set in cachePermTypes.get(ptype)<br>
	 * It creates one if it doesn't exist yet.
	 * 
	 * 
	 */
	//"c","afsd90ujpj"
	private static void cpermtype(String ptype, String type) {
		String lptype = ptype.toLowerCase();
		Set<String> l = cachePermTypes.get(lptype);
		if (l == null) l = new HashSet<String>();
		l.add(type.toLowerCase());

		synchronized (cachePermTypes) {
			cachePermTypes.put(lptype, l);
		}
	}

	private static List<TRCacheItem> processModString(String permType, String type, String ins) {
		//"", "ee", "27520-27599;126-130"
		cpermtype(permType, type);
		return processMultiString(permType + ";" + type, ins, -1);
	}

	private static List<TRCacheItem> processMultiString(String permType, String type, String ins, int data2) {
		cpermtype(permType, type);
		return processMultiString(permType + ";" + type, ins, data2);
	}

	private static List<TRCacheItem> processMultiString(String type, String ins, int data2) {
		if (ins.contains(";")) {
			String[] rs = ins.split(";");
			List<TRCacheItem> l = new LinkedList<TRCacheItem>();
			for (String re : rs) {
				l.addAll(processItemString(type, re, data2));
			}
			return l;
		} else if (ins.length() > 0) {
			return processItemString(type, ins, data2);
		}
		return new LinkedList<TRCacheItem>();
	}

	/** Clears the cache so new perms can be added. */
	private static void clearCache() {
		synchronized (cache) {
			for (String s : cache.keySet()){
				Log.Debug("cache.get("+s+"): "+cache.get(s).toString());
			}
			cache.clear();
		}
		synchronized (cacheMods) {
			for (String s : cacheMods.keySet()){
				List<TRCacheItem> cis = cacheMods.get(s);
				for (TRCacheItem ci : cis){
					Log.Debug("cacheMods.get("+s+"): " + ci.toString());
				}
			}
			cacheMods.clear();
		}
		synchronized (cachePermTypes) {
			for (String s : cachePermTypes.keySet()){
				Set<String> strs = cachePermTypes.get(s);
				for (String str : strs){
					Log.Debug("cachePermTypes.get("+s+"): " + str);
				}
			}
			cachePermTypes.clear();
		}
	}

}
