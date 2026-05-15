package com.example.p2;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class AvatarHelper {
    private static final int TARGET_SIZE = 512;

    public static List<Bitmap> getAvatars(Context context) {
        List<Bitmap> avatars = new ArrayList<>();
        Bitmap source = BitmapFactory.decodeResource(context.getResources(), R.drawable.avatars);
        if (source == null) {
            Log.e("AvatarHelper", "Could not decode R.drawable.avatars");
            return avatars;
        }

        int width = source.getWidth();
        int height = source.getHeight();
        boolean[][] visited = new boolean[width][height];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (!visited[x][y] && Color.alpha(source.getPixel(x, y)) > 20) {
                    Rect bounds = findComponentBounds(source, x, y, visited);

                    if (bounds.width() > 30 && bounds.height() > 30) {
                        try {
                            Bitmap avatar = Bitmap.createBitmap(source, bounds.left, bounds.top, bounds.width(), bounds.height());

                            Bitmap scaled = Bitmap.createScaledBitmap(avatar, TARGET_SIZE, TARGET_SIZE, true);
                            avatars.add(scaled);
                        } catch (Exception e) {
                            Log.e("AvatarHelper", "Error processing avatar component", e);
                        }
                    }
                }
            }
        }

        if (avatars.isEmpty()) {
            int cols = 3;
            int rows = 2;
            int tileW = width / cols;
            int tileH = height / rows;
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    Bitmap tile = Bitmap.createBitmap(source, c * tileW, r * tileH, tileW, tileH);
                    avatars.add(Bitmap.createScaledBitmap(tile, TARGET_SIZE, TARGET_SIZE, true));
                }
            }
        }

        return avatars;
    }

    private static Rect findComponentBounds(Bitmap bitmap, int startX, int startY, boolean[][] visited) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        int minX = startX, maxX = startX, minY = startY, maxY = startY;

        Stack<int[]> stack = new Stack<>();
        stack.push(new int[]{startX, startY});
        visited[startX][startY] = true;

        while (!stack.isEmpty()) {
            int[] curr = stack.pop();
            int cx = curr[0];
            int cy = curr[1];

            if (cx < minX) minX = cx;
            if (cx > maxX) maxX = cx;
            if (cy < minY) minY = cy;
            if (cy > maxY) maxY = cy;

            // Check 4-connected neighbors
            int[] dx = {0, 0, 1, -1};
            int[] dy = {1, -1, 0, 0};

            for (int i = 0; i < 4; i++) {
                int nx = cx + dx[i];
                int ny = cy + dy[i];

                if (nx >= 0 && nx < w && ny >= 0 && ny < h && !visited[nx][ny]) {
                    if (Color.alpha(bitmap.getPixel(nx, ny)) > 20) {
                        visited[nx][ny] = true;
                        stack.push(new int[]{nx, ny});
                    }
                }
            }

            if (stack.size() > 100000) break;
        }

        return new Rect(minX, minY, maxX + 1, maxY + 1);
    }
}
