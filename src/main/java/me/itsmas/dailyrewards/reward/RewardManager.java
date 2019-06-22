package me.itsmas.dailyrewards.reward;

import me.itsmas.dailyrewards.DailyRewards;
import me.itsmas.dailyrewards.util.Util;
import org.apache.commons.lang.math.NumberUtils;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class RewardManager
{
    private final DailyRewards plugin;

    public RewardManager(DailyRewards plugin)
    {
        this.plugin = plugin;

        new RewardListener(plugin);

        parseRewards();
    }

    private final Map<UUID, RewardData> rewards = new HashMap<>();

    public RewardData getData(Player player)
    {
        return rewards.get(player.getUniqueId());
    }

    void insertData(Player player, RewardData data)
    {
        rewards.put(player.getUniqueId(), data);
    }

    void saveData(Player player)
    {
        RewardData data = rewards.remove(player.getUniqueId());

        plugin.getDataStorage().save(player, data);
    }

    private final List<Reward> baseRewards = new ArrayList<>();

    public List<Reward> getRewards(Player player)
    {
        int tier = getData(player).getTier();

        return baseRewards.stream().filter(reward -> reward.getTier() == tier).collect(Collectors.toList());
    }

    public Reward getRewardFromStack(ItemStack stack)
    {
        String itemName = stack.getItemMeta().getDisplayName();

        for (Reward reward : baseRewards)
        {
            if (reward.getName().equals(itemName))
            {
                return reward;
            }
        }

        return null;
    }

    private int highestTier = 1;

    int getHighestTier()
    {
        return highestTier;
    }

    private void parseRewards()
    {
        File tiersFolder = new File(plugin.getDataFolder(), "tiers");
        assert tiersFolder.isDirectory();

        for (File file : tiersFolder.listFiles())
        {
            String name = file.getName().split("\\.yml")[0];

            if (!NumberUtils.isNumber(name))
            {
                Util.logFatal("\"" + name + "\" is not a valid tier number");
                continue;
            }

            int tier = Integer.parseInt(name);

            if (tier > highestTier)
            {
                highestTier = tier;
            }

            parseTier(file, tier);
        }
    }

    private void parseTier(File file, int tier)
    {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        for (String section : config.getKeys(false))
        {
            if (!NumberUtils.isNumber(section))
            {
                Util.logFatal("\"" + section + "\" is not a valid reward number");
                continue;
            }

            parseSection(config, section, tier);
        }
    }

    private void parseSection(YamlConfiguration config, String section, int tier)
    {
        String basePath = section + ".";

        String rawMaterial = config.getString(basePath + "material");

        try
        {
            Material material = Material.valueOf(rawMaterial);
            int data = config.getInt(basePath + "data", 0);

            String name = Util.colour(config.getString(basePath + "name"));

            List<String> lore = config.getStringList(basePath + "lore");
            lore = lore.stream().map(Util::colour).collect(Collectors.toList());

            List<String> actions = config.getStringList(basePath + "actions");

            baseRewards.add(new Reward(plugin, name, tier, material, data, lore, actions));
        }
        catch (IllegalArgumentException ex)
        {
            Util.logFatal("Unable to parse reward " + section + " in tier " + tier);
            ex.printStackTrace();
        }
    }
}
