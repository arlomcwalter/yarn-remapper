package me.seasnail.yarnremapper;

import javax.swing.*;
import java.awt.*;

public class GUI extends JFrame {
    public JPanel content;

    public JTextField inputPath;
    public JButton inputBrowse;

    public JTextField outputPath;
    public JButton outputBrowse;

    public JComboBox<String> mcVersionSelector;
    public JCheckBox mcVersionSnapshots;

    public JComboBox<String> yarnVersionSelector;

    public JButton remapButton;

    public GUI() {
        setTitle("Yarn Remapper");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(600, 300));
        setSize(600, 300);
        setResizable(false);

        content = new JPanel();
        setContentPane(content);

        content.add(inputPanel());
        content.add(outputPanel());
        content.add(minecraftPanel());
        content.add(yarnPanel());
        content.add(remapPanel());
    }

    public JPanel inputPanel() {
        JPanel inputPanel = new JPanel();

        // Components
        JLabel inputLabel = new JLabel("Input JAR:", SwingConstants.CENTER);
        inputPath = new JTextField();
        inputBrowse = new JButton("Browse");

        // Layout
        GroupLayout inputLayout = new GroupLayout(inputPanel);

        inputLayout.setHorizontalGroup(
            inputLayout.createParallelGroup()
                .addGroup(inputLayout.createSequentialGroup()
                    .addGap(Short.MAX_VALUE)
                    .addGroup(inputLayout.createParallelGroup()
                        .addGroup(inputLayout.createSequentialGroup()
                            .addComponent(inputLabel)
                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(inputPath, 400, 400, 400)
                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(inputBrowse)
                        )
                    )
                    .addGap(Short.MAX_VALUE)
                )
        );

        inputLayout.setVerticalGroup(
            inputLayout.createParallelGroup()
                .addGroup(inputLayout.createSequentialGroup()
                    .addGap(28)
                    .addGroup(inputLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(inputLabel)
                        .addComponent(inputPath, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addComponent(inputBrowse)
                    )
                )
        );

        inputPanel.setLayout(inputLayout);

        return inputPanel;
    }

    public JPanel outputPanel() {
        JPanel outputPanel = new JPanel();

        JLabel outputLabel = new JLabel("Output Dir:", SwingConstants.CENTER);
        outputPath = new JTextField();
        outputBrowse = new JButton("Browse");

        GroupLayout inputLayout = new GroupLayout(outputPanel);

        inputLayout.setHorizontalGroup(
            inputLayout.createParallelGroup()
                .addGroup(inputLayout.createSequentialGroup()
                    .addGap(Short.MAX_VALUE)
                    .addGroup(inputLayout.createParallelGroup()
                        .addGroup(inputLayout.createSequentialGroup()
                            .addComponent(outputLabel)
                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(outputPath, 400, 400, 400)
                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(outputBrowse)
                        )
                    )
                    .addGap(Short.MAX_VALUE)
                )
        );

        inputLayout.setVerticalGroup(
            inputLayout.createParallelGroup()
                .addGroup(inputLayout.createSequentialGroup()
                    .addGroup(inputLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(outputLabel)
                        .addComponent(outputPath, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addComponent(outputBrowse)
                    )
                    .addGap(20)
                )
        );

        outputPanel.setLayout(inputLayout);

        return outputPanel;
    }

    public JPanel minecraftPanel() {
        JPanel minecraftPanel = new JPanel();

        // Components
        JLabel mcVersionLabel = new JLabel("Minecraft Version:", SwingConstants.CENTER);
        mcVersionSelector = new JComboBox<>();
        mcVersionSnapshots = new JCheckBox("Snapshots");

        // Layout
        GroupLayout inputLayout = new GroupLayout(minecraftPanel);

        inputLayout.setHorizontalGroup(
            inputLayout.createParallelGroup()
                .addGroup(inputLayout.createSequentialGroup()
                    .addGap(Short.MAX_VALUE)
                    .addGroup(inputLayout.createParallelGroup()
                        .addGroup(inputLayout.createSequentialGroup()
                            .addComponent(mcVersionLabel)
                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(mcVersionSelector, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(mcVersionSnapshots)
                        )
                    )
                    .addGap(Short.MAX_VALUE)
                )
        );

        inputLayout.setVerticalGroup(
            inputLayout.createParallelGroup()
                .addGroup(inputLayout.createSequentialGroup()
                    .addGroup(inputLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(mcVersionLabel)
                        .addComponent(mcVersionSelector, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addComponent(mcVersionSnapshots)
                    )
                )
        );

        minecraftPanel.setLayout(inputLayout);

        return minecraftPanel;
    }

    public JPanel yarnPanel() {
        JPanel yarnPanel = new JPanel();

        // Components
        JLabel yarnVersionLabel = new JLabel("Yarn Version:", SwingConstants.CENTER);
        yarnVersionSelector = new JComboBox<>();

        // Layout
        GroupLayout inputLayout = new GroupLayout(yarnPanel);

        inputLayout.setHorizontalGroup(
            inputLayout.createParallelGroup()
                .addGroup(inputLayout.createSequentialGroup()
                    .addGap(Short.MAX_VALUE)
                    .addGroup(inputLayout.createParallelGroup()
                        .addGroup(inputLayout.createSequentialGroup()
                            .addComponent(yarnVersionLabel)
                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(yarnVersionSelector, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        )
                    )
                    .addGap(Short.MAX_VALUE)
                )
        );

        inputLayout.setVerticalGroup(
            inputLayout.createParallelGroup()
                .addGroup(inputLayout.createSequentialGroup()
                    .addGroup(inputLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(yarnVersionLabel)
                        .addComponent(yarnVersionSelector, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    )
                    .addGap(20)
                )
        );

        yarnPanel.setLayout(inputLayout);

        return yarnPanel;
    }

    public JPanel remapPanel() {
        JPanel remapPanel = new JPanel();

        remapButton = new JButton("Remap");

        GroupLayout remapLayout = new GroupLayout(remapPanel);

        remapLayout.setHorizontalGroup(
            remapLayout.createSequentialGroup()
                .addGap(Short.MAX_VALUE)
                .addComponent(remapButton)
                .addGap(Short.MAX_VALUE)
        );

        remapLayout.setVerticalGroup(remapLayout.createParallelGroup().addComponent(remapButton));

        remapPanel.setLayout(remapLayout);

        return remapPanel;
    }
}
