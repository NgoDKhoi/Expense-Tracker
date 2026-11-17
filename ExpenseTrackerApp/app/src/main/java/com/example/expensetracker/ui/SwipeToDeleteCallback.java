package com.example.expensetracker.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expensetracker.R;

public abstract class SwipeToDeleteCallback extends ItemTouchHelper.SimpleCallback {

    private final Drawable deleteIcon;
    private final Drawable editIcon;
    private final ColorDrawable deleteBackground;
    private final ColorDrawable editBackground;
    private final int deleteWidth;
    private final int deleteHeight;
    private final int editWidth;
    private final int editHeight;

    public SwipeToDeleteCallback(Context context) {
        super(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
        deleteIcon = ContextCompat.getDrawable(context, R.drawable.ic_delete);
        editIcon = ContextCompat.getDrawable(context, R.drawable.ic_edit);
        deleteBackground = new ColorDrawable(Color.parseColor("#E53935"));
        editBackground = new ColorDrawable(Color.parseColor("#2196F3"));

        if (deleteIcon != null) {
            deleteWidth = deleteIcon.getIntrinsicWidth() > 0 ? deleteIcon.getIntrinsicWidth() : 60;
            deleteHeight = deleteIcon.getIntrinsicHeight() > 0 ? deleteIcon.getIntrinsicHeight() : 60;
            deleteIcon.setTint(Color.WHITE);
        } else {
            deleteWidth = 60;
            deleteHeight = 60;
        }

        if (editIcon != null) {
            editWidth = editIcon.getIntrinsicWidth() > 0 ? editIcon.getIntrinsicWidth() : 60;
            editHeight = editIcon.getIntrinsicHeight() > 0 ? editIcon.getIntrinsicHeight() : 60;
            editIcon.setTint(Color.WHITE);
        } else {
            editWidth = 60;
            editHeight = 60;
        }
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder,
                            float dX, float dY, int actionState, boolean isCurrentlyActive) {
        View itemView = viewHolder.itemView;
        int itemHeight = itemView.getBottom() - itemView.getTop();

        if (dX < 0) { // Swiping left -> Delete
            deleteBackground.setBounds(itemView.getRight() + ((int) dX), itemView.getTop(), itemView.getRight(), itemView.getBottom());
            deleteBackground.draw(c);

            int iconTop = itemView.getTop() + (itemHeight - deleteHeight) / 2;
            int iconMargin = (itemHeight - deleteHeight) / 2;
            int iconLeft = itemView.getRight() - iconMargin - deleteWidth;
            int iconRight = itemView.getRight() - iconMargin;
            int iconBottom = iconTop + deleteHeight;

            if (deleteIcon != null) {
                deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                deleteIcon.draw(c);
            }
        } else if (dX > 0) { // Swiping right -> Edit
            editBackground.setBounds(itemView.getLeft(), itemView.getTop(), itemView.getLeft() + ((int) dX), itemView.getBottom());
            editBackground.draw(c);

            int iconTop = itemView.getTop() + (itemHeight - editHeight) / 2;
            int iconMargin = (itemHeight - editHeight) / 2;
            int iconLeft = itemView.getLeft() + iconMargin;
            int iconRight = iconLeft + editWidth;
            int iconBottom = iconTop + editHeight;

            if (editIcon != null) {
                editIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                editIcon.draw(c);
            }
        }

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }
}
