// -*- mode:java; encoding:utf-8 -*-
// vim:set fileencoding=utf-8:
// @homepage@

package example;

import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.swing.*;

public final class MainPanel extends JPanel {
  private MainPanel() {
    super(new BorderLayout(5, 5));
    ProgressMonitor monitor = new ProgressMonitor(this, "message", "note", 0, 100);

    JTextArea area = new JTextArea();
    area.setEditable(false);

    JButton runButton = new JButton("run");
    runButton.addActionListener(e -> {
      // System.out.println("actionPerformed() is EDT?: " + EventQueue.isDispatchThread());
      runButton.setEnabled(false);
      monitor.setProgress(0);
      SwingWorker<String, String> worker = new BackgroundTask() {
        @Override protected void process(List<String> chunks) {
          // System.out.println("process() is EDT?: " + EventQueue.isDispatchThread());
          // if (isCancelled()) {
          //   return;
          // }
          if (!isDisplayable()) {
            cancel(true);
            return;
          }
          for (String message: chunks) {
            monitor.setNote(message);
          }
        }

        @Override public void done() {
          // System.out.println("done() is EDT?: " + EventQueue.isDispatchThread());
          runButton.setEnabled(true);
          monitor.close();
          try {
            if (isCancelled()) {
              area.append("Cancelled\n");
            } else {
              area.append(get() + "\n");
            }
          } catch (InterruptedException ex) {
            area.append("Interrupted\n");
            Thread.currentThread().interrupt();
          } catch (ExecutionException ex) {
            area.append("ExecutionException\n");
          }
          area.setCaretPosition(area.getDocument().getLength());
        }
      };
      worker.addPropertyChangeListener(new ProgressListener(monitor));
      worker.execute();
    });

    Box box = Box.createHorizontalBox();
    box.add(Box.createHorizontalGlue());
    box.add(runButton);
    add(new JScrollPane(area));
    add(box, BorderLayout.NORTH);
    setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    setPreferredSize(new Dimension(320, 240));
  }

  public static void main(String[] args) {
    EventQueue.invokeLater(MainPanel::createAndShowGui);
  }

  private static void createAndShowGui() {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
      ex.printStackTrace();
      Toolkit.getDefaultToolkit().beep();
    }
    JFrame frame = new JFrame("@title@");
    // frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    frame.getContentPane().add(new MainPanel());
    frame.pack();
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }
}

class BackgroundTask extends SwingWorker<String, String> {
  @Override public String doInBackground() throws InterruptedException {
    // System.out.println("doInBackground() is EDT?: " + EventQueue.isDispatchThread());
    int current = 0;
    int lengthOfTask = 120; // list.size();
    while (current < lengthOfTask && !isCancelled()) {
      Thread.sleep(50);
      current++;
      setProgress(100 * current / lengthOfTask);
      publish(current + "/" + lengthOfTask);
    }
    return "Done";
  }
}

class ProgressListener implements PropertyChangeListener {
  private final ProgressMonitor monitor;

  protected ProgressListener(ProgressMonitor monitor) {
    this.monitor = monitor;
    this.monitor.setProgress(0);
  }

  @Override public void propertyChange(PropertyChangeEvent e) {
    String strPropertyName = e.getPropertyName();
    if ("progress".equals(strPropertyName)) {
      monitor.setProgress((Integer) e.getNewValue());
      Object o = e.getSource();
      if (o instanceof SwingWorker) {
        SwingWorker<?, ?> task = (SwingWorker<?, ?>) o;
        if (task.isDone() || monitor.isCanceled()) {
          task.cancel(true);
        }
      }
    }
  }
}
