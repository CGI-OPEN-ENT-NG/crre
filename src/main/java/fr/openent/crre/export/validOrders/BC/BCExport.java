package fr.openent.crre.export.validOrders.BC;

import fr.openent.crre.export.validOrders.PDF_OrderHElper;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import java.util.*;

public class BCExport extends PDF_OrderHElper {
    private Logger log = LoggerFactory.getLogger(BCExport.class);


    public BCExport(EventBus eb, Vertx vertx, JsonObject config)

    {
        super(eb,vertx,config);

    }


    public void create(JsonArray validationNumbersArray, Handler<Either<String, Buffer>> exportHandler){
        List validationNumbers = validationNumbersArray.getList();
        supplierService.getSupplierByValidationNumbers(new fr.wseduc.webutils.collections.JsonArray(validationNumbers), event -> {
            if (event.isRight()) {
                JsonObject supplier = event.right().getValue();
                getOrdersData( exportHandler,"", "", "", supplier.getInteger("id"), new fr.wseduc.webutils.collections.JsonArray(validationNumbers),false,
                        data -> {
                            data.put("print_order", true);
                            data.put("print_certificates", false);
                            generatePDF(exportHandler, null, data,
                                    "BC.xhtml",
                                    pdf -> exportHandler.handle(new Either.Right(pdf))
                            );
                        });
            }else {
                log.error("error when getting supplier");
            }
        });
    }
}
