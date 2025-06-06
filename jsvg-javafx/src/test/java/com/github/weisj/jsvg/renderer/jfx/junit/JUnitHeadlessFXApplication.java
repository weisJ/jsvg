package com.github.weisj.jsvg.renderer.jfx.junit;

import javafx.application.Application;
import javafx.stage.Stage;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

//Based on: http://awhite.blogspot.com/2013/04/javafx-junit-testing.html
public class JUnitHeadlessFXApplication extends Application {

    private static final ReentrantLock LOCK = new ReentrantLock();
    private static final AtomicBoolean started = new AtomicBoolean();

    public static void checkJavaFXThread(){
        LOCK.lock();
        try{
            if (!started.get()){
                ExecutorService executor = Executors.newSingleThreadExecutor();
                executor.execute(Application::launch);
                while (!started.get()){
                    Thread.yield();
                }
            }
        }
        finally{
            LOCK.unlock();
        }
    }

    @Override
    public void start(final Stage stage) {
        started.set(Boolean.TRUE);
    }
}
