package de.gaalop.gui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import org.apache.commons.beanutils.BeanUtils;

import de.gaalop.CodeGeneratorPlugin;
import de.gaalop.ConfigurationProperty;
import de.gaalop.OptimizationStrategyPlugin;
import de.gaalop.Plugin;
import de.gaalop.Plugins;

/**
 * This class represents the configuration panel.
 */
public class ConfigurationPanel extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7710406876645754914L;

	private JTabbedPane pluginPanes = new JTabbedPane();

	public ConfigurationPanel(JTabbedPane tabbedPanel) {
		tabbedPanel.addTab("Configuration", this);
		int indexOfPanel = tabbedPanel.indexOfComponent(this);
		tabbedPanel.setSelectedIndex(indexOfPanel);

		initialize();
	}

	private void initialize() {
		setLayout(new BorderLayout());
		add(pluginPanes, BorderLayout.CENTER);
		for (OptimizationStrategyPlugin strategy : Plugins.getOptimizationStrategyPlugins()) {
			addPluginConfig(strategy);
		}
		List<CodeGeneratorPlugin> sortedPlugins = new ArrayList<CodeGeneratorPlugin>(Plugins.getCodeGeneratorPlugins());
		Collections.sort(sortedPlugins, new PluginSorter());
		for (CodeGeneratorPlugin generator : sortedPlugins) {
			addPluginConfig(generator);
		}
		pluginPanes.setSelectedIndex(0);
	}

	private void addOptimizationConfig(OptimizationStrategyPlugin strategy) {
		JPanel panel = new JPanel();
		for (final Field property : getConfigurationProperties(strategy)) {
			
		}
	}

	private void addPluginConfig(final Plugin plugin) {
		JPanel configPanel = new JPanel();
		configPanel.setLayout(new BorderLayout());
		List<Field> properties = getConfigurationProperties(plugin);
		JPanel labels = new JPanel();
		labels.setLayout(new GridLayout(properties.size(), 1));
		JPanel fields = new JPanel();
		fields.setLayout(new GridLayout(properties.size(), 1));
		configPanel.add(labels, BorderLayout.WEST);
		configPanel.add(fields, BorderLayout.CENTER);
		for (final Field property : properties) {
			labels.add(new JLabel(property.getName()));
			try {
				String value = BeanUtils.getProperty(plugin, property.getName());
				ConfigurationProperty clazz = property.getAnnotation(ConfigurationProperty.class);
				switch (clazz.type()) {
				case BOOLEAN:
					final JCheckBox checkBox = new JCheckBox();
					checkBox.setSelected(Boolean.parseBoolean(value));
					checkBox.addActionListener(new ActionListener() {
						
						@Override
						public void actionPerformed(ActionEvent e) {
							try {
								BeanUtils.setProperty(plugin, property.getName(), checkBox.isSelected());
							} catch (IllegalAccessException e1) {
								e1.printStackTrace();
							} catch (InvocationTargetException e1) {
								e1.printStackTrace();
							}
						}
						
					});
					fields.add(checkBox);
					break;
				case NUMBER:
					final JTextField numberField = new JTextField(value);
					numberField.addKeyListener(new KeyAdapter() {
						
						@Override
						public void keyReleased(KeyEvent e) {
							int intVal;
							if (numberField.getText().length() > 0) {
								intVal = Integer.parseInt(numberField.getText());
							} else {
								intVal = 0;
							}
							try {
								BeanUtils.setProperty(plugin, property.getName(), intVal);
							} catch (IllegalAccessException e1) {
								e1.printStackTrace();
							} catch (InvocationTargetException e1) {
								e1.printStackTrace();
							}
						}
						
					});
					fields.add(numberField);
					break;
				case TEXT:
					// fall through to default
				default:
					final JTextField textField = new JTextField(value);
					textField.addKeyListener(new KeyAdapter() {
						@Override
						public void keyReleased(KeyEvent e) {
							try {
								BeanUtils.setProperty(plugin, property.getName(), textField.getText());
							} catch (IllegalAccessException e1) {
								e1.printStackTrace();
							} catch (InvocationTargetException e1) {
								e1.printStackTrace();
							}
						}
					});
					fields.add(textField);
					break;
				}
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			}
		}
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.add(configPanel, BorderLayout.NORTH);
		panel.add(new JPanel(), BorderLayout.CENTER);
		pluginPanes.add(plugin.getName(), panel);
	}

	private List<Field> getConfigurationProperties(Plugin plugin) {
		List<Field> configProperties = new ArrayList<Field>();
		for (Field field : plugin.getClass().getFields()) {
			if (field.isAnnotationPresent(ConfigurationProperty.class)) {
				configProperties.add(field);
			}
		}
		return configProperties;
	}

}