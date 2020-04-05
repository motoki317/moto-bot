package update.base;

public interface UserResponseListener<T> {
    /**
     * Handles event.
     * Should update the value returned by {@link this.getUpdatedAt()}.
     * @param event User response event.
     * @return If the manager should discard this instance.
     */
    boolean handle(T event);

    /**
     * Called when this instance is discarded by manager.
     */
    void onDestroy();
    void setOnDestroy(Runnable onDestroy);
    long getUpdatedAt();
    long getMaxLive();
}
