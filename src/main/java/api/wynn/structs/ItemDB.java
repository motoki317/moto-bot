package api.wynn.structs;

import api.wynn.structs.common.Request;

import java.util.List;

public class ItemDB {
    private List<Item> items;
    private Request request;

    public List<Item> getItems() {
        return items;
    }

    public Request getRequest() {
        return request;
    }
}
