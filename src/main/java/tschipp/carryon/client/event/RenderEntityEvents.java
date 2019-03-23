package tschipp.carryon.client.event;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import tschipp.carryon.common.handler.RegistrationHandler;
import tschipp.carryon.common.helper.ScriptParseHelper;
import tschipp.carryon.common.item.ItemEntity;
import tschipp.carryon.common.scripting.CarryOnOverride;
import tschipp.carryon.common.scripting.ScriptChecker;

public class RenderEntityEvents
{
	
	/*
	 * Renders the Entity in First Person
	 */
	@OnlyIn(Dist.CLIENT)
	@SubscribeEvent
	public void renderHand(RenderHandEvent event)
	{
		World world = Minecraft.getInstance().world;
		EntityPlayer player = Minecraft.getInstance().player;
		ItemStack stack = player.getHeldItemMainhand();
		int perspective = Minecraft.getInstance().gameSettings.thirdPersonView;
		float partialticks = event.getPartialTicks();

		if (!stack.isEmpty() && stack.getItem() == RegistrationHandler.itemEntity && ItemEntity.hasEntityData(stack))
		{
			if(ModList.get().isLoaded("realrender") || ModList.get().isLoaded("rfpr"))
				return;
			
			
			Entity entity = ItemEntity.getEntity(stack, world);

			if (entity != null)
			{
				double d0 = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialticks;
				double d1 = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialticks;
				double d2 = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialticks;

				entity.setPosition(d0, d1, d2);
				entity.rotationYaw = 0.0f;
				entity.prevRotationYaw = 0.0f;
				entity.setRotationYawHead(0.0f);
				
				float height = entity.height;
				float width = entity.width;
				GlStateManager.pushMatrix();
				GlStateManager.scaled(.8, .8, .8);
				GlStateManager.rotatef(180, 0, 1, 0);
				GlStateManager.translated(0.0, -height - .1, width + 0.1);
				GlStateManager.enableAlphaTest();

				if (perspective == 0)
				{
					RenderHelper.enableStandardItemLighting();
					Minecraft.getInstance().getRenderManager().setRenderShadow(false);

					CarryOnOverride carryOverride = ScriptChecker.getOverride(player);
					if (carryOverride != null)
					{
						double[] translation = ScriptParseHelper.getXYZArray(carryOverride.getRenderTranslation());
						double[] rotation = ScriptParseHelper.getXYZArray(carryOverride.getRenderRotation());
						double[] scaled = ScriptParseHelper.getscaled(carryOverride.getRenderscaled());
						String entityname = carryOverride.getRenderNameEntity();
						if (entityname != null)
						{
							Entity newEntity = EntityType.create(world, new ResourceLocation(entityname));
							if (newEntity != null)
							{
								NBTTagCompound nbttag = carryOverride.getRenderNBT();
								if (nbttag != null)
									newEntity.deserializeNBT(nbttag);
								entity = newEntity;
								entity.setPosition(d0, d1, d2);
								entity.rotationYaw = 0.0f;
								entity.prevRotationYaw = 0.0f;
								entity.setRotationYawHead(0.0f);
							}
						}

						GlStateManager.translated(translation[0], translation[1], translation[2]);
						GlStateManager.rotatef((float) rotation[0], 1, 0, 0);
						GlStateManager.rotatef((float) rotation[1], 0, 1, 0);
						GlStateManager.rotatef((float) rotation[2], 0, 0, 1);
						GlStateManager.scaled(scaled[0], scaled[1], scaled[2]);

					}

					if(entity instanceof EntityLiving)
						((EntityLiving) entity).hurtTime = 0;
					
					this.renderEntityStatic(entity);
					Minecraft.getInstance().getRenderManager().setRenderShadow(true);
				}

				GlStateManager.disableAlphaTest();
				GlStateManager.scaled(1, 1, 1);
				GlStateManager.popMatrix();

				RenderHelper.disableStandardItemLighting();
				GlStateManager.disableRescaleNormal();
				GlStateManager.activeTexture(OpenGlHelper.GL_TEXTURE1);
				GlStateManager.disableTexture2D();
				GlStateManager.activeTexture(OpenGlHelper.GL_TEXTURE0);

				if (perspective == 0)
				{
					event.setCanceled(true);
				}
			}
		}
	}

	@OnlyIn(Dist.CLIENT)
	private void renderEntityStatic(Entity entity)
	{
		if (entity.ticksExisted == 0)
		{
			entity.lastTickPosX = entity.posX;
			entity.lastTickPosY = entity.posY;
			entity.lastTickPosZ = entity.posZ;
		}

		float f = entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw);
		int i = this.getBrightnessForRender(entity, Minecraft.getInstance().player);

		if (entity.isBurning())
		{
			i = 15728880;
		}

