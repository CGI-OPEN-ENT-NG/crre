package fr.openent.crre;

import fr.openent.crre.controllers.*;
import fr.openent.crre.cron.synchTotalStudents;
import fr.openent.crre.export.ExportCrreWorker;
import fr.wseduc.cron.CronTrigger;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import org.entcore.common.http.BaseServer;
import org.entcore.common.storage.Storage;
import org.entcore.common.storage.StorageFactory;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;

public class Crre extends BaseServer {

    public static final String ORDERSSENT ="ORDERSSENT" ;
    public static String crreSchema;
    public static Integer iterationWorker;
    public static JsonObject CONFIG;
    public static Storage STORAGE;
    public static Integer PAGE_SIZE = 50;
    public static final String CRRE_COLLECTION = "crre_export";
    public static final String ADMINISTRATOR_RIGHT = "crre.administrator";
    public static final String VALIDATOR_RIGHT = "crre.validator";
    public static final String PRESCRIPTOR_RIGHT = "crre.prescriptor";
    public static final String REASSORT_RIGHT = "crre.reassort";
    public static final String UPDATE_STUDENT_RIGHT = "crre.updateStudent";
    public static long timeout = 99999999999L;
    public static final String ORDERS = "ORDERS";
    public static final String INSTRUCTIONS = "INSTRUCTION";
    public static final String XLSX = "xlsx";
    public static final String PDF = "pdf";


    @Override
    public void start() throws Exception {
        super.start();
        crreSchema = config.getString("db-schema");
        if(config.containsKey("iteration-worker")){
            iterationWorker = config.getInteger("iteration-worker");
        }else{
            log.info("no iteration worker in config");
            iterationWorker = 10 ;
        }
        EventBus eb = getEventBus(vertx);
        Storage storage = new StorageFactory(vertx, config).getStorage();
        STORAGE = storage;
        JsonObject mail = config.getJsonObject("mail", new JsonObject());

        try {
            new CronTrigger(vertx, config.getString("timeSecondSynchCron")).schedule(
                    new synchTotalStudents(vertx, new CrreController())
            );
        } catch (Exception e) {
            log.fatal("Invalid CRRE cron expression.", e);
        }

        addController(new CrreController());
        addController(new EquipmentController());
        addController(new LogController());
        addController(new CampaignController());
        addController(new PurseController(vertx));
        addController(new StructureGroupController(vertx));
        addController(new StructureController());
        addController(new BasketController(vertx, config.getJsonObject("slack", new JsonObject()), mail));
        addController(new OrderController(storage, vertx, config, eb));
        addController(new UserController());
        addController(new OrderRegionController(vertx, config));
        addController(new ExportController(storage));
        CONFIG = config;
        vertx.deployVerticle(ExportCrreWorker.class, new DeploymentOptions().setConfig(config).setWorker(true));
        launchWorker(eb);

    }

    public static void launchWorker(EventBus eb) {
        eb.send(ExportCrreWorker.class.getSimpleName(), new JsonObject(), new DeliveryOptions().setSendTimeout(1000 * 1000L), handlerToAsyncHandler(eventExport ->{
                    if(!eventExport.body().getString("status").equals("ok"))
                        launchWorker(eb);
                    log.info("Ok calling worker " + eventExport.body().toString());
                }
        ));
    }
}
