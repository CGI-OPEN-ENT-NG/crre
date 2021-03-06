import {Behaviours, model, ng, routes} from 'entcore';
import * as controllers from './controllers';
import * as directives from './directives';
import * as filters from './filters';

for (let controller in controllers) {
    ng.controllers.push(controllers[controller]);
}

for (let directive in directives) {
    ng.directives.push(directives[directive]);
}

for (let filter in filters) {
    ng.filters.push(filters[filter]);
}

routes.define(($routeProvider) => {
    $routeProvider
        .when('/', {
            action: 'main'
        });
    if (model.me.hasWorkflow(Behaviours.applicationsBehaviours.crre.rights.workflow.administrator)) {
        $routeProvider
            .when('/equipments/catalog', {
                action: 'showAdminCatalog'
            })
            .when('/campaigns/create', {
                action: 'createOrUpdateCampaigns'
            })
            .when('/campaigns/update', {
                action: 'createOrUpdateCampaigns'
            })
            .when('/logs', {
                action: 'viewLogs'
            })
            .when('/structureGroups/create', {
                action: 'createStructureGroup'
            })
            .when('/structureGroups', {
                action: 'manageStructureGroups'
            })
            .when('/purses', {
                action: 'managePurse'
            })
            .when('/equipments/catalog/equipment/:idEquipment/:idCampaign', {
                action: 'adminEquipmentDetail'
            })
            .when('/order/historic', {
                action: 'orderHistoricAdmin'
            })
            .when('/order/waiting', {
                action: 'orderWaitingAdmin'
            })
            .when('/campaigns', {
                action: 'manageCampaigns'
            })
            .when('/exports', {
                action: 'exportList'
            });
    } else {
        $routeProvider
            .when('/equipments/catalog/:idCampaign', {
                action: 'showCatalog'
            })
            .when('/equipments/catalog/equipment/:idEquipment/:idCampaign', {
                action: 'equipmentDetail'
            });
    }
    if (model.me.hasWorkflow(Behaviours.applicationsBehaviours.crre.rights.workflow.validator) &&
    !model.me.hasWorkflow(Behaviours.applicationsBehaviours.crre.rights.workflow.administrator)) {
        $routeProvider
            .when('/order/:idCampaign/waiting', {
                action: 'orderWaiting'
            })
            .when('/order/:idCampaign/historic', {
                action: 'orderHistoric'
            });

    }
    if (model.me.hasWorkflow(Behaviours.applicationsBehaviours.crre.rights.workflow.prescriptor) &&
    !model.me.hasWorkflow(Behaviours.applicationsBehaviours.crre.rights.workflow.administrator)) {
        $routeProvider
            .when('/campaign/:idCampaign/order', {
                action: 'campaignOrder'
            })
            .when('/campaign/:idCampaign/basket', {
                action: 'campaignBasket'
            });
    }
    $routeProvider.otherwise({
        redirectTo: '/'
    });
});