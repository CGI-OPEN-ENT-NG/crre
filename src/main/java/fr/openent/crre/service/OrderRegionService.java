package fr.openent.crre.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.user.UserInfos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public interface OrderRegionService {
    void createOrdersRegion(JsonObject order, UserInfos event, Number id_project, Handler<Either<String, JsonObject>> handler);

    void deleteOneOrderRegion(int idOrderRegion, Handler<Either<String, JsonObject>> handler);

    void equipmentAlreadyPayed(String idEquipment, String idStructure, Handler<Either<String, JsonObject>> handler);

    void getOneOrderRegion(int idOrderRegion, Handler<Either<String, JsonObject>> handler);

    void createProject (String title,  Handler<Either<String, JsonObject>> handler);

    void getAllOrderRegionByProject(int idProject, boolean filterRejectedOrders, Handler<Either<String, JsonArray>> arrayResponseHandler);

    void getOrdersRegionById(List<Integer> idsOrder, Handler<Either<String, JsonArray>> arrayResponseHandler);

    void getAllProjects(UserInfos user, Integer page, boolean filterRejectedSentOrders, Handler<Either<String, JsonArray>> arrayResponseHandler);

    void search(UserInfos user, JsonArray equipTab, String query, String startDate, String endDate, JsonArray filters,
                Integer page, Handler<Either<String, JsonArray>> arrayResponseHandler);

    void searchName(String word, Handler<Either<String, JsonArray>> handler);

    void filter_only(UserInfos user, JsonArray equipTab, String startDate, String endDate, JsonArray filters,
                     Integer page, Handler<Either<String, JsonArray>> arrayResponseHandler);

    void filterSearch(UserInfos user, JsonArray equipTab, String query, String startDate, String endDate, JsonArray filters,
                      Integer page, Handler<Either<String, JsonArray>> arrayResponseHandler);

    void filterSearchWithoutEquip(UserInfos user, String query, String startDate, String endDate, JsonArray filters,
                                  Integer page, Handler<Either<String, JsonArray>> arrayResponseHandler);

    void getLastProject(UserInfos user, Handler<Either<String, JsonObject>> arrayResponseHandler);

    void updateOrders(List<Integer> ids, String status, String justification, Handler<Either<String, JsonObject>> handler);

    void filterES(HashMap<String, ArrayList<String>> params, String query, Handler<Either<String, JsonArray>> handlerJsonArray);

}
