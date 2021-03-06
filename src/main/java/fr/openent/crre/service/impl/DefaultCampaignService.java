package fr.openent.crre.service.impl;

import fr.openent.crre.Crre;
import fr.openent.crre.model.Campaign;
import fr.openent.crre.security.WorkflowActionUtils;
import fr.openent.crre.security.WorkflowActions;
import fr.openent.crre.service.CampaignService;
import fr.wseduc.webutils.Either;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;

import java.util.List;
import java.util.Map;

import static fr.openent.crre.helpers.FutureHelper.handlerJsonArray;

public class DefaultCampaignService extends SqlCrudService implements CampaignService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultCampaignService.class);


    public DefaultCampaignService(String schema, String table) {
        super(schema, table);
    }

    public void listCampaigns(Handler<Either<String, JsonArray>> handler) {
        Future<JsonArray> campaignFuture = Future.future();
        Future<JsonArray> purseFuture = Future.future();
        Future<JsonArray> orderFuture = Future.future();


        CompositeFuture.all(campaignFuture, purseFuture, orderFuture).setHandler(event -> {
            if (event.succeeded()) {
                JsonArray campaigns = campaignFuture.result();
                JsonArray purses = purseFuture.result();
                JsonArray orders = orderFuture.result();


                JsonObject campaignMap = new JsonObject();
                JsonObject object, campaign;
                for (int i = 0; i < campaigns.size(); i++) {
                    object = campaigns.getJsonObject(i);
                    object.put("nb_orders_waiting", 0).put("nb_orders_valid", 0).put("nb_orders_sent", 0);
                    campaignMap.put(object.getInteger("id").toString(), object);
                }

                for (int i = 0; i < purses.size(); i++) {
                    object = purses.getJsonObject(i);
                    try {
                        campaign = campaignMap.getJsonObject(object.getInteger("id_campaign").toString());
                        campaign.put("purse_amount", object.getString("amount"));
                    }catch (NullPointerException e){
                        LOGGER.info("A purse is present on this structure but the structure is not linked to the campaign");
                    }
                }

                for (int i = 0; i < orders.size(); i++) {
                    object = orders.getJsonObject(i);
                    try {
                        campaign = campaignMap.getJsonObject(object.getInteger("id_campaign").toString());
                        campaign.put("nb_order", object.getLong("nb_order"));
                        campaign.put("nb_order_waiting", object.getInteger("nb_order_waiting"));
                    }catch (NullPointerException e){
                        LOGGER.info("An order is present on this structure but the structure is not linked to the campaign");
                    }
                }
                JsonArray campaignList = new JsonArray();
                for (Map.Entry<String, Object> aCampaign : campaignMap) {
                    campaignList.add(aCampaign.getValue());
                }
                handler.handle(new Either.Right<>(campaignList));

            } else {
                handler.handle(new Either.Left<>("An error occurred when retrieving campaigns"));
            }
        });
        getCampaignsInfo(handlerJsonArray(campaignFuture));
        getCampaignsPurses(handlerJsonArray(purseFuture));
        getCampaignOrderStatusCount(handlerJsonArray(orderFuture));
    }

    private void getCampaignsPurses(Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT SUM(amount) as purse " +
                "FROM " + Crre.crreSchema + ".purse " +
                "GROUP BY id_structure;";

        Sql.getInstance().prepared(query, new JsonArray(), SqlResult.validResultHandler(handler));
    }

    private void getCampaignsPurses(String idStructure, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT amount, initial_amount, id_structure " +
                "FROM " + Crre.crreSchema + ".purse " +
                "WHERE id_structure = ?";

        Sql.getInstance().prepared(query, new JsonArray().add(idStructure), SqlResult.validResultHandler(handler));
    }

    private void getCampaignsLicences(String idStructure, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT \"Seconde\", \"Premiere\", \"Terminale\", pro, amount, initial_amount " +
                "FROM " + Crre.crreSchema + ".licences l " +
                "JOIN " + Crre.crreSchema + ".students s ON (s.id_structure = l.id_structure) "+
                "WHERE l.id_structure = ?";

        Sql.getInstance().prepared(query, new JsonArray().add(idStructure), SqlResult.validResultHandler(handler));
    }

    private void getCampaignOrderStatusCount(Handler<Either<String, JsonArray>> handler) {
        String sub_query_waiting_order = "WITH count_order_waiting AS (" +
                "SELECT COUNT(order_client_equipment.id) as nb_order_waiting, campaign.id as id_campaign " +
                "FROM " + Crre.crreSchema +".order_client_equipment " +
                "INNER JOIN " + Crre.crreSchema +".campaign ON (order_client_equipment.id_campaign = campaign.id) " +
                "WHERE status = 'WAITING' GROUP BY campaign.id) ";

        String query = sub_query_waiting_order +
                "SELECT bo.id_campaign, id_user as user_id, COUNT(bo.id) as nb_order, cow.nb_order_waiting " +
                "FROM " + Crre.crreSchema + ".basket_order bo " +
                "INNER JOIN count_order_waiting cow ON bo.id_campaign = cow.id_campaign " +
                "GROUP BY bo.id_campaign, id_user, nb_order_waiting;";

        Sql.getInstance().prepared(query, new JsonArray(), SqlResult.validResultHandler(handler));
    }

    private void getCampaignOrderStatusCount(String idStructure, Handler<Either<String, JsonArray>> handler) {
        String sub_query_waiting_order = "WITH count_order_waiting AS (" +
                "SELECT COUNT(order_client_equipment.id) as nb_order_waiting, campaign.id as id_campaign " +
                "FROM " + Crre.crreSchema +".order_client_equipment " +
                "INNER JOIN " + Crre.crreSchema +".campaign ON (order_client_equipment.id_campaign = campaign.id) " +
                "WHERE id_structure = ? AND status = 'WAITING' GROUP BY campaign.id) ";

        String query = sub_query_waiting_order +
                "SELECT bo.id_campaign, id_user as user_id, COUNT(bo.id) as nb_order, cow.nb_order_waiting " +
                "FROM " + Crre.crreSchema + ".basket_order bo " +
                "INNER JOIN count_order_waiting cow ON bo.id_campaign = cow.id_campaign " +
                "WHERE id_structure = ? " +
                "GROUP BY bo.id_campaign, id_user, nb_order_waiting;";

        Sql.getInstance().prepared(query, new JsonArray().add(idStructure).add(idStructure), SqlResult.validResultHandler(handler));
    }

    private void getCampaignsInfo(Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT campaign.*, COUNT(DISTINCT rel_group_structure.id_structure) as nb_structures " +
                "FROM " + Crre.crreSchema + ".campaign " +
                "INNER JOIN " + Crre.crreSchema + ".rel_group_campaign ON (campaign.id = rel_group_campaign.id_campaign) " +
                "INNER JOIN " + Crre.crreSchema + ".rel_group_structure ON (rel_group_structure.id_structure_group = rel_group_campaign.id_structure_group) " +
                "GROUP BY campaign.id;";

        Sql.getInstance().prepared(query, new JsonArray(), SqlResult.validResultHandler(handler));
    }

    private void getCampaignsInfo(String idStructure, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT DISTINCT campaign.*, count(DISTINCT rel_group_structure.id_structure) as nb_structures " +
                "FROM " + Crre.crreSchema + ".campaign " +
                "INNER JOIN " + Crre.crreSchema + ".rel_group_campaign ON (campaign.id = rel_group_campaign.id_campaign) " +
                "INNER JOIN " + Crre.crreSchema + ".rel_group_structure ON (rel_group_campaign.id_structure_group = rel_group_structure.id_structure_group) " +
                "WHERE rel_group_structure.id_structure = ? " +
                "GROUP BY campaign.id;";

        Sql.getInstance().prepared(query, new JsonArray().add(idStructure), SqlResult.validResultHandler(handler));
    }

    public void listCampaigns(String idStructure,  Handler<Either<String, JsonArray>> handler, UserInfos user) {
        Future<JsonArray> campaignFuture = Future.future();
        Future<JsonArray> purseFuture = Future.future();
        Future<JsonArray> basketFuture = Future.future();
        Future<JsonArray> orderFuture = Future.future();
        Future<JsonArray> licenceFuture = Future.future();

        CompositeFuture.all(campaignFuture, purseFuture, basketFuture, orderFuture, licenceFuture).setHandler(event -> {
            if (event.succeeded()) {
                JsonArray campaigns = campaignFuture.result();
                JsonArray baskets = basketFuture.result();
                JsonArray purses = purseFuture.result();
                JsonArray orders = orderFuture.result();
                JsonArray licences = licenceFuture.result();

                JsonObject campaignMap = new JsonObject();
                JsonObject object, campaign;
                for (int i = 0; i < campaigns.size(); i++) {
                    campaign = campaigns.getJsonObject(i);
                        object = purses.getJsonObject(0);
                        try {
                            campaign.put("purse_amount", object.getString("amount"));
                            campaign.put("initial_purse_amount",object.getString("initial_amount"));
                        }catch (NullPointerException e){
                            LOGGER.info("A purse is present on this structure but the structure is not linked to the campaign");
                        }
                    campaignMap.put(campaign.getInteger("id").toString(), campaign);
                }

                for (int i = 0; i < baskets.size(); i++) {
                    object = baskets.getJsonObject(i);
                    try {
                        campaign = campaignMap.getJsonObject(object.getInteger("id_campaign").toString());
                        campaign.put("nb_panier", object.getLong("nb_panier"));
                    }catch (NullPointerException e){
                        LOGGER.info("A basket is present on this structure but the structure is not linked to the campaign");
                    }
                }
                for (int i = 0; i < licences.size(); i++) {
                    object = licences.getJsonObject(i);
                    try {
                        int nb_seconde;
                        int nb_premiere;
                        int nb_terminale;
                        if(object.getBoolean("pro")) {
                             nb_seconde = object.getInteger("Seconde") * 3;
                             nb_premiere = object.getInteger("Premiere") * 3;
                             nb_terminale = object.getInteger("Terminale") * 3;
                        } else {
                             nb_seconde = object.getInteger("Seconde") * 9;
                             nb_premiere = object.getInteger("Premiere") * 8;
                             nb_terminale = object.getInteger("Terminale") * 7;
                        }

                        int nb_total = Integer.parseInt(object.getString("initial_amount"));
                        int nb_total_available = Integer.parseInt(object.getString("amount"));
                        for (int s = 0; s < campaigns.size(); s++) {
                            campaign = campaignMap.getJsonObject(campaigns.getJsonObject(s).getInteger("id").toString());
                            campaign.put("nb_licences_total", nb_total);
                            campaign.put("nb_licences_available", nb_total_available);
                            campaign.put("nb_licences_2de", nb_seconde);
                            campaign.put("nb_licences_1ere", nb_premiere);
                            campaign.put("nb_licences_Tale", nb_terminale);
                        }
                    } catch (NullPointerException e){
                        LOGGER.info("A licence is present on this structure but the structure is not linked to the campaign");
                    }
                }
                for (int i = 0; i < orders.size(); i++) {
                    object = orders.getJsonObject(i);
                    try {
                        campaign = campaignMap.getJsonObject(object.getInteger("id_campaign").toString());
                        if(user.getUserId().equals(object.getString("user_id")))
                            campaign.put("nb_order", object.getLong("nb_order"));
                        if(WorkflowActionUtils.hasRight(user, WorkflowActions.VALIDATOR_RIGHT.toString()))
                            campaign.put("nb_order_waiting", object.getInteger("nb_order_waiting"));
                        campaign.put("order_notification",0);
                        campaign.put("historic_etab_notification",0);
                    }catch (NullPointerException e){
                        LOGGER.info("An order is present on this structure but the structure is not linked to the campaign");
                    }
                }
                JsonArray campaignList = new JsonArray();
                for (Map.Entry<String, Object> aCampaign : campaignMap) {
                    campaignList.add(aCampaign.getValue());
                }
                handler.handle(new Either.Right<>(campaignList));
            } else {
                handler.handle(new Either.Left<>("[DefaultCampaignService@listCampaigns] An error occured. CompositeFuture returned failed :" + event.cause()));
            }
        });

        getCampaignsInfo(idStructure, handlerJsonArray(campaignFuture));
        getCampaignsPurses(idStructure, handlerJsonArray(purseFuture));
        getCampaignOrderStatusCount(idStructure, handlerJsonArray(orderFuture));
        getCampaignsLicences(idStructure, handlerJsonArray(licenceFuture));
        getBasketCampaigns(idStructure, handlerJsonArray(basketFuture), user);
    }

    private void getBasketCampaigns(String idStructure, Handler<Either<String, JsonArray>> handler, UserInfos user) {
        String query = "SELECT COUNT(basket_equipment.id) as nb_panier, campaign.id as id_campaign " +
                "FROM " + Crre.crreSchema + ".basket_equipment " +
                "INNER JOIN " + Crre.crreSchema + ".campaign ON (campaign.id = basket_equipment.id_campaign) " +
                "WHERE id_structure = ? " +
                "AND basket_equipment.owner_id = ? " +
                "GROUP BY campaign.id;";

        Sql.getInstance().prepared(query, new JsonArray().add(idStructure).add(user.getUserId()), SqlResult.validResultHandler(handler));
    }

    public void getCampaign(Integer id, Handler<Either<String, JsonObject>> handler){
        String query = "  SELECT campaign.*,array_to_json(array_agg(groupe)) as  groups"+
                "FROM  " + Crre.crreSchema + ".campaign campaign  "+
                "LEFT JOIN  "+
                "(SELECT rel_group_campaign.id_campaign, structure_group.*)" +
                "FROM " + Crre.crreSchema + ".structure_group " +
                "INNER JOIN " + Crre.crreSchema + ".rel_group_campaign" +
                " ON structure_group.id = rel_group_campaign.id_structure_group "+
                "WHERE rel_group_campaign.id_campaign = ?  "+
                "GROUP BY (rel_group_campaign.id_campaign, structure_group.id)) as groupe " +
                "ON groupe.id_campaign = campaign.id "+
                "where campaign.id = ?  "+
                "group By (campaign.id);  " ;

        sql.prepared(query, new fr.wseduc.webutils.collections.JsonArray().add(id).add(id), SqlResult.validUniqueResultHandler(handler));
    }
    public void create(final JsonObject campaign, final Handler<Either<String, JsonObject>> handler) {
        String getIdQuery = "SELECT nextval('" + Crre.crreSchema + ".campaign_id_seq') as id";
        sql.raw(getIdQuery, SqlResult.validUniqueResultHandler(event -> {
            if (event.isRight()) {
                try {
                    final Number id = event.right().getValue().getInteger("id");

                    Campaign createCampaign = new Campaign();
                    createCampaign.setFromJson(campaign);
                    createCampaign.setId((int) id);

                    createCampaign.create(resultObject -> {
                        if(resultObject.isRight()) {
                            JsonArray statements = new fr.wseduc.webutils.collections.JsonArray();
                            JsonArray groups = campaign.getJsonArray("groups");
                            statements.add(getCampaignTagsGroupsRelationshipStatement(id, groups));
                            sql.transaction(statements, event1 -> handler.handle(getTransactionHandler(event1, id)));
                        } else {
                            LOGGER.error("An error occurred when creating campaign", resultObject.left().getValue());
                            handler.handle(new Either.Left<>(""));
                        }
                    });

                } catch (ClassCastException e) {
                    LOGGER.error("An error occurred when casting tags ids", e);
                    handler.handle(new Either.Left<>(""));
                }
            } else {
                LOGGER.error("An error occurred when selecting next val");
                handler.handle(new Either.Left<>(""));
            }
        }));
    }

    public void update(final Integer id, JsonObject campaign,final Handler<Either<String, JsonObject>> handler){

        Campaign createCampaign = new Campaign();
        createCampaign.setFromJson(campaign);
        createCampaign.setId(id);

        createCampaign.update(resultObject -> {
            if(resultObject.isRight()) {
                JsonArray statements = new fr.wseduc.webutils.collections.JsonArray()
                        .add(getCampaignTagGroupRelationshipDeletion(id));
                JsonArray groups = campaign.getJsonArray("groups");
                statements.add(getCampaignTagsGroupsRelationshipStatement(id, groups));
                sql.transaction(statements, event -> handler.handle(getTransactionHandler(event, id)));
            } else {
                LOGGER.error("An error occurred when creating campaign", resultObject.left().getValue());
                handler.handle(new Either.Left<>(""));
            }
        });
    }

    public void delete(final List<Integer> ids, final Handler<Either<String, JsonObject>> handler) {
        JsonArray statements = new fr.wseduc.webutils.collections.JsonArray()
                .add(getCampaignsGroupRelationshipDeletion(ids))
                .add(getCampaignsDeletion(ids));

        sql.transaction(statements, event -> handler.handle(getTransactionHandler(event,ids.get(0))));
    }

    @Override
    public void getCampaignStructures(Integer campaignId, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT distinct id_structure FROM " + Crre.crreSchema + ".campaign " +
                "INNER JOIN " + Crre.crreSchema + ".rel_group_campaign ON (campaign.id = rel_group_campaign.id_campaign) " +
                "INNER JOIN " + Crre.crreSchema + ".rel_group_structure " +
                "ON (rel_group_structure.id_structure_group = rel_group_campaign.id_structure_group) " +
                "WHERE campaign.id = ?;";

        sql.prepared(query, new fr.wseduc.webutils.collections.JsonArray().add(campaignId), SqlResult.validResultHandler(handler));
    }

    @Override
    public void updatePreference(Integer campaignId,Integer projectId, String structureId,
                                 JsonArray projectOrders, Handler<Either<String, JsonObject>> handler) {
        String query= "UPDATE " + Crre.crreSchema + ".project SET "+
                "preference = ? " +
                "WHERE id = ?; " +
                "UPDATE " + Crre.crreSchema + ".project SET "+
                "preference = ? " +
                "WHERE id = ?; ";
        JsonArray values = new JsonArray();
        for(Object object : projectOrders){
            values.add(((JsonObject) object).getInteger("preference"));
            values.add(((JsonObject) object).getInteger("id"));
        }
        JsonArray statements = new fr.wseduc.webutils.collections.JsonArray();
        statements.add(new JsonObject()
                .put("statement",query)
                .put("values",values)
                .put("action","prepared"));
        sql.transaction(statements, jsonObjectMessage -> handler.handle(getTransactionHandler(jsonObjectMessage,projectId)));
    }

    @Override
    public void getStructures(Integer idCampaign, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT id_structure as id " +
                "FROM " + Crre.crreSchema + ".campaign " +
                "INNER JOIN " + Crre.crreSchema + ".rel_group_campaign ON (campaign.id = rel_group_campaign.id_campaign) " +
                "INNER JOIN " + Crre.crreSchema + ".rel_group_structure ON (rel_group_campaign.id_structure_group = rel_group_structure.id_structure_group) " +
                "WHERE id = ? " +
                "GROUP BY id_structure";
        JsonArray params = new JsonArray()
                .add(idCampaign);

        Sql.getInstance().prepared(query, params, SqlResult.validResultHandler(handler));
    }

    public void updateAccessibility(final Integer id,final JsonObject campaign,
                                    final Handler<Either<String, JsonObject>> handler){
        JsonArray statements = new fr.wseduc.webutils.collections.JsonArray();
        String query = "UPDATE " + Crre.crreSchema + ".campaign SET " +
                "accessible= ? " +
                "WHERE id = ?";
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(campaign.getBoolean("accessible"))
                .add(id);
        statements.add(new JsonObject()
                .put("statement", query)
                .put("values",params)
                .put("action", "prepared"));
        sql.transaction(statements, event -> handler.handle(getTransactionHandler(event, id)));
    }

    private JsonObject getCampaignTagsGroupsRelationshipStatement(Number id, JsonArray groups) {
        StringBuilder insertTagCampaignRelationshipQuery = new StringBuilder("INSERT INTO " +
                Crre.crreSchema + ".rel_group_campaign" +
                "(id_campaign, id_structure_group) VALUES ");
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();
        for(int j = 0; j < groups.size(); j++ ){
            JsonObject group =  groups.getJsonObject(j);
            insertTagCampaignRelationshipQuery.append("(?, ?)");
            params.add(id)
                    .add(group.getInteger("id"));
        }
        System.out.print(insertTagCampaignRelationshipQuery.toString());

        return new JsonObject()
                .put("statement", insertTagCampaignRelationshipQuery.toString())
                .put("values", params)
                .put("action", "prepared");
    }

    private JsonObject getCampaignTagGroupRelationshipDeletion(Number id) {
        String query = "DELETE FROM " + Crre.crreSchema + ".rel_group_campaign " +
                "WHERE id_campaign = ?;";

        return new JsonObject()
                .put("statement", query)
                .put("values", new fr.wseduc.webutils.collections.JsonArray().add(id))
                .put("action", "prepared");
    }

    /**
     * Returns the update statement.
     *
     * @param id        resource Id
     * @param campaign campaign to update
     * @return Update statement
     */
    private JsonObject getCampaignUpdateStatement(Number id, JsonObject campaign) {
        String query = "UPDATE " + Crre.crreSchema + ".campaign " +
                "SET  name=?, description=?, image=?, purse_enabled=?, priority_enabled=?, priority_field=? " +
                "WHERE id = ?";

        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(campaign.getString("name"))
                .add(campaign.getString("description"))
                .add(campaign.getString("image"))
                .add(campaign.getBoolean("purse_enabled"))
                .add(campaign.getBoolean("priority_enabled"))
                .add(campaign.getString("priority_field"))
                .add(id);

        return new JsonObject()
                .put("statement", query)
                .put("values", params)
                .put("action", "prepared");
    }
    private JsonObject getCampaignCreationStatement(Number id, JsonObject campaign) {
        String insertCampaignQuery =
                "INSERT INTO " + Crre.crreSchema + ".campaign(id, name, description, image, accessible, purse_enabled, priority_enabled, priority_field )" +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING id; ";
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(id)
                .add(campaign.getString("name"))
                .add(campaign.getString("description"))
                .add(campaign.getString("image"))
                .add(campaign.getBoolean("accessible"))
                .add(campaign.getBoolean("purse_enabled"))
                .add(campaign.getBoolean("priority_enabled"))
                .add(campaign.getString("priority_field"));

        return new JsonObject()
                .put("statement", insertCampaignQuery)
                .put("values", params)
                .put("action", "prepared");
    }
    private JsonObject getCampaignsGroupRelationshipDeletion(List<Integer> ids) {
        String query = "DELETE FROM " + Crre.crreSchema + ".rel_group_campaign " +
                " WHERE id_campaign in  " +
                Sql.listPrepared(ids.toArray());
        JsonArray value = new fr.wseduc.webutils.collections.JsonArray();
        for (Integer id : ids) {
            value.add(id);
        }
        return new JsonObject()
                .put("statement", query)
                .put("values", value)
                .put("action", "prepared");
    }

    private JsonObject getCampaignsDeletion(List<Integer> ids) {
        String query = "DELETE FROM " + Crre.crreSchema + ".campaign " +
                " WHERE id in  " + Sql.listPrepared(ids.toArray());
        JsonArray value = new fr.wseduc.webutils.collections.JsonArray();
        for (Integer id : ids) {
            value.add(id);
        }
        return new JsonObject()
                .put("statement", query)
                .put("values", value)
                .put("action", "prepared");
    }
    /**
     * Returns transaction handler. Manage response based on PostgreSQL event
     *
     * @param event PostgreSQL event
     * @param id    resource Id
     * @return Transaction handler
     */
    private static Either<String, JsonObject> getTransactionHandler(Message<JsonObject> event, Number id) {
        Either<String, JsonObject> either;
        JsonObject result = event.body();
        if (result.containsKey("status") && "ok".equals(result.getString("status"))) {
            JsonObject returns = new JsonObject()
                    .put("id", id);
            either = new Either.Right<>(returns);
        } else {
            LOGGER.error("An error occurred when launching campaign transaction");
            either = new Either.Left<>("");
        }
        return either;
    }

}
