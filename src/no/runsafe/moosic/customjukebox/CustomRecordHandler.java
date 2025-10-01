package no.runsafe.moosic.customjukebox;

import com.google.common.collect.Lists;
import no.runsafe.framework.api.IConfiguration;
import no.runsafe.framework.api.ILocation;
import no.runsafe.framework.api.block.IBlock;
import no.runsafe.framework.api.block.IJukebox;
import no.runsafe.framework.api.event.block.IBlockBreakEvent;
import no.runsafe.framework.api.event.player.IPlayerRightClickBlock;
import no.runsafe.framework.api.event.plugin.IConfigurationChanged;
import no.runsafe.framework.api.event.plugin.IPluginEnabled;
import no.runsafe.framework.api.player.IPlayer;
import no.runsafe.framework.internal.log.Console;
import no.runsafe.framework.minecraft.Item;
import no.runsafe.framework.minecraft.event.block.RunsafeBlockBreakEvent;
import no.runsafe.framework.minecraft.item.RunsafeItemStack;
import no.runsafe.framework.minecraft.item.meta.RunsafeMeta;
import no.runsafe.moosic.MusicHandler;
import no.runsafe.moosic.MusicTrack;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class CustomRecordHandler implements IConfigurationChanged, IPlayerRightClickBlock, IBlockBreakEvent, IPluginEnabled
{
	public CustomRecordHandler(MusicHandler musicHandler, CustomJukeboxRepository repository)
	{
		this.musicHandler = musicHandler;
		this.repository = repository;
	}

	@Override
	public void OnBlockBreakEvent(RunsafeBlockBreakEvent event)
	{
		CustomJukebox jukebox = getJukeboxAtLocation(event.getBlock().getLocation());
		if (jukebox != null)
			stopJukebox(jukebox);
	}

	@Override
	public boolean OnPlayerRightClick(IPlayer player, RunsafeMeta usingItem, IBlock targetBlock)
	{
		ILocation blockLocation = targetBlock.getLocation();
		if (!(targetBlock instanceof IJukebox))
			return true;

		CustomJukebox jukebox = getJukeboxAtLocation(blockLocation);
		if (jukebox != null)
		{
			stopJukebox(jukebox);
			return false;
		}

		if (usingItem == null || !usingItem.is(Item.Special.Crafted.EnchantedBook) || !isCustomRecord(usingItem))
			return true;

		((IJukebox) targetBlock).eject();
		player.removeExactItem(usingItem, 1);
		jukebox = playJukebox(player, new CustomJukebox(blockLocation, usingItem));
		repository.storeJukebox(blockLocation, usingItem);
		jukeboxes.add(jukebox);
		return false;
	}

	@Override
	public void OnPluginEnabled()
	{
		// Restore any active jukeboxes from the DB.
		jukeboxes.addAll(repository.getJukeboxes());
	}

	private void stopJukebox(CustomJukebox jukebox)
	{
		int playerID = jukebox.getPlayerID();
		if (musicHandler.playerExists(playerID))
			musicHandler.forceStop(playerID);

		jukebox.ejectRecord();
		jukeboxes.remove(jukebox);
		repository.deleteJukeboxes(jukebox.getLocation());
	}

	private boolean isCustomRecord(RunsafeItemStack item)
	{
		return item instanceof RunsafeMeta
			&& customRecordName.equalsIgnoreCase(((RunsafeMeta) item).getDisplayName())
			&& ((RunsafeMeta) item).hasLore();
	}

	public CustomJukebox getJukeboxAtLocation(ILocation location)
	{
		for (CustomJukebox jukebox : jukeboxes)
			if (jukebox.getLocation().getWorld().equals(location.getWorld()))
				if (jukebox.getLocation().distance(location) < 1)
					return jukebox;

		return null;
	}

	private CustomJukebox playJukebox(IPlayer player, CustomJukebox jukebox)
	{
		File musicFile = musicHandler.loadSongFile(jukebox.getSongName() + ".nbs");
		if (musicFile.exists())
		{
			MusicTrack musicTrack = null;
			try
			{
				musicTrack = new MusicTrack(musicFile);
			}
			catch (Exception e)
			{
				Console.Global().logException(e);
			}

			if (musicTrack != null)
				jukebox.setPlayerID(musicHandler.startSong(musicTrack, jukebox.getLocation(), 10));
		}
		else
		{
			// Corrupt record or invalid file.
			player.sendColouredMessage("&cThe record cracks and scratches as you put it in the jukebox.");
		}
		return jukebox;
	}

	public void onTrackPlayerStopped(int trackPlayerID)
	{
		for (CustomJukebox jukebox : Lists.newArrayList(jukeboxes))
		{
			if (jukebox.getPlayerID() == trackPlayerID)
			{
				jukeboxes.remove(jukebox);
				jukebox.setPlayerID(-1);
				jukeboxes.add(jukebox);
			}
		}
	}

	@Override
	public void OnConfigurationChanged(IConfiguration configuration)
	{
		customRecordName = configuration.getConfigValueAsString("customRecordName");
	}

	private final List<CustomJukebox> jukeboxes = new ArrayList<>();
	private String customRecordName;
	private final MusicHandler musicHandler;
	private final CustomJukeboxRepository repository;
}
