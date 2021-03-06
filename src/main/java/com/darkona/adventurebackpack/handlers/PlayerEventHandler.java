package com.darkona.adventurebackpack.handlers;

import com.darkona.adventurebackpack.AdventureBackpack;
import com.darkona.adventurebackpack.common.ServerActions;
import com.darkona.adventurebackpack.config.ConfigHandler;
import com.darkona.adventurebackpack.entity.EntityFriendlySpider;
import com.darkona.adventurebackpack.entity.ai.EntityAIHorseFollowOwner;
import com.darkona.adventurebackpack.init.ModBlocks;
import com.darkona.adventurebackpack.init.ModItems;
import com.darkona.adventurebackpack.item.IBackWearableItem;
import com.darkona.adventurebackpack.playerProperties.BackpackProperty;
import com.darkona.adventurebackpack.proxy.ServerProxy;
import com.darkona.adventurebackpack.reference.BackpackNames;
import com.darkona.adventurebackpack.util.LogHelper;
import com.darkona.adventurebackpack.util.Wearing;
import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.monster.EntitySpider;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemNameTag;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChunkCoordinates;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.player.EntityInteractEvent;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;

/**
 * Created on 11/10/2014
 * Handle ALL the events!
 *
 * @author Darkona
 * @see com.darkona.adventurebackpack.client.ClientActions
 */
public class PlayerEventHandler
{

    private static int tickCounter = 0;
    @SubscribeEvent
    public void registerBackpackProperty(EntityEvent.EntityConstructing event)
    {
        if (event.entity instanceof EntityPlayer && BackpackProperty.get((EntityPlayer) event.entity) == null)
        {
            BackpackProperty.register((EntityPlayer) event.entity);
        }
    }

    @SubscribeEvent
    public void joinPlayer(EntityJoinWorldEvent event)
    {
        if (event.entity instanceof EntityPlayer)
        {
            AdventureBackpack.proxy.joinPlayer((EntityPlayer) event.entity);
        }
    }


    @SubscribeEvent
    public void playerLogsIn(PlayerEvent.PlayerLoggedInEvent event)
    {
        if (event.player instanceof EntityPlayerMP)
        {
            BackpackProperty.sync(event.player);
        }
    }

    @SubscribeEvent
    public void playerTravelsAcrossDimensions(PlayerEvent.PlayerChangedDimensionEvent event)
    {
        if (event.player instanceof EntityPlayerMP)
        {
            BackpackProperty.sync(event.player);
        }
    }

    /**
     * Used for the Piston Boots to give them their amazing powers.
     *
     * @param event
     */
    @SubscribeEvent
    public void onPlayerJump(LivingEvent.LivingJumpEvent event)
    {
        if (event.entity != null &&
                event.entityLiving instanceof EntityPlayer)
        {
            EntityPlayer player = (EntityPlayer) event.entity;


            if (Wearing.isWearingBoots(player) && player.onGround)
            {
                ServerActions.pistonBootsJump(player);
            }
        }
    }

    /**
     * Used by the Piston boots to lessen the fall damage. It's hacky, but I don't care.
     *
     * @param event
     */
    @SubscribeEvent
    public void onFall(LivingFallEvent event)
    {
        if (event.entity != null)
        {
            if (event.entityLiving instanceof EntityCreature && ConfigHandler.FIX_LEAD)
            {
                EntityCreature creature = (EntityCreature) event.entityLiving;
                if (creature.getLeashed() && creature.getLeashedToEntity() != null && creature.getLeashedToEntity() instanceof EntityPlayer)
                {
                    EntityPlayer player = (EntityPlayer) creature.getLeashedToEntity();
                    if (creature.motionY > -2.0f && player.motionY > -2.0f)
                    {
                        event.setCanceled(true);
                    }
                }
            }

            if (event.entityLiving instanceof EntityFriendlySpider)
            {
                if (((EntityFriendlySpider) event.entityLiving).riddenByEntity != null
                        && ((EntityFriendlySpider) event.entityLiving).riddenByEntity instanceof EntityPlayer
                        && event.distance < 5)
                {
                    event.setCanceled(true);
                }
            }

            if (event.entityLiving instanceof EntityPlayer &&
                    Wearing.isWearingBoots(((EntityPlayer) event.entityLiving)) &&
                    event.distance < 8)
            {
                event.setCanceled(true);
            }
        }


    }


