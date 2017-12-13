package example;
//-*- mode:java; encoding:utf-8 -*-
// vim:set fileencoding=utf-8:
//@homepage@
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import javax.imageio.*;
import javax.imageio.stream.*;
import javax.swing.*;

public final class MainPanel extends JPanel {
    private MainPanel() {
        super(new BorderLayout());
        JLabel label = new JLabel();
        label.setIcon(new ImageIcon(getClass().getResource("duke.running.gif")));
        label.setBorder(BorderFactory.createTitledBorder("duke.running.gif"));

        Box p = Box.createHorizontalBox();
        p.setBorder(BorderFactory.createTitledBorder("Extract frames from Animated GIF"));

        // https://bugs.openjdk.java.net/browse/JDK-8080225
        // try (InputStream is = getClass().getResourceAsStream("duke.running.gif"); ImageInputStream iis = ImageIO.createImageInputStream(is)) {
        try (InputStream is = Files.newInputStream(Paths.get(getClass().getResource("duke.running.gif").toURI())); ImageInputStream iis = ImageIO.createImageInputStream(is)) {
            for (BufferedImage image: loadFromStream(iis)) {
                p.add(new JLabel(new ImageIcon(image)));
            }
        } catch (IOException | URISyntaxException ex) {
            ex.printStackTrace();
        }
        add(label, BorderLayout.WEST);
        add(new JScrollPane(p));
        setPreferredSize(new Dimension(320, 240));
    }
    // https://community.oracle.com/thread/1271862 Reading gif animation frame rates and such?
    private List<BufferedImage> loadFromStream(ImageInputStream imageStream) throws IOException {
        String format = "gif";
        String meta = "javax_imageio_gif_image_1.0";
        ImageReader reader = null;
        Iterator<ImageReader> readers = ImageIO.getImageReaders(imageStream);
        while (readers.hasNext()) {
            reader = readers.next();
            String metaFormat = reader.getOriginatingProvider().getNativeImageMetadataFormatName();
            if (format.equalsIgnoreCase(reader.getFormatName()) && meta.equals(metaFormat)) {
                break;
            }
        }
        ImageReader imageReader = Objects.requireNonNull(reader, "Can not read image format!");
        boolean isGif = format.equalsIgnoreCase(imageReader.getFormatName());
        imageReader.setInput(imageStream, false, !isGif);
        List<BufferedImage> list = new ArrayList<>();
        for (int i = 0; i < imageReader.getNumImages(true); i++) {
            IIOImage frame = imageReader.readAll(i, null);
            list.add((BufferedImage) frame.getRenderedImage());
        }
        imageReader.dispose();
        return list;
    }
    public static void main(String... args) {
        EventQueue.invokeLater(new Runnable() {
            @Override public void run() {
                createAndShowGUI();
            }
        });
    }
    public static void createAndShowGUI() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException
               | IllegalAccessException | UnsupportedLookAndFeelException ex) {
            ex.printStackTrace();
        }
        JFrame frame = new JFrame("@title@");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.getContentPane().add(new MainPanel());
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
