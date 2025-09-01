package com.gamerforea.eventhelper.util;

import com.gamerforea.eventhelper.config.ConfigUtils;
import com.gamerforea.eventhelper.fake.FakePlayerContainer;
import com.mojang.authlib.GameProfile;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.ReflectionHelper;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.init.Blocks;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

public final class FastUtils
{
	@Deprecated
	@Nonnull
	public static Configuration getConfig(@Nonnull String cfgName)
	{
		return ConfigUtils.getConfig(cfgName);
	}

	public static Optional<EntityPlayerMP> getOnlinePlayer(@Nullable String name) {
		return getOnlinePlayer(name, false);
	}

	/**
	 * Поиск онлайн игрока по его нику
	 * @param name игрока
	 * @param ignoreCase если true, то ищет игрока по нику без проверки регистра, иначе обязательно его проверяет
	 * @return {@link Optional} с найденным игроком
	 */
	public static Optional<EntityPlayerMP> getOnlinePlayer(@Nullable String name, boolean ignoreCase) {
		if (name == null) return Optional.empty();
		for (EntityPlayerMP p : getOnlinePlayers()) {
			String pName = p.getCommandSenderName();
			if (ignoreCase ? name.equalsIgnoreCase(pName) : name.equals(pName))
				return Optional.of(p);
		}
		return Optional.empty();
	}

	public static Optional<EntityPlayerMP> getOnlinePlayer(@Nullable UUID uuid) {
		if (uuid == null) return Optional.empty();
		for (EntityPlayerMP p : getOnlinePlayers()) {
			if (p.getPersistentID().getLeastSignificantBits() == uuid.getLeastSignificantBits() && p.getPersistentID().getMostSignificantBits() == uuid.getMostSignificantBits())
				return Optional.of(p);
		}
		return Optional.empty();
	}


	public static List<EntityPlayerMP> getOnlinePlayers() {
		return  MinecraftServer.getServer().getConfigurationManager().playerEntityList;
	}
	/**
	 * Проверка на то, является ли объект игрока реальным игроком
	 * @param o объект игрока. Может быть {@link UUID} ююид, {@link EntityPlayerMP} игрок, {@link String} ник
	 * @return true, если игрок реальный
	 */
	public static boolean isReallyPlayer(@Nullable Object o) {
		try {
			EntityPlayerMP pl = null;
			if(o instanceof EntityPlayerMP) {
				pl = (EntityPlayerMP) o;
			} else if(o instanceof UUID) {
				pl = getOnlinePlayer((UUID) o).orElse(null);
			} else if(o instanceof String) pl = getOnlinePlayer((String) o).orElse(null);
			return pl != null && !(pl instanceof FakePlayer) && pl.playerNetServerHandler != null && pl.playerNetServerHandler.netManager != null && pl.playerNetServerHandler.netManager.channel() != null && pl.playerNetServerHandler.netManager.channel().isOpen();
		} catch (Exception ignored) {}
		return false;
	}
	/**
	 * Получение тайла без загрузки чанка, если чанк не загружен, вернет null
	 */
	@Nullable
	public static TileEntity getTileEntitySafe(@Nonnull World world, int x, int y, int z) {
		return world.blockExists(x, y, z) ? world.getTileEntity(x, y, z) : null;
	}

	/**
	 * Получение блока без загрузки чанка, если чанк не загружен, вернет Blocks.air
	 */
	public static Block getBlockSafe(@Nonnull World world, int x, int y, int z) {
		//К сожалению, Bukkit игнорирует loadChunkOnProvideRequest
		return world.blockExists(x, y, z) ? world.getBlock(x, y, z) : Blocks.air;
	}

	/**
	 * Получение метадаты блока без загрузки чанка, если чанк не загружен, вернет 0
	 */
	public static int getBlockMetadataSafe(@Nonnull World world, int x, int y, int z) {
		//К сожалению, Bukkit игнорирует loadChunkOnProvideRequest
		return world.blockExists(x, y, z) ? world.getBlockMetadata(x, y, z) : 0;
	}

	/**
	 * Получение чанка без его чанка, если чанк не загружен, вернет null
	 */
	@Nullable
	public static Chunk getChunkSafe(@Nonnull World world, int chunkX, int chunkZ) {
		//К сожалению, Bukkit игнорирует loadChunkOnProvideRequest
		return world.blockExists(chunkX << 4, 60, chunkZ << 4) ? world.getChunkFromChunkCoords(chunkX, chunkZ) : null;
	}

