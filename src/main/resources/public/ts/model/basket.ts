import {Mix, Selectable, Selection} from 'entcore-toolkit';
import {_, toasts} from 'entcore';
import http from 'axios';
import {Equipment, EquipmentOption, Filter, Structure, Utils} from './index';


export class Basket implements Selectable {
    id?: number;
    amount: number;
    processing_date: string| Date;
    equipment: Equipment ;
    options: EquipmentOption[];
    id_campaign: number;
    id_structure: string;
    selected: boolean;
    comment?: string;
    price_proposal?: number;
    price_editable: boolean;
    display_price_editable: boolean;
    basket_name: string;
    reassort:boolean;


    files?: any;

    constructor (equipment: Equipment , id_campaign?: number, id_structure?: string ) {
        this.equipment = Mix.castAs(Equipment, equipment) ;
        this.id_campaign = id_campaign;
        this.id_structure = id_structure;
        this.amount = 1;
        this.display_price_editable = false;
    }

    toJson () {
        let options = _.filter( this.equipment.options , function(option) { return option.required || option.selected ; });
        return {
            amount: this.amount,
            processing_date : this.processing_date,
            equipment : this.equipment.id,
            options: options.length > 0 ?  _.pluck( options , 'id') : null ,
            id_campaign : this.id_campaign,
            id_structure : this.id_structure,
        };
    }

    async save () {
        if (this.id) {
            this.update();
        } else {
            this.create();
        }
    }

    async create () {
        try {
            return await  http.post(`/crre/basket/campaign/${this.id_campaign}`, this.toJson());
        } catch (e) {
            toasts.warning('crre.basket.create.err');
        }
    }

    async update () {
        try {
            http.put(`/crre/basket/${this.id}`, this.toJson());
        } catch (e) {
            toasts.warning('crre.basket.update.err');
            throw e;
        }
    }

    async updateAmount () {
        try {
            http.put(`/crre/basket/${this.id}/amount`, this.toJson());
        } catch (e) {
            toasts.warning('crre.basket.update.err');
            throw e;
        }
    }

    async updateComment(){
        try{
            http.put(`/crre/basket/${this.id}/comment`, { comment: this.comment });
        }catch (e){
            toasts.warning('crre.basket.update.err');
            throw e;
        }
    }

    async updateReassort(){
        try{
            http.put(`/crre/basket/${this.id}/reassort`, { reassort: this.reassort });
        }catch (e){
            toasts.warning('crre.basket.update.err');
            throw e;
        }
    }

    async updatePriceProposal() {
        try {
            http.put(`/crre/basket/${this.id}/priceProposal`, {price_proposal: this.price_proposal});
        } catch (e) {
            toasts.warning('crre.basket.update.err');
            throw e;
        }
    }

    async delete () {
        try {
            return await  http.delete(`/crre/basket/${this.id}/campaign/${this.id_campaign}`);
        } catch (e) {
            toasts.warning('crre.basket.delete.err');
        }
    }

    async deleteDocument(file) {
        try {
            await http.delete(`/crre/basket/${this.id}/file/${file.id}`);
        } catch (err) {
            throw err;
        }
    }

    downloadFile(file) {
        window.open(`/crre/basket/${this.id}/file/${file.id}`);
    }

    amountIncrease = () => {
        this.amount += 1;
    };

    amountDecrease = () => {
        if(this.amount)
            this.amount -= 1;
    };

}

export class Baskets extends Selection<Basket> {
    basketsToOrder: Selection<Basket>
    constructor() {
        super([]);
        this.basketsToOrder = new Selection<Basket>([]);
    }

    async sync (idCampaign: number, idStructure: string ) {
        try {
            let { data } = await http.get(`/crre/basket/${idCampaign}/${idStructure}`);
            this.all = Mix.castArrayAs(Basket, data);
            this.all.map((basket) => {
                basket.equipment = Mix.castAs(Equipment, basket.equipment);
            });
        } catch (e) {
            toasts.warning('crre.basket.sync.err');
        }
    }

    async takeOrder(idCampaign: number, Structure: Structure, basket_name: string) {
        try {
            let baskets = [];
            let newlistBaskets = new Selection<Basket>([]);
            this.all.map((basket: Basket) => {
                    if (basket.selected) {
                        baskets.push(basket.id);
                    } else {
                        newlistBaskets.push(basket);
                    }
                }
            );
            this.all = newlistBaskets.all;

            let data = {
                id_structure: Structure.id,
                structure_name: Structure.name,
                baskets: baskets,
                basket_name: basket_name
            };

            return await http.post(`/crre/baskets/to/orders/${idCampaign}`, data);
        } catch (e) {
            toasts.warning('crre.order.create.err');
        }
    }
}

