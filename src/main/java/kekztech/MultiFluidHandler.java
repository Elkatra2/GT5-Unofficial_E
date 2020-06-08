package kekztech;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.fluids.FluidStack;

public class MultiFluidHandler {
	
	public static final int MAX_DISTINCT_FLUIDS = 25;
	
	private final List<FluidStack> fluids = new ArrayList<>(MAX_DISTINCT_FLUIDS);
	private int capacityPerFluid;
	
	private boolean locked = true;
	private boolean doVoidExcess = false;
	private byte fluidSelector = -1;
	
	public MultiFluidHandler() {
		
	}
	
	public MultiFluidHandler(int capacityPerFluid) {
		this.capacityPerFluid = capacityPerFluid;
	}
	
	public MultiFluidHandler(int capacityPerFluid, List<FluidStack> fluids) {
		this.capacityPerFluid = capacityPerFluid;
		this.fluids.addAll(fluids);
	}
	
	/**
	 * Lock internal tanks in case T.F.F.T is not running.
	 * 
	 * @param state
	 * 				Lock state.
	 */
	public void setLock(boolean state) {
		locked = state;
	}

	public void setDoVoidExcess(boolean doVoidExcess) { this.doVoidExcess = doVoidExcess; }

	/**
	 * Used to tell the MFH if a fluid is selected by
	 * an Integrated Circuit in the controller.
	 * 
	 * @param fluidSelector
	 * 				Selected fluid or -1 if no fluid is selected
	 */
	public void setFluidSelector(byte fluidSelector) {
		this.fluidSelector = fluidSelector;
	}
	
	/**
	 * 
	 * @return
	 * 				Selected fluid or -1 if no fluid is selected
	 */
	public byte getSelectedFluid() {
		return fluidSelector;
	}
	
	public boolean contains(FluidStack fluid) {
		return !locked && fluids.contains(fluid);
	}
	
	public int getCapacity() {
		return capacityPerFluid;
	}

	// TODO return deep copy instead
	public List<FluidStack> getFluids(){
		return (!locked) ? fluids : new ArrayList<>();
	}
	
	public FluidStack getFluid(int slot) {
		return (!locked && fluids.size() > 0 && slot >= 0 && slot < MAX_DISTINCT_FLUIDS) 
				? fluids.get(slot).copy() : null;
	}
	
	public NBTTagCompound saveNBTData(NBTTagCompound nbt) {
		nbt = (nbt == null) ? new NBTTagCompound() : nbt;
		
		nbt.setInteger("capacityPerFluid", getCapacity());
		int c = 0;
		for(FluidStack f : fluids) {
			nbt.setTag("" + c, f.writeToNBT(new NBTTagCompound()));
			c++;
		}
		return nbt;
	}
	
	public void loadNBTData(NBTTagCompound nbt) {
		nbt = (nbt == null) ? new NBTTagCompound() : nbt;
		
		capacityPerFluid = nbt.getInteger("capacityPerFluid");
		
		fluids.clear();
		final NBTTagCompound fluidsTag = (NBTTagCompound) nbt.getTag("fluids");
		for(int i = 0; i < MultiFluidHandler.MAX_DISTINCT_FLUIDS; i++) {
			final NBTTagCompound fnbt = (NBTTagCompound) fluidsTag.getTag("" + i);
			if(fnbt == null) {
				break;
			}
			fluids.add(FluidStack.loadFluidStackFromNBT(fnbt));
		}
	}
	
	public ArrayList<String> getInfoData() {
		final ArrayList<String> lines = new ArrayList<>(fluids.size());
		lines.add(EnumChatFormatting.YELLOW + "Stored Fluids:" + EnumChatFormatting.RESET);
		for(int i = 0; i < fluids.size(); i++) {
			lines.add(i + " - " + fluids.get(i).getLocalizedName() + ": " 
					+ fluids.get(i).amount + "L (" 
					+ (Math.round(100.0f * fluids.get(i).amount / getCapacity())) + "%)");
		}
		
		return lines;
	}
	
