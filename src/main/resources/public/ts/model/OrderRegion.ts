import http from "axios";
import {_, moment, toasts} from "entcore";
import {
    Campaign,
    Order,
    Structure,
    TechnicalSpec,
    OrderClient,
    Equipment
} from "./index";
import {Mix, Selection} from "entcore-toolkit";


export class OrderRegion implements Order  {
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
    description:string;
    files: string;
    id_campaign:number;
    id_orderClient: number;
    id_project:number;
    name:string;
    name_structure: string;
    order_client: OrderClient;
    structure_groups: any;
    summary:string;
    image:string;
    status:string;
    technical_spec:TechnicalSpec;
    title_id ?: number;
    user_name:string;
    user_id:string;
    reassort: boolean;
    priceTotalTTC ?: number;

    constructor() {
        this.typeOrder = "region";
    }

    toJson():any {
        return {
            amount: this.amount,
            price: this.price,
            ...(this.id_orderClient && {id_order_client_equipment: this.id_orderClient}),
            creation_date: moment().format('YYYY-MM-DD'),
            status: this.status,
            ...(this.title_id && {title_id: this.title_id}),
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

    createFromOrderClient(order: OrderClient):void {
        this.order_client = order;
        this.id_orderClient = order.id;
        this.amount = order.amount;
        this.name = order.name;
        this.summary = order.summary;
        this.description = order.description;
        this.image = order.image;
        this.creation_date = order.creation_date;
        this.status = order.status;
        this.technical_spec = order.technical_spec;
        this.campaign = order.campaign;
        this.structure_groups = order.structure_groups;
        this.files = order.files;
        this.name_structure = order.name_structure;
        this.id_campaign = order.id_campaign;
        this.id_structure = order.id_structure;
        this.id_project = order.id_project;
        this.comment = order.comment;
        this.price = order.price;
        this.structure = order.structure;
        this.equipment_key = order.equipment_key;
        this.user_name = order.user_name;
        this.user_id = order.user_id;
        this.reassort = order.reassort;
    }
}

export class OrdersRegion extends Selection<OrderRegion> {
    constructor() {
        super([]);
    }

    async create():Promise<any> {
        let orders = [];
        this.all.map(order => {
            orders.push(order.toJson());
        });
        try {
            return await http.post(`/crre/region/orders/`, {orders: orders});
        } catch (e) {
            toasts.warning('crre.order.create.err');
            throw e;
        }
    }

    toJson (status: string, justification:string):any {
        const ids = _.pluck(this.all, 'id');
        let orders = [];
        this.all.map(order => {
            orders.push(order.toJson());
        });
        return {
            ids,
            orders: orders,
            status : status,
            justification : justification
        };
    }

    async updateStatus(status: string, justification?:string):Promise<any> {
        try {
            if(!justification)
                justification="";
            return await  http.put(`/crre/region/orders/${status.toLowerCase()}`, this.toJson(status,justification));
        } catch (e) {
            toasts.warning('crre.order.update.error');
            throw e;
        }
    }
}