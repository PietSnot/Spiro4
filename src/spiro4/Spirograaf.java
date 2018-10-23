/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package spiro4;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Random;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 *
 * @author Piet
 */
public class Spirograaf {
    
    public static void main(String... args) {
        int sizeOfPanel = args.length > 0 ? Integer.parseInt(args[0]) : 600;
        SwingUtilities.invokeLater(() -> new Spirograaf(sizeOfPanel));
    }
    
    //***********************************************************
    // member variables
    //***********************************************************
    final BufferedImage buf_spiro;      // where the sprites are drawn
    BufferedImage buf_paintComponent;   // is painted in the panel
    BufferedImage buf_save;             // for saving ourposes
    Timer timer;                        // used to initiale a repaint
    JPanel spiroPanel;                  // the panel in which the drawing takes place
    JLabel label_spf;                   // gives the sprites per frame
    volatile int spritesPerFrame;       // determines the speed of drawing
    Thread thread;                      // is the thread that makes the spiro
    ImageIcon sprite;                   // the sprite, loaded from the file to be used
    ImageIcon createdIcon, loadedIcon;  // the sprites to be used
    JFileChooser jfs;                   // filechooser for saving images
    Color transparant;                  // used to clear the buffered images
    final int MAX_SPEED = 500;          // maximum srpites per seond?
    final int START_SPEED = 150;        // initial speed
    Random random = new Random();
    