	/**
	 * Fill fluid into a tank.
	 * 
	 * @param push
	 * 				Fluid type and quantity to be inserted.
	 * @param doPush
	 * 				If false, fill will only be simulated.
	 * @return Amount of fluid that was (or would have been, if simulated) filled.
	 */
	public int pushFluid(FluidStack push, boolean doPush) {
		if(locked) {
			return 0;
		}
		if(fluids.size() == MAX_DISTINCT_FLUIDS && !contains(push)) {
			// Already contains 25 fluids and this isn't one of them
			return 0;
		} else if (fluids.size() < MAX_DISTINCT_FLUIDS && !contains(push)) {
			// Add new fluid
			final int fit = Math.min(getCapacity(), push.amount);
			if(doPush) {
				fluids.add(new FluidStack(push.getFluid(), fit));	
			}
			// If doVoidExcess, pretend all of it fit
			return doVoidExcess ? push.amount : fit;
		} else {
			// Add to existing fluid
			final FluidStack existing = fluids.get(fluids.indexOf(push));
			final int fit = Math.min(getCapacity() - existing.amount, push.amount);
			if(doPush) {
				existing.amount += fit;
			}
			// If doVoidExcess, pretend all of it fit
			return doVoidExcess ? push.amount : fit;
		}
	}
	
	/**
	 * Fill fluid into the specified tank.
	 * 
	 * @param push
	 * 				Fluid type and quantity to be inserted.
	 * @param slot
	 * 				Tank the fluid should go into.
	 * @param doPush
	 * 				If false, fill will only be simulated.
	 * @return Amount of fluid that was (or would have been, if simulated) filled.
	 */
	public int pushFluid(FluidStack push, int slot, boolean doPush) {
		if(locked) {
			return 0;
		}
		if(slot < 0 || slot >= MAX_DISTINCT_FLUIDS) {
			// Invalid slot
			return 0;
		}
		if((fluids.get(slot) != null) && !fluids.get(slot).equals(push)) {
			// Selected slot is taken by a non-matching fluid
			return 0;
		} else {
			// Add to existing fluid
			final FluidStack existing = fluids.get(slot);
			final int fit = Math.min(getCapacity() - existing.amount, push.amount);
			if(doPush) {
				existing.amount += fit;
			}
			// If doVoidExcess, pretend all of it fit
			return doVoidExcess ? push.amount : fit;
		}
	}
	
	/**
	 * Drains fluid out of the internal tanks.
	 *  
	 * @param pull
	 * 				Fluid type and quantity to be pulled.
	 * @param doPull
	 * 				If false, drain will only be simulated.
	 * @return Amount of fluid that was (or would have been, if simulated) pulled.
	 */
	public int pullFluid(FluidStack pull, boolean doPull) {
		if (locked || !contains(pull)) {
			return 0;
		} else {
			final FluidStack src = fluids.get(fluids.indexOf(pull));
			final int rec = Math.min(pull.amount, src.amount);
			if (doPull) {
				src.amount -= rec;
			}
			if (src.amount == 0) {
				fluids.remove(src);
			}
			return rec;
		}
	}
	
	/**
	 * Drains fluid out of the specified internal tank.
	 *  
	 * @param pull
	 * 				Fluid type and quantity to be pulled.
	 * @param slot
	 * 				Tank fluid should be drained from.
	 * @param doPull
	 * 				If false, drain will only be simulated.
	 * @return Amount of fluid that was (or would have been, if simulated) pulled.
	 */
	public int pullFluid(FluidStack pull, int slot, boolean doPull) {
		if(locked) {
			return 0;
		}
		if(slot < 0 || slot >= MAX_DISTINCT_FLUIDS) {
			return 0;
		}
		if(!fluids.get(slot).equals(pull)) {
			return 0;
		} else {
			final FluidStack src = fluids.get(slot);
			final int rec = Math.min(pull.amount, src.amount);
			if(doPull) {
				src.amount -= rec;
			}
			if(src.amount == 0) {
				fluids.remove(src);
			}
			return rec;
		}
	}
	
	/**
	 * Test whether the given fluid type and quantity can be inserted into the internal tanks.
	 * @param push
	 * 				Fluid type and quantity to be tested
	 * @return True if there is sufficient space
	 */
	public boolean couldPush(FluidStack push) {
		if(locked) {
			return false;
		}
		if(fluids.size() == MAX_DISTINCT_FLUIDS && !contains(push)) {
			return false;
		} else if (fluids.size() < MAX_DISTINCT_FLUIDS && !contains(push)) {
			return Math.min(getCapacity(), push.amount) > 0;
		} else {
			final int remcap = getCapacity() - fluids.get(fluids.indexOf(push)).amount;
			return doVoidExcess ? true : (Math.min(remcap, push.amount) > 0);
		}
	}
}
