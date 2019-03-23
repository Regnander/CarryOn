package tschipp.carryon.common.scripting;

import java.lang.reflect.Method;

import javax.annotation.Nullable;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementManager;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import tschipp.carryon.common.config.Configs.Settings;
import tschipp.carryon.common.handler.ListHandler;
import tschipp.carryon.common.helper.ScriptParseHelper;

public class ScriptChecker
{
	@Nullable
	public static CarryOnOverride inspectBlock(IBlockState state, World world, BlockPos pos, @Nullable NBTTagCompound tag)
	{
		if (!Settings.useScripts.get())
			return null;

		Block block = state.getBlock();
		Material material = state.getMaterial();
		float hardness = state.getBlockHardness(world, pos);
		@SuppressWarnings("deprecation")
		float resistance = block.getExplosionResistance();
		NBTTagCompound nbt = tag;

		boolean isAllowed = Settings.useWhitelistBlocks.get() ? ListHandler.isAllowed(block) : !ListHandler.isForbidden(block);

		if (isAllowed)
		{
			for (CarryOnOverride override : ScriptReader.OVERRIDES.values())
			{
				if (override.isBlock())
				{
					if (matchesAll(override, block, material, hardness, resistance, nbt))
						return override;
				}
			}
		}

		return null;
	}

	@Nullable
	public static CarryOnOverride inspectEntity(Entity entity)
	{
		if (!Settings.useScripts.get())
			return null;

		String name = entity.getType().getRegistryName().toString();
		float height = entity.height;
		float width = entity.width;
		float health = entity instanceof EntityLivingBase ? ((EntityLivingBase) entity).getHealth() : 0.0f;
		NBTTagCompound tag = new NBTTagCompound();
		entity.deserializeNBT(tag);

		boolean isAllowed = Settings.useWhitelistEntities.get() ? ListHandler.isAllowed(entity) : !ListHandler.isForbidden(entity);

		if (isAllowed)
		{
			for (CarryOnOverride override : ScriptReader.OVERRIDES.values())
			{
				if (override.isEntity())
				{
					if (matchesAll(override, name, height, width, health, tag))
						return override;
				}
			}
		}

		return null;
	}

	public static boolean matchesAll(CarryOnOverride override, String name, float height, float width, float health, NBTTagCompound tag)
	{
		boolean matchname = override.getTypeNameEntity() == null ? true : name.equals(override.getTypeNameEntity());
		boolean matchheight = ScriptParseHelper.matches(height, override.getTypeHeight());
		boolean matchwidth = ScriptParseHelper.matches(width, override.getTypeWidth());
		boolean matchhealth = ScriptParseHelper.matches(health, override.getTypeHealth());
		boolean matchnbt = ScriptParseHelper.matches(tag, override.getTypeEntityTag());

		return (matchname && matchheight && matchwidth && matchhealth && matchnbt);
	}

	public static boolean matchesAll(CarryOnOverride override, Block block, Material material, float hardness, float resistance, NBTTagCompound nbt)
	{
		boolean matchnbt = ScriptParseHelper.matches(nbt, override.getTypeBlockTag());
		boolean matchblock = ScriptParseHelper.matches(block, override.getTypeNameBlock());
		boolean matchmaterial = ScriptParseHelper.matches(material, override.getTypeMaterial());
		boolean matchhardness = ScriptParseHelper.matches(hardness, override.getTypeHardness());
		boolean matchresistance = ScriptParseHelper.matches(resistance, override.getTypeResistance());

		return (matchnbt && matchblock && matchmaterial && matchhardness && matchresistance);
	}

	public static boolean fulfillsConditions(CarryOnOverride override, EntityPlayer player)
	{
		AdvancementManager manager = ((EntityPlayerMP) player).server.getAdvancementManager();
		Advancement adv = manager.getAdvancement(new ResourceLocation((override.getConditionAchievement()) == null ? "" : override.getConditionAchievement()));

		boolean achievement = adv == null ? true : ((EntityPlayerMP) player).getAdvancements().getProgress(adv).isDone();
		boolean gamemode = ScriptParseHelper.matches(((EntityPlayerMP) player).interactionManager.getGameType().getID(), override.getConditionGamemode());
		boolean gamestage = true;
		if (ModList.get().isLoaded("gamestages"))
		{
			if (override.getConditionGamestage() != null)
			{
				try
				{
					Class<?> gameStageHelper = Class.forName("net.darkhax.gamestages.GameStageHelper");
					Class<?> iStageData = Class.forName("net.darkhax.gamestages.data.IStageData");

					Method getPlayerData = ObfuscationReflectionHelper.findMethod(gameStageHelper, "getPlayerData", EntityPlayer.class);
					Method hasStage = ObfuscationReflectionHelper.findMethod(iStageData, "hasStage", String.class);

					Object stageData = getPlayerData.invoke(null, player);
					String condition = override.getConditionGamestage();
					gamestage = (boolean) hasStage.invoke(stageData, condition);
				}
				catch (Exception e)
				{
					try
					{
						Class<?> playerDataHandler = Class.forName("net.darkhax.gamestages.capabilities.PlayerDataHandler");
						Class<?> iStageData = Class.forName("net.darkhax.gamestages.capabilities.PlayerDataHandler$IStageData");

						Method getStageData = ObfuscationReflectionHelper.findMethod(playerDataHandler, "getStageData", EntityPlayer.class);
						Method hasUnlockedStage = ObfuscationReflectionHelper.findMethod(iStageData, "hasUnlockedStage",  String.class);

						Object stageData = getStageData.invoke(null, player);
						String condition = override.getConditionGamestage();
						gamestage = (boolean) hasUnlockedStage.invoke(stageData, condition);

					}
					catch (Exception ex)
					{
					}
				}

			}
		}

		boolean position = ScriptParseHelper.matches(player.getPosition(), override.getConditionPosition());
		boolean xp = ScriptParseHelper.matches(player.experienceLevel, override.getConditionXp());
		boolean scoreboard = ScriptParseHelper.matchesScore(player, override.getConditionScoreboard());
		boolean effects = ScriptParseHelper.hasEffects(player, override.getConditionEffects());

		return (achievement && gamemode && gamestage && position && xp && scoreboard && effects);
	}

	@Nullable
	public static CarryOnOverride getOverride(EntityPlayer player)
	{
		NBTTagCompound tag = player.getEntityData();

		if (tag != null && tag.hasKey("overrideKey"))
		{
			int key = tag.getInt("overrideKey");

			return ScriptReader.OVERRIDES.get(key);
		}

		return null;
	}

	public static void setCarryOnOverride(EntityPlayer player, int i)
	{
		NBTTagCompound tag = player.getEntityData();

		if (tag != null)
			tag.setInt("overrideKey", i);
	}

}
