package de.gaalop.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.StringWriter;
import java.io.PrintWriter;

public class ErrorDialog extends JDialog {
    /**
	 * 
	 */
	private static final long serialVersionUID = 2734636520548199800L;
	private JPanel contentPane;
    private JButton buttonOk;
    private JEditorPane editorPane1;
    private JScrollPane scrollPane;
    private JTextPane textPanel;

    public ErrorDialog() {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOk);

        buttonOk.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });
        
        this.addWindowListener(new WindowAdapter() {
          @Override
          public void windowActivated(WindowEvent e) {
            buttonOk.requestFocus();
          }
        });
    }

    private void onOK() {
// add your code here
        dispose();
    }

    public static void show(Throwable e) {
        ErrorDialog dialog = new ErrorDialog();
        dialog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        dialog.textPanel.setText(e.getMessage());
        dialog.editorPane1.setText(getStackTrace(e));
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }

    private static String getStackTrace(Throwable e) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        e.printStackTrace(printWriter);
        printWriter.close();
        return stringWriter.toString();
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        contentPane = new JPanel();
        contentPane.setLayout(new BorderLayout(0, 0));
        contentPane.setPreferredSize(new Dimension(480, 300));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 10));
        contentPane.add(panel1, BorderLayout.SOUTH);
        buttonOk = new JButton();
        buttonOk.setText("Ok");
        panel1.add(buttonOk);
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new FlowLayout(FlowLayout.LEFT, 15, 5));
        contentPane.add(panel2, BorderLayout.NORTH);
        final JLabel label1 = new JLabel();
        label1.setFont(new Font(label1.getFont().getName(), Font.BOLD, 28));
        label1.setIcon(new ImageIcon(getClass().getResource("/de/gaalop/gui/dialog-error.png")));
        label1.setText("Error");
        panel2.add(label1);
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new BorderLayout(0, 0));
        contentPane.add(panel3, BorderLayout.CENTER);
        panel3.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5), null));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new BorderLayout(0, 0));
        panel3.add(panel4, BorderLayout.NORTH);
        panel4.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0), null));
        textPanel = new JTextPane();
        textPanel.setBackground(UIManager.getColor("Panel.background"));
        textPanel.setEditable(false);
        panel4.add(textPanel, BorderLayout.CENTER);
        scrollPane = new JScrollPane();
        panel3.add(scrollPane, BorderLayout.CENTER);
        scrollPane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLoweredBevelBorder(), null));
        editorPane1 = new JEditorPane();
        editorPane1.setBackground(UIManager.getColor("Panel.background"));
        editorPane1.setEditable(false);
        scrollPane.setViewportView(editorPane1);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return contentPane;
    }
}
