package me.seasnail.yarnremapper;

import javax.swing.*;

public class Theme {
    public final String name;
    public final String title;
    private final Runnable apply;

    public Theme(String name, String title, Runnable apply) {
        this.name = name;
        this.title = title;
        this.apply = apply;
    }

    public JMenuItem createMenuItem() {
        JMenuItem item = new JMenuItem(title);

        item.addActionListener(e -> {
            apply.run();

            if (Main.gui != null) {
                SwingUtilities.updateComponentTreeUI(Main.gui);
                Main.saveConfig(this);
            }
        });

        return item;
    }

    public void apply() {
        apply.run();
    }
}
