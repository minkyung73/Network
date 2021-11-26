package omokTest;

import javax.swing.*;
import java.awt.*;
import java.util.Timer;
import java.util.TimerTask;


public class Countdown {
    public Countdown(Panel timePanel) {

//    	Panel jframe = new Panel();
        Label jLabel = new Label();
//        timePanel.setLayout(new FlowLayout());
//        timePanel.setBounds(500, 300, 400, 100);

        timePanel.add(jLabel);
        timePanel.setVisible(true);

        Timer timer = new Timer();

        timer.scheduleAtFixedRate(new TimerTask() {
            int i = 20;

            public void run() {

                jLabel.setText("Time left: " + i);
                i--;

                if (i < 0) {
                    timer.cancel();
                    jLabel.setText("Time Over");
                }
            }
        }, 0, 1000);
    }
}
