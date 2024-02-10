package com.example.game;

import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.util.Objects;
import java.util.Random;
import java.util.TimerTask;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Game {

    public interface OnMain {
        void runOnMainThread(Runnable r);
    }


    private final Object lock = new Object();
    public static final int LANES = 3;
    public static final int ROWS = 12;
    public static final int MAX_LIVES = 3;


    private GameObject[][] gameObjects;

    private GridLayout gameLayout;
    private LinearLayout livesLayout;
    private int[] obstacle_lanes;

    private boolean[] active_obstacle_lanes;
    private int player_lane;

    private OnMain onMain;

    private int lives;


    private ScheduledThreadPoolExecutor obstacleExecutor = new ScheduledThreadPoolExecutor(3);

    public Game(MainActivity context) {
        player_lane = LANES / 2;
        lives = 3;
        obstacle_lanes = new int[LANES];
        active_obstacle_lanes = new boolean[LANES];
        livesLayout = context.findViewById(R.id.livesLayout);
        gameLayout = context.findViewById(R.id.gameLayout);

        gameLayout.setRowCount(ROWS);
        gameLayout.setColumnCount(LANES);


        gameObjects = new GameObject[ROWS][LANES];

        gameObjects[ROWS - 1][player_lane] = new Player(context);
        gameObjects[ROWS - 1][player_lane].setPosition(ROWS - 1, LANES / 2);
        for (int i = 0; i < LANES; i++) {
            gameObjects[0][i] = new Obstacle(context);
            gameObjects[0][i].setPosition(0, i);
        }

        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < LANES; j++) {
                if (gameObjects[i][j] == null) {
                    gameObjects[i][j] = new GameObject(new ImageView(context));
                    gameObjects[i][j].setPosition(i, j);
                }
                gameLayout.addView(gameObjects[i][j].getView());
            }
        }
        onMain = context;

        Random rand = new Random();
        ScheduledFuture<?> obstaclesTask = obstacleExecutor.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (activeLanes() < LANES - 1) {
                    int randomLane = Math.min(LANES - 1, rand.nextInt(LANES));
                    if (active_obstacle_lanes[randomLane]) {
                        return;
                    }
                    active_obstacle_lanes[randomLane] = true;
                    final int movementLane = randomLane;

                    int randomMilisec = Math.max(1000, rand.nextInt(3200));
                    obstacleExecutor.scheduleAtFixedRate(new TimerTask() {
                        @Override
                        public void run() {
                            onMain.runOnMainThread(() -> {
                                if (!moveObstacle(movementLane)) {
                                    cancel();
                                    active_obstacle_lanes[movementLane] = false;
                                }
                            });
                        }
                    }, 1000, randomMilisec, TimeUnit.MILLISECONDS);
                }
            }
        }, 2, 5, TimeUnit.SECONDS);

    }

    private int activeLanes() {
        int active = 0;
        for (boolean activeLane : active_obstacle_lanes)
            if (activeLane) active++;
        return active;
    }


    public void movePlayerLeft() {
        synchronized (lock) {
            if (player_lane == 0) return;
            int row = ROWS - 1;
            GameObject o = gameObjects[row][player_lane - 1];
            if (o instanceof Obstacle) {
                resetObstacle(player_lane - 1);
                playerHit();
            }
            swap(row, player_lane, row, player_lane - 1);
            player_lane--;
        }

    }

    private void playerHit() {
        lives--;
        if(lives == 0)
            lives = MAX_LIVES;
        renderLives();
    }

    public void movePlayerRight() {
        synchronized (lock) {
            if (player_lane == LANES - 1) return;
            int row = ROWS - 1;
            GameObject o = gameObjects[row][player_lane + 1];
            if (o instanceof Obstacle) {
                resetObstacle(player_lane + 1);
                playerHit();
            }
            swap(row, player_lane, row, player_lane + 1);
            player_lane++;
        }
    }

    private void resetObstacle(int lane) {
        int row = obstacle_lanes[lane];
        obstacle_lanes[lane] = 0;
        swap(row, lane, 0, lane);
    }

    private boolean moveObstacle(int lane) { // returns true if moved down
        synchronized (lock) {

            int row = obstacle_lanes[lane];

            if (row + 1 >= ROWS) {
                resetObstacle(lane);
                return false; // moved up (reset)
            }

            // check collision
            GameObject o = gameObjects[row + 1][lane];
            if (o instanceof Player) {
                resetObstacle(lane);
                playerHit();
                renderLives();
                return false;
            }

            obstacle_lanes[lane]++;
            swap(row, lane, row + 1, lane);
            return true;
        }
    }

    private void renderLives() {
        for (int i = 0; i < MAX_LIVES; i++) {
            livesLayout.getChildAt(i).setVisibility(View.INVISIBLE);
        }
        for (int i = lives-1; i >= 0; i--) {
            livesLayout.getChildAt(i).setVisibility(View.VISIBLE);
        }
    }


    private void swap(int r1, int c1, int r2, int c2) {
        GameObject temp = gameObjects[r1][c1];
        gameObjects[r1][c1] = gameObjects[r2][c2];
        gameObjects[r2][c2] = temp;
        gameObjects[r1][c1].setPosition(r1, c1);
        gameObjects[r2][c2].setPosition(r2, c2);
    }


}
