package no.runsafe.moosic.commands;

import no.runsafe.framework.api.IConfiguration;
import no.runsafe.framework.api.command.argument.IArgumentList;
import no.runsafe.framework.api.command.argument.TrailingArgument;
import no.runsafe.framework.api.command.player.PlayerCommand;
import no.runsafe.framework.api.event.plugin.IConfigurationChanged;
import no.runsafe.framework.api.player.IPlayer;
import no.runsafe.framework.minecraft.Item;
import no.runsafe.framework.minecraft.item.meta.RunsafeMeta;

public class MakeRecord extends PlayerCommand implements IConfigurationChanged
{
	public MakeRecord()
	{
		super(
			"makerecord", "Forges a record with the item currently being held.", "runsafe.moosic.record.make",
			new TrailingArgument("song")
		);
	}

	@Override
	public String OnExecute(IPlayer executor, IArgumentList parameters)
	{
		RunsafeMeta item = Item.Special.Crafted.EnchantedBook.getItem();
		item.setAmount(1);
		item.addLore(parameters.getValue("song")).setDisplayName(customRecordName);
		executor.getInventory().addItems(item);
		return "&2Success!";
	}

	@Override
	public void OnConfigurationChanged(IConfiguration configuration)
	{
		this.customRecordName = configuration.getConfigValueAsString("customRecordName");
	}

	private String customRecordName;
}
