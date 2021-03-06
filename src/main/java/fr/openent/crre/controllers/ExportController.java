package fr.openent.crre.controllers;

import fr.openent.crre.Crre;
import fr.openent.crre.logging.Actions;
import fr.openent.crre.logging.Contexts;
import fr.openent.crre.logging.Logging;
import fr.openent.crre.security.AccessExportDownload;
import fr.openent.crre.security.AdministratorRight;
import fr.openent.crre.service.ExportService;
import fr.openent.crre.service.impl.DefaultExportServiceService;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Delete;
import fr.wseduc.rs.Get;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.storage.Storage;
import org.entcore.common.user.UserUtils;

import static fr.wseduc.webutils.http.response.DefaultResponseHandler.arrayResponseHandler;

public class ExportController extends ControllerHelper {
    private final ExportService exportService;

    public ExportController(Storage storage) {
        super();
        this.exportService = new DefaultExportServiceService(storage);
    }

    @Get("/exports")
    @ApiDoc("Returns all exports in database filtered by owner")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void getExports(HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> exportService.getExports(arrayResponseHandler(request), user));
    }

    @Get("/export/:fileId")
    @ApiDoc("Returns all exports in database filtered by owner")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AccessExportDownload.class)
    public void getExport(HttpServerRequest request) {
        String fileId = request.getParam("fileId");
        exportService.getExportName(fileId, event -> {
            if(event.isRight()) {
                if(event.right().getValue().getJsonObject(0).getString("extension").equals(Crre.PDF)){
                    exportService.getExport(fileId, file ->
                            request.response()
                                    .putHeader("Content-Type", "application/pdf; charset=utf-8")
                                    .putHeader("Content-Length", file.length() + "")
                                    .putHeader("Content-Disposition", "filename=" + event.right().getValue().getJsonObject(0).getString("filename"))
                                    .end(file));
                }else {
                    exportService.getExport(fileId, file ->
                            request.response()
                                    .putHeader("Content-type", "application/application/vnd.openxmlformats-officedocument.spreadsheetml.sheet; charset=utf-8")
                                    .putHeader("Content-Length", file.length() + "")
                                    .putHeader("Content-Disposition", "filename=" + event.right().getValue().getJsonObject(0).getString("filename"))
                                    .end(file));
                }
            } else
                badRequest(request);
        });

    }

    @Delete("/exports")
    @ApiDoc("Delete all exports and files")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(AdministratorRight.class)
    public void deleteExportExcel(HttpServerRequest request) {
        RequestUtils.bodyToJson( request, ids -> exportService.deleteExport( ids.getJsonArray("idsFiles"), event -> {
            if (event.getString("status").equals("ok")) {
                exportService.deleteExportMongo( ids.getJsonArray("idsExport"), Logging.defaultResponseHandler(eb,
                        request,
                        Contexts.EXPORT.toString(),
                        Actions.DELETE.toString(),
                        ids.getJsonArray("idsExport").toString(),
                        new JsonObject().put("ids", ids)));
            } else {
                badRequest(request);
                log.error("Erreur deleting file in storage");
                log.error(event.getString("message"));
            }
        }));
    }
}
