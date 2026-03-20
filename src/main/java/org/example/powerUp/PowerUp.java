package org.example.powerUp;

import org.example.Triple;

public class PowerUp {
    public Triple position;
    public long activationTime;
    public int duration = 1;
    public float effectiveness = 1;
    public int type = 10;

    boolean flip;
    float hoverTempHeight;

    public PowerUp(Triple position) {
        float v = (float) (Math.random() / 2f);
        this.position = position;
        position.y += v;
    }

    public boolean isExpired(){
        return System.currentTimeMillis() - activationTime > duration* 1000L;
    }

    public void makeHover() {
        if(hoverTempHeight >= 0.5f || hoverTempHeight <= 0)
            flip = !flip;
        if(flip) {
            position.y += 0.005f;
            hoverTempHeight += 0.005f;
        }
        else {
            position.y -= 0.005f;
            hoverTempHeight -= 0.005f;
        }
    }
}