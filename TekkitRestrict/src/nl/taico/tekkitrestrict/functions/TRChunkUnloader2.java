package nl.taico.tekkitrestrict.functions;

import java.util.Iterator;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.event.world.ChunkUnloadEvent;

import net.minecraft.server.Chunk;
import net.minecraft.server.ChunkProviderServer;
import net.minecraft.server.EntityHuman;
import net.minecraft.server.WorldServer;

import nl.taico.tekkitrestrict.tekkitrestrict;
import nl.taico.tekkitrestrict.TRConfigCache.ChunkUnloader;
import nl.taico.tekkitrestrict.objects.TRChunkIndex;
import nl.taico.tekkitrestrict.objects.TREnums.ChunkUnloadMethod;

public class TRChunkUnloader2 {
	private final World world;
	private ChunkUnloadMethod method;
	private TRChunkIndex index;
	public TRChunkUnloader2(World world){
		this.world = world;
		this.method = ChunkUnloadMethod.UnloadLowWhenForced;
	}
	
	public ChunkUnloadMethod getMethod() {
		return method;
	}
	
	public void setMethod(ChunkUnloadMethod method) {
		this.method = method;
	}
	
	/**
	 * WARNING: This method should be called synchronized
	 * @param chunk
	 * @param force
	 * @return
	 */
	public boolean unloadChunk(Chunk chunk, boolean force){
		WorldServer world = (WorldServer) chunk.world;
		ChunkProviderServer cps = world.chunkProviderServer;
		if (world.savingDisabled) return false;
	    
		ChunkUnloadEvent event = new ChunkUnloadEvent(chunk.bukkitChunk);
		Bukkit.getPluginManager().callEvent(event);
		if (!event.isCancelled() || force) {
			chunk.removeEntities();
			cps.saveChunk(chunk);
			cps.saveChunkNOP(chunk);
			cps.chunks.remove(chunk.x, chunk.z);
			cps.chunkList.remove(chunk);
		}
		return true;
	}
	
	public void unload(){
		if (index == null) index = new TRChunkIndex(world);
		index.index();//TODO check last index time
		final List<Chunk> toUnload;
		//if (method.isForced()){
			switch (method.nr){
				case 1:
					toUnload = index.getNormalChunks();
					final Iterator<Chunk> it = toUnload.iterator();
					while (it.hasNext()){
						final Chunk chunk = it.next();
						if (isChunkInUse(chunk.x, chunk.z, ChunkUnloader.maxRadii*2)) it.remove();
					}
					break;
				case 2:
					toUnload = index.getNormalChunks();
					final Iterator<Chunk> it2 = toUnload.iterator();
					while (it2.hasNext()) {
						final Chunk chunk = it2.next();
						if (isChunkInUse(chunk.x, chunk.z, ChunkUnloader.maxRadii)) it2.remove();
					}
					break;
				case 3:
					toUnload = index.getAllChunks();
					final Iterator<Chunk> it3 = toUnload.iterator();
					while (it3.hasNext()){
						final Chunk chunk = it3.next();
						if (isChunkInUse(chunk.x, chunk.z, ChunkUnloader.maxRadii)) it3.remove();
					}
					break;
				case 4:
					toUnload = index.getAllChunks();
					break;
				default: return;
			}
		//} else {
			
		//}
		Bukkit.getScheduler().scheduleSyncDelayedTask(tekkitrestrict.getInstance(), new Runnable(){
			public void run(){
				int i = 0;
				boolean forced = method.isForced();
				for (Chunk chunk : toUnload){
					if (unloadChunk(chunk, forced)) i++;
				}
				tekkitrestrict.log.fine("Unloaded " + i + " chunks");
			}
		});
		
		
		if (!method.isExtreme()){
			
		} else {
			
		}
		
	}
	
	/** @return If there are currently players near that chunk. */
	private boolean isChunkInUse(final int x, final int z, final int dist) {
		final List<EntityHuman> k = ((CraftWorld) world).getHandle().players;
		for (EntityHuman h : k){
			if (Math.abs(h.x - (x << 4)) <= dist && Math.abs(h.z - (z << 4)) <= dist) {
				return true;
			}
		}
		
		return false;
	}
	
}
