import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Panel extends JPanel {
    private int autoCharWidth = 8;
    private int autoCharHeight = 12;
    private int manualCharWidth = 0;
    private int manualCharHeight = 0;
    private final String h = "Ø@#W$987654321!abc;:+=-,._         ";
    private final char[] ascii = h.toCharArray();
    private BufferedImage asciiImage;
    private String asciiArt;
    private BufferedImage originalImage;

    Panel() {
        setBackground(Color.BLACK);

        if (!selectImageFile()) {
            System.exit(0);
        }
        calculateAutoCharSize();

        if (!getUserPreferences()) {
            System.exit(0);
        }

        processImage();
    }

    private boolean selectImageFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select an image file");
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                originalImage = ImageIO.read(fileChooser.getSelectedFile());
                return true;
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error loading image: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
        return false;
    }

    private void calculateAutoCharSize() {
        double ratio = (double)originalImage.getWidth() / originalImage.getHeight();
        autoCharWidth = Math.max(4, (int)(8 * ratio));
        autoCharHeight = Math.max(6, (int)(12 / ratio));

        int outputWidth = originalImage.getWidth() / (autoCharWidth / 2);
        int outputHeight = originalImage.getHeight() / (autoCharHeight / 2);
    }

    private boolean getUserPreferences() {
        JPanel resolutionPanel = new JPanel(new GridLayout(0, 2, 5, 5));

        JLabel autoSizeLabel = new JLabel("Auto-calculated: " + autoCharWidth + "×" + autoCharHeight);
        resolutionPanel.add(new JLabel("Recommended char size:"));
        resolutionPanel.add(autoSizeLabel);

        JTextField charWField = new JTextField(String.valueOf(manualCharWidth > 0 ? manualCharWidth : ""));
        JTextField charHField = new JTextField(String.valueOf(manualCharHeight > 0 ? manualCharHeight : ""));

        resolutionPanel.add(new JLabel("Custom Width (px):"));
        resolutionPanel.add(charWField);
        resolutionPanel.add(new JLabel("Custom Height (px):"));
        resolutionPanel.add(charHField);

        int result = JOptionPane.showConfirmDialog(null, resolutionPanel,
                "Set Character Dimensions", JOptionPane.OK_CANCEL_OPTION);

        if (result == JOptionPane.OK_OPTION) {
            try {
                if (!charWField.getText().isEmpty()) {
                    manualCharWidth = Integer.parseInt(charWField.getText());
                }
                if (!charHField.getText().isEmpty()) {
                    manualCharHeight = Integer.parseInt(charHField.getText());
                }
                return true;
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Please enter valid numbers", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
        return false;
    }

    private void processImage() {
        File outputDir = new File("ascii_output");
        if (!outputDir.exists()) outputDir.mkdir();

        try {
            int charWidth = manualCharWidth > 0 ? manualCharWidth : autoCharWidth;
            int charHeight = manualCharHeight > 0 ? manualCharHeight : autoCharHeight;

            asciiArt = convertToAscii(originalImage, charWidth, charHeight);

            String baseName = new File(outputDir,
                    originalImage.getWidth() + "x" + originalImage.getHeight() +
                            "_" + charWidth + "x" + charHeight + "_ascii").toString();
            saveAsTextFile(baseName);

            asciiImage = createAsciiImage(asciiArt, charWidth, charHeight);
            saveAsImageFile(baseName);

            setPreferredSize(new Dimension(asciiImage.getWidth(), asciiImage.getHeight()));
            revalidate();

            JOptionPane.showMessageDialog(this,
                    "ASCII art saved to:\n" + baseName + ".txt\n" +
                            baseName + ".png\n\n" +
                            "Original: " + originalImage.getWidth() + "×" + originalImage.getHeight() + "\n" +
                            "Output: " + asciiImage.getWidth() + "×" + asciiImage.getHeight() + "\n" +
                            "Char size: " + charWidth + "×" + charHeight + "px",
                    "Conversion Complete",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    private String convertToAscii(BufferedImage image, int charWidth, int charHeight) {
        StringBuilder sb = new StringBuilder();
        BufferedImage scaled = new BufferedImage(
                image.getWidth() / charWidth * charWidth,
                image.getHeight() / charHeight * charHeight,
                BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2d = scaled.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(image, 0, 0, scaled.getWidth(), scaled.getHeight(), null);
        g2d.dispose();

        for (int y = 0; y < scaled.getHeight(); y += charHeight) {
            for (int x = 0; x < scaled.getWidth(); x += charWidth) {
                int r = 0, g = 0, b = 0;
                int count = 0;

                for (int dy = 0; dy < charHeight && y + dy < scaled.getHeight(); dy++) {
                    for (int dx = 0; dx < charWidth && x + dx < scaled.getWidth(); dx++) {
                        Color color = new Color(scaled.getRGB(x + dx, y + dy));
                        r += color.getRed();
                        g += color.getGreen();
                        b += color.getBlue();
                        count++;
                    }
                }

                int brightness = (r + g + b) / (3 * count);
                char c = ascii[(ascii.length - 1) - brightness * (ascii.length - 1) / 255];
                sb.append(c);
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private BufferedImage createAsciiImage(String asciiArt, int charWidth, int charHeight) {
        String[] lines = asciiArt.split("\n");
        int imageWidth = lines[0].length() * charWidth;
        int imageHeight = lines.length * charHeight;

        BufferedImage image = new BufferedImage(
                imageWidth,
                imageHeight,
                BufferedImage.TYPE_INT_RGB);

        Graphics2D g = image.createGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, imageWidth, imageHeight);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Monospaced", Font.PLAIN, charWidth));

        FontMetrics fm = g.getFontMetrics();
        int y = fm.getAscent();

        for (String line : lines) {
            g.drawString(line, 0, y);
            y += fm.getHeight();
        }
        g.dispose();
        return image;
    }

    private void saveAsTextFile(String baseName) throws IOException {
        Path txtPath = Paths.get(baseName + ".txt");
        Files.write(txtPath, asciiArt.getBytes());
    }

    private void saveAsImageFile(String baseName) throws IOException {
        Path imgPath = Paths.get(baseName + ".png");
        ImageIO.write(asciiImage, "png", imgPath.toFile());
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (asciiImage != null) {
            int x = (getWidth() - asciiImage.getWidth()) / 2;
            int y = (getHeight() - asciiImage.getHeight()) / 2;
            g.drawImage(asciiImage, x, y, null);
        }
    }
}
