package com.github.dreadslicer.tekkitrestrict.eepatch;

import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;

import com.github.dreadslicer.tekkitrestrict.tekkitrestrict;
import com.github.dreadslicer.tekkitrestrict.eepatch.ringlisteners.EEArcaneRingListener;
import com.github.dreadslicer.tekkitrestrict.eepatch.ringlisteners.EEBHBListener;
import com.github.dreadslicer.tekkitrestrict.eepatch.ringlisteners.EEHarvestRingListener;
import com.github.dreadslicer.tekkitrestrict.eepatch.ringlisteners.EEIgnitionRingListener;
import com.github.dreadslicer.tekkitrestrict.eepatch.ringlisteners.EESWRGListener;
import com.github.dreadslicer.tekkitrestrict.eepatch.ringlisteners.EEVoidRingListener;
import com.github.dreadslicer.tekkitrestrict.eepatch.ringlisteners.EEZeroRingListener;

public class EEAssigner {
	public static void assign(){
		PluginManager PM = Bukkit.getPluginManager();
		tekkitrestrict tr = tekkitrestrict.getInstance();
		if (!EEPSettings.arcanering.isEmpty())
			PM.registerEvents(new EEArcaneRingListener(), tr);
		
		if (!EEPSettings.blackholeband.isEmpty())
			PM.registerEvents(new EEBHBListener(), tr);
		
		if (!EEPSettings.harvestring.isEmpty())
			PM.registerEvents(new EEHarvestRingListener(), tr);
		
		if (!EEPSettings.firering.isEmpty())
			PM.registerEvents(new EEIgnitionRingListener(), tr);
		
		if (!EEPSettings.flyring.isEmpty())
			PM.registerEvents(new EESWRGListener(), tr);
		
		if (!EEPSettings.voidring.isEmpty())
			PM.registerEvents(new EEVoidRingListener(), tr);
		
		if (!EEPSettings.zeroring.isEmpty())
			PM.registerEvents(new EEZeroRingListener(), tr);
		
		
		if (!tekkitrestrict.config.getBoolean("AllowRMFurnaceOreDuplication", true))
			PM.registerEvents(new EEDuplicateListener(), tr);
		
		if (!EEPSettings.MaxCharge.isEmpty())
			PM.registerEvents(new EEChargeListener(), tr);
	}
}