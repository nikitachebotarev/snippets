package ru.cnv.helper;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Retrofit;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

@SuppressWarnings("unchecked")
public class BaseResponseCallAdapterFactory extends CallAdapter.Factory {

    @Override
    public CallAdapter<?, ?> get(Type returnType, Annotation[] annotations, Retrofit retrofit) {
        return new BaseResponseCallAdapter<>((Class) returnType);
    }

    public class BaseResponseCallAdapter<T extends BaseResponseEntity> implements CallAdapter<ResponseBody, T> {

        private Class<T> clazz;

        public BaseResponseCallAdapter(Class<T> type) {
            this.clazz = type;
        }

        @Override
        public Type responseType() {
            return new TypeToken<ResponseBody>() {
            }.getType();
        }

        @Override
        public T adapt(Call<ResponseBody> call) {
            T parsedWithError = createParsedForError();
            if (parsedWithError == null) {
                return null;
            }

            ResponseBody responseBody;
            try {
                responseBody = call.execute().body();
            } catch (Exception e) {
                parsedWithError.addException(e);
                return parsedWithError;
            }

            String string;
            try {
                string = responseBody.string();
                parsedWithError.setString(string);
            } catch (Exception e) {
                parsedWithError.addException(e);
                return parsedWithError;
            }

            try {
                T parsed = new Gson().fromJson(string, clazz);
                parsed.setString(string);
                return parsed;
            } catch (Exception e) {
                parsedWithError.addException(e);
                return parsedWithError;
            }
        }

        private T createParsedForError() {
            T parsed;
            try {
                parsed = clazz.newInstance();
            } catch (InstantiationException e) {
                e.printStackTrace();
                return null;
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                return null;
            }
            return parsed;
        }
    }
}