export class BasketOrder implements Selectable {
    id?: number;
    name: string;
    id_structure: string;
    id_campaign: number;
    name_user: string;
    id_user: string;
    total: number;
    amount: number;
    created: string| Date;
    selected: boolean;

    constructor (id_campaign?: number, id_structure?: string, id_user?: string) {
        this.id_campaign = id_campaign;
        this.id_structure = id_structure;
        this.id_user = id_user;
        this.amount = 1;
    }
    setIdBasket (id_basket: number) {
        this.id = id_basket;
    }
    toJson () {
        return {
            id: this.id,
            name: this.name,
            id_structure: this.id_structure,
            id_campaign: this.id_campaign,
            name_user: this.name_user,
            id_user: this.id_user,
            total: this.total,
            amount: this.amount,
            created: this.created,
            selected: this.selected
        };
    }

    async updateAllAmount():Promise<void>{
        try {
            let {data} = await http.get(`/crre/basketOrder/${this.id}/amount`);
            this.amount = data.amount;
        }
        catch {
            toasts.warning('crre.order.getMine.err');
        }
    }

}

export class BasketsOrders extends Selection<BasketOrder> {
    constructor() {
        super([]);
    }

    async sync (idCampaign: number) {
        try {
            let { data } = await http.get(`/crre/basketOrder/${idCampaign}`);
            this.all = Mix.castArrayAs(BasketOrder, data);
        } catch (e) {
            toasts.warning('crre.basket.sync.err');
        }
    }

    async search(text: String, id_campaign: number) {
        try {
            if ((text.trim() === '' || !text)) return;
            const {data} = await http.get(`/crre/basketOrder/search?q=${text}&id=${id_campaign}`);
            this.all = Mix.castArrayAs(BasketOrder, data);
        } catch (err) {
            toasts.warning('crre.basket.sync.err');
            throw err;
        }
    }


    async filter_order(filters: Filter[], id_campaign: number, word?: string){
        try {
            let format = /^[`@#$%^&*()_+\-=\[\]{};:"\\|,.<>\/?~]/;
            let params = "";
            filters.forEach(function (f, index) {
                params += f.name + "=" + f.value;
                if(index != filters.length - 1) {
                    params += "&";
                }});
            let url;
            if(!format.test(word)) {
                if(word) {
                    url = `/crre/basketOrder/filter?q=${word}&id=${id_campaign}&${params}`;
                } else {
                    url = `/crre/basketOrder/filter?id=${id_campaign}&${params}`;
                }
                let {data} = await http.get(url);
                this.all = Mix.castArrayAs(BasketOrder, data);
            } else {
                toasts.warning('crre.equipment.special');
            }
        } catch (e) {
            toasts.warning('crre.equipment.sync.err');
            throw e;
        }
    }


    async getMyOrders () {
        try {
            let { data } = await http.get(`/crre/basketOrder/allMyOrders`);
            this.all = Mix.castArrayAs(BasketOrder, data);
        }
        catch {
            toasts.warning('crre.order.getMine.err');
        }
    }

    async getStructureHistory () {
        try {
            let { data } = await http.get(`/crre/basketOrder/history`);
            this.all = Mix.castArrayAs(BasketOrder, data);
        }
        catch {
            toasts.warning('crre.order.getMine.err');
        }
    }

    // async getAllBasketOrders (idCampaign: number) {
    //     try {
    //         let {data} = await http.get(`/crre/basket/${idCampaign}`);
    //         this.all = Mix.castArrayAs(Basket, data);
    //         this.all.map((basket) => {
    //             basket.equipment = Mix.castAs(Equipment, JSON.parse(basket.equipment.toString())[0]);
    //             basket.options = basket.options.toString() !== '[null]' && basket.options !== null
    //                 ? Mix.castArrayAs(EquipmentOption, JSON.parse(basket.options.toString()))
    //                 : [];
    //             basket.equipment.options = basket.options;
    //             basket.files = (basket.files.toString !== '[null]') ? Utils.parsePostgreSQLJson(basket.files) : [];
    //             basket.equipment.options.map((option) => option.selected = true);
    //         });
    //     }
    //     catch (e) {
    //         toasts.warning('crre.order.getAll.err');
    //     }
    // }
    //
    // async getOrderById(idOrder: number) {
    //     try {
    //         return await http.get(`/crre/basket/${idOrder}`);
    //     }
    //     catch {
    //         toasts.warning('crre.order.getOne.err');
    //     }
    // }
}