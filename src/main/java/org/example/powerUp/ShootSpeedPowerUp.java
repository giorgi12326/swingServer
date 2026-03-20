package org.example.powerUp;

import org.example.Triple;

public class ShootSpeedPowerUp extends PowerUp{
    public ShootSpeedPowerUp(Triple position) {
        super(position);
        effectiveness = 2;
        duration = 5;
        type = 2;
    }
}