import {_, ng, template} from 'entcore';
import {Basket, Campaign, Equipment, Filter, Filters, Offer, Offers, Utils} from '../../model';

export const catalogController = ng.controller('catalogController',
    ['$scope', '$routeParams', ($scope) => {
        this.init = () => {
            $scope.pageSize = 20;
            $scope.nbItemsDisplay = $scope.pageSize;
            $scope.alloptionsSelected = false;
            $scope.equipment = new Equipment();
            $scope.subjects = [];
            $scope.loading = true;
            $scope.labels = ["technologie", "dispositifDYS", "webAdaptatif", "exercicesInteractifs", "availableViaENT", "availableViaGAR", "canUseOffline", "needFlash", "corrigesPourEnseignants"];
            $scope.initPopUpFilters();
            $scope.filters = new Filters();
            if($scope.isAdministrator()){
                $scope.goBackUrl = "/equipments/catalog";
            }else if($scope.hasAccess() && !$scope.isValidator() && !$scope.isPrescriptor()){
                $scope.goBackUrl = "/equipments/catalog/0";
            }else{
                $scope.goBackUrl = "/equipments/catalog/" + $scope.campaign.id;
            }
        };

        $scope.addFilter = async () => {
            $scope.query.word = $scope.queryWord;
            $scope.nbItemsDisplay = $scope.pageSize;
            $scope.equipments.all = [];
            $scope.equipments.loading = true;
            Utils.safeApply($scope);
            await $scope.equipments.getFilterEquipments($scope.query.word, $scope.filters);
            Utils.safeApply($scope);
        };

        $scope.getFilter = async (word: string, filter: string) => {
            $scope.nbItemsDisplay = $scope.pageSize;
            $scope.equipments.all = [];
            $scope.equipments.loading = true;
            Utils.safeApply($scope);
            let newFilter = new Filter();
            newFilter.name = filter;
            newFilter.value = word;
            if ($scope.filters.all.some(f => f.value === word)) {
                $scope.filters.all.splice($scope.filters.all.findIndex(a => a.value === word) , 1);
            } else {
                $scope.filters.all.push(newFilter);
            }
            if($scope.filters.all.length > 0) {
                await $scope.equipments.getFilterEquipments($scope.query.word, $scope.filters);
                Utils.safeApply($scope);
            } else {
                await $scope.equipments.getFilterEquipments($scope.query.word);
                Utils.safeApply($scope);
            }
        };

        $scope.validArticle = () => {
            return $scope.basket.amount > 0;
        };

        $scope.computeOffer = () => {
            let amount = $scope.basket.amount;
            let gratuit = 0;
            let gratuite = 0;
            let offre = null;
            $scope.offers = new Offers();
            $scope.basket.equipment.offres[0].leps.forEach(function (offer) {
                offre = new Offer();
                offre.name = offer.licence[0].valeur;
                if(offer.conditions.length > 1) {
                    offer.conditions.forEach(function (condition) {
                        if(amount >= condition.conditionGratuite && gratuit < condition.conditionGratuite) {
                            gratuit = condition.conditionGratuite;
                            gratuite = condition.gratuite;
                        }
                    });
                } else {
                    gratuit = offer.conditions[0].conditionGratuite;
                    gratuite = offer.conditions[0].gratuite * Math.floor(amount/gratuit);
                }
                offre.value = gratuite;
                $scope.offers.all.push(offre);
            });
            return $scope.offers;
        };


        $scope.switchAll = (model: boolean, collection) => {
            collection.forEach((col) => {col.selected = col.required ? false : col.selected = model; });
            Utils.safeApply($scope);
        };

        $scope.chooseCampaign = async () => {
            await $scope.initStructures();
            await $scope.initCampaign($scope.current.structure);
            template.open('campaign.name', 'customer/campaign/basket/campaign-name-confirmation');
            $scope.display.lightbox.choosecampaign = true;
            Utils.safeApply($scope);
        };

        $scope.cancelChooseCampaign = () => {
            $scope.display.lightbox.choosecampaign = false;
            template.close('campaign.name');
            Utils.safeApply($scope);
        };

        $scope.setCampaignId = (campaign: Campaign) => {
          $scope.campaign = campaign;
        }

        $scope.formatGrade = (grades: any[]) => {
            let grade_string = "";
            grades.forEach(function(grade, index) {
               grade_string += grade.libelle;
               if(grades.length - 1 != index) {
                   grade_string += ", ";
               }
            });
            return grade_string;
        }

        $scope.addBasketItem = async (basket: Basket, campaign?: Campaign, id_structure?: string) => {
            if(basket.id_campaign === undefined && campaign.accessible) {
                basket.id_campaign = campaign.id;
                basket.id_structure= id_structure;
                $scope.$emit('eventEmitedCampaign', campaign);
                $scope.campaign = campaign;
                $scope.display.lightbox.choosecampaign = false;
            }
            let { status } = await basket.create();
            if (status === 200 && basket.amount > 0 ) {
                if( $scope.campaign.nb_panier)
                    $scope.campaign.nb_panier += 1;
                else
                    $scope.campaign.nb_panier = 1;
                await $scope.notifyBasket('added', basket);
            }

            Utils.safeApply($scope);
        };
        $scope.amountIncrease = () => {
            $scope.basket.amount += 1;
            if($scope.basket.equipment.type === 'articlenumerique') {
                $scope.computeOffer();
            }
        };
        $scope.amountDecrease = () => {
            if($scope.basket.amount)
                $scope.basket.amount -= 1;
            if($scope.basket.equipment.type === 'articlenumerique') {
                $scope.computeOffer();
            }
        };

        $scope.durationFormat = (nbr : number) => {
            if(nbr == 0)
                return "Illimitée";
            else if(nbr == 1)
                return nbr.toString() + " année scolaire";
            else
                return nbr.toString() + " années scolaires";
        };

        $scope.initPopUpFilters = (filter?:string) => {
            let value = $scope.$eval(filter);

            $scope.showPopUpColumnsGrade = $scope.showPopUpColumnsEditor = $scope.showPopUpColumnsSubject =
                $scope.showPopUpColumnsOS = $scope.showPopUpColumnsDocumentsTypes = $scope.showPopUpColumnsPublic = false;

            if (!value) {
                switch (filter) {
                    case 'showPopUpColumnsSubject': $scope.showPopUpColumnsSubject = true; break;
                    case 'showPopUpColumnsPublic': $scope.showPopUpColumnsPublic = true; break;
                    case 'showPopUpColumnsGrade': $scope.showPopUpColumnsGrade = true; break;
                    case 'showPopUpColumnsDocumentsTypes': $scope.showPopUpColumnsDocumentsTypes = true; break;
                    case 'showPopUpColumnsEditor': $scope.showPopUpColumnsEditor = true; break;
                    case 'showPopUpColumnsOS': $scope.showPopUpColumnsOS = true; break;
                    default: break;
                }
            }
        };

        this.init();
    }]);