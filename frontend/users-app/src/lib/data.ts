export interface ProductView {
    productId: string,
    productName: string,
    price: number,
    image: string
}

export abstract class ProductQuery {
    minPrice: number | undefined;
    maxPrice: number | undefined;

    abstract category(): string;

    toUrlQuery(): string {
        return Object.entries(this)
        .filter(e => e[1])
        .map(e => {
            let value = Array.isArray(e[1]) ? e[1].join(',') : String(e[1])
            return e[0] + '=' + value
        })
        .join('&')   
    }
}

export class LaptopsQuery extends ProductQuery {
    ram: number | undefined
    category(){
        return "laptop"
    }
}