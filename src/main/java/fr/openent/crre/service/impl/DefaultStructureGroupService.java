package fr.openent.crre.service.impl;


import fr.openent.crre.Crre;
import fr.openent.crre.controllers.CrreController;
import fr.openent.crre.service.StructureGroupService;
import fr.openent.crre.utils.SqlQueryUtils;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.service.impl.SqlCrudService;
import org.entcore.common.sql.Sql;
import org.entcore.common.sql.SqlResult;

import java.util.List;

/**
 * Created by agnes.lapeyronnie on 28/12/2017.
 */
public class DefaultStructureGroupService extends SqlCrudService implements StructureGroupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultStructureGroupService.class);

    public DefaultStructureGroupService(String schema, String table){
        super(schema, table);
    }

    @Override
    public void listStructureGroups(Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT id, name, description, array_to_json(array_agg(id_structure)) as structures FROM "
                + Crre.crreSchema + ".structure_group " +
                "INNER JOIN " + Crre.crreSchema + ".rel_group_structure" +
                " on structure_group.id = rel_group_structure.id_structure_group group by (id, name , description ) " +
                "ORDER BY id;";

        this.sql.prepared(query,new fr.wseduc.webutils.collections.JsonArray(), SqlResult.validResultHandler(handler));
    }

    @Override
    public void create(final JsonObject structureGroup, final Handler<Either<String, JsonObject>> handler) {
        String getIdQuery = "Select nextval('"+ Crre.crreSchema + ".structure_group_id_seq') as id";
        sql.raw(getIdQuery, SqlResult.validUniqueResultHandler(event -> {
            if(event.isRight()) {
                try{
                    final Number id = event.right().getValue().getInteger("id");
                    JsonArray statements = new fr.wseduc.webutils.collections.JsonArray()
                            .add(getStructureGroupCreationStatement(id,structureGroup));

                    JsonArray idsStructures = structureGroup.getJsonArray("structures");
                    JsonArray allIds = new JsonArray();
                    JsonArray newIds = new JsonArray();
                    CrreController crreController = new CrreController();
                    sql.raw("SELECT DISTINCT r.id_structure FROM " + Crre.crreSchema + ".rel_group_structure r ", SqlResult.validResultHandler(event2 -> {
                        JsonArray structure_id = event2.right().getValue();
                        for (int i = 0; i < structure_id.size(); i++) {
                            allIds.add(structure_id.getJsonObject(i).getString("id_structure"));
                        }
                        for (int j = 0; j < idsStructures.size(); j++) {
                            if (!allIds.contains(idsStructures.getString(j))) {
                                newIds.add(idsStructures.getString(j));
                            }
                        }
                        statements.add(getGroupStructureRelationshipStatement(id, idsStructures));

                        sql.transaction(statements, event1 -> {
                            if(!newIds.isEmpty()) {
                                crreController.getStudentsByStructures(newIds, handler);
                            }
                            handler.handle(SqlQueryUtils.getTransactionHandler(event1, id));
                        });
                    }));

                }catch(ClassCastException e){
                    LOGGER.error("An error occured when casting structures ids " + e);
                    handler.handle(new Either.Left<>(""));
                }
            }else{
               LOGGER.error("An error occurred when selecting next val");
                handler.handle(new Either.Left<>(""));
            }
        }));
    }

    @Override
    public void update(final Integer id, JsonObject structureGroup,final Handler<Either<String, JsonObject>> handler) {
        JsonArray idsStructures = structureGroup.getJsonArray("structures");
        JsonArray allIds = new JsonArray();
        JsonArray newIds = new JsonArray();
        CrreController crreController = new CrreController();
        sql.raw("SELECT DISTINCT r.id_structure FROM " + Crre.crreSchema + ".rel_group_structure r ", SqlResult.validResultHandler(event -> {
            JsonArray structure_id = event.right().getValue();
            for (int i = 0; i < structure_id.size(); i++) {
                allIds.add(structure_id.getJsonObject(i).getString("id_structure"));
            }
            for(int j = 0; j < idsStructures.size(); j++) {
                if(!allIds.contains(idsStructures.getString(j))) {
                    newIds.add(idsStructures.getString(j));
                }
            }
            JsonArray statements = new fr.wseduc.webutils.collections.JsonArray()
                    .add(getStructureGroupUpdateStatement(id,structureGroup))
                    .add(getStrctureGroupRelationshipDeletion(id))
                    .add(getGroupStructureRelationshipStatement(id,idsStructures));
            sql.transaction(statements, event2 -> {
                if(!newIds.isEmpty()) {
                    crreController.getStudentsByStructures(newIds, handler);
                }
                handler.handle(SqlQueryUtils.getTransactionHandler(event2, id));
            });
        }));
    }