    //***********************************************************
    // constructor
    //***********************************************************
    public Spirograaf(int panelSize) {
        
        //-------------------------------------------------------
        // spirograafpanel
        //-------------------------------------------------------
        spiroPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.drawImage(buf_paintComponent, 0, 0, this.getWidth(), this.getHeight(), null);
            }
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(panelSize, panelSize);
            }
        };
        spiroPanel.setBackground(Color.BLACK);
        
        //-------------------------------------------------------
        // buffered images
        //-------------------------------------------------------
        buf_spiro = new BufferedImage(panelSize, panelSize, BufferedImage.TYPE_INT_ARGB);
        buf_paintComponent = new BufferedImage(panelSize, panelSize, BufferedImage.TYPE_INT_ARGB);
        buf_save = new BufferedImage(panelSize, panelSize, BufferedImage.TYPE_INT_ARGB);
        
        //-------------------------------------------------------
        // buttonpanel
        //-------------------------------------------------------
        JPanel buttonPanel = new JPanel();
        JButton again = new JButton("Again");
        JButton snapshot = new JButton("Snapshot");
        JButton end = new JButton("End");
        JSlider slider = new JSlider(JSlider.HORIZONTAL, 0, MAX_SPEED, Math.min(MAX_SPEED, START_SPEED));
        label_spf = new JLabel("" + slider.getValue());
        label_spf.setOpaque(true);
        label_spf.setBorder(BorderFactory.createLoweredSoftBevelBorder());
        label_spf.setBackground(Color.ORANGE);
        
        buttonPanel.add(again);
        buttonPanel.add(snapshot);
        buttonPanel.add(end);
        buttonPanel.add(new JLabel("Speed min "));
        buttonPanel.add(slider);
        buttonPanel.add(new JLabel(" max"));
        buttonPanel.add(label_spf);
        
        //-------------------------------------------------------
        // other members
        //-------------------------------------------------------
        transparant = new Color(0,0,0,0);
        spritesPerFrame = slider.getValue();
        
        //-------------------------------------------------------
        // the filechooser
        //-------------------------------------------------------
        jfs = new JFileChooser();
        FileNameExtensionFilter filefilter = 
           new FileNameExtensionFilter("Png, png", "png");
        jfs.setFileFilter(filefilter);
        
        //-------------------------------------------------------
        // adding listeners to the buttons
        //-------------------------------------------------------
        again.addActionListener( e -> {
            if (timer.isRunning()) timer.stop();
            if (thread != null) {
                thread.interrupt();
                try {
                    thread.join();
                }
                catch (Exception exc) {
                    System.out.println("raar, maar een fout in de interrupt van thread!?!?!?");
                }
            }
            // we gaan buf_spiro legen
            emptyBuf(buf_spiro, transparant);
            emptyBuf(buf_paintComponent, transparant);
            thread = new Thread(new Piet());
            sprite = (loadedIcon == null || random.nextBoolean()) ? createdIcon : loadedIcon;
            thread.start();
            timer.start();
        });
        
        //-------------------------------------------------
        snapshot.addActionListener(e -> saveImage());
        
        //-------------------------------------------------
        end.addActionListener( e -> {
            if (thread != null) thread.interrupt();
            System.exit(0);
        });
        
        //-------------------------------------------------
        slider.addChangeListener( e -> {
            spritesPerFrame = slider.getValue();
            label_spf.setText("" + spritesPerFrame);
        });
        
        // laden en creëren van de sprites
        createImageIcon(16, 16);
        loadSprite();
        
        //-------------------------------------------------------
        // defining the timer
        //-------------------------------------------------------
        timer = new Timer(16, e -> {
            synchronized(buf_spiro) {
                Graphics graph = buf_paintComponent.getGraphics();
                int w = buf_paintComponent.getWidth();
                int h = buf_paintComponent.getHeight();
                graph.drawImage(buf_spiro, 0, 0, w, h, null);
                spiroPanel.repaint();
                buf_spiro.notifyAll();
            }
        });
        
        //-------------------------------------------------------
        // creating frame
        //-------------------------------------------------------
        JFrame frame = new JFrame("press 'again' to start afresh");
        Container c = frame.getContentPane();
        c.add(spiroPanel, BorderLayout.CENTER);
        c.add(buttonPanel, BorderLayout.PAGE_START);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        //----------------------------------------------------------
    
    }   // end of constructor

    //--------------------------------------------------------------
    //**************************************************************
    //--------------------------------------------------------------
    private void emptyBuf(BufferedImage buf, Color bgc) {
        Graphics2D g2d = buf.createGraphics();
        g2d.setBackground(bgc);
        g2d.clearRect(0, 0, buf.getWidth(), buf.getHeight());
        g2d.dispose();
    }
    
    //--------------------------------------------------------------
    private void createImageIcon(int width, int height) {
        BufferedImage buf = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        int[] c0 = {255, 255, 255};
        int[] c1 = {255, 100, 0};
        int[] c2 = {0, 255, 100};
        int[] c3 = {100, 0, 255};

        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                double v = 1.0 * row / height;
                double h = 1.0 * col / width;
                int r = (int) ((1 - v) * (1 - h) * c0[0]
                        + v * (1 - h) * c1[0]
                        + (1 - v) * h * c2[0]
                        + v * h * c3[0] + .5);
                int g = (int) ((1 - v) * (1 - h) * c0[1]
                        + (v * (1 - h)) * c1[1]
                        + (1 - v) * h * c2[1]
                        + v * h * c3[1] + .5);
                int b = (int) ((1 - v) * (1 - h) * c0[2]
                        + v * (1 - h) * c1[2]
                        + (1 - v) * h * c2[2]
                        + v * h * c3[2] + .5);
                buf.setRGB(col, row, 128 << 24 | (r << 16) | (g << 8) | b);
            }
        }
        createdIcon = new ImageIcon(buf);
    }
    
    //----------------------------------------------------------------
    private void saveImage() {
        Graphics g = buf_save.getGraphics();
        g.drawImage(buf_paintComponent, 0, 0, null);
        g.dispose();
        int x = jfs.showSaveDialog(spiroPanel);
        if (x != JFileChooser.APPROVE_OPTION) return;
        File f = jfs.getSelectedFile();
        String s = determineSaveName(f);
        try {
            ImageIO.write(buf_save, "png", new File(s));
            JOptionPane.showMessageDialog(spiroPanel, "Saved as: \n" + s,
                                         "Saved!", JOptionPane.OK_OPTION
            );
        }
        catch (IOException e) {
            JOptionPane.showMessageDialog(spiroPanel, "Saving has failed for some reason!");
        }
    }
    
    //----------------------------------------------------------------
    private String determineSaveName(File f) {
        String s = f.getAbsolutePath().toLowerCase();
        if (!s.toLowerCase().endsWith(".png")) s += ".png";
        while (!s.endsWith(".png")) {
            s += ".png";
            if (!(new File(s)).exists()) break;
        }
        return s;
    }
        
    
    //----------------------------------------------------------------
    private void loadSprite() {
        loadedIcon = null;
        try {
            URL imageURL = getClass().getResource("resources/BLOB.PNG");
            BufferedImage temp = ImageIO.read(imageURL);
            loadedIcon = new ImageIcon(temp);
        }
        catch (IOException e) {
            System.out.println("IO Exception helaas! " + e.toString());
        }
        catch (IllegalArgumentException e) {
            System.out.println("Ill. Arg. expression verdemme! " + e.toString());
        }
    }
    
    //******************************************************************
    // the Runnable class, that does all the nice drawings of a sprite
    //******************************************************************
    private class Piet implements Runnable {
       
        @Override
        public void run() {
            double A, B, C, D, x, y;
            int S, hulpbreedte, hulphoogte;
            Random random = new Random();
            hulpbreedte = (buf_spiro.getWidth() - sprite.getIconWidth()) / 4;
            hulphoogte = (buf_spiro.getHeight() - sprite.getIconHeight()) / 4;
            S = random.nextInt(Math.min(hulpbreedte, hulphoogte));
            A = 0.; 
            B = Math.PI * random.nextDouble();
            C = 0.; 
            D = Math.PI * random.nextDouble();
            Graphics2D g2d = buf_spiro.createGraphics();
            g2d.translate(buf_spiro.getWidth() / 2, buf_spiro.getHeight() / 2);
    
            while (!Thread.interrupted()) {
                synchronized(buf_spiro) {
                    for (int teller = spritesPerFrame; teller > 0; teller--) {
                        x = hulpbreedte * Math.sin(A);
                        y = hulphoogte * Math.cos(A);
                        sprite.paintIcon(
                                null, g2d, (int)(x + S * Math.cos(C)), 
                                (int)(y + S * Math.sin(C))
                        );
                        A += B; C += D;     
                    }
                    try {
                        buf_spiro.wait();
                    }
                    catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }   // end of run
    }   // end of class Piet
}   // end of class Spirograaf
