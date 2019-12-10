package utils;

public abstract class StoppableThread extends Thread {
    protected boolean isActive;

    protected StoppableThread() {
        this.isActive = true;
    }

    public void terminate() {
        this.isActive = false;
        this.interrupt();
        this.cleanUp();
    }

    protected abstract void cleanUp();
}
