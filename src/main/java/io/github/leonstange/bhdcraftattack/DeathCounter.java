package io.github.leonstange.bhdcraftattack;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import net.kyori.adventure.text.Component;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.RenderType;
import org.bukkit.scoreboard.Scoreboard;

public final class DeathCounter {
    private static final String NAME = "bhd_ca_deaths";
    private final Map<Scoreboard, Tracked> boards = new HashMap<>();

    public void update(Collection<? extends Player> players) {
        for (Player viewer : players) {
            Scoreboard board = viewer.getScoreboard();
            Tracked tracked = boards.get(board);
            if (tracked != null && board.getObjective(NAME) == null) {
                boards.remove(board);
                tracked = null;
            }
            if (tracked == null) {
                Objective own = board.getObjective(NAME);
                if (own != null && !own.getTrackedCriteria().equals(Criteria.DUMMY)) {
                    continue;
                }
                Objective displayed = board.getObjective(DisplaySlot.PLAYER_LIST);
                String previous = displayed == null || displayed.getName().equals(NAME) ? null : displayed.getName();
                // A scoreboard can survive a hard server stop; resume our reserved objective.
                if (own == null) {
                    own = board.registerNewObjective(NAME, Criteria.DUMMY, Component.text("Tode"), RenderType.INTEGER);
                }
                own.setDisplaySlot(DisplaySlot.PLAYER_LIST);
                tracked = new Tracked(own, previous);
                boards.put(board, tracked);
            }
            for (Player player : players) {
                tracked.own().getScore(player.getName()).setScore(player.getStatistic(Statistic.DEATHS));
            }
        }
    }

    public void close() {
        boards.forEach((board, tracked) -> {
            Objective own = board.getObjective(NAME);
            if (own == null) {
                return;
            }
            Objective active = board.getObjective(DisplaySlot.PLAYER_LIST);
            boolean displayed = active != null && active.getName().equals(NAME);
            own.unregister();
            Objective previous = tracked.previous() == null ? null : board.getObjective(tracked.previous());
            if (displayed && previous != null) {
                previous.setDisplaySlot(DisplaySlot.PLAYER_LIST);
            }
        });
        boards.clear();
    }

    private record Tracked(Objective own, String previous) { }
}
