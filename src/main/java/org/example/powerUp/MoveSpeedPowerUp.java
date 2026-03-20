package org.example.powerUp;

import org.example.Triple;

public class MoveSpeedPowerUp extends PowerUp{
    public MoveSpeedPowerUp(Triple position) {
        super(position);
        effectiveness = 3f;
        duration = 5;
        type = 1;
    }
}