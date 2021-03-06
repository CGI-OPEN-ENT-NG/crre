package fr.openent.crre.security;

import fr.openent.crre.Crre;
import fr.openent.crre.helpers.MongoHelper;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.http.Binding;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.http.filter.ResourcesProvider;
import org.entcore.common.user.UserInfos;

public class AccessExportDownload implements ResourcesProvider {
    @Override
    public void authorize(HttpServerRequest request, Binding binding, UserInfos userInfos, Handler<Boolean> handler) {
        request.pause();
        String id;
        MongoHelper mongo = new MongoHelper(Crre.CRRE_COLLECTION);
        id = request.getParam("fileId");
        if (id != null && WorkflowActionUtils.hasRight(userInfos, WorkflowActions.ADMINISTRATOR_RIGHT.toString())){
            JsonObject params = new JsonObject();
            params.put("fileId",id);
            params.put("userId",userInfos.getUserId());
           mongo.getExport(params, event -> {
               if (event.isRight()) {
                   request.resume();
                   JsonArray result = event.right().getValue();
                   handler.handle(result.size() == 1 && WorkflowActionUtils.hasRight(userInfos, WorkflowActions.ADMINISTRATOR_RIGHT.toString()));
               } else {
                   request.response().setStatusCode(500).end();
               }
           });
        } else {
            request.response().setStatusCode(400).end();
        }
    }
}
