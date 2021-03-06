import {_, model, moment, toasts} from 'entcore';
import {Mix, Selection} from 'entcore-toolkit';
import {
    Campaign,
    Equipment, Filter,
    Order,
    OrderRegion,
    OrderUtils,
    Structure,
    Structures,
    TechnicalSpec,
    Utils
} from './index';
import http from 'axios';

export class OrderClient implements Order  {

    amount: number;
    campaign: Campaign;
    comment: string;
    creation_date: Date;
    equipment: Equipment;
    equipment_key:number;
    id?: number;
    id_structure: string;
    inheritedClass:Order|OrderClient|OrderRegion;
    order_parent?:any;
    price: number;
    selected:boolean;
    structure: Structure;
    typeOrder:string;
    total?:number;
    action?:string;
    cause_status?:string;
    description:string;
    files;
    id_campaign:number;
    id_order:number;
    id_project:number;
    id_basket:number;
    name:string;
    name_structure: string;
    priceTotalTTC: number;
    structure_groups: any;
    summary:string;
    image:string;
    status:string;
    technical_spec:TechnicalSpec;
    user_name:string;
    user_id:string;
    grade: string;
    reassort: boolean;

    constructor() {
        this.typeOrder= "client";
    }

    async updateComment():Promise<void>{
        try{
            http.put(`/crre/order/${this.id}/comment`, { comment: this.comment });
        }catch (e){
            toasts.warning('crre.basket.update.err');
            throw e;
        }
    }

    async delete ():Promise<any> {
        try {
            return await http.delete(`/crre/order/${this.id}/${this.id_structure}/${this.id_campaign}`);
        } catch (e) {
            toasts.warning('crre.order.delete.err');
        }
    }

    async updateAmount(amount: number):Promise<void>{
        try {
            await http.put(`/crre/order/${this.id}/amount`,{ amount: amount });
        }
        catch {
            toasts.warning('crre.order.getMine.err');
        }
    }

    async updateReassort():Promise<void>{
        try {
            await http.put(`/crre/order/${this.id}/reassort`,{ reassort: this.reassort });
        }
        catch {
            toasts.warning('crre.order.getMine.err');
        }
    }

    async updateStatusOrder(status: String, id:number = this.id):Promise<void>{
        try {
            await http.put(`/crre/order/${id}`, {status: status});
        } catch (e) {
            toasts.warning('crre.order.update.err');
        }
    }

    static formatSqlDataToModel(data: any):any {
        return {
            action: data.action,
            amount: data.amount,
            cause_status: data.cause_status,
            comment: data.comment,
            creation_date: moment(data.creation_date).format('DD-MM-YYYY').toString(),
            description: data.description,
            equipment_key: data.equipment_key,
            id: data.id,
            id_campaign: data.id_campaign,
            id_contract: data.id_contract,
            id_operation: data.id_operation,
            id_order: data.id_order,
            id_project: data.id_project,
            id_structure: data.id_structure,
            image: data.image,
            name: data.name,
            price: data.price,
            program: data.program,
            status: data.status,
            summary: data.summary,
            tax_amount: data.tax_amount
        }
            ;
    }

    toJson():any {
        return {
            amount: this.amount,
            price: this.price,
            creation_date: moment().format('YYYY-MM-DD'),
            status: this.status,
            files: this.files,
            name_structure: this.name_structure,
            id_campaign: this.id_campaign,
            id_structure: this.id_structure,
            id_project: this.id_project,
            equipment_key: this.equipment_key,
            comment: (this.comment) ? this.comment : "",
            user_name: this.user_name,
            user_id: this.user_id,
            reassort: this.reassort,
            priceTotalTTC: this.priceTotalTTC
        }
    }
}
export class OrdersClient extends Selection<OrderClient> {

    dateGeneration?: Date;
    filters: Array<string>;

    constructor() {
        super([]);
        this.dateGeneration = new Date();
        this.filters = [];
    }

    async search(text: String, id_campaign: number, page?: number) {
        try {
            if ((text.trim() === '' || !text)) return;
            let params = "";
            if(id_campaign)
                params += `&id=${id_campaign}`
            if(page)
                params = `&page=${page}`;
            const {data} = await http.get(`/crre/orders/search?q=${text}${params}`);
            let newOrderClient = Mix.castArrayAs(OrderClient, data);
            if(newOrderClient.length>0) {
                await this.getEquipments(newOrderClient).then(equipments => {
                    for (let order of newOrderClient) {
                        let equipment = equipments.data.find(equipment => order.equipment_key == equipment.id);
                        order.priceTotalTTC = Utils.calculatePriceTTC(equipment, 2) * order.amount;
                        order.price = Utils.calculatePriceTTC(equipment,2);
                        order.name = equipment.titre;
                        order.image = equipment.urlcouverture;
                        order.creation_date = moment(order.creation_date).format('L');
                    }
                });
                this.all = this.all.concat(newOrderClient);
                return true;
            }
        } catch (err) {
            toasts.warning('crre.basket.sync.err');
            throw err;
        }
    }

