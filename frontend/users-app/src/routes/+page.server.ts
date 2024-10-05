import type { PageServerLoad } from "./$types";
import type { ProductView } from "$lib/data";

export const load: PageServerLoad = async ({ fetch }) => {


    // const products: ProductView[] = await fetch(`${PRODUCT_SERVICE_URL}/products/search/laptop`, {
    //     method: 'GET',
    //     headers: {
    //         'Accept': 'application/json'
    //     }
    // }).then(resp => resp.json())
    // .catch(err => console.error(err))

    return {
        
    }
};