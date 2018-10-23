/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package spiro4;

import javax.swing.SwingUtilities;

/**
 *
 * @author Piet
 */
public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        int a;
        if (args.length > 0) a = Integer.parseInt(args[0]);
        else a = 600;
        SwingUtilities.invokeLater( () -> new Spirograaf(a));
    }
}