    async filter_order(filters: Filter[], id_campaign: number, word?: string, page?: number){
        try {
            let format = /^[`@#$%^&*()_+\-=\[\]{};:"\\|,.<>\/?~]/;
            let params = "";
            if(id_campaign)
                params += `&id=${id_campaign}`
            filters.forEach(function (f, index) {
                params += "&" + f.name + "=" + f.value;
            });
            let pageParams = "";
            if(page)
                pageParams = `&page=${page}`;
            let url;
            if(!format.test(word)) {
                if(word) {
                    url = `/crre/orders/filter?q=${word}${params}${pageParams}`;
                } else {
                    url = `/crre/orders/filter?${params.substring(1)}${pageParams}`;
                }
                let {data} = await http.get(url);
                let newOrderClient = Mix.castArrayAs(OrderClient, data);
                if(newOrderClient.length>0) {
                    await this.getEquipments(newOrderClient).then(equipments => {
                        for (let order of newOrderClient) {
                            let equipment = equipments.data.find(equipment => order.equipment_key == equipment.id);
                            order.priceTotalTTC = Utils.calculatePriceTTC(equipment, 2) * order.amount;
                            order.price = Utils.calculatePriceTTC(equipment,2);
                            order.name = equipment.titre;
                            order.image = equipment.urlcouverture;
                            order.creation_date = moment(order.creation_date).format('L');
                        }
                    });
                    this.all = this.all.concat(newOrderClient);
                    return true;
                }
            } else {
                toasts.warning('crre.equipment.special');
            }
        } catch (e) {
            toasts.warning('crre.equipment.sync.err');
            throw e;
        } finally {
        }
    }

    async sync (status: string, structures: Structures = new Structures(), idCampaign?: number, idStructure?: string, ordersId?, page?:number):Promise<boolean> {
        try {
            if (idCampaign && idStructure) {
                let params = '';
                if(ordersId) {
                    params += '?';
                    ordersId.map((order) => {
                        params += `order_id=${order}&`;
                    });
                    params = params.slice(0, -1);
                }
                const { data } = await http.get(  `/crre/orders/mine/${idCampaign}/${idStructure}${params}` );
                this.all = Mix.castArrayAs(OrderClient, data);
                this.syncWithIdsCampaignAndStructure();
                return true;
            } else {
                let pageParams = '';
                if(page)
                    pageParams = `&page=${page}`;
                const { data } = await http.get(  `/crre/orders?status=${status}${pageParams}`);
                let newOrderClient = Mix.castArrayAs(OrderClient, data);
                if(newOrderClient.length>0) {
                    await this.getEquipments(newOrderClient).then(equipments => {
                        for (let order of newOrderClient) {
                            let equipment = equipments.data.find(equipment => order.equipment_key == equipment.id);
                            order.priceTotalTTC = Utils.calculatePriceTTC(equipment, 2) * order.amount;
                            order.price = Utils.calculatePriceTTC(equipment,2);
                            order.name = equipment.titre;
                            order.image = equipment.urlcouverture;
                            order.name_structure = structures && structures.length > 0 ? OrderUtils.initNameStructure(order.id_structure, structures) : '';
                            order.structure = structures && structures.length > 0 ? OrderUtils.initStructure(order.id_structure, structures) : new Structure();
                            order.structure_groups = Utils.parsePostgreSQLJson(order.structure_groups);
                            order.files = order.files !== '[null]' ? Utils.parsePostgreSQLJson(order.files) : [];
                            if (order.files.length > 1)
                                order.files.sort(function (a, b) {
                                    return a.filename.localeCompare(b.filename);
                                });
                            if (status !== 'VALID') {
                                this.makeOrderNotValid(order);
                            }
                            order.creation_date = moment(order.creation_date, 'DD/MM/YYYY').format('DD-MM-YYYY');
                        }
                    });
                    this.all = this.all.concat(newOrderClient);
                    return true;
                }
            }
        } catch (e) {
            toasts.warning('crre.order.sync.err');
        }
    }

    async getUsers (status: string):Promise<boolean> {
        const { data } = await http.get(  `/crre/orders/users?status=${status}`);
        return data;
    }

    getEquipments (orders):Promise<any>{
        let params = '';
        orders.map((order) => {
            params += `order_id=${order.equipment_key}&`;
        });
        params = params.slice(0, -1);
        return http.get(`/crre/equipments?${params}`);
    }

    syncWithIdsCampaignAndStructure():void{
        this.all.map((order) => {
            order.price = parseFloat(order.price.toString());
            order.files = order.files !== '[null]' ? Utils.parsePostgreSQLJson(order.files) : [];
        });
    }

    makeOrderNotValid(order:OrderClient):void{
        order.campaign = Mix.castAs(Campaign,  JSON.parse(order.campaign.toString()));
        order.creation_date = moment(order.creation_date).format('L');
        order.priceTotalTTC = this.choosePriceTotal(order);
    }

    choosePriceTotal(order:OrderClient):number{
        return parseFloat((OrderUtils.calculatePriceTTC(2, order) as number).toString()) * order.amount;
    }

    toJson (status: string):any {
        const ids = _.pluck(this.all, 'id');
        return {
            ids,
            status : status,
            dateGeneration: moment(this.dateGeneration).format('DD/MM/YYYY') || null,
            userId : model.me.userId
        };
    }

    async updateStatus(status: string):Promise<any> {
        try {
            let statusURL = status;
            if (status === "IN PROGRESS") {
                statusURL = "inprogress";
            }
            let config = status === 'SENT' ? {responseType: 'arraybuffer'} : {};
            return await  http.put(`/crre/orders/${statusURL.toLowerCase()}`, this.toJson(status), config);
        } catch (e) {
            toasts.warning('crre.order.update.err');
            throw e;
        }
    }

    calculTotalAmount ():number {
        let total = 0;
        this.all.map((order) => {
            total += order.amount;
        });
        return total;
    }

    calculTotalPriceTTC ():number {
        let total = 0;
        for (let i = 0; i < this.all.length; i++) {
            let order = this.all[i];
            total += order.price*order.amount;
        }
        return total;
    }
}
