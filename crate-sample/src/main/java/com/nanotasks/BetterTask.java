package com.nanotasks;

import android.content.Context;

public class BetterTask<T> extends Task<T> {
    public BetterTask(Context context, BackgroundWork<T> backgroundWork, Completion<T> completion) {
        super(context, backgroundWork, completion);
    }
}
