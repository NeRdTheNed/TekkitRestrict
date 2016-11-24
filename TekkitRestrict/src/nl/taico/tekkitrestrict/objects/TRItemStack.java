package nl.taico.tekkitrestrict.objects;

import ic2.common.ItemArmorJetpack;
import ic2.common.ItemArmorJetpackElectric;

import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;

import net.minecraft.server.Item;
import net.minecraft.server.NBTTagCompound;

public class TRItemStack{
	private ItemStack bukkitStack;
	/**
	 * An itemStack of id:0 with an amount of 1.
	 */
	public TRItemStack(final int id) {
		this(id, 1, (short) 0, null);
	}

	/**
	 * An ItemStack of id:0 with an amount of 1.
	 */
	public TRItemStack(final int id, final int amount) {
		this(id, amount, (short) 0, null);
	}
	
	/**
	 * An ItemStack of id:damage with an amount of amount.
	 */
	public TRItemStack(final int id, final int amount, final short damage) {
		this(id, amount, damage, null);
	}
	
	public TRItemStack(final int type, final int amount, final short damage, final Byte data) {
		bukkitStack = new ItemStack(type, amount, damage, data);
	}

	public TRItemStack(final ItemStack stack) {
		this.bukkitStack = stack;
	}
	
	public int getId(){
		return bukkitStack.getTypeId();
	}
	public void setId(final int id){
		bukkitStack.setTypeId(id);
	}
	
	public byte getByteData(){
		return bukkitStack.getData().getData();
	}
	public void setByteData(final byte data){
		bukkitStack.getData().setData(data);
	}
	
	public short getShortData(){
		return bukkitStack.getDurability();
	}
	public void setShortData(final short data){
		bukkitStack.setDurability(data);
	}
	
	public int getAmount(){
		return bukkitStack.getAmount();
	}
	public void setAmount(final int amount){
		bukkitStack.setAmount(amount);
	}
	
	public ItemStack getBukkitStack(){
		return bukkitStack;
	}
	
	public net.minecraft.server.ItemStack getMCStack(){
		return ((CraftItemStack) bukkitStack).getHandle();
	}
	
	public CraftItemStack getCraftStack(){
		return (CraftItemStack) bukkitStack;
	}
	
	public NBTTagCompound getTag(){
		return getMCStack().tag;
	}
	
	public Item getMCItem(){
		return getMCStack().getItem();
	}

	public static int getId(final ItemStack itemStack){
		return itemStack.getTypeId();
	}
	public static void setId(final ItemStack itemStack, final int id){
		itemStack.setTypeId(id);
	}
	
	public static byte getByteData(final ItemStack itemStack){
		return itemStack.getData().getData();
	}
	public static void setByteData(final ItemStack itemStack, final byte data){
		itemStack.getData().setData(data);
	}
	
	public static short getShortData(final ItemStack itemStack){
		return itemStack.getDurability();
	}
	public static void setShortData(final ItemStack itemStack, final short data){
		itemStack.setDurability(data);
	}
	
	public static int getAmount(final ItemStack itemStack){
		return itemStack.getAmount();
	}
	public static void setAmount(final ItemStack itemStack, final int amount){
		itemStack.setAmount(amount);
	}
	
	public static net.minecraft.server.ItemStack getMCStack(final ItemStack itemStack){
		return ((CraftItemStack) itemStack).getHandle();
	}
	public static CraftItemStack getCraftStack(final ItemStack itemStack){
		return (CraftItemStack) itemStack;
	}
	public static Item getMCItem(final ItemStack itemStack){
		return ((CraftItemStack) itemStack).getHandle().getItem();
	}
	
	public static NBTTagCompound getTag(final ItemStack itemStack){
		return ((CraftItemStack) itemStack).getHandle().getTag();
	}
	public static NBTTagCompound getTagOrCreate(final ItemStack itemStack){
		NBTTagCompound tag = ((CraftItemStack) itemStack).getHandle().getTag();
		if (tag == null){
			tag = new NBTTagCompound();
			((CraftItemStack) itemStack).getHandle().setTag(tag);
		}
		return tag;
	}
	public static NBTTagCompound getTagOrCreate(final net.minecraft.server.ItemStack mcItemStack){
		NBTTagCompound tag = mcItemStack.getTag();
		if (tag == null){
			tag = new NBTTagCompound();
			mcItemStack.setTag(tag);
		}
		return tag;
	}
	public static void setTag(final ItemStack itemStack, final NBTTagCompound tag){
		((CraftItemStack) itemStack).getHandle().setTag(tag);
	}

	public static int getJetpackCharge(ItemStack itemstack){
		final net.minecraft.server.ItemStack mcis = getMCStack(itemstack);
		final Item item = mcis.getItem();
		if (item instanceof ItemArmorJetpack) return ((ItemArmorJetpack) item).getCharge(mcis);
		else if (item instanceof ItemArmorJetpackElectric) return ((ItemArmorJetpackElectric) item).getCharge(mcis);
		return 0;
	}
}
