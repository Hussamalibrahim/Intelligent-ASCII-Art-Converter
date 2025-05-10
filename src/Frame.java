import javax.swing.*;
class Frame extends JFrame {
    Frame() {
        add(new Panel());
        pack();
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setResizable(false);
        setVisible(true);
    }
}
