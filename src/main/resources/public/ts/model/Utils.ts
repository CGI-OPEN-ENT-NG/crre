import {moment} from "entcore";

export class Utils {

    static parsePostgreSQLJson (json: any): any {
        try {
            if (json === '[null]') return [];
            let res = JSON.parse(json);
            if (typeof res !== 'string') {
                return res;
            }
            return JSON.parse(res);
        } catch (e) {
            return '';
        }
    }

    static safeApply ($scope: any) {
        let phase = $scope.$root.$$phase;
        if (phase !== '$apply' && phase !== '$digest') {
            $scope.$apply();
        }
    }


    static formatKeyToParameter (values: any[], key: string): string {
        let params: string = '';
        values.map((value) => params += value.hasOwnProperty(key) ? `${key}=${value[key]}&` : '');
        return params.slice(0, -1);
    }

    static calculatePriceTTC (equipment, roundNumber?: number) {
        let prixht,price_TTC: number;
        let tvas;
        if(!equipment){
            return 0;
        }else {
            if (equipment.type == 'articlenumerique') {
                prixht = equipment.offres[0].prixht;
                tvas = equipment.offres[0].tvas;
            } else {
                prixht = equipment.prixht;
                tvas = equipment.tvas;
            }
            price_TTC = prixht;
            tvas.forEach((tva) => {
                let taxFloat = tva.taux;
                price_TTC += (((prixht) * tva.pourcent / 100) * taxFloat) / 100;
            });
            return (!isNaN(price_TTC)) ? (roundNumber ? parseFloat(price_TTC.toFixed(roundNumber)) : price_TTC) : 0;
        }
    }

    static formatGetParameters (obj: any): string {
        let parameters = '';
        Object.keys(obj).map((key) => {
            if (obj[key] == null || obj[key] === undefined) return;
            let type = obj[key].constructor.name;
            switch (type) {
                case 'Array' : {
                    obj[key].map((value) => parameters += `${key}=${value.toString()}&`);
                    break;
                }
                case 'Object': {
                    for (let innerKey in obj[key]) {
                        parameters += `${innerKey}=${obj[key][innerKey].toString()}&`;
                    }
                    break;
                }
                default: {
                    parameters += `${key}=${obj[key].toString()}&`;
                    break;
                }
            }
        });

        return parameters.slice(0, -1);
    }
    static formatDate (date:Date) {
        if (date === null) return '-';
        return moment(date).format('DD/MM/YY');
    }

    static formatDatePost (date:Date) {
        return  moment(date).format('YYYY-MM-DD');
    }

    static generateRegexp (words: string[]): RegExp {
        function escapeRegExp(str: string) {
            return str.replace(/[\-\[\]\/{}()*+?.\\^$|]/g, '\\$&');
        }
        let reg;
        if (words.length > 0) {
            reg = '.*(';
            words.map((word: string) => reg += `${escapeRegExp(word.toLowerCase())}|`);
            reg = reg.slice(0, -1);
            reg += ').*';
        } else {
            reg = '.*';
        }
        return new RegExp(reg);
    }
}