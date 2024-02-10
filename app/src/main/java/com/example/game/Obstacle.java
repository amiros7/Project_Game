package com.example.game;

import android.content.Context;
import android.widget.ImageView;

public class Obstacle extends GameObject {
    public Obstacle(Context context) {
        super(new ImageView(context));
        getView().setImageResource(R.drawable.obstacle);
    }
}
