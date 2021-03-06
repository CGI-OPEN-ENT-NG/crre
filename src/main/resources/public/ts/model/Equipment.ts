import {toasts} from 'entcore';
import {Mix, Selectable, Selection} from 'entcore-toolkit';
import http from 'axios';
import {Filters} from "./Filter";

export class Equipment implements Selectable {
    id?: string;
    ean: string;
    summary: string;
    description: string;
    price: number;
    status: string;
    urlcouverture: string;
    technical_specs: TechnicalSpec[];
    selected: boolean;
    _loading: boolean;
    priceTTC?: number;
    disponibilite: any[];
    disciplines: any[];
    discipline: string;
    titre: string;
    ark: string;
    type: string;
    offres: any;
    prixht: number;
    tvas: any;

    constructor () {
        this._loading = false;
        this.technical_specs = [];
    }

    async sync (id, idStructure?:string) {
        try {
            let url = `/crre/equipment/${id}`;
            if(idStructure){
                url +=  `?idStructure=${idStructure}`
            }
            let { data } =  await http.get(url);
             Mix.extend(this, data);
                this.id = this.ean;
                this.status = this.disponibilite[0].valeur;
                if(this.disciplines.length != 0) {
                    this.discipline = this.disciplines[0].libelle;
                }
                if(this.type === 'articlenumerique') {
                    this.offres[0].leps.forEach(function (offre) {
                        offre.conditions.sort(function (a, b) {
                            return a.gratuite - b.gratuite;
                        });
                    });
                }
        } catch (e) {
            console.error(e);
            toasts.warning('crre.equipment.sync.err');
        }
        finally {
            this.loading = false;
        }
    }

    set loading(state: boolean) {
        this._loading = state;
    }

    get loading() {
        return this._loading;
    }

}

export class TechnicalSpec {
    name: string;
    value: string;
    constructor(){
    }
    toJson () {
        return {
            name: this.name,
            value: this.value
        };
    }
    toString () {
        return this.name + ' ' + this.value;
    }
}

export interface Equipments {
    page: number;
    _loading: boolean;
    all: Equipment[];
    page_count: number;
    subjects: String[];
    grades: String[];
    editors: String[];
    os: String[];
    public: String[];
    docsType: any;
    filterFulfilled: boolean;
    distributeurs: String[];
}

export class Equipments extends Selection<Equipment> {
    constructor() {
        super([]);
        this.subjects = [];
        this.grades = [];
        this.os = [];
        this.public = [];
        this.editors = [];
        this.docsType = [];
        this.distributeurs = [];
        this._loading = false;
        this.filterFulfilled = false;
    }

    async syncEquip (data: any) {
        if(data.length > 0 ) {
            if(data[0].hasOwnProperty("ressources")) {
                if(!this.filterFulfilled) {
                    let filters = data[1].filters[0];
                    this.subjects = filters.disciplines.map(v => ({name: v}));
                    this.grades = filters.niveaux.map(v => ({name: v}));
                    this.os = filters.os.map(v => ({name: v}));
                    this.public = filters.public.map(v => ({name: v}));
                    this.editors = filters.editors.map(v => ({name: v}));
                    this.docsType = [{name: "articlepapier"}, {name: "articlenumerique"}];
                    this.distributeurs = filters.distributeurs.map(v => ({name:v}));
                    this.filterFulfilled = true;
                }
                data = data[0].ressources;
            }
            this.all = Mix.castArrayAs(Equipment, data);
            this.all.map((equipment) => {
                equipment.id = equipment.ean;
                equipment.status = equipment.disponibilite[0].valeur;
                if(equipment.disciplines.length != 0) {
                    equipment.discipline = equipment.disciplines[0].libelle;
                }
            });
        } else {
            this.all = [];
        }

    }

    async getFilterEquipments(queryword?: string, filters?: Filters){
        try {
            let uri: string;
            let params = "";
            var format = /^[`@#$%^&*()_+\-=\[\]{};:"\\|,.<>\/?~]/;
            if(filters) {
                params = "&";
                filters.all.forEach(function (f) {
                    params += f.name + "=" + f.value + "&";
                });
            }
            if(!format.test(queryword)) {
                    if(!!queryword) {
                        uri = (`/crre/equipments/catalog/search?word=${queryword}${params}`);
                    } else {
                        uri = (`/crre/equipments/catalog/filter?emptyFilter=${!this.filterFulfilled}${params}`);
                    }
                let {data} = await http.get(uri);
                this.syncEquip(data);
            } else {
                toasts.warning('crre.equipment.special');
            }

        } catch (e) {
            toasts.warning('crre.equipment.sync.err');
            throw e;
        } finally {
            this.loading = false;
        }
    }

    async sync() {
        try {
            let {data} = await http.get(`/crre/equipments/catalog`);
            this.syncEquip(data);

        } catch (e) {
            toasts.warning('crre.equipment.sync.err');
            throw e;
        } finally {
            this.loading = false;
        }
    }

    set loading(state: boolean) {
        this._loading = state;
    }

    get loading() {
        return this._loading;
    }
}

export class Offer {
    name: string;
    value: string;
}

export class Offers {
    all: Offer[];

    constructor() {
        this.all = [];
    }
}