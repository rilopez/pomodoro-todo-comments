package com.ril;

import com.intellij.openapi.components.ApplicationComponent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class TimerEventsSourceAppComponent implements ApplicationComponent {
    private static final int ONE_SECOND = 1000;

    private final Timer timer = new Timer();
    private final List<Listener> listeners = new ArrayList<Listener>();

    @Override public void initComponent() {
        timer.schedule(new TimerTask() {
            @Override public void run() {
                for (Listener listener : listeners) {
                    listener.onTimerEvent();
                }
            }
        }, 0, ONE_SECOND);
    }

    @Override public void disposeComponent() {
        timer.cancel();
        timer.purge();
    }

    @NotNull @Override public String getComponentName() {
        return "Pomodoro-TimeEventsSource";
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    public interface Listener {
        void onTimerEvent();
    }
}

