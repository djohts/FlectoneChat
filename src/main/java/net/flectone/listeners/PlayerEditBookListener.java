package net.flectone.listeners;

import net.flectone.utils.ObjectUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

public class PlayerEditBookListener implements Listener {

    @EventHandler
    public void onPlayerEditBook(PlayerEditBookEvent event) {
        BookMeta bookMeta = event.getNewBookMeta();
        String command = "book";
        Player player = event.getPlayer();
        ItemStack itemInHand = player.getItemInUse();

        for (int x = 1; x <= event.getNewBookMeta().getPages().size(); x++) {
            String string = bookMeta.getPage(x);

            if (string.isEmpty()) continue;

            bookMeta.setPage(x, ObjectUtil.buildFormattedMessage(player, command, string, itemInHand));
        }

        bookMeta.setTitle(ObjectUtil.buildFormattedMessage(player, command, bookMeta.getTitle(), itemInHand));

        event.setNewBookMeta(bookMeta);
    }
}