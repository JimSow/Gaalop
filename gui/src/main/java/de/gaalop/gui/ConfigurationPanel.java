package de.gaalop.gui;

import java.awt.BorderLayout;
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
		List<CodeGeneratorPlugin> sortedPlugins = new ArrayList<CodeGeneratorPlugin>(Plugins.getCodeGeneratorPlugins());
		Collections.sort(sortedPlugins, new PluginSorter());
		for (final CodeGeneratorPlugin generator : sortedPlugins) {
			JPanel panel = new JPanel();
			List<Field> configProperties = new ArrayList<Field>();
			for (Field field : generator.getClass().getFields()) {
				if (field.isAnnotationPresent(ConfigurationProperty.class)) {
					configProperties.add(field);
				}
			}
			for (final Field property : configProperties) {
				panel.add(new JLabel(property.getName()));
				try {
					String value = BeanUtils.getProperty(generator, property.getName());
					ConfigurationProperty clazz = property.getAnnotation(ConfigurationProperty.class);
					switch (clazz.type()) {
					case BOOLEAN:
						final JCheckBox checkBox = new JCheckBox();
						checkBox.setSelected(Boolean.parseBoolean(value));
						checkBox.addActionListener(new ActionListener() {
							
							@Override
							public void actionPerformed(ActionEvent e) {
								try {
									BeanUtils.setProperty(generator, property.getName(), checkBox.isSelected());
								} catch (IllegalAccessException e1) {
									e1.printStackTrace();
								} catch (InvocationTargetException e1) {
									e1.printStackTrace();
								}
							}
							
						});
						panel.add(checkBox);
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
									BeanUtils.setProperty(generator, property.getName(), intVal);
								} catch (IllegalAccessException e1) {
									e1.printStackTrace();
								} catch (InvocationTargetException e1) {
									e1.printStackTrace();
								}
							}
							
						});
						break;
					case TEXT:
						// fall through to default
					default:
						final JTextField textField = new JTextField(value);
						textField.setColumns(10);
						textField.addKeyListener(new KeyAdapter() {
							@Override
							public void keyReleased(KeyEvent e) {
								try {
									BeanUtils.setProperty(generator, property.getName(), textField.getText());
								} catch (IllegalAccessException e1) {
									e1.printStackTrace();
								} catch (InvocationTargetException e1) {
									e1.printStackTrace();
								}
							}
						});
						panel.add(textField);
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
			pluginPanes.add(generator.getName(), panel);
		}
		pluginPanes.setSelectedIndex(0);
	}

}
