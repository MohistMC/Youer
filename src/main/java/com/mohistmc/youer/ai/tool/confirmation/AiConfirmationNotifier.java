package com.mohistmc.youer.ai.tool.confirmation;

import com.mohistmc.youer.util.I18n;
import java.util.function.Function;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

public final class AiConfirmationNotifier {
    private final Function<java.util.UUID, Player> players;
    public AiConfirmationNotifier(Function<java.util.UUID, Player> players) { this.players = players; }
    public void notify(AiPendingAction action) {
        Player player = players.apply(action.playerId());
        if (player != null) {
            player.sendMessage(Component.text(I18n.as("ai.tool.confirmation.required", action.summary()),
                            NamedTextColor.YELLOW)
                    .append(Component.space())
                    .append(Component.text(I18n.as("ai.tool.confirmation.confirm"), NamedTextColor.GREEN)
                            .clickEvent(ClickEvent.runCommand("/ai confirm " + action.id())))
                    .append(Component.space())
                    .append(Component.text(I18n.as("ai.tool.confirmation.cancel"), NamedTextColor.RED)
                            .clickEvent(ClickEvent.runCommand("/ai cancel " + action.id()))));
        }
    }
}
