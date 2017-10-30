package board;

import java.awt.AWTException;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

public class Screen {
    public static void main(String[] args) throws InterruptedException {
        try {
            for (int i = 0; i < 21; i++) {
                Thread.sleep(4000);
                Robot robot = new Robot();
                String format = "jpg";
                String fileName = "FullScreenshot" + i + "." + format;
                
                Rectangle screenRect = new Rectangle(690, 45, 540, 450);
                BufferedImage screenFullImage =
                        robot.createScreenCapture(screenRect);
                ImageIO.write(screenFullImage, format, new File(fileName));
                
                System.out.println("A full screenshot " + i + " saved!");
            }
            
        }
        catch (AWTException | IOException ex) {
            System.err.println(ex);
        }
    }
}
