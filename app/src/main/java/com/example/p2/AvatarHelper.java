package com.example.p2;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

public class AvatarHelper {
    public static List<Bitmap> getAvatars(Context context) {
        List<Bitmap> avatars = new ArrayList<>();
        Bitmap source = BitmapFactory.decodeResource(context.getResources(), R.drawable.avatars);
        if (source == null) {
            Log.e("AvatarHelper", "Could not decode R.drawable.avatars");
            return avatars;
        }

        // Based on the labels in Profile.java, we assume 6 avatars.
        // Usually these are in a grid, e.g., 2 rows and 3 columns.
        int rows = 2;
        int cols = 3;
        int width = source.getWidth() / cols;
        int height = source.getHeight() / rows;

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                // Ensure we don't go out of bounds if rows*cols != count, but here we assume 6.
                Bitmap avatar = Bitmap.createBitmap(source, j * width, i * height, width, height);
                avatars.add(avatar);
            }
        }
        return avatars;
    }
}
