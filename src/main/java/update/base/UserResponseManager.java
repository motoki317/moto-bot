package update.base;

public interface UserResponseManager<E, L extends UserResponseListener<E>> {
    void addEventListener(L botResponse);
    void handle(E event);
}
