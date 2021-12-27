package io.github.polymeta.separator;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.source.ConsoleSource;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.command.SendCommandEvent;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.plugin.Dependency;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.Tuple;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Plugin(id = "separator",
        name = "Separator",
        version = "1.0-SNAPSHOT",
        description = "Forces two players to ignore each other!",
        authors = {"Polymeta"},
        dependencies = {@Dependency(id = "nucleus")})
public class Separator
{
    @Inject
    private Logger logger;

    private final Map<Tuple<UUID, UUID>, Integer> currentlySeparated = new HashMap<>();

    @Listener
    public void onServerStart(GameStartedServerEvent event)
    {
        Sponge.getCommandManager().register(
                this,
                CommandSpec.builder()
                        .permission("separator.admin")
                        .arguments(
                                GenericArguments.player(Text.of("playerOne")),
                                GenericArguments.player(Text.of("playerTwo")),
                                GenericArguments.integer(Text.of("minutes"))
                        )
                        .executor((src, args) ->
                        {
                            Player player1 = args.<Player>getOne("playerOne").orElseThrow(() ->
                                    new CommandException(Text.of("Need player to be online!")));
                            Player player2 = args.<Player>getOne("playerTwo").orElseThrow(() ->
                                    new CommandException(Text.of("Need player to be online!")));
                            int minutes = args.<Integer>getOne("minutes").orElse(5);

                            ConsoleSource console = Sponge.getServer().getConsole();

                            Sponge.getCommandManager().process(console,
                                    "sudo " + player1.getName() + " ignore " + player2.getName());
                            Sponge.getCommandManager().process(console,
                                    "sudo " + player2.getName() + " ignore " + player1.getName());
                            this.currentlySeparated.put(
                                    new Tuple<>(player1.getUniqueId(), player2.getUniqueId()),
                                    minutes);

                            player1.sendMessage(Text.of(TextColors.RED,
                                    "You have been separated from " + player2.getName() + " for " + minutes + " minutes!"));
                            player2.sendMessage(Text.of(TextColors.RED,
                                    "You have been separated from " + player1.getName() + " for " + minutes + " minutes!"));

                            return CommandResult.success();
                        })
                        .build(),
                "separate", "s");

        Task.builder()
                .execute(() ->
                        this.currentlySeparated.entrySet().forEach(entry ->
                        {
                            int current = this.currentlySeparated.get(entry.getKey());
                            if (--current <= 0)
                                this.currentlySeparated.remove(entry.getKey());
                            else
                                this.currentlySeparated.put(entry.getKey(), current);
                        }))
                .interval(1, TimeUnit.MINUTES)
                .name("Separator Ticker")
                .submit(this);

        this.logger.info("Separator by Polymeta ready!");
    }

    @Listener
    public void onCommand(SendCommandEvent event, @Root Player player)
    {
        if(event.getCommand().equalsIgnoreCase("ignore") && this.currentlySeparated.keySet()
                .stream()
                .anyMatch((key) ->
                        key.getFirst().equals(player.getUniqueId()) || key.getSecond().equals(player.getUniqueId())))
        {
            event.setCancelled(true);
            player.sendMessage(Text.of(TextColors.RED, "You can currently not use this command! Try again after separation expired."));
        }
    }
}
