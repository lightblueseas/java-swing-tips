// -*- mode:java; encoding:utf-8 -*-
// vim:set fileencoding=utf-8:
// @homepage@

package example;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetListener;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

public final class MainPanel extends JPanel {
  public final AtomicInteger imageId = new AtomicInteger(0);
  public final FileModel model = new FileModel();
  public transient MediaTracker tracker;

  private class ImageDropTargetListener extends DropTargetAdapter {
    @Override public void dragOver(DropTargetDragEvent e) {
      if (e.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
        e.acceptDrag(DnDConstants.ACTION_COPY);
        return;
      }
      e.rejectDrag();
    }

    @Override public void drop(DropTargetDropEvent e) {
      try {
        if (e.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
          e.acceptDrop(DnDConstants.ACTION_COPY);
          Transferable transferable = e.getTransferable();
          ((List<?>) transferable.getTransferData(DataFlavor.javaFileListFlavor))
              .stream().filter(File.class::isInstance).map(File.class::cast)
              .map(File::toPath)
              .forEach(MainPanel.this::addImage);
          e.dropComplete(true);
        } else {
          e.rejectDrop();
        }
      } catch (UnsupportedFlavorException | IOException ex) {
        e.rejectDrop();
      }
    }
  }

  private MainPanel() {
    super(new BorderLayout());
    JTable table = new JTable(model);
    table.setAutoCreateRowSorter(true);

    JScrollPane scroll = new JScrollPane(table);
    scroll.setComponentPopupMenu(new TablePopupMenu());
    table.setInheritsPopupMenu(true);

    DropTargetListener dtl = new ImageDropTargetListener();
    new DropTarget(table, DnDConstants.ACTION_COPY, dtl, true);
    new DropTarget(scroll.getViewport(), DnDConstants.ACTION_COPY, dtl, true);

    TableColumn col = table.getColumnModel().getColumn(0);
    col.setMinWidth(60);
    col.setMaxWidth(60);
    col.setResizable(false);

    try {
      addImage(Paths.get(getClass().getResource("test.png").toURI()));
    } catch (URISyntaxException ex) {
      ex.printStackTrace();
      UIManager.getLookAndFeel().provideErrorFeedback(this);
    }

    add(scroll);
    setPreferredSize(new Dimension(320, 240));
  }

  protected void addImage(Path path) {
    int id = imageId.getAndIncrement();
    Image img = Toolkit.getDefaultToolkit().createImage(path.toAbsolutePath().toString());
    tracker = Optional.ofNullable(tracker).orElseGet(() -> new MediaTracker(this));
    tracker.addImage(img, id);
    new SwingWorker<Void, Void>() {
      @Override protected Void doInBackground() throws InterruptedException {
        tracker.waitForID(id);
        return null;
      }

      @Override public void done() {
        if (!isDisplayable()) {
          cancel(true);
          return;
        }
        if (!tracker.isErrorID(id)) {
          Container c = MainPanel.this;
          model.addRowData(new RowData(id, path, img.getWidth(c), img.getHeight(c)));
        }
        tracker.removeImage(img);
      }
    }.execute();
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
    frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    frame.getContentPane().add(new MainPanel());
    frame.pack();
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }
}

class FileModel extends DefaultTableModel {
  private static final List<ColumnContext> COLUMN_LIST = Arrays.asList(
      new ColumnContext("No.", Integer.class, false),
      new ColumnContext("Name", String.class, false),
      new ColumnContext("Full Path", String.class, false),
      new ColumnContext("Width", Integer.class, false),
      new ColumnContext("Height", Integer.class, false)
  );

  public void addRowData(RowData t) {
    Object[] obj = {
        t.getId(), t.getName(), t.getAbsolutePath(), t.getWidth(), t.getHeight()
    };
    super.addRow(obj);
  }

  @Override public boolean isCellEditable(int row, int col) {
    return COLUMN_LIST.get(col).isEditable;
  }

  @Override public Class<?> getColumnClass(int column) {
    return COLUMN_LIST.get(column).columnClass;
  }

  @Override public int getColumnCount() {
    return COLUMN_LIST.size();
  }

  @Override public String getColumnName(int column) {
    return COLUMN_LIST.get(column).columnName;
  }

  private static class ColumnContext {
    public final String columnName;
    public final Class<?> columnClass;
    public final boolean isEditable;

    protected ColumnContext(String columnName, Class<?> columnClass, boolean isEditable) {
      this.columnName = columnName;
      this.columnClass = columnClass;
      this.isEditable = isEditable;
    }
  }
}

class RowData {
  private final int id;
  private final String name;
  private final String absolutePath;
  private final int width;
  private final int height;

  protected RowData(int id, Path path, int width, int height) {
    this.id = id;
    this.name = Objects.toString(path.getFileName());
    this.absolutePath = Objects.toString(path.toAbsolutePath());
    this.width = width;
    this.height = height;
  }

  // public void setName(String str) {
  //   name = str;
  // }

  // public void setAbsolutePath(String str) {
  //   absolutePath = str;
  // }

  // public void setWidth(int width) {
  //   this.width = width;
  // }

  // public void setHeight(int height) {
  //   this.height = height;
  // }

  public int getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getAbsolutePath() {
    return absolutePath;
  }

  public int getWidth() {
    return this.width;
  }

  public int getHeight() {
    return this.height;
  }
}

class TablePopupMenu extends JPopupMenu {
  private final JMenuItem delete;

  protected TablePopupMenu() {
    super();
    delete = add("Remove from list");
    delete.addActionListener(e -> {
      JTable table = (JTable) getInvoker();
      DefaultTableModel model = (DefaultTableModel) table.getModel();
      int[] selection = table.getSelectedRows();
      for (int i = selection.length - 1; i >= 0; i--) {
        model.removeRow(table.convertRowIndexToModel(selection[i]));
      }
    });
  }

  @Override public void show(Component c, int x, int y) {
    if (c instanceof JTable) {
      delete.setEnabled(((JTable) c).getSelectedRowCount() > 0);
      super.show(c, x, y);
    }
  }
}
