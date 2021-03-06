package com.darkona.adventurebackpack.client.audio;

import com.darkona.adventurebackpack.inventory.InventorySteamJetpack;
import com.darkona.adventurebackpack.item.ItemSteamJetpack;
import com.darkona.adventurebackpack.reference.ModInfo;
import com.darkona.adventurebackpack.util.Wearing;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

/**
 * Created on 16/01/2015
 *
 * @author Darkona
 */
public class BoilingBoilerSound extends MovingSound
{

    public EntityPlayer thePlayer;
    protected boolean repeat = true;
    protected int repeatDelay = 0;

    protected float pitch;

    public BoilingBoilerSound(EntityPlayer player)
    {
        super(new ResourceLocation(ModInfo.MOD_ID, "s_boiling"));
        volume = 0.1f;
        pitch = 0.4F;
        thePlayer = player;
    }

    public EntityPlayer getThePlayer()
    {
        return thePlayer;
    }

    public void setThePlayer(EntityPlayer player){
        thePlayer = player;
    }

     public void setDonePlaying()
    {
        repeat = false;
        donePlaying = true;
        repeatDelay = 0;
    }

    @Override
    public boolean isDonePlaying()
    {
        return this.donePlaying;
    }

    @Override
    public void update()
    {
        ItemStack jetpack = Wearing.getWearingSteam(thePlayer);
        if (thePlayer == null || thePlayer.worldObj == null || jetpack == null || !(jetpack.getItem() instanceof ItemSteamJetpack))
        {
            setDonePlaying();
        }

        InventorySteamJetpack inv = new InventorySteamJetpack(jetpack);
        if(inv.isBoiling())
        {
            xPosF = (float)thePlayer.posX;
            yPosF = (float)thePlayer.posY;
            zPosF = (float)thePlayer.posZ;
        }else
        {
            setDonePlaying();
        }
    }

    @Override
    public boolean canRepeat()
    {
        return this.repeat;
    }

    @Override
    public float getVolume()
    {
        return this.volume;
    }

    @Override
    public float getPitch()
    {
        return this.pitch;
    }

    @Override
    public int getRepeatDelay(){ return this.repeatDelay; }

    @Override
    public AttenuationType getAttenuationType()
    {
        return AttenuationType.LINEAR;
    }

}