/*    private void getAllStructures(Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT DISTINCT r.id_structure " +
                "FROM " + Crre.crreSchema + ".rel_group_structure r ";
        sql.prepared(query, idsStructures, SqlResult.validResultHandler(handler));
    }*/

    @Override
    public void delete(final List<Integer> ids, final Handler<Either<String, JsonObject>> handler) {
        JsonArray statements = new fr.wseduc.webutils.collections.JsonArray()
                .add(getStrctureGroupRelationshipDeletion(ids))
                .add(getStructureGroupDeletion(ids));

        sql.transaction(statements, event -> handler.handle(SqlQueryUtils.getTransactionHandler(event,ids.get(0))));
    }

    @Override
    public void listStructureGroupsByCampaign(Integer campaignId, Handler<Either<String, JsonArray>> handler) {
        String query = "SELECT structure.id as id, name, description, array_to_json(array_agg(DISTINCT id_structure)) as structures " +
                " FROM " + Crre.crreSchema + ".structure_group  as structure " +
                " INNER JOIN " + Crre.crreSchema + ".rel_group_campaign" +
                " on structure.id = rel_group_campaign.id_structure_group " +
                " INNER JOIN " + Crre.crreSchema + ".rel_group_structure" +
                " on structure.id = rel_group_structure.id_structure_group " +
                " WHERE rel_group_campaign.id_campaign = ?" +
                " group by (structure.id, name , description ) ";
        JsonArray params = new JsonArray().add(campaignId);

        sql.prepared(query, params, SqlResult.validResultHandler(handler));
    }

    /**
     * Returns a structureGroup creation statement
     * @param id             structureGroup id
     * @param structureGroup structureGroup to create
     * @return structureGroup creation statement
     */
    private JsonObject getStructureGroupCreationStatement(Number id, JsonObject structureGroup){
        String insertStructureGroupQuery = "INSERT INTO "+ Crre.crreSchema +
                ".structure_group(id, name, description) VALUES (?,?,?) RETURNING id;";
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
       .add(id)
       .add(structureGroup.getString("name"))
       .add(structureGroup.getString("description"));
        return new JsonObject()
                .put("statement", insertStructureGroupQuery)
                .put("values",params)
                .put("action","prepared");
    }

    /**
     * Returns  a structureGroup idStructure relationship transaction statement
     * @param idStructureGroup group id
     * @param idsStructure structure ids
     * @return structureGroup idStructure relationship transaction statement
     */
    private JsonObject getGroupStructureRelationshipStatement(Number idStructureGroup, JsonArray idsStructure) {
        StringBuilder insertGroupStructureRelationshipQuery = new StringBuilder();
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();
        insertGroupStructureRelationshipQuery.append("INSERT INTO ").append(Crre.crreSchema)
        .append(".rel_group_structure(id_structure,id_structure_group) VALUES ");

        for(int i = 0; i < idsStructure.size();i++ ){
            String idStructure = idsStructure.getString(i);
            insertGroupStructureRelationshipQuery.append("(?,?)");
            params.add(idStructure)
                    .add(idStructureGroup);
            if(i != idsStructure.size()-1){
                insertGroupStructureRelationshipQuery.append(",");
            }
        }
        insertGroupStructureRelationshipQuery.append(" ON CONFLICT DO NOTHING RETURNING id_structure;");
        return new JsonObject()
                .put("statement",insertGroupStructureRelationshipQuery.toString())
                .put("values",params)
                .put("action","prepared");
    }

    /**
     * Returns the update statement
     * @param id structure_group
     * @param structureGroup to update
     * @return update statement
     */
    private JsonObject getStructureGroupUpdateStatement(Number id, JsonObject structureGroup){
        String query = "UPDATE "+ Crre.crreSchema + ".structure_group " +
                "SET name = ?, description = ? WHERE id = ?;";
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray()
                .add(structureGroup.getString("name"))
                .add(structureGroup.getString("description"))
                .add(id);
        return new JsonObject()
        .put("statement", query)
        .put("values",params)
        .put("action","prepared");
    }

    /**
     * Delete in rel_group_structure
     * @param idStructureGroup of structureGroup
     * @return Delete statement
     */
    private JsonObject getStrctureGroupRelationshipDeletion(Number idStructureGroup){
        String query = "DELETE FROM " + Crre.crreSchema + ".rel_group_structure WHERE id_structure_group = ?;";

        return new JsonObject()
                .put("statement", query)
                .put("values", new fr.wseduc.webutils.collections.JsonArray().add(idStructureGroup))
                .put("action", "prepared");

    }

    /**
     * Delete all ids group in rel_group_structure
     * @param ids list of id group
     * @return Delete statement
     */
    private JsonObject getStrctureGroupRelationshipDeletion(List<Integer> ids){
        String query = "DELETE FROM " + Crre.crreSchema + ".rel_group_structure " +
                "WHERE id_structure_group IN " +Sql.listPrepared(ids.toArray());
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();

        for (Integer id : ids) {
            params.add(id);
        }
        return new JsonObject()
                .put("statement", query)
                .put("values",params)
                .put("action","prepared");

    }

    /**
     * Delete all ids structureGroup in structure_group
     * @param ids list of id_group_structure
     * @return Delete statement
     */
    private JsonObject getStructureGroupDeletion(List<Integer> ids){
        String query = "DELETE FROM "+ Crre.crreSchema +".structure_group " +
                "WHERE id IN "+Sql.listPrepared(ids.toArray());
        JsonArray params = new fr.wseduc.webutils.collections.JsonArray();

        for (Integer id : ids) {
            params.add(id);
        }
        return new JsonObject()
                .put("statement", query)
                .put("values",params)
                .put("action","prepared");
    }

}
