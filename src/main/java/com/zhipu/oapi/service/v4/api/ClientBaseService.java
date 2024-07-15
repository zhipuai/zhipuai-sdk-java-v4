package com.zhipu.oapi.service.v4.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.zhipu.oapi.service.v4.deserialize.ModelDataDeserializer;
import com.zhipu.oapi.service.v4.model.ModelData;
import com.zhipu.oapi.service.v4.model.ZhiPuAiError;
import com.zhipu.oapi.service.v4.model.ZhiPuAiHttpException;
import com.zhipu.oapi.utils.StringUtils;
import io.reactivex.Single;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.HttpException;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.io.IOException;
import java.util.concurrent.ExecutorService;

import static com.zhipu.oapi.Constants.BASE_URL;

public class ClientBaseService {

    protected final static Logger logger = LoggerFactory.getLogger(ClientBaseService.class);

    protected static final ObjectMapper mapper = defaultObjectMapper();

    protected final Retrofit retrofit;


    /**
     * Creates a new ClientBaseService that wraps OkHttpClient
     * @param client retrofit instance
     * @param baseUrl base url of the api
     */
    public ClientBaseService(final OkHttpClient client, final String baseUrl) {

        this.retrofit = defaultRetrofit(baseUrl, client, mapper);
        ExecutorService executorService = client.dispatcher().executorService();
    }

    public static <T> T execute(Single<T> apiCall) {
        try {
            return apiCall.blockingGet();
        } catch (HttpException e) {
            logger.error("HTTP exception: {}", e.getMessage());
            try {
                if (e.response() == null || e.response().errorBody() == null) {
                    throw e;
                }
                String errorBody = e.response().errorBody().string();

                ZhiPuAiError error = mapper.readValue(errorBody, ZhiPuAiError.class);

                throw new ZhiPuAiHttpException(error, e, e.code());
            } catch (IOException ex) {
                // couldn't parse ZhiPuAiError error
                throw e;
            }
        }
    }


    private static ObjectMapper defaultObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);

        SimpleModule module = new SimpleModule();

        module.addDeserializer(ModelData.class, new ModelDataDeserializer());
        mapper.registerModule(module);

        return mapper;
    }


    private static Retrofit defaultRetrofit(final String baseUrl,
                                           OkHttpClient client,
                                           ObjectMapper mapper) {
        return new Retrofit.Builder()
                .baseUrl(StringUtils.isEmpty(baseUrl) ? BASE_URL:baseUrl)
                .client(client)
                .addConverterFactory(JacksonConverterFactory.create(mapper))
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build();
    }

}