    /**
     * @param event
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void playerDies(LivingDeathEvent event)
    {
        if (event.entity instanceof EntityPlayer)
        {
            EntityPlayer player = (EntityPlayer) event.entity;


            if (!player.worldObj.isRemote)
            {
                LogHelper.info("Player died");
                BackpackProperty props = BackpackProperty.get(player);
                if (props.hasWearable())
                {
                    ItemStack wearable = props.getWearable();
                    LogHelper.info("Executing death protocol.");
                    ((IBackWearableItem) wearable.getItem()).onPlayerDeath(player.worldObj, player, wearable);
                }
                if (props.isForceCampFire())
                {
                    ChunkCoordinates lastCampFire = BackpackProperty.get(player).getCampFire();
                    if (lastCampFire != null)
                    {
                        player.setSpawnChunk(lastCampFire, false, player.dimension);
                    }
                    //Set the forced spawn coordinates on the campfire. False, because the player must respawn at spawn point if there's no campfire.
                }
                ServerProxy.storePlayerProps(player.getUniqueID(), props.getData());
            }
        }

        event.setResult(Event.Result.ALLOW);
    }

    @SubscribeEvent
    public void playerCraftsBackpack(PlayerEvent.ItemCraftedEvent event)
    {
        if (event.crafting.getItem() == ModItems.adventureBackpack)
        {
            LogHelper.info("Player crafted a backpack, and that backpack's appearance is: " + event.crafting.getTagCompound().getString("colorName"));
            if (BackpackNames.getBackpackColorName(event.crafting).equals("Dragon"))
            {
                event.player.dropPlayerItemWithRandomChoice(new ItemStack(Blocks.dragon_egg, 1), false);
                event.player.playSound("mob.enderdragon.growl", 1.0f, 5.0f);
            }
        }
    }

    @SubscribeEvent
    public void interactWithCreatures(EntityInteractEvent event)
    {
        EntityPlayer player = event.entityPlayer;
        if (!event.entityPlayer.worldObj.isRemote)
        {
            if (event.target instanceof EntitySpider)
            {
                if (Wearing.isWearingTheRightBackpack(player, "Spider"))
                {
                    EntityFriendlySpider pet = new EntityFriendlySpider(event.target.worldObj);
                    pet.setLocationAndAngles(event.target.posX, event.target.posY, event.target.posZ, event.target.rotationYaw, event.target.rotationPitch);
                    event.target.setDead();
                    event.entityPlayer.worldObj.spawnEntityInWorld(pet);
                    event.entityPlayer.mountEntity(pet);
                }
            }
            if (event.target instanceof EntityHorse)
            {
                ItemStack stack = player.getCurrentEquippedItem();
                EntityHorse horse = (EntityHorse) event.target;
                if (stack != null && stack.getItem() != null && stack.getItem() instanceof ItemNameTag && stack.hasDisplayName())
                {
                    if (horse.getCustomNameTag() == null || horse.getCustomNameTag().equals("") && horse.isTame())
                    {
                        horse.setTamedBy(player);
                        horse.tasks.addTask(4, new EntityAIHorseFollowOwner(horse, 1.5d, 2.0f, 20.0f));

                        if (horse.getAttributeMap().getAttributeInstance(SharedMonsterAttributes.followRange) != null)
                        {
                            horse.getAttributeMap().getAttributeInstance(SharedMonsterAttributes.followRange).setBaseValue(100.0D);
                            LogHelper.info("the horse follow range is now: " + horse.getEntityAttribute(SharedMonsterAttributes.followRange).getBaseValue());
                        }
                    }
                }
            }
        }
        event.setResult(Event.Result.ALLOW);
    }

    @SubscribeEvent
    public void playerWokeUp(PlayerWakeUpEvent event)
    {
        if (event.entity.worldObj.isRemote) return;
        ChunkCoordinates bedLocation = event.entityPlayer.getBedLocation(event.entityPlayer.dimension);
        if (bedLocation != null && event.entityPlayer.worldObj.getBlock(bedLocation.posX, bedLocation.posY, bedLocation.posZ) == ModBlocks.blockSleepingBag)
        {
            //If the player wakes up in one of those super confortable SleepingBags (tm) (Patent Pending)
            BackpackProperty.get(event.entityPlayer).setForceCampFire(true);
            LogHelper.info("Player just woke up in a sleeping bag, forcing respawn in the last lighted campfire, if there's any");
        } else
        {
            //If it's a regular bed or whatever
            BackpackProperty.get(event.entityPlayer).setForceCampFire(false);
        }
    }


    @SubscribeEvent
    public void tickPlayer(TickEvent.PlayerTickEvent event)
    {


        if (event.player != null)
        {
            if (!event.player.isDead)
            {
                BackpackProperty prop = BackpackProperty.get(event.player);
                if (Wearing.isWearingWearable(event.player) && event.phase == TickEvent.Phase.END )
                {
                    ((IBackWearableItem)Wearing.getWearingWearable(event.player).getItem()).onEquippedUpdate(event.player.worldObj, event.player, prop.getWearable());
                    if(event.side.isServer())
                    {
                        BackpackProperty.syncToNear(event.player);
                        if(--tickCounter == 0)
                        {
                            tickCounter = 20;
                            BackpackProperty.syncToNear(event.player);
                        }
                    }
                }
            }
        }
    }
}

