import {Structure} from './index';
import {Mix, Selectable, Selection} from 'entcore-toolkit';
import {_, toasts} from 'entcore';
import http from 'axios';
import {Structures} from './Structure';

export class StructureGroup implements Selectable {
    id?: number;
    name: string;
    description: string;
    structures: Structure [];
    selected: boolean;

    constructor (name?: string, description?: string) {
        if (name) this.name = name;
        if (description) this.description = description;
        this.structures = [];
    }

    toJson() {
        return {
            id: this.id,
            name: this.name,
            description: this.description,
            structures : ( typeof(this.structures[0]) === 'string' ) ? this.structures : this.structures.map((structure) => structure.id) ,
        };
    }

    async save () {
        if (this.id) {
            await this.update();
        } else {
            await this.create();
        }
    }

    async create () {
        try {
            await http.post(`/crre/structure/group`, this.toJson());
        } catch (e) {
            toasts.warning('crre.structureGroup.create.err');
        }
    }

    async update () {
        try {
            await http.put(`/crre/structure/group/${this.id}`, this.toJson());

        } catch (e) {
            toasts.warning('crre.structureGroup.update.err');
        }
    }

    async delete () {
        try {
            let id = `id=${this.id}`;
            await http.delete(`/crre/structure/group?${id}`);
        } catch (e) {
            toasts.warning('crre.structureGroup.delete.err');
        }
    }

    structureIdToObject(idsStructure: String[], structures: Structures) {
        this.structures = [];
        idsStructure.forEach((idStructure) => {
            let structure: Structure;
            structure = _.findWhere(structures.all, {id: idStructure});
            if (structure !== undefined ) {
                this.structures.push(structure);
            }
        });
    }
}

export class StructureGroupImporter {
    files: File[];
    message: string;

    constructor() {
        this.files = [];
    }

    isValid(): boolean {
        return this.files.length > 0
            ? this.files[0].name.endsWith('.csv') && this.files[0].name.trim() !== ''
            : false;
    }

    async validate(): Promise<any> {
        try {
            await this.postFile();
        } catch (err) {
            throw err;
        }
    }

    private async postFile(): Promise<any> {
        let formData = new FormData();
        formData.append('file', this.files[0], this.files[0].name);
        let response;
        try {
            response = await http.post(`/crre/structure/group/import`,
                formData, {'headers': {'Content-Type': 'multipart/form-data'}});
        } catch (err) {
            throw err.response.data;
        }
        return response;
    }
}

export class StructureGroups extends Selection<StructureGroup> {

    constructor () {
        super([]);
    }

    async syncByCampaign(idCampaign: number) {
        let {data} = await http.get(`/crre/structure/groups/campaign/${idCampaign}`);
        this.all = Mix.castArrayAs(StructureGroup, data);
        this.all.map((structureGroup) => {
            structureGroup.structures = JSON.parse(structureGroup.structures.toString());
        });
    }
    async delete (structureGroups: StructureGroup[]): Promise<void> {
        try {
            let filter = '';
            structureGroups.map((structureGroup) => filter += `ìd=${structureGroup.id}&`);
            filter = filter.slice(0, -1);
            await http.delete(`/crre/structure/group?${filter}`);
        } catch (e) {
            toasts.warning('crre.structureGroup.sync.err');
        }
    }

    async sync() {
        let {data} = await http.get(`/crre/structure/groups`);
        this.all = Mix.castArrayAs(StructureGroup, data);
        this.all.map((structureGroup) => {
            structureGroup.structures = JSON.parse(structureGroup.structures.toString());
        });
    }
}