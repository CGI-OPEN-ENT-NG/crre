package fr.openent.crre.service.impl;

import fr.openent.crre.Crre;
import fr.openent.crre.service.OrderRegionService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;
import org.entcore.common.user.UserInfos;

public class DefaultOrderRegionService extends SqlCrudService implements OrderRegionService {

    public DefaultOrderRegionService(String table) {
        super(table);
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultOrderRegionService.class);
    @Override
    public void setOrderRegion(JsonObject order, UserInfos user, Handler<Either<String, JsonObject>> handler) {
        String query = "";
        JsonArray params = new JsonArray();
        query = "" +
                "INSERT INTO  " + Crre.crreSchema + ".\"order-region-equipment\" AS ore " +
                "(price, " +
                "amount, " +
                "creation_date, " +
                "owner_name, " +
                "owner_id, " +
                "equipment_key, " +
                "name, " +
                "comment, " +
                "id_order_client_equipment, " +
                "rank, ";
        query += "status, " +
                "id_campaign, " +
                "id_structure, " +
                "summary, " +
                "description, " +
                "image, " +
                "technical_spec, " +
                "id_contract, " +
                "cause_status, " +
                "number_validation, " +
                "id_order, " +
                "id_project) ";

        query += "SELECT " +
                "? ," +
                "? ," +
                "? ," +
                "? ," +
                "? ," +
                "? ," +
                "? ," +
                "? ," +
                "? ,";
        query += order.getInteger("rank") != -1 ? "?, " : "NULL, ";
        query += " 'IN PROGRESS', " +
                "       id_campaign, " +
                "       id_structure, " +
                "       summary, " +
                "       description, " +
                "       image, " +
                "       technical_spec, " +
                "       ? as id_contract, " +
                "       cause_status, " +
                "       number_validation, " +
                "       id_order, " +
                "       id_project " +
                "FROM  " + Crre.crreSchema + ".order_client_equipment " +
                "WHERE id = ? " +
                "RETURNING id;";

        params.add(order.getDouble("price"))
                .add(order.getInteger("amount"))
                .add(order.getString("creation_date"))
                .add(user.getUsername())
                .add(user.getUserId())
                .add(order.getInteger("equipment_key"))
                .add(order.getString("name"))
                .add(order.getString("comment"))
                .add(order.getInteger("id_order_client_equipment"));
        if (order.getInteger("rank") != -1) {
            params.add(order.getInteger("rank"));
        }
        params.add(order.getInteger("id_contract"));
        params.add(order.getInteger("id_order_client_equipment"));
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    public void updateOrderRegion(JsonObject order, int idOrder, UserInfos user, Handler<Either<String, JsonObject>> handler) {
        String query = "";
        JsonArray params = new JsonArray();
        query = "" +
                "UPDATE " + Crre.crreSchema + ".\"order-region-equipment\" " +
                " SET " +
                "price = ?, " +
                "amount = ?, " +
                "modification_date = ? , " +
                "owner_name = ? , " +
                "owner_id = ?, " +
                "name = ?, " +
                "equipment_key = ?, " +
                "cause_status = 'IN PROGRESS', ";

        query += order.getInteger("rank") != -1 ? "rank=?," : "rank = NULL, ";
        query += order.containsKey("id_contract") ? "id_contract = ?, " : "";
        query += "comment = ? " +
                "WHERE id = ?" +
                "RETURNING id;";

        params.add(order.getDouble("price"))
                .add(order.getInteger("amount"))
                .add(order.getString("creation_date"))
                .add(user.getUsername())
                .add(user.getUserId())
                .add(order.getString("name"))
                .add(order.getInteger("equipment_key"));
        if (order.getInteger("rank") != -1) {
            params.add(order.getInteger("rank"));
        }
        if (order.containsKey("id_contract")) {
            params.add(order.getInteger("id_contract"));
        }
        params.add(order.getString("comment"))
                .add(idOrder);
        Sql.getInstance().prepared(query, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void createOrdersRegion(JsonObject order, UserInfos user, Number id_project, Handler<Either<String, JsonObject>> handler) {
        JsonArray params;
        String queryOrderRegionEquipment = "" +
                " INSERT INTO " + Crre.crreSchema + ".\"order-region-equipment\" ";

        if (order.getInteger("rank") != -1) {
            queryOrderRegionEquipment += " ( price, amount, creation_date,  owner_name, owner_id, name, summary, description, image," +
                    " technical_spec, status, id_contract, equipment_key, id_campaign, id_structure," +
                    " comment,  id_project, rank) " +
                    "  VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? ) RETURNING id ; ";
        } else {
            queryOrderRegionEquipment += " ( price, amount, creation_date,  owner_name, owner_id, name, summary, description, image," +
                    " technical_spec, status, id_contract, equipment_key, id_campaign, id_structure," +
                    " comment,  id_project) " +
                    "  VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? ) RETURNING id ; ";
        }

        params = new fr.wseduc.webutils.collections.JsonArray()
                .add(order.getDouble("price"))
                .add(order.getInteger("amount"))
                .add(order.getString("creation_date"))
                .add(user.getUsername())
                .add(user.getUserId())
                .add(order.getString("name"))
                .add(order.getString("summary"))
                .add(order.getString("description"))
                .add(order.getString("image"))
                .add(order.getJsonArray("technical_specs"))
                .add("IN PROGRESS")
                .add(order.getInteger("id_contract"))
                .add(order.getInteger("equipment_key"))
                .add(order.getInteger("id_campaign"))
                .add(order.getString("id_structure"))
                .add(order.getString("comment"))
                .add(id_project);
        if (order.getInteger("rank") != -1) {
            params.add(order.getInteger("rank"));
        }
        Sql.getInstance().prepared(queryOrderRegionEquipment, params, SqlResult.validUniqueResultHandler(handler));
    }

    public void createProject( Integer id_title, Handler<Either<String, JsonObject>> handler) {
        JsonArray params;

        String queryProjectEquipment = "" +
                "INSERT INTO " + Crre.crreSchema + ".project " +
                "( id_title ) VALUES " +
                "( ? )  RETURNING id ;";
        params = new fr.wseduc.webutils.collections.JsonArray();

        params.add(id_title);

        Sql.getInstance().prepared(queryProjectEquipment, params, SqlResult.validUniqueResultHandler(handler));
    }

    @Override
    public void deleteOneOrderRegion(int idOrderRegion, Handler<Either<String, JsonObject>> handler) {
        String query = "" +
                "DELETE FROM " +
                Crre.crreSchema + ".\"order-region-equipment\" " +
                "WHERE id = ? " +
                "RETURNING id";
        sql.prepared(query, new JsonArray().add(idOrderRegion), SqlResult.validRowsResultHandler(handler));
    }

    @Override
    public void getOneOrderRegion(int idOrder, Handler<Either<String, JsonObject>> handler) {
        String query = "" +
                "SELECT ore.*, " +
                "       ore.price AS price_single_ttc, " +
                "       to_json(contract.*) contract, " +
                "       to_json(ct.*) contract_type, " +
                "       to_json(campaign.*) campaign, " +
                "       to_json(prj.*) AS project, " +
                "       to_json(tt.*) AS title, " +
                "       to_json(oce.*) AS order_parent " +
                "FROM  " + Crre.crreSchema + ".\"order-region-equipment\" AS ore " +
                "LEFT JOIN " + Crre.crreSchema + ".order_client_equipment AS oce ON ore.id_order_client_equipment = oce.id " +
                "LEFT JOIN  " + Crre.crreSchema + ".contract ON ore.id_contract = contract.id " +
                "INNER JOIN  " + Crre.crreSchema + ".contract_type ct ON ct.id = contract.id_contract_type " +
                "INNER JOIN  " + Crre.crreSchema + ".campaign ON ore.id_campaign = campaign.id " +
                "INNER JOIN  " + Crre.crreSchema + ".project AS prj ON ore.id_project = prj.id " +
                "INNER JOIN  " + Crre.crreSchema + ".title AS tt ON tt.id = prj.id_title " +
                "INNER JOIN  " + Crre.crreSchema + ".rel_group_campaign ON (ore.id_campaign = rel_group_campaign.id_campaign) " +
                "INNER JOIN  " + Crre.crreSchema + ".rel_group_structure ON (ore.id_structure = rel_group_structure.id_structure) " +
                "WHERE ore.status = 'IN PROGRESS' AND ore.id = ? " +
                "GROUP BY ( prj.id, " +
                "          ore.id, " +
                "          contract.id, " +
                "          ct.id, " +
                "          campaign.id, " +
                "          tt.id, " +
                "          oce.id )";

        Sql.getInstance().prepared(query, new JsonArray().add(idOrder), SqlResult.validUniqueResultHandler(handler));
    }
}
