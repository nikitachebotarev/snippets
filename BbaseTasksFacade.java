package ru.cnv.helper;

import android.content.Context;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import ru.cnv.utils.JsonPreferenceUtils;


public class BaseTasksFacade {

    public JsonArray list(String entity) {
        BaseResponseEntity baseResponseEntity = RetrofitFactory.create(BaseApiInterface.class, BaseApiInterface.BASE_URL).list(entity);
        String stringResponseBody = baseResponseEntity.getString();
        return new JsonParser().parse(stringResponseBody).getAsJsonObject().get("data").getAsJsonArray();
    }

    public JsonArray get(String entity, String id) {
        BaseResponseEntity baseResponseEntity = RetrofitFactory.create(BaseApiInterface.class, BaseApiInterface.BASE_URL).get(entity, id);
        String stringResponseBody = baseResponseEntity.getString();
        return new JsonParser().parse(stringResponseBody).getAsJsonObject().get("data").getAsJsonArray();
    }

    public JsonArray field(String entity, String field, String value) {
        BaseResponseEntity baseResponseEntity = RetrofitFactory.create(BaseApiInterface.class, BaseApiInterface.BASE_URL).field(entity, field, value);
        String stringResponseBody = baseResponseEntity.getString();
        return new JsonParser().parse(stringResponseBody).getAsJsonObject().get("data").getAsJsonArray();
    }

    public Boolean add(String entity) {
        RetrofitFactory.create(BaseApiInterface.class, BaseApiInterface.BASE_URL).add(entity);
        return true;
    }

    public Boolean delete(String entity, String id) {
        RetrofitFactory.create(BaseApiInterface.class, BaseApiInterface.BASE_URL).delete(entity, id);
        return true;
    }

    public JsonArray related(String relEntity, String relEntityField, String relEntityValue, String relEntityOtherField, String desiredEntity) {
        JsonArray ids = new BaseTasksFacade().field(relEntity, relEntityField, relEntityValue);
        JsonArray objects = new JsonArray();
        for (JsonElement id : ids) {
            String idAsString = id.getAsJsonObject().get(relEntityOtherField).getAsString();
            JsonObject speaker = new BaseTasksFacade().get(desiredEntity, idAsString).get(0).getAsJsonObject();
            objects.add(speaker);
        }
        return objects;
    }

    public JsonArray prefAll(Context context, String name) {
        return  JsonPreferenceUtils.all(context, name);
    }

    public void prefAdd(Context context, String name, JsonElement jsonElement) {
         JsonPreferenceUtils.add(context, name, jsonElement);
    }

    public JsonObject prefGet(Context context, String name, String id) {
        return  JsonPreferenceUtils.get(context, name, id);
    }

    public void prefDelete(Context context, String name, String id) {
         JsonPreferenceUtils.delete(context, name, id);
    }

    public void prefAddAll(Context context, String name, JsonArray jsonArray) {
        for (JsonElement element : jsonArray) {
            prefAdd(context, name, element);
        }
    }
}