	public static void stopPotionEffect(@Nonnull EntityLivingBase entity, @Nonnull Potion potion)
	{
		stopPotionEffect(entity.getActivePotionEffect(potion));
	}

	public static void stopPotionEffect(@Nullable PotionEffect potionEffect)
	{
		if (potionEffect != null && potionEffect.getDuration() > 0)
			ReflectionHelper.setPrivateValue(PotionEffect.class, potionEffect, 0, "field_76460_b", "duration");
	}

	public static <T extends TileEntity> boolean setProfile(
			@Nonnull World world, int x, int y, int z,
			@Nonnull Entity entity, Class<T> tileClass, Function<T, FakePlayerContainer> mapper)
	{
		if (entity instanceof EntityPlayer && world.blockExists(x, y, z))
		{
			TileEntity tile = world.getTileEntity(x, y, z);
			if (tile != null && tileClass.isInstance(tile))
			{
				FakePlayerContainer fake = mapper.apply((T) tile);
				fake.setProfile(entity);
				return true;
			}
		}
		return false;
	}

	public static boolean isOnline(@Nonnull EntityPlayer player)
	{
		if (player instanceof FakePlayer)
			return true;

		for (EntityPlayer playerOnline : (Iterable<EntityPlayer>) getServer().getConfigurationManager().playerEntityList)
		{
			if (playerOnline.equals(player))
				return true;
		}

		return false;
	}

	public static boolean isValidRealPlayer(@Nullable EntityPlayer player)
	{
		return isValidRealPlayer(player, true);
	}

	public static boolean isValidRealPlayer(@Nullable EntityPlayer player, boolean checkAlive)
	{
		if (player == null || player instanceof FakePlayer)
			return false;

		if (player instanceof EntityPlayerMP)
		{
			NetHandlerPlayServer connection = ((EntityPlayerMP) player).playerNetServerHandler;
			if (connection == null || !connection.func_147362_b().isChannelOpen())
				return false;
		}

		return !checkAlive || player.isEntityAlive();
	}

	@Nonnull
	public static FakePlayer getFake(@Nullable World world, @Nonnull FakePlayer fake)
	{
		fake.worldObj = world == null ? getEntityWorld() : world;
		return fake;
	}

	@Nonnull
	public static FakePlayer getFake(@Nullable World world, @Nonnull GameProfile profile)
	{
		return getFake(world, FakePlayerFactory.get((WorldServer) (world == null ? getEntityWorld() : world), profile));
	}

	@Nonnull
	public static EntityPlayer getLivingPlayer(@Nullable EntityLivingBase entity, @Nonnull FakePlayer modFake)
	{
		return entity instanceof EntityPlayer ? (EntityPlayer) entity : getFake(entity == null ? null : entity.worldObj, modFake);
	}

	@Nonnull
	public static EntityPlayer getLivingPlayer(@Nullable EntityLivingBase entity, @Nonnull GameProfile modFakeProfile)
	{
		return entity instanceof EntityPlayer ? (EntityPlayer) entity : getFake(entity == null ? null : entity.worldObj, modFakeProfile);
	}

	@Nonnull
	public static EntityPlayer getThrowerPlayer(@Nullable EntityThrowable entity, @Nonnull FakePlayer modFake)
	{
		return getLivingPlayer(entity == null ? null : entity.getThrower(), modFake);
	}

	@Nonnull
	public static EntityPlayer getThrowerPlayer(@Nullable EntityThrowable entity, @Nonnull GameProfile modFakeProfile)
	{
		return getLivingPlayer(entity == null ? null : entity.getThrower(), modFakeProfile);
	}

	@Nonnull
	public static EntityLivingBase getThrower(@Nullable EntityThrowable entity, @Nonnull FakePlayer modFake)
	{
		if (entity == null)
			return getFake(getEntityWorld(), modFake);
		EntityLivingBase thrower = entity.getThrower();
		return thrower == null ? getFake(entity.worldObj, modFake) : thrower;
	}

	@Nonnull
	public static EntityLivingBase getThrower(@Nullable EntityThrowable entity, @Nonnull GameProfile modFakeProfile)
	{
		if (entity == null)
			return getFake(getEntityWorld(), modFakeProfile);
		EntityLivingBase thrower = entity.getThrower();
		return thrower == null ? getFake(entity.worldObj, modFakeProfile) : thrower;
	}

	@Nonnull
	private static MinecraftServer getServer()
	{
		return FMLCommonHandler.instance().getMinecraftServerInstance();
	}

	@Nonnull
	private static World getEntityWorld()
	{
		return getServer().getEntityWorld();
	}
}