		int j = i % 65536;
		int k = i / 65536;
		OpenGlHelper.glMultiTexCoord2f(OpenGlHelper.GL_TEXTURE1, j, k);
		GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);

		
		this.setLightmapDisabled(false);
		
		
		
		Minecraft.getInstance().getRenderManager().renderEntity(entity, 0.0D, 0.0D, 0.0D, f, 0.0F, true);
		this.setLightmapDisabled(true);
	}

	@OnlyIn(Dist.CLIENT)
	private int getBrightnessForRender(Entity entity, EntityPlayer player)
	{
		BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos(MathHelper.floor(player.posX), 0, MathHelper.floor(player.posZ));

		if (entity.world.isBlockLoaded(blockpos$mutableblockpos))
		{
			blockpos$mutableblockpos.setY(MathHelper.floor(player.posY + entity.getEyeHeight()));
			return entity.world.getCombinedLight(blockpos$mutableblockpos, 0);
		}
		else
		{
			return 0;
		}
	}

	@OnlyIn(Dist.CLIENT)
	private void setLightmapDisabled(boolean disabled)
	{
		GlStateManager.activeTexture(OpenGlHelper.GL_TEXTURE1);

		if (disabled)
		{
			GlStateManager.disableTexture2D();
		}
		else
		{
			GlStateManager.enableTexture2D();
		}

		GlStateManager.activeTexture(OpenGlHelper.GL_TEXTURE0);
	}

	/*
	 * Renders the Entity in Third Person
	 */
	@OnlyIn(Dist.CLIENT)
	@SubscribeEvent
	public void onPlayerRenderPost(RenderPlayerEvent.Post event)
	{
		World world = Minecraft.getInstance().world;
		EntityPlayer player = event.getEntityPlayer();
		event.getRenderer().getMainModel();
		EntityPlayerSP clientPlayer = Minecraft.getInstance().player;
		ItemStack stack = player.getHeldItemMainhand();
		float partialticks = event.getPartialRenderTick();

		if (!stack.isEmpty() && stack.getItem() == RegistrationHandler.itemEntity && ItemEntity.hasEntityData(stack))
		{
			Entity entity = ItemEntity.getEntity(stack, world);
			float rotation = 0;

			if (player.getRidingEntity() != null && player.getRidingEntity() instanceof EntityLivingBase)
				rotation = -(player.prevRotationYawHead + (player.rotationYawHead - player.prevRotationYawHead) * partialticks);
			else
				rotation = -(player.prevRenderYawOffset + (player.renderYawOffset - player.prevRenderYawOffset) * partialticks);
			
			if (entity != null)
			{
				double d0 = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialticks;
				double d1 = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialticks;
				double d2 = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialticks;

				double c0 = clientPlayer.lastTickPosX + (clientPlayer.posX - clientPlayer.lastTickPosX) * partialticks;
				double c1 = clientPlayer.lastTickPosY + (clientPlayer.posY - clientPlayer.lastTickPosY) * partialticks;
				double c2 = clientPlayer.lastTickPosZ + (clientPlayer.posZ - clientPlayer.lastTickPosZ) * partialticks;

				double xOffset = d0 - c0;
				double yOffset = d1 - c1;
				double zOffset = d2 - c2;

				float height = entity.height;
				float width = entity.width;
				float multiplier = height * width;

				entity.setPosition(c0, c1, c2);
				entity.rotationYaw = 0.0f;
				entity.prevRotationYaw = 0.0f;
				entity.setRotationYawHead(0.0f);

				GlStateManager.pushMatrix();
				GlStateManager.translated(xOffset, yOffset, zOffset);
				GlStateManager.scaled((10 - multiplier) * 0.08, (10 - multiplier) * 0.08, (10 - multiplier) * 0.08);
				GlStateManager.rotatef(rotation, 0, 1f, 0);
				GlStateManager.translated(0.0, height / 2 + -(height / 2) + 1, width - 0.1 < 0.7 ? width - 0.1 + (0.7 - (width - 0.1)) : width - 0.1);

				if((ModList.get().isLoaded("realrender") || ModList.get().isLoaded("rfpr")) && Minecraft.getInstance().gameSettings.thirdPersonView == 0)
					GlStateManager.translated(0, 0, -0.3);
				
				if (player.isSneaking())
				{
					GlStateManager.translated(0, -0.3, 0);
				}

				Minecraft.getInstance().getRenderManager().setRenderShadow(false);
				
				CarryOnOverride carryOverride = ScriptChecker.getOverride(player);
				if (carryOverride != null)
				{
					double[] translation = ScriptParseHelper.getXYZArray(carryOverride.getRenderTranslation());
					double[] rot = ScriptParseHelper.getXYZArray(carryOverride.getRenderRotation());
					double[] scaled = ScriptParseHelper.getscaled(carryOverride.getRenderscaled());
					String entityname = carryOverride.getRenderNameEntity();
					if (entityname != null)
					{
						Entity newEntity = EntityType.create(world, new ResourceLocation(entityname));
						if (newEntity != null)
						{
							NBTTagCompound nbttag = carryOverride.getRenderNBT();
							if (nbttag != null)
								newEntity.deserializeNBT(nbttag);
							entity = newEntity;
							entity.setPosition(c0, c1, c2);
							entity.rotationYaw = 0.0f;
							entity.prevRotationYaw = 0.0f;
							entity.setRotationYawHead(0.0f);
						}
					}

					GlStateManager.translated(translation[0], translation[1], translation[2]);
					GlStateManager.rotatef((float) rot[0], 1, 0, 0);
					GlStateManager.rotatef((float) rot[1], 0, 1, 0);
					GlStateManager.rotatef((float) rot[2], 0, 0, 1);
					GlStateManager.scaled(scaled[0], scaled[1], scaled[2]);

				}
				
				if(entity instanceof EntityLiving)
					((EntityLiving) entity).hurtTime = 0;
				
				Minecraft.getInstance().getRenderManager().renderEntityStatic(entity, 0.0f, false);
				Minecraft.getInstance().getRenderManager().setRenderShadow(true);

				GlStateManager.scaled(1, 1, 1);
				GlStateManager.popMatrix();
			}
		}
	}
}
